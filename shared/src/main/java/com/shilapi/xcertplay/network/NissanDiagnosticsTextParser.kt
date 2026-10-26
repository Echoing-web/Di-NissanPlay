package com.shilapi.xcertplay.network

/**
 * 把一段纯文本诊断记录解析为 [NissanDiagnosticsInput]。
 *
 * 职责边界：
 *  - 纯文本变换：不执行 shell、ADB 或其他命令，不做任何 I/O；识别职责在
 *    [NissanHeadUnit]，detected 与 source 由调用方提供并原样透传，这里绝不推断。
 *  - 缺失或空白的字段保持 null，由 [NissanDiagnosticsReport] 统一输出 unknown，
 *    不回填任何默认值。
 *
 * 文本约定：
 *  - 行分隔兼容 `\n`、`\r\n` 与 `\r`；
 *  - 属性行支持 getprop 的 `[key]: [value]` 与裸 `key=value` 两种写法，任意位置
 *    （包括任何分区内与首个标记之前）的属性行都会被解析，值去除首尾空白后保留，
 *    空白值视为缺失，重复键按后出现者覆盖；
 *  - 分区标记为行首（忽略首尾空白、区分大小写）的 `wm size:`、`wm density:`、
 *    `dumpsys display:`；标记之后的同行剩余内容（去首尾空白）视为该分区首行；
 *  - size 分区只保留 `Physical size:` / `Override size:` 行，density 分区只保留
 *    `Physical density:` / `Override density:` 行（保留行同样去首尾空白），
 *    均维持原始顺序；
 *  - 同一分区再次出现时跨块按原始顺序累计；
 *  - `dumpsys display` 只在显式标记之后采集，直到下一个 wm 标记或输入结束，
 *    没有显式标记时不得采集；采集文本保留内部行与缩进，仅去除首尾空白。
 */
object NissanDiagnosticsTextParser {

    private const val WM_SIZE_MARKER = "wm size:"
    private const val WM_DENSITY_MARKER = "wm density:"
    private const val DUMPSYS_DISPLAY_MARKER = "dumpsys display:"

    private val PROPERTY_KEYS = listOf(
        "ro.product.product",
        "ro.product.device",
        "ro.product.model",
        "ro.product.manufacturer",
        "ro.product.brand",
        "ro.build.version.release",
        "ro.build.version.sdk",
    )

    /** (键, getprop 前缀, 裸前缀) 预生成，避免逐行重复拼接字符串。 */
    private val PROPERTY_PATTERNS: List<Triple<String, String, String>> = PROPERTY_KEYS.map { key ->
        Triple(key, "[$key]: [", "$key=")
    }

    private val WM_SIZE_LINE_PREFIXES = listOf("Physical size:", "Override size:")
    private val WM_DENSITY_LINE_PREFIXES = listOf("Physical density:", "Override density:")

    fun parse(
        text: String,
        source: NissanDiagnosticsSource,
        detected: Boolean,
    ): NissanDiagnosticsInput {
        val properties = mutableMapOf<String, String>()
        val wmSizeLines = mutableListOf<String>()
        val wmDensityLines = mutableListOf<String>()
        val dumpsysLines = mutableListOf<String>()
        var section = ""

        fun consume(content: String, verbatim: String) {
            parseProperty(content, properties)
            when (section) {
                WM_SIZE_MARKER -> collectWmLine(wmSizeLines, content, WM_SIZE_LINE_PREFIXES)
                WM_DENSITY_MARKER -> collectWmLine(wmDensityLines, content, WM_DENSITY_LINE_PREFIXES)
                DUMPSYS_DISPLAY_MARKER -> dumpsysLines.add(verbatim)
            }
        }

        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            val marker = markerOf(trimmed)
            if (marker != null) {
                section = marker
                val inline = trimmed.substring(marker.length).trim()
                if (inline.isNotEmpty()) consume(inline, inline)
            } else {
                consume(trimmed, line)
            }
        }

        return NissanDiagnosticsInput(
            detected = detected,
            source = source,
            product = properties["ro.product.product"],
            device = properties["ro.product.device"],
            model = properties["ro.product.model"],
            manufacturer = properties["ro.product.manufacturer"],
            brand = properties["ro.product.brand"],
            buildVersionRelease = properties["ro.build.version.release"],
            buildVersionSdk = properties["ro.build.version.sdk"],
            wmSize = wmSizeLines.joinToString("\n").takeIf { it.isNotEmpty() },
            wmDensity = wmDensityLines.joinToString("\n").takeIf { it.isNotEmpty() },
            dumpsysDisplay = dumpsysLines.joinToString("\n").trim().takeIf { it.isNotEmpty() },
        )
    }

    /** 行首（忽略首尾空白）匹配标记文本才视为分区标记，嵌在句中的子串不算。 */
    private fun markerOf(trimmed: String): String? = when {
        trimmed.startsWith(WM_SIZE_MARKER) -> WM_SIZE_MARKER
        trimmed.startsWith(WM_DENSITY_MARKER) -> WM_DENSITY_MARKER
        trimmed.startsWith(DUMPSYS_DISPLAY_MARKER) -> DUMPSYS_DISPLAY_MARKER
        else -> null
    }

    /** 解析 `[key]: [value]` 或 `key=value`；空白值视为缺失，不记录。 */
    private fun parseProperty(line: String, into: MutableMap<String, String>) {
        for ((key, bracketPrefix, barePrefix) in PROPERTY_PATTERNS) {
            val value = when {
                line.startsWith(bracketPrefix) && line.endsWith("]") ->
                    line.substring(bracketPrefix.length, line.length - 1)
                line.startsWith(barePrefix) -> line.substring(barePrefix.length)
                else -> continue
            }
            value.trim().takeIf { it.isNotEmpty() }?.let { into[key] = it }
            return
        }
    }

    /** wm 分区按各自分区的完整前缀保留行。 */
    private fun collectWmLine(lines: MutableList<String>, line: String, prefixes: List<String>) {
        if (prefixes.any(line::startsWith)) lines.add(line)
    }
}
