package com.shilapi.xcertplay.network

import android.os.Build

/**
 * 识别日产超智联车机，当前目标是中国市场 2021 款天籁 2.0T。
 *
 * 车机的精确 `ro.product.*` 值尚未在实车上采集，因此现阶段只接受明确包含 Nissan
 * 的系统标识。实车诊断报告采集完成后，再把确认过的值加入 [KNOWN_UNIT_IDENTIFIERS]。
 * 识别结果按访问时重新计算，避免测试或诊断过程中 Build 字段变化后仍使用旧快照。
 */
object NissanHeadUnit {

    private val currentIdentifiers: List<String>
        get() = runCatching {
            listOf(Build.PRODUCT, Build.DEVICE, Build.MODEL, Build.MANUFACTURER, Build.BRAND)
        }.getOrDefault(emptyList())

    /** 实车确认过的产品、设备或型号标识。目标车完成采集前保持为空。 */
    private val KNOWN_UNIT_IDENTIFIERS: List<String> = emptyList()

    internal fun matchesIdentifiers(identifiers: Iterable<String>): Boolean = identifiers
        .map(String::lowercase)
        .any { id ->
            KNOWN_UNIT_IDENTIFIERS.any(id::contains) || id.contains("nissan")
        }

    val detected: Boolean
        get() = matchesIdentifiers(currentIdentifiers)
}
