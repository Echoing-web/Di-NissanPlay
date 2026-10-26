package com.shilapi.xcertplay.bluetooth

/**
 * 无线蓝牙混合门规划：原生在场永远优先（既有路径零变化）；原生缺失且厂商
 * 服务可达走 AnwPhoneLink 桥接；两者皆无返回 [WirelessBluetoothPlan.None]，
 * 由调用方按既有 NO_ADAPTER 语义 fail fast。纯函数，全分支纯 JVM 可测。
 */
sealed interface WirelessBluetoothPlan {
    /** 原生 framework 蓝牙在场：控制器继续既有 adapter 代码路径。 */
    object Native : WirelessBluetoothPlan

    /** 原生缺失、厂商服务可达：桥接路径（目标选择/字节流/MAC 全走厂商栈）。 */
    data class Vendor(val bridge: CarPlayBluetooth) : WirelessBluetoothPlan

    object None : WirelessBluetoothPlan
}

object WirelessBluetoothPlanner {
    fun plan(nativeAdapterPresent: Boolean, vendorBridge: CarPlayBluetooth?): WirelessBluetoothPlan {
        if (nativeAdapterPresent) return WirelessBluetoothPlan.Native
        val bridge = runCatching { vendorBridge?.takeIf { it.isSupported() } }.getOrNull()
        return if (bridge != null) WirelessBluetoothPlan.Vendor(bridge) else WirelessBluetoothPlan.None
    }
}
