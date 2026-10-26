package com.shilapi.xcertplay.network

/**
 * 采集命令执行抽象层：可注入的命令执行器 + 调研验证的采集命令 + 采集运行器。
 *
 * 职责边界：
 *  - 只定义「执行哪四条命令、输出如何对应到四段」，不执行真实命令——真实执行器
 *    （Runtime.exec / shell 等）由 Android 薄壳提供，纯 JVM 测试注入桩。
 *  - 执行器契约：[NissanDiagnosticsCommandExecutor.execute] 返回命令的合并输出
 *    （实现可合并 stderr，失败信号检测依赖文本）；执行失败或无输出返回 null；
 *    超时与重试策略由实现自定，本层不做。
 *  - 单条命令失败（null）不阻塞其余采集；null 段在 [NissanDiagnosticsTranscript]
 *    组装时按空白段落跳过。
 *  - 四条命令为 AOSP 调研验证的形式（见 NISSAN_TEANA_RESEARCH_20261026.md）：
 *    全量 `getprop`——多参数形式不可用（getprop 最多 2 个参数）。
 */
object NissanDiagnosticsCommands {
    const val GETPROP = "getprop"
    const val WM_SIZE = "wm size"
    const val WM_DENSITY = "wm density"
    const val DUMPSYS_DISPLAY = "dumpsys display"
}

/** 只读采集命令执行器：Android 薄壳注入真实实现，纯 JVM 测试注入桩。 */
fun interface NissanDiagnosticsCommandExecutor {
    /** 执行命令返回合并输出；执行失败或无输出返回 null。 */
    fun execute(command: String): String?
}

/** 四条采集命令的输出，直接对应 [NissanDiagnosticsCapture.from] 的四个段落参数。 */
data class NissanDiagnosticsCommandOutput(
    val getprop: String?,
    val wmSize: String?,
    val wmDensity: String?,
    val dumpsysDisplay: String?,
)

/** 用注入的执行器按固定顺序运行四条采集命令。 */
class NissanDiagnosticsCommandCapture(private val executor: NissanDiagnosticsCommandExecutor) {

    fun run(): NissanDiagnosticsCommandOutput = NissanDiagnosticsCommandOutput(
        getprop = executor.execute(NissanDiagnosticsCommands.GETPROP),
        wmSize = executor.execute(NissanDiagnosticsCommands.WM_SIZE),
        wmDensity = executor.execute(NissanDiagnosticsCommands.WM_DENSITY),
        dumpsysDisplay = executor.execute(NissanDiagnosticsCommands.DUMPSYS_DISPLAY),
    )
}
