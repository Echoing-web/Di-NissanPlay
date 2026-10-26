package com.shilapi.xcertplay.network

/**
 * 调用方侧的转录组装与采集失败信号检测（纯文本变换）。
 *
 * 职责边界：
 *  - 把各命令的采集输出组装为带分区标签的转录，可被 [NissanDiagnosticsTextParser]
 *    直接解析；空白段落整体跳过，不产生空分区。
 *  - 检测采集输出中的失败信号行（`Permission Denial` / `Security exception` /
 *    `Remote exception` / `Error` 等），供调用方提示重采；信号行在 dumpsys 采集区
 *    会原样保留为证据，丢弃与否由解析器按分区规则决定。
 *  - 不执行命令、不做 I/O、不识别车机、不产生 UI；Android 侧采集流程是独立切片，
 *    只组合本组件与解析器。
 */
object NissanDiagnosticsTranscript {

    private const val WM_SIZE_MARKER = "wm size:"
    private const val WM_DENSITY_MARKER = "wm density:"
    private const val DUMPSYS_DISPLAY_MARKER = "dumpsys display:"

    /** 采集失败信号行的行首前缀（区分大小写；来源见 NISSAN_TEANA_RESEARCH_20261026.md）。 */
    private val CAPTURE_FAILURE_PREFIXES = listOf(
        "Permission Denial",
        "Security exception",
        "Remote exception",
        "Error:",
        "Error with service",
        "Can't find service",
    )

    /** 按接线规则组装带分区标签的转录：getprop 段在前，随后 wm / dumpsys 标签段。 */
    fun assemble(
        getprop: String?,
        wmSize: String?,
        wmDensity: String?,
        dumpsysDisplay: String?,
    ): String = buildString {
        section(getprop)?.let { append(it).append('\n') }
        section(wmSize)?.let { append(WM_SIZE_MARKER).append('\n').append(it).append('\n') }
        section(wmDensity)?.let { append(WM_DENSITY_MARKER).append('\n').append(it).append('\n') }
        section(dumpsysDisplay)?.let { append(DUMPSYS_DISPLAY_MARKER).append('\n').append(it).append('\n') }
    }.trimEnd('\n')

    /** 返回首个失败信号行（去首尾空白）；无信号时返回 null。 */
    fun captureFailureSignal(section: String): String? =
        section.lineSequence()
            .map(String::trim)
            .firstOrNull { line -> CAPTURE_FAILURE_PREFIXES.any(line::startsWith) }

    /** 段落去首尾空白；空白段落视为未采集，返回 null。 */
    private fun section(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }
}
