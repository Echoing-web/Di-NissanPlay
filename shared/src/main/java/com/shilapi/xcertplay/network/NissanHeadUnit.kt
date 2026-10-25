package com.shilapi.xcertplay.network

import android.os.Build

/**
 * Identifies the Nissan Connect (超智联 / 智联导航) head unit family that this port targets:
 * the 2021 Teana 2.0T (China-market seventh-generation Altima) and its 2019–2023 siblings.
 *
 * What is known about these units so far comes from owner reports, not from this project's own
 * car yet (see docs/NISSAN_TEANA_HEADUNIT.md):
 *
 *  - The firmware is a closed Android 9/10 build; third-party APKs install through ADB
 *    (wireless on port 5555 after the engineering-mode toggle) or a U-disk installer.
 *  - The center screen of the 2021 Teana is an 8-inch 1280x720 panel.
 *  - No BYD DiLink services exist on these units, so every DiLink-gated route must stay off
 *    exactly as on any other non-BYD head unit.
 *
 * The exact `ro.product.*` values of the 2021 Teana 2.0T unit are not yet confirmed on the car.
 * Detection therefore only matches an identifier that says Nissan; a diagnostic report or
 * `adb shell getprop ro.product.*` collected on the car refines [KNOWN_UNIT_IDENTIFIERS].
 *
 * Nothing gates on [detected] yet — this is the collection point the Nissan port hangs its
 * first integrations (steering-wheel keys, hotspot guidance) on. Unit tests, where `Build`
 * reports Robolectric values, never take this path unless they set it up.
 */
object NissanHeadUnit {

    private val identifiers: List<String>
        get() = runCatching {
            listOf(Build.PRODUCT, Build.DEVICE, Build.MODEL, Build.MANUFACTURER, Build.BRAND)
                .map { it.lowercase() }
        }.getOrDefault(emptyList())

    /**
     * Product, device or model strings observed on real Nissan Connect units. Empty until the
     * target car reports its own values, so today only the Nissan name itself matches.
     */
    private val KNOWN_UNIT_IDENTIFIERS: List<String> = emptyList()

    val detected: Boolean = identifiers.any { id ->
        KNOWN_UNIT_IDENTIFIERS.any(id::contains) || id.contains("nissan")
    }
}
