package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 采集编排的纯 JVM 单元测试：NissanDiagnosticsCapture 组合转录组装、失败信号检测、
 * 文本解析与报告生成；不执行命令、不做 I/O、不推断 detected。
 */
class NissanDiagnosticsCaptureTest {

    /** 四段干净采集端到端生成报告。 */
    @Test
    fun `四段干净采集生成报告`() {
        val outcome = NissanDiagnosticsCapture.from(
            getprop = "[ro.product.device]: [NTDA3_P330]\n" +
                "[ro.product.model]: [NTDA3_BOARD_S02]\n" +
                "[ro.build.version.sdk]: [29]",
            wmSize = "Physical size: 800x1280",
            wmDensity = "Physical density: 320",
            dumpsysDisplay = "DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        val render = (outcome as NissanDiagnosticsCapture.Outcome.Reported).report.render()
        assertTrue(render.contains("kind=nissan-headunit-diagnostics"))
        assertTrue(render.contains("source=on-car"))
        assertTrue(render.contains("detected=true"))
        assertTrue(render.contains("ro.product.device=NTDA3_P330"))
        assertTrue(render.contains("ro.product.model=NTDA3_BOARD_S02"))
        assertTrue(render.contains("ro.build.version.sdk=29"))
        assertTrue(render.contains("wm.size=Physical size: 800x1280"))
        assertTrue(render.contains("wm.density=Physical density: 320"))
        assertTrue(render.contains("dumpsys.display=DISPLAY MANAGER (dumpsys display)\\n  mDisplayId=0"))
    }

    /** 任一段失败信号则要求重采且不生成报告，多段失败按段序收集；拒绝优先于重采。 */
    @Test
    fun `失败信号要求重采且不生成报告`() {
        val failed = NissanDiagnosticsCapture.from(
            getprop = "[ro.product.device]: [NTDA3_P330]",
            wmSize = "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
            wmDensity = null,
            dumpsysDisplay = "Permission Denial: can't dump DisplayManagerService from pid=1234",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        ) as NissanDiagnosticsCapture.Outcome.CaptureFailed
        assertEquals(
            listOf(
                "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
                "Permission Denial: can't dump DisplayManagerService from pid=1234",
            ),
            failed.signals,
        )

        // detected=false 优先于重采提示：未识别车机不做任何生成或重采引导。
        val undetected = NissanDiagnosticsCapture.from(
            getprop = null,
            wmSize = "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
            wmDensity = null,
            dumpsysDisplay = null,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = false,
        )
        assertTrue(undetected is NissanDiagnosticsCapture.Outcome.Undetected)
    }

    /** detected=false 时即使采集干净也拒绝生成。 */
    @Test
    fun `detected为false时拒绝生成`() {
        val outcome = NissanDiagnosticsCapture.from(
            getprop = "[ro.product.device]: [NTDA3_P330]",
            wmSize = "Physical size: 800x1280",
            wmDensity = "Physical density: 320",
            dumpsysDisplay = "DISPLAY MANAGER (dumpsys display)",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = false,
        )
        assertTrue(outcome is NissanDiagnosticsCapture.Outcome.Undetected)
    }

    /** 全部段落空白：无信号；detected=true 生成全 unknown 报告，false 拒绝。 */
    @Test
    fun `全部段落空白时生成全unknown报告或拒绝`() {
        val reported = NissanDiagnosticsCapture.from(
            getprop = null,
            wmSize = "  ",
            wmDensity = null,
            dumpsysDisplay = "",
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            detected = true,
        ) as NissanDiagnosticsCapture.Outcome.Reported
        val render = reported.report.render()
        assertTrue(render.contains("source=owner-reported"))
        assertTrue(render.contains("ro.product.product=unknown"))
        assertTrue(render.contains("wm.size=unknown"))
        assertTrue(render.contains("dumpsys.display=unknown"))

        val rejected = NissanDiagnosticsCapture.from(
            getprop = null,
            wmSize = null,
            wmDensity = null,
            dumpsysDisplay = null,
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            detected = false,
        )
        assertTrue(rejected is NissanDiagnosticsCapture.Outcome.Undetected)
    }
}
