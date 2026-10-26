package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 只读诊断报告的纯 JVM 单元测试：不依赖 Android framework，不连接车机。
 * 报告不承担识别职责——detected 由实车采集方给出，这里只验证数据被如实记录与渲染。
 */
class NissanDiagnosticsReportTest {

    /** 完整输入：字段按固定顺序渲染，来源与 platformHint 原样保留。 */
    @Test
    fun `完整输入按固定顺序逐行渲染`() {
        val report = NissanDiagnosticsReport.from(
            NissanDiagnosticsInput(
                detected = true,
                source = NissanDiagnosticsSource.ON_CAR,
                product = "unit-product",
                device = "unit-device",
                model = "unit-model",
                manufacturer = "NISSAN MOTOR CO., LTD.",
                brand = "nissan",
                buildVersionRelease = "10",
                buildVersionSdk = "29",
                wmSize = "Physical size: 111x222",
                wmDensity = "Physical density: 123",
                dumpsysDisplay = "Display unit-display state=ON",
                platformHint = NissanPlatformHint(
                    "P999（虚构示例，未验证）",
                    NissanDiagnosticsSource.OWNER_REPORTED,
                ),
            ),
        )!!

        assertEquals(
            """
            kind=nissan-headunit-diagnostics
            source=on-car
            detected=true
            ro.product.product=unit-product
            ro.product.device=unit-device
            ro.product.model=unit-model
            ro.product.manufacturer=NISSAN MOTOR CO., LTD.
            ro.product.brand=nissan
            ro.build.version.release=10
            ro.build.version.sdk=29
            wm.size=Physical size: 111x222
            wm.density=Physical density: 123
            dumpsys.display=Display unit-display state=ON
            platform_hint=P999（虚构示例，未验证）
            platform_hint_source=owner-reported
            """.trimIndent(),
            report.render(),
        )
    }

    /** 缺失字段与空白字段等价：统一输出 unknown，绝不回填任何传闻默认值。 */
    @Test
    fun `缺失或空白字段统一输出unknown且不出现传闻默认值`() {
        val allMissing = NissanDiagnosticsInput(
            detected = true,
            source = NissanDiagnosticsSource.OWNER_REPORTED,
        )
        val allBlank = NissanDiagnosticsInput(
            detected = true,
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            product = "   ",
            device = "",
            model = "\r\n",
            manufacturer = " ",
            brand = "",
            buildVersionRelease = "  ",
            buildVersionSdk = "",
            wmSize = " ",
            wmDensity = "",
            dumpsysDisplay = "\t",
            platformHint = NissanPlatformHint(" ", NissanDiagnosticsSource.OWNER_REPORTED),
        )
        val text = NissanDiagnosticsReport.from(allMissing)!!.render()
        assertEquals(text, NissanDiagnosticsReport.from(allBlank)!!.render())

        val lines = text.split('\n')
        for (key in listOf(
            "ro.product.product", "ro.product.device", "ro.product.model",
            "ro.product.manufacturer", "ro.product.brand",
            "ro.build.version.release", "ro.build.version.sdk",
            "wm.size", "wm.density", "dumpsys.display",
            "platform_hint", "platform_hint_source",
        )) {
            assertTrue("缺少 $key=unknown 行：$text", lines.contains("$key=unknown"))
        }
        assertFalse(text.contains("1280x720"))
        assertFalse(text.contains("NTDA3"))
        assertFalse(text.contains("P1"))
    }

    /** detected=false 一律拒绝生成，即使其余字段齐全。 */
    @Test
    fun `detected为false时拒绝生成报告`() {
        assertNull(
            NissanDiagnosticsReport.from(
                NissanDiagnosticsInput(
                    detected = false,
                    source = NissanDiagnosticsSource.ON_CAR,
                    product = "unit-product",
                    model = "unit-model",
                    manufacturer = "NISSAN MOTOR CO., LTD.",
                    wmSize = "Physical size: 111x222",
                ),
            ),
        )
    }

