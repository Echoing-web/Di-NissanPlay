package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 调用方侧转录组装与失败信号检测的纯 JVM 单元测试：不依赖 Android framework，
 * 不执行命令，不做任何 I/O。组件只做纯文本变换，识别与采集职责都在调用方。
 */
class NissanDiagnosticsTranscriptTest {

    /** 四段齐全时按接线规则组装带分区标签的转录。 */
    @Test
    fun `组装带分区标签的完整转录`() {
        val transcript = NissanDiagnosticsTranscript.assemble(
            getprop = "[ro.product.model]: [NTDA3_BOARD_S02]\n[ro.product.brand]: [nissan]",
            wmSize = "Physical size: 800x1280\nOverride size: 720x1152",
            wmDensity = "Physical density: 320",
            dumpsysDisplay = "DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0",
        )
        assertEquals(
            "[ro.product.model]: [NTDA3_BOARD_S02]\n[ro.product.brand]: [nissan]\n" +
                "wm size:\nPhysical size: 800x1280\nOverride size: 720x1152\n" +
                "wm density:\nPhysical density: 320\n" +
                "dumpsys display:\nDISPLAY MANAGER (dumpsys display)\n  mDisplayId=0",
            transcript,
        )
    }

    /** 空白段落整体跳过，不产生空分区；全部空白时返回空转录。 */
    @Test
    fun `空白段落被跳过不产生空分区`() {
        assertEquals(
            "wm size:\nPhysical size: 800x1280",
            NissanDiagnosticsTranscript.assemble(
                getprop = null,
                wmSize = "Physical size: 800x1280",
                wmDensity = "  ",
                dumpsysDisplay = null,
            ),
        )
        assertEquals(
            "",
            NissanDiagnosticsTranscript.assemble(
                getprop = "",
                wmSize = "   ",
                wmDensity = null,
                dumpsysDisplay = "\t",
            ),
        )
    }

    /** 组装结果可被解析器完整还原：字段与源段落一致（调用方↔解析器契约）。 */
    @Test
    fun `组装结果可被解析器完整还原`() {
        val transcript = NissanDiagnosticsTranscript.assemble(
            getprop = "[ro.product.model]: [roundtrip-model]\n[ro.build.version.sdk]: [29]",
            wmSize = "Physical size: 800x1280",
            wmDensity = "Physical density: 320",
            dumpsysDisplay = "DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0",
        )
        val input = NissanDiagnosticsTextParser.parse(
            transcript,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("roundtrip-model", input.model)
        assertEquals("29", input.buildVersionSdk)
        assertEquals("Physical size: 800x1280", input.wmSize)
        assertEquals("Physical density: 320", input.wmDensity)
        assertEquals("DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0", input.dumpsysDisplay)
    }

    /** 检测采集失败信号：返回首个信号行，无信号返回 null。 */
    @Test
    fun `检测采集失败信号行`() {
        assertEquals(
            "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
            NissanDiagnosticsTranscript.captureFailureSignal(
                "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
            ),
        )
        assertEquals(
            "Permission Denial: can't dump DisplayManagerService from pid=1234",
            NissanDiagnosticsTranscript.captureFailureSignal(
                "DISPLAY MANAGER (dumpsys display)\nPermission Denial: can't dump DisplayManagerService from pid=1234",
            ),
        )
        assertEquals(
            "Error: bad size 720x1280",
            NissanDiagnosticsTranscript.captureFailureSignal("Error: bad size 720x1280"),
        )
        assertEquals(
            "Remote exception: binder failed",
            NissanDiagnosticsTranscript.captureFailureSignal("Remote exception: binder failed"),
        )
        assertEquals(
            "Can't find service: display",
            NissanDiagnosticsTranscript.captureFailureSignal("Can't find service: display"),
        )
        assertNull(NissanDiagnosticsTranscript.captureFailureSignal("Physical density: 320"))
        assertNull(
            NissanDiagnosticsTranscript.captureFailureSignal(
                "DISPLAY MANAGER (dumpsys display)\n  mDisplayId=0",
            ),
        )
    }

    /** 失败信号行组装后仍保留在 dumpsys 采集区，解析器原样采集为证据。 */
    @Test
    fun `失败信号行组装后保留为证据`() {
        val denial = "Permission Denial: can't dump DisplayManagerService from pid=1234"
        assertEquals(denial, NissanDiagnosticsTranscript.captureFailureSignal(denial))

        val transcript = NissanDiagnosticsTranscript.assemble(
            getprop = null,
            wmSize = null,
            wmDensity = null,
            dumpsysDisplay = denial,
        )
        val input = NissanDiagnosticsTextParser.parse(
            transcript,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals(denial, input.dumpsysDisplay)
        assertNull(input.wmSize)
    }
}
