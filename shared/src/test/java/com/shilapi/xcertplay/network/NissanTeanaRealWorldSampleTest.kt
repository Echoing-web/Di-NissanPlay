package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真机拆解实测样本模拟测试（2026-10-26 轮次 30）。
 *
 * 样本来源（RESEARCH「真机拆解样本数据集」节，均附来源）：
 *  - 屏幕：9 英寸 1280x720 电容屏（知乎/腾讯新闻 日产车机拆解，歌乐制造）；
 *  - 主控：NXP i.MX6 SABRESD-MX6DQ（DiPlay-Legacy-Android 实车日志 + T32
 *    CN202SR 同板），NTDA3 为屏驱/解码芯片代号而非主控；
 *  - 平台双世代：Android 4.4.3（i.MX6 实车日志）与 Android 9 ARM64（同代
 *    第二平台社区实测）。
 *
 * 红线说明：这些值只作为**测试样本**钉住解析器对真实形态的行为；运行时代码
 * 不回填任何默认值（SDD 非目标不变）。
 */
class NissanTeanaRealWorldSampleTest {

    @Test
    fun teanaAndroid44SampleTranscriptParsesEndToEnd() {
        val transcript = """
            [ro.product.board]: [SABRESD-MX6DQ]
            [ro.product.device]: [sabresd]
            [ro.product.model]: [Teana Head Unit]
            [ro.product.manufacturer]: [NISSAN]
            [ro.product.brand]: [nissan]
            [ro.build.version.release]: [4.4.3]
            [ro.build.version.sdk]: [19]
            [ro.hardware]: [sabresd]
            [dalvik.vm.heapsize]: [256m]
            wm size:
            Physical size: 1280x720
            wm density:
            Physical density: 160
            dumpsys display:
            DISPLAY MANAGER (dumpsys display)
              mGlobalDisplayState=ON
        """.trimIndent()

        val input = NissanDiagnosticsTextParser.parse(transcript, NissanDiagnosticsSource.ON_CAR, detected = true)
        val report = NissanDiagnosticsReport.from(input)

        assertNotNull("real-world 4.4.3 sample must produce a report", report)
        val text = report!!.render()
        assertTrue(text.contains("ro.product.manufacturer=NISSAN"))
        assertTrue(text.contains("ro.build.version.release=4.4.3"))
        assertTrue("actual report: $text", text.contains("wm.size=Physical size: 1280x720"))
        assertTrue("actual report: $text", text.contains("wm.density=Physical density: 160"))
        assertTrue("actual report: $text", text.contains("dumpsys.display=DISPLAY MANAGER"))
        assertEquals(15, text.split('\n').size)
    }

    @Test
    fun teanaAndroid9SecondPlatformSampleParsesEndToEnd() {
        val transcript = """
            [ro.product.device]: [arm64_teana]
            [ro.product.model]: [Teana Smart Connect]
            [ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
            [ro.product.brand]: [nissan]
            [ro.build.version.release]: [9]
            [ro.build.version.sdk]: [28]
            wm size:
            Physical size: 1280x720
            Override size: 1920x1080
            wm density:
            Physical density: 213
        """.trimIndent()

        val input = NissanDiagnosticsTextParser.parse(transcript, NissanDiagnosticsSource.ON_CAR, detected = true)
        val report = NissanDiagnosticsReport.from(input)!!

        val text = report.render()
        assertTrue(text.contains("ro.build.version.release=9"))
        // wm 分区按拆解样本保留 Physical 与 Override 两行、维持原序（多行以字面 \n 转义）。
        assertTrue("actual report: $text", text.contains("wm.size=Physical size: 1280x720\\nOverride size: 1920x1080"))
        assertTrue(text.contains("wm.density=Physical density: 213"))
        // 未出现 dumpsys 标记：缺失字段渲染 unknown，绝不猜测。
        assertTrue(text.contains("dumpsys.display=unknown"))
    }

    @Test
    fun sampleWithMcuBluetoothOnlyStillReportsSystemFields() {
        // 拆解结论：蓝牙在 MCU 后、系统蓝牙不可用——诊断报告仍应完整呈现系统侧
        // 字段（本测试钉住“诊断不依赖蓝牙状态”）。
        val transcript = """
            [ro.product.manufacturer]: [NISSAN]
            wm size:
            Physical size: 1280x720
        """.trimIndent()

        val input = NissanDiagnosticsTextParser.parse(transcript, NissanDiagnosticsSource.ON_CAR, detected = true)
        val report = NissanDiagnosticsReport.from(input)!!

        val text = report.render()
        assertTrue(text.contains("ro.product.manufacturer=NISSAN"))
        assertTrue(text.contains("wm.size=Physical size: 1280x720"))
        assertTrue(text.contains("ro.product.model=unknown"))
    }
}