    /** platformHint 只是被原样标注的文本，不产生也不暗示任何识别结论。 */
    @Test
    fun `platformHint仅作原样标注不改变识别语义`() {
        val base = NissanDiagnosticsInput(
            detected = true,
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            model = "unit-model",
        )
        val withHint = NissanDiagnosticsReport.from(
            base.copy(platformHint = NissanPlatformHint("NTDA3", NissanDiagnosticsSource.OWNER_REPORTED)),
        )!!
        val withoutHint = NissanDiagnosticsReport.from(base)!!

        // 除 platform_hint 两行外逐行相同：hint 不影响任何其他字段的渲染。
        assertEquals(
            withoutHint.render().split('\n').filterNot { it.startsWith("platform_hint") },
            withHint.render().split('\n').filterNot { it.startsWith("platform_hint") },
        )
        assertTrue(withHint.render().contains("platform_hint=NTDA3"))
        assertTrue(withHint.render().contains("platform_hint_source=owner-reported"))
        assertFalse(withoutHint.render().contains("platform_hint=NTDA3"))
        // 报告不提供 matched/verified 之类的识别结论字段。
        assertFalse(withHint.render().contains("matched"))
        assertFalse(withHint.render().contains("verified"))
    }

    /** 报告来源与 hint 来源相互独立且各自保留。 */
    @Test
    fun `来源标记被原样保留`() {
        val onCar = NissanDiagnosticsReport.from(
            NissanDiagnosticsInput(
                detected = true,
                source = NissanDiagnosticsSource.ON_CAR,
                platformHint = NissanPlatformHint("P999", NissanDiagnosticsSource.ON_CAR),
            ),
        )!!
        assertTrue(onCar.render().contains("source=on-car"))
        assertTrue(onCar.render().contains("platform_hint_source=on-car"))

        val ownerReported = NissanDiagnosticsReport.from(
            NissanDiagnosticsInput(
                detected = true,
                source = NissanDiagnosticsSource.OWNER_REPORTED,
                platformHint = NissanPlatformHint("P999", NissanDiagnosticsSource.ON_CAR),
            ),
        )!!
        assertTrue(ownerReported.render().contains("source=owner-reported"))
        // 报告来自车主转述，hint 却是实车标注时，两者互不覆盖。
        assertTrue(ownerReported.render().contains("platform_hint_source=on-car"))
    }

    /**
     * 输入包含刷机/删除命令时：命令只是被记录的文本，报告为纯数据变换——
     * 多次生成与渲染结果一致，注入的换行不会伪造新的 key=value 行，不执行任何动作。
     */
    @Test
    fun `包含刷机或删除命令的输入只被记录为文本且无副作用`() {
        val hostileInput = NissanDiagnosticsInput(
            detected = true,
            source = NissanDiagnosticsSource.ON_CAR,
            product = "'; adb shell 'rm -rf /sdcard/Android'",
            wmSize = "Physical size: 111x222\nOverride size: 1x1\n\$(rm -rf /)",
            dumpsysDisplay = "echo pwned\nrm -rf /data\nflash_image recovery /sdcard/twrp.img\nreboot bootloader",
        )
        val first = NissanDiagnosticsReport.from(hostileInput)!!.render()

        // 纯函数：重复生成与渲染不产生任何差异。
        assertEquals(first, NissanDiagnosticsReport.from(hostileInput)!!.render())
        assertEquals(first, NissanDiagnosticsReport.from(hostileInput.copy())!!.render())

        // 命令文本原样保留为数据（内部换行被转义），不丢字也不改写。
        assertTrue(first.contains("ro.product.product='; adb shell 'rm -rf /sdcard/Android"))
        assertTrue(first.contains("dumpsys.display=echo pwned\\nrm -rf /data\\nflash_image recovery /sdcard/twrp.img\\nreboot bootloader"))
        assertTrue(first.contains("wm.size=Physical size: 111x222\\nOverride size: 1x1\\n\$(rm -rf /)"))

        // 行结构恒定：15 行、每行都是 key= 前缀，注入内容无法伪造新行。
        val lines = first.split('\n')
        assertEquals(15, lines.size)
        assertTrue(lines.all { Regex("^[a-z_.]+=").containsMatchIn(it) })
        assertFalse(lines.any { it.startsWith("rm ") || it.startsWith("echo ") || it.startsWith("reboot ") })
    }
}
