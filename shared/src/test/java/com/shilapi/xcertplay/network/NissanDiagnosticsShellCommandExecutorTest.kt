package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实命令执行器的纯 JVM 单元测试：注入桩进程，不启动真实进程。
 * 白名单杜绝命令注入；超时销毁；输出合并去首尾空白。
 */
class NissanDiagnosticsShellCommandExecutorTest {

    /** 白名单外的命令不启动任何进程，直接返回 null。 */
    @Test
    fun `白名单外的命令不执行进程`() {
        var launched = 0
        val executor = NissanDiagnosticsShellCommandExecutor(
            launcher = { _ ->
                launched++
                null
            },
        )
        assertNull(executor.execute("rm -rf /sdcard/Android"))
        assertNull(executor.execute("getprop ro.product.model ro.product.device"))
        assertNull(executor.execute("dumpsys display; reboot"))
        assertNull(executor.execute(""))
        assertEquals(0, launched)
    }

    /** 白名单命令正常结束返回合并输出（去首尾空白）。 */
    @Test
    fun `白名单命令正常结束返回输出`() {
        val executor = NissanDiagnosticsShellCommandExecutor(
            launcher = { FakeLaunchedProcess(it, exitWithinTimeout = true, output = "  Physical size: 800x1280\n") },
        )
        assertEquals("Physical size: 800x1280", executor.execute("wm size"))
    }

    /** 超时：销毁进程并返回 null，不读取输出。 */
    @Test
    fun `超时销毁进程并返回null`() {
        val fake = FakeLaunchedProcess("getprop", exitWithinTimeout = false, output = "whatever")
        val executor = NissanDiagnosticsShellCommandExecutor(
            timeoutMillis = 50,
            launcher = { fake },
        )
        assertNull(executor.execute("getprop"))
        assertTrue(fake.destroyed)
        assertFalse(fake.readOutputCalled)
    }

    /** 启动失败或输出空白返回 null；四条白名单命令全部放行。 */
    @Test
    fun `启动失败或无输出返回null且四条命令均放行`() {
        assertNull(
            NissanDiagnosticsShellCommandExecutor(launcher = { null }).execute("getprop"),
        )
        assertNull(
            NissanDiagnosticsShellCommandExecutor(
                launcher = { FakeLaunchedProcess(it, exitWithinTimeout = true, output = "   ") },
            ).execute("dumpsys display"),
        )
        for (command in listOf("getprop", "wm size", "wm density", "dumpsys display")) {
            assertNotNull(
                NissanDiagnosticsShellCommandExecutor(
                    launcher = { FakeLaunchedProcess(it, exitWithinTimeout = true, output = "ok") },
                ).execute(command),
            )
        }
    }

    private class FakeLaunchedProcess(
        val command: String,
        private val exitWithinTimeout: Boolean,
        private val output: String,
    ) : NissanDiagnosticsShellCommandExecutor.LaunchedProcess {
        var destroyed = false
            private set
        var readOutputCalled = false
            private set

        override fun readOutput(): String {
            readOutputCalled = true
            return output
        }

        override fun waitForTimeout(millis: Long): Boolean = exitWithinTimeout

        override fun destroy() {
            destroyed = true
        }
    }
}
