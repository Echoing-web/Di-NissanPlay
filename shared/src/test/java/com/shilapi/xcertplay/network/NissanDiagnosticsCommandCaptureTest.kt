package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 命令执行抽象层的纯 JVM 单元测试：注入桩执行器，不依赖实车或模拟器环境。
 * 命令常量与采集行为经 AOSP 调研验证（全量 getprop；多参数形式不可用）。
 */
class NissanDiagnosticsCommandCaptureTest {

    /** 四条采集命令按调研验证的形式与固定顺序执行。 */
    @Test
    fun `四条采集命令按固定顺序执行`() {
        val executed = mutableListOf<String>()
        val output = NissanDiagnosticsCommandCapture { command ->
            executed.add(command)
            null
        }.run()
        assertNull(output.getprop)
        assertNull(output.wmSize)
        assertNull(output.wmDensity)
        assertNull(output.dumpsysDisplay)
        assertEquals(
            listOf("getprop", "wm size", "wm density", "dumpsys display"),
            executed,
        )
    }

    /** 各段输出与执行器返回一一对应，失败或未执行段保持 null。 */
    @Test
    fun `各段输出与执行器返回一一对应`() {
        val outputs = mapOf(
            "getprop" to "[ro.product.device]: [NTDA3_P330]",
            "wm size" to "Physical size: 800x1280",
            "dumpsys display" to "DISPLAY MANAGER (dumpsys display)",
        )
        val output = NissanDiagnosticsCommandCapture { command -> outputs[command] }.run()
        assertEquals("[ro.product.device]: [NTDA3_P330]", output.getprop)
        assertEquals("Physical size: 800x1280", output.wmSize)
        assertNull(output.wmDensity)
        assertEquals("DISPLAY MANAGER (dumpsys display)", output.dumpsysDisplay)
    }

    /** 单条命令失败不阻塞其余采集。 */
    @Test
    fun `单条命令失败不阻塞其余采集`() {
        val output = NissanDiagnosticsCommandCapture { command ->
            if (command == "wm size") null else "text-for-$command"
        }.run()
        assertNull(output.wmSize)
        assertEquals("text-for-getprop", output.getprop)
        assertEquals("text-for-wm density", output.wmDensity)
        assertEquals("text-for-dumpsys display", output.dumpsysDisplay)
    }

    /** 全链路：干净输出 → Reported；含失败信号输出 → CaptureFailed。 */
    @Test
    fun `执行器输出直接进入编排分流`() {
        val clean = NissanDiagnosticsCommandCapture { command ->
            when (command) {
                "getprop" -> "[ro.product.device]: [NTDA3_P330]\n[ro.build.version.sdk]: [29]"
                "wm size" -> "Physical size: 800x1280"
                "wm density" -> "Physical density: 320"
                else -> "DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0"
            }
        }.run()
        val reported = NissanDiagnosticsCapture.from(
            clean.getprop,
            clean.wmSize,
            clean.wmDensity,
            clean.dumpsysDisplay,
            NissanDiagnosticsSource.ON_CAR,
            true,
        ) as NissanDiagnosticsCapture.Outcome.Reported
        assertTrue(reported.report.render().contains("ro.product.device=NTDA3_P330"))
        assertTrue(reported.report.render().contains("wm.size=Physical size: 800x1280"))

        val failing = NissanDiagnosticsCommandCapture { command ->
            if (command == "wm size") {
                "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS"
            } else {
                null
            }
        }.run()
        val failed = NissanDiagnosticsCapture.from(
            failing.getprop,
            failing.wmSize,
            failing.wmDensity,
            failing.dumpsysDisplay,
            NissanDiagnosticsSource.ON_CAR,
            true,
        ) as NissanDiagnosticsCapture.Outcome.CaptureFailed
        assertEquals(
            listOf("Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS"),
            failed.signals,
        )
    }
}
