package com.shilapi.xcertplay.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 纯文本诊断记录解析的纯 JVM 单元测试：不依赖 Android framework，
 * 不连接车机，不执行命令，不做任何 I/O。
 * source 与 detected 由调用方提供，解析器只做文本到数据的变换，绝不推断识别结论。
 */
class NissanDiagnosticsTextParserTest {

    /** getprop 括号格式的七个属性被解析，source 与 detected 原样透传。 */
    @Test
    fun `getprop括号格式的属性被解析且来源与识别结论原样透传`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [ro.product.product]: [unit-product]
            [ro.product.device]: [unit-device]
            [ro.product.model]: [unit-model]
            [ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
            [ro.product.brand]: [nissan]
            [ro.build.version.release]: [10]
            [ro.build.version.sdk]: [29]
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )

        assertEquals(NissanDiagnosticsSource.ON_CAR, input.source)
        assertTrue(input.detected)
        assertEquals("unit-product", input.product)
        assertEquals("unit-device", input.device)
        assertEquals("unit-model", input.model)
        assertEquals("NISSAN MOTOR CO., LTD.", input.manufacturer)
        assertEquals("nissan", input.brand)
        assertEquals("10", input.buildVersionRelease)
        assertEquals("29", input.buildVersionSdk)
    }

    /** 裸 key=value 格式同样被解析；注入的命令文本只作数据保留，原样不变。 */
    @Test
    fun `裸key等号格式的属性同样被解析且命令文本只作数据`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            ro.product.model=SM-G960F
            ro.product.manufacturer=NISSAN MOTOR CO., LTD.
            ro.product.product='; adb shell 'rm -rf /sdcard/Android
            """.trimIndent(),
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            detected = true,
        )

        assertEquals("SM-G960F", input.model)
        assertEquals("NISSAN MOTOR CO., LTD.", input.manufacturer)
        assertEquals("'; adb shell 'rm -rf /sdcard/Android", input.product)
    }

    /** 缺失与空白的属性保持 null，由现有报告层统一输出 unknown。 */
    @Test
    fun `缺失或空白的属性保持null`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [ro.product.model]: []
            [ro.product.brand]: [   ]
            ro.build.version.release=
            [ro.build.version.sdk]: [29]
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )

        assertNull(input.product)
        assertNull(input.device)
        assertNull(input.model)
        assertNull(input.manufacturer)
        assertNull(input.brand)
        assertNull(input.buildVersionRelease)
        assertEquals("29", input.buildVersionSdk)
    }

    /** wm 分区只保留 Physical 与 Override 行，噪声行被丢弃，原始顺序不变，容忍 CRLF。 */
    @Test
    fun `wm分区只保留Physical与Override行且维持原始顺序`() {
        val text = """
            wm size:
            Physical size: 1280x720
            Security exception: not allowed to read window size
            Override size: 800x600

            wm density:
            Override density: 280
            Physical density: 320
            """.trimIndent().replace("\n", "\r\n")
        val input = NissanDiagnosticsTextParser.parse(
            text,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )

        assertEquals("Physical size: 1280x720\nOverride size: 800x600", input.wmSize)
        // Override 在前也按原始顺序保留。
        assertEquals("Override density: 280\nPhysical density: 320", input.wmDensity)
    }

    /** wm 分区只有 Override 行时只保留该行，不因缺少 Physical 而丢弃或补默认值。 */
    @Test
    fun `wm分区仅有Override行时只保留该行`() {
        val input = NissanDiagnosticsTextParser.parse(
            "wm size:\nOverride size: 800x600",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("Override size: 800x600", input.wmSize)
    }

    /** wm 分区没有任何 Physical 或 Override 行时保持 null。 */
    @Test
    fun `wm分区无Physical或Override行时保持null`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            wm size:
            Security exception: not allowed to read window size
            wm density:
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(input.wmSize)
        assertNull(input.wmDensity)
    }

    /** 没有显式 dumpsys display: 整行标记时不得采集，即使文本酷似 dumpsys 输出。 */
    @Test
    fun `无显式标记时不得采集dumpsys`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            DISPLAY MANAGER (dumpsys display)
              Display 0: name=Built-in screen
            dumpsys window follows
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(input.dumpsysDisplay)
    }

    /** 显式标记之后开始采集，直到下一个 wm 标记或输入结束为止。 */
    @Test
    fun `dumpsys在显式标记后采集并在wm标记或输入结束处停止`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            dumpsys display:
            DISPLAY MANAGER (dumpsys display)
              mGlobalDisplayState=ON

            wm size:
            Physical size: 1280x720
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals(
            "DISPLAY MANAGER (dumpsys display)\n  mGlobalDisplayState=ON",
            input.dumpsysDisplay,
        )
        assertEquals("Physical size: 1280x720", input.wmSize)

        val endsAtDensity = NissanDiagnosticsTextParser.parse(
            """
            dumpsys display:
            first line
            wm density:
            Physical density: 320
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("first line", endsAtDensity.dumpsysDisplay)
        assertEquals("Physical density: 320", endsAtDensity.wmDensity)
    }

    /** 解析器绝不推断识别：detected 只由调用方决定，满篇 Nissan 字样也不改写结论。 */
    @Test
    fun `detected由调用方提供解析器绝不推断`() {
        val text = """
            [ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
            [ro.product.brand]: [nissan]
            dumpsys display:
              Display 0: Nissan built-in screen
            """.trimIndent()
        val rejected = NissanDiagnosticsTextParser.parse(
            text,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = false,
        )
        assertFalse(rejected.detected)
        // 现有报告层对 detected=false 拒绝生成：解析结果同样被拒绝。
        assertNull(NissanDiagnosticsReport.from(rejected))

        val accepted = NissanDiagnosticsTextParser.parse(
            text,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertTrue(accepted.detected)
    }

    /** 纯文本变换：同样输入重复解析结果一致，platformHint 不由解析器产生。 */
    @Test
    fun `解析是纯文本变换且不产生platformHint`() {
        val text = """
            [ro.product.model]: [unit-model]
            wm size:
            Physical size: 1280x720
            dumpsys display:
              mGlobalDisplayState=ON
            """.trimIndent()
        val first = NissanDiagnosticsTextParser.parse(
            text,
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals(
            first,
            NissanDiagnosticsTextParser.parse(text, NissanDiagnosticsSource.ON_CAR, true),
        )
        assertNull(first.platformHint)
    }

    /** 标记与首行内容写在同一行时，剩余部分作为该分区首行处理。 */
    @Test
    fun `标记行内联的分区内容按首行处理`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            wm size: Physical size: 800x1280
            wm density: Physical density: 320
            dumpsys display: DISPLAY MANAGER (dumpsys display)
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("Physical size: 800x1280", input.wmSize)
        assertEquals("Physical density: 320", input.wmDensity)
        assertEquals("DISPLAY MANAGER (dumpsys display)", input.dumpsysDisplay)
    }

    /** 分区标记区分大小写：大写或混写标记不是标记，也不开始任何采集。 */
    @Test
    fun `分区标记区分大小写`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            WM SIZE:
            Physical size: 1280x720
            Dumpsys Display:
              mGlobalDisplayState=ON
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(input.wmSize)
        assertNull(input.dumpsysDisplay)
    }

    /** Physical 与 Override 行按分区匹配完整前缀：跨分区行与前缀撞车行都不采集。 */
    @Test
    fun `Physical与Override按分区匹配前缀`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            wm size:
            Physical size: 1280x720
            Physical density: 320
            Physicality: not a wm line
            OverrideRide: not a wm line
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("Physical size: 1280x720", input.wmSize)
        assertNull(input.wmDensity)

        // 出现在任何标记之前的 Physical 行同样不得采集。
        val beforeAnyMarker = NissanDiagnosticsTextParser.parse(
            "Physical size: 1280x720",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(beforeAnyMarker.wmSize)
    }

    /** 同一分区再次出现时跨块累计，仍按原始顺序。 */
    @Test
    fun `同一分区多次出现时跨块按原始顺序累计`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            wm size:
            Physical size: 1280x720
            wm density:
            Physical density: 320
            wm size:
            Override size: 800x600
            dumpsys display:
            first block
            wm density:
            dumpsys display:
            second block
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("Physical size: 1280x720\nOverride size: 800x600", input.wmSize)
        assertEquals("Physical density: 320", input.wmDensity)
        assertEquals("first block\nsecond block", input.dumpsysDisplay)
    }

    /** 属性行在任意分区都会被解析，重复键按后出现者覆盖，采集文本仍原样保留。 */
    @Test
    fun `属性行在任意分区被解析且重复键后值覆盖`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [ro.product.model]: [first-model]
            dumpsys display:
            ro.product.model=second-model
            Physical size: 1280x720
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("second-model", input.model)
        assertEquals("ro.product.model=second-model\nPhysical size: 1280x720", input.dumpsysDisplay)
        assertNull(input.wmSize)
    }

    /** 属性值保留等号与右括号；更长的键名不得越界匹配。 */
    @Test
    fun `属性值保留等号与右括号且键前缀不越界`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            ro.product.model=a=b
            [ro.product.device]: [x]y]
            ro.product.modelX=1
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("a=b", input.model)
        assertEquals("x]y", input.device)
    }

    /** 孤立回车（CR）也按行分隔处理。 */
    @Test
    fun `孤立回车也按行分隔处理`() {
        val input = NissanDiagnosticsTextParser.parse(
            "wm size:\rPhysical size: 800x1280\rwm density:\rPhysical density: 320\r",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("Physical size: 800x1280", input.wmSize)
        assertEquals("Physical density: 320", input.wmDensity)
    }

    /** 标记文本嵌在行中或行尾不构成分区标记。 */
    @Test
    fun `标记文本嵌在行中不构成标记`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            Output of wm size: below
            run dumpsys display: later for details
            Physical size: 1280x720
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(input.wmSize)
        assertNull(input.dumpsysDisplay)
    }

    /** 属性值包含分隔符序列不产生歧义（AOSP 键字符集保证键内不会出现 `]: [`）。 */
    @Test
    fun `属性值包含分隔符序列不产生歧义`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [ro.product.model]: [x]: [y]
            [ro.product.manufacturer]: [a] b ]: [ c]
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("x]: [y", input.model)
        assertEquals("a] b ]: [ c", input.manufacturer)

        // 括号未闭合的行是畸形转录，按非属性行丢弃，不得误解析。
        val malformed = NissanDiagnosticsTextParser.parse(
            "[ro.product.model]: [x]: [y",
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(malformed.model)
    }

    /** 属性值去除首尾空白后保留，内部空白原样保留；裸格式同样去首尾空白。 */
    @Test
    fun `属性值去除首尾空白后保留`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [ro.product.model]: [  padded model  ]
            [ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
            ro.product.brand=  nissan
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertEquals("padded model", input.model)
        assertEquals("NISSAN MOTOR CO., LTD.", input.manufacturer)
        assertEquals("nissan", input.brand)
    }

    /** 大输入结构化冒烟：约 1.4MB 转录逐项解析正确，宽限时上限只拦病态回归。 */
    @Test
    fun `大输入结构化冒烟测试`() {
        val noiseProps = (0 until 20_000)
            .joinToString("\n") { i -> "[ro.dummy.prop$i]: [noise-value-$i]" }
        val dumpsysLines = (0 until 20_000)
            .joinToString("\n") { i -> "  mNoiseState$i=value-$i" }
        val text = buildString {
            append(noiseProps).append('\n')
            append("[ro.product.product]: [smoke-product]\n")
            append("[ro.product.device]: [smoke-device]\n")
            append("[ro.product.model]: [smoke-model]\n")
            append("[ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]\n")
            append("[ro.product.brand]: [nissan]\n")
            append("[ro.build.version.release]: [10]\n")
            append("[ro.build.version.sdk]: [29]\n")
            append("wm size:\nPhysical size: 800x1280\nOverride size: 720x1152\n")
            append("wm density:\nPhysical density: 320\n")
            append("dumpsys display:\n").append(dumpsysLines)
        }
        val startedAt = System.nanoTime()
        val input = NissanDiagnosticsTextParser.parse(text, NissanDiagnosticsSource.ON_CAR, true)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals("smoke-product", input.product)
        assertEquals("smoke-device", input.device)
        assertEquals("smoke-model", input.model)
        assertEquals("NISSAN MOTOR CO., LTD.", input.manufacturer)
        assertEquals("nissan", input.brand)
        assertEquals("10", input.buildVersionRelease)
        assertEquals("29", input.buildVersionSdk)
        assertEquals("Physical size: 800x1280\nOverride size: 720x1152", input.wmSize)
        assertEquals("Physical density: 320", input.wmDensity)
        // 采集行数与生成数一致：O(n) 且无丢行。
        assertEquals(20_000, input.dumpsysDisplay!!.split('\n').size)
        // 数量级冒烟上限（正常耗时远低于此），只为拦截 O(n²) 类病态回归。
        assertTrue("解析耗时异常：${elapsedMs}ms", elapsedMs < 10_000)
    }

    /** 全量 getprop 转录：只有七个目标属性进入字段与报告，无关属性与空值被忽略。 */
    @Test
    fun `全量getprop转录只提取七个目标属性`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            [dhcp.wlan0.gateway]: []
            [init.svc.adbd]: [running]
            [persist.sys.locale]: [zh-CN]
            [ro.baseband]: [msm]
            [ro.build.version.release]: [10]
            [ro.build.version.sdk]: [29]
            [ro.product.board]: [SABRESD]
            [ro.product.brand]: [nissan]
            [ro.product.device]: [NTDA3_P330]
            [ro.product.model]: [NTDA3_BOARD_S02]
            [ro.product.name]: [NTDA3]
            [ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
            [ro.product.product]: [unit-product]
            [ro.hardware]: [freescale]
            [sys.usb.config]: [adb]
            """.trimIndent(),
            source = NissanDiagnosticsSource.OWNER_REPORTED,
            detected = true,
        )
        assertEquals("unit-product", input.product)
        assertEquals("NTDA3_P330", input.device)
        assertEquals("NTDA3_BOARD_S02", input.model)
        assertEquals("NISSAN MOTOR CO., LTD.", input.manufacturer)
        assertEquals("nissan", input.brand)
        assertEquals("10", input.buildVersionRelease)
        assertEquals("29", input.buildVersionSdk)

        // 端到端：无关属性值不得流入报告。
        val render = NissanDiagnosticsReport.from(input)!!.render()
        assertTrue(render.contains("ro.product.device=NTDA3_P330"))
        assertFalse(render.contains("SABRESD"))
        assertFalse(render.contains("freescale"))
        assertFalse(render.contains("running"))
    }

    /** 采集失败信号：wm 分区按噪声丢弃；dumpsys 采集区保留信号行作为证据。 */
    @Test
    fun `采集失败信号按噪声丢弃或保留为证据`() {
        val input = NissanDiagnosticsTextParser.parse(
            """
            wm size:
            Security exception: not allowed to read window size
            wm density:
            Remote exception: binder failed
            dumpsys display:
            Permission Denial: can't dump DisplayManagerService from pid=1234 uid=2000
            """.trimIndent(),
            source = NissanDiagnosticsSource.ON_CAR,
            detected = true,
        )
        assertNull(input.wmSize)
        assertNull(input.wmDensity)
        assertEquals(
            "Permission Denial: can't dump DisplayManagerService from pid=1234 uid=2000",
            input.dumpsysDisplay,
        )
    }

    /** 空白输入返回全 null，来源与识别结论仍原样透传。 */
    @Test
    fun `空白输入返回全null且透传来源与识别结论`() {
        for (text in listOf("", "  \n\t\n\r\n")) {
            val input = NissanDiagnosticsTextParser.parse(
                text,
                source = NissanDiagnosticsSource.OWNER_REPORTED,
                detected = true,
            )
            assertNull(input.product)
            assertNull(input.device)
            assertNull(input.model)
            assertNull(input.manufacturer)
            assertNull(input.brand)
            assertNull(input.buildVersionRelease)
            assertNull(input.buildVersionSdk)
            assertNull(input.wmSize)
            assertNull(input.wmDensity)
            assertNull(input.dumpsysDisplay)
            assertNull(input.platformHint)
            assertEquals(NissanDiagnosticsSource.OWNER_REPORTED, input.source)
            assertTrue(input.detected)
        }
    }
}
