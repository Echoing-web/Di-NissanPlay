package com.shilapi.xcertplay.network

import android.os.Build
import kotlin.concurrent.thread

/**
 * 真实命令执行器：白名单 + 超时 + 输出合并，实现 [NissanDiagnosticsCommandExecutor]。
 *
 * 安全与健壮性约定：
 *  - 白名单：只执行 [NissanDiagnosticsCommands] 的四条只读采集命令，其余任何字符串
 *    一律返回 null 且不启动进程（杜绝命令注入）。
 *  - 超时：进程在时限内未结束则销毁（destroy 后仍存活则 destroyForcibly）并返回
 *    null，不读取输出；默认时限 10 秒。
 *  - 输出：stdout 与 stderr 由后台线程并发读取合并（避免管道写满导致死锁），
 *    去首尾空白；启动失败、超时或输出空白一律返回 null。
 *  - 失败不抛出：单条命令失败返回 null，不阻塞其余采集（由 CommandCapture 组合）。
 *
 * [ProcessLauncher] / [LaunchedProcess] 是可注入的进程抽象：真实实现用
 * `sh -c <白名单命令>`（白名单保证安全）；纯 JVM 测试注入桩，不启动真实进程。
 */
class NissanDiagnosticsShellCommandExecutor(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val launcher: ProcessLauncher = ProcessLauncher(::launchRuntimeProcess),
) : NissanDiagnosticsCommandExecutor {

    /** 进程启动抽象，纯 JVM 测试注入桩。 */
    fun interface ProcessLauncher {
        fun launch(command: String): LaunchedProcess?
    }

    /** 已启动进程的可观测操作；真实实现包装 [java.lang.Process]。 */
    interface LaunchedProcess {
        /** 返回合并输出（实现需在等待退出期间并发读取流以防死锁）。 */
        fun readOutput(): String

        /** 等待进程退出，超时时限返回 false。 */
        fun waitForTimeout(millis: Long): Boolean

        fun destroy()
    }

    override fun execute(command: String): String? {
        if (command !in ALLOWED_COMMANDS) return null
        val process = try {
            launcher.launch(command) ?: return null
        } catch (_: Exception) {
            return null
        }
        return try {
            if (!process.waitForTimeout(timeoutMillis)) return null
            process.readOutput().trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            process.destroy()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS: Long = 10_000L

        private val ALLOWED_COMMANDS = setOf(
            NissanDiagnosticsCommands.GETPROP,
            NissanDiagnosticsCommands.WM_SIZE,
            NissanDiagnosticsCommands.WM_DENSITY,
            NissanDiagnosticsCommands.DUMPSYS_DISPLAY,
        )
    }
}

private const val POLL_MILLIS = 10L

private fun launchRuntimeProcess(command: String): NissanDiagnosticsShellCommandExecutor.LaunchedProcess? =
    runCatching { RuntimeProcess(ProcessBuilder("sh", "-c", command).start()) }.getOrNull()

/** 包装 java.lang.Process：后台线程并发读取 stdout/stderr，退出后合并。 */
private class RuntimeProcess(private val process: Process) : NissanDiagnosticsShellCommandExecutor.LaunchedProcess {

    private val stdout = StringBuilder()
    private val stderr = StringBuilder()
    private val readers = listOf(
        thread(isDaemon = true, name = "nissan-diag-stdout") {
            process.inputStream.bufferedReader().forEachLine { stdout.appendLine(it) }
        },
        thread(isDaemon = true, name = "nissan-diag-stderr") {
            process.errorStream.bufferedReader().forEachLine { stderr.appendLine(it) }
        },
    )

    override fun readOutput(): String {
        readers.forEach { it.join(1_000) }
        return stdout.toString() + stderr.toString()
    }

    /** API 24 兼容：`Process.waitFor(long, TimeUnit)` 与 `isAlive` 均为 API 26+，用 `exitValue()` 轮询替代。 */
    override fun waitForTimeout(millis: Long): Boolean {
        val deadline = System.nanoTime() + millis * 1_000_000L
        while (System.nanoTime() < deadline) {
            if (hasExited()) return true
            try {
                Thread.sleep(POLL_MILLIS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return hasExited()
            }
        }
        return hasExited()
    }

    private fun hasExited(): Boolean = try {
        process.exitValue()
        true
    } catch (_: IllegalThreadStateException) {
        false
    }

    override fun destroy() {
        process.destroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && process.isAlive) process.destroyForcibly()
    }
}
