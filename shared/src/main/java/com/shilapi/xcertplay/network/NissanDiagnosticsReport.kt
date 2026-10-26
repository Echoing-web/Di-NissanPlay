package com.shilapi.xcertplay.network

/** 诊断信息的来源：实车采集，或车主转述（未经实车验证）。 */
enum class NissanDiagnosticsSource(val label: String) {
    ON_CAR("on-car"),
    OWNER_REPORTED("owner-reported"),
}

/**
 * 网络传闻的平台代号标注：只能原样记录并携带其自身来源，不参与 [NissanHeadUnit]
 * 的识别判定，也不得据此扩充识别列表。
 */
data class NissanPlatformHint(val value: String, val source: NissanDiagnosticsSource)

/**
 * 一次只读采集得到的原始诊断输入。
 *
 * 所有诊断字段均为可选：缺失或空白视为未知，渲染时统一输出 unknown，
 * 这里不做任何默认填充（禁止回填 1280x720、NTDA3、P1xx 等传闻值）。
 */
data class NissanDiagnosticsInput(
    /** 识别结论由 NissanHeadUnit 在实车侧给出；false 时 [NissanDiagnosticsReport.from] 拒绝生成。 */
    val detected: Boolean,
    /** 本次诊断采集的来源。 */
    val source: NissanDiagnosticsSource,
    /** `ro.product.product`。 */
    val product: String? = null,
    /** `ro.product.device`。 */
    val device: String? = null,
    /** `ro.product.model`。 */
    val model: String? = null,
    /** `ro.product.manufacturer`。 */
    val manufacturer: String? = null,
    /** `ro.product.brand`。 */
    val brand: String? = null,
    /** `ro.build.version.release`。 */
    val buildVersionRelease: String? = null,
    /** `ro.build.version.sdk`。 */
    val buildVersionSdk: String? = null,
    /** `adb shell wm size` 的输出摘要。 */
    val wmSize: String? = null,
    /** `adb shell wm density` 的输出摘要。 */
    val wmDensity: String? = null,
    /** `adb shell dumpsys display` 的摘要。 */
    val dumpsysDisplay: String? = null,
    /** 可选的平台代号标注，仅作记录。 */
    val platformHint: NissanPlatformHint? = null,
)

/**
 * 只读车机诊断报告：不可变数据 + 稳定的逐行 key=value 渲染。
 *
 * 职责边界：
 *  - 纯数据变换，不依赖 Android framework，不连接车机，不执行命令，不写任何文件。
 *  - 识别职责在 [NissanHeadUnit]；报告不是识别逻辑，只有实车采集方给出
 *    detected=true 时才允许生成（[from] 对 detected=false 返回 null）。
 */
class NissanDiagnosticsReport private constructor(private val input: NissanDiagnosticsInput) {

    companion object {
        private const val KIND = "nissan-headunit-diagnostics"

        /** 字段缺失或空白时的统一占位。 */
        private const val UNKNOWN = "unknown"

        /** detected=false 时拒绝生成报告。 */
        fun from(input: NissanDiagnosticsInput): NissanDiagnosticsReport? =
            if (input.detected) NissanDiagnosticsReport(input) else null
    }

    /**
     * 渲染为稳定的逐行 key=value 文本：行序固定，同一输入永远得到同一文本。
     * 值中的换行被转义为字面 `\n`，注入内容无法伪造新的 key=value 行。
     */
    fun render(): String {
        val hint = input.platformHint?.takeIf { it.value.isNotBlank() }
        return listOf(
            "kind" to KIND,
            "source" to input.source.label,
            "detected" to input.detected.toString(),
            "ro.product.product" to input.product,
            "ro.product.device" to input.device,
            "ro.product.model" to input.model,
            "ro.product.manufacturer" to input.manufacturer,
            "ro.product.brand" to input.brand,
            "ro.build.version.release" to input.buildVersionRelease,
            "ro.build.version.sdk" to input.buildVersionSdk,
            "wm.size" to input.wmSize,
            "wm.density" to input.wmDensity,
            "dumpsys.display" to input.dumpsysDisplay,
            "platform_hint" to hint?.value,
            "platform_hint_source" to hint?.source?.label,
        ).joinToString("\n") { (key, value) -> "$key=${renderedValue(value)}" }
    }

    private fun renderedValue(value: String?): String =
        value?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace("\r\n", "\n")
            ?.replace('\r', '\n')
            ?.replace("\n", "\\n")
            ?: UNKNOWN
}
