package com.shilapi.xcertplay.network

/**
 * 把各命令的采集输出编排为诊断结论（纯数据编排）。
 *
 * 职责边界：
 *  - 只组合既有纯组件，不新增任何文本处理规则：失败信号检测与转录组装在
 *    [NissanDiagnosticsTranscript]，文本解析在 [NissanDiagnosticsTextParser]，
 *    报告生成与 unknown 渲染在 [NissanDiagnosticsReport]。
 *  - 不执行命令、不做 I/O、不识别车机；`detected` 由调用方提供（实车侧取自
 *    [NissanHeadUnit.detected]），本对象不推断。
 *
 * 分流优先级：`detected == false` 一律拒绝（[Outcome.Undetected]，即使采集干净）；
 * 存在采集失败信号则要求重采（[Outcome.CaptureFailed]，按段落顺序收集信号行）；
 * 否则生成报告（[Outcome.Reported]）。全部段落空白不算失败：`detected == true` 时
 * 生成全 unknown 报告。
 */
object NissanDiagnosticsCapture {

    /** 采集编排结果。 */
    sealed interface Outcome {
        /** 存在采集失败信号，需重采；[signals] 为各段的信号行（按段落顺序）。 */
        data class CaptureFailed(val signals: List<String>) : Outcome

        /** `detected == false`，一律拒绝生成。 */
        object Undetected : Outcome

        /** 报告生成成功。 */
        data class Reported(val report: NissanDiagnosticsReport) : Outcome
    }

    fun from(
        getprop: String?,
        wmSize: String?,
        wmDensity: String?,
        dumpsysDisplay: String?,
        source: NissanDiagnosticsSource,
        detected: Boolean,
    ): Outcome {
        if (!detected) return Outcome.Undetected

        val signals = listOf(getprop, wmSize, wmDensity, dumpsysDisplay)
            .mapNotNull { raw -> raw?.trim()?.takeIf { it.isNotEmpty() } }
            .mapNotNull { section -> NissanDiagnosticsTranscript.captureFailureSignal(section) }
        if (signals.isNotEmpty()) return Outcome.CaptureFailed(signals)

        val input = NissanDiagnosticsTextParser.parse(
            NissanDiagnosticsTranscript.assemble(getprop, wmSize, wmDensity, dumpsysDisplay),
            source,
            detected,
        )
        return Outcome.Reported(requireNotNull(NissanDiagnosticsReport.from(input)))
    }
}
