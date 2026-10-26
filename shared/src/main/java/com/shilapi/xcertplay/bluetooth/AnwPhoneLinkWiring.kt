package com.shilapi.xcertplay.bluetooth

/**
 * AnwPhoneLink 生产装配（SDD：检测门 = 服务 action 解析，包名+action 双匹配）。
 *
 * 纯 JVM 边界内可测的组装逻辑都在这里；实车侧的 Android 装配
 * （PackageManager 探针、IAnwPhoneLink binder 通道）经注入接入：
 *  - 探针生产实现：`PackageManager.queryIntentServices(Intent(ACTION).setPackage(PKG), 0)`
 *    非空即服务可达（对齐 T32 实机路线，比裸包名存在严谨）；
 *  - 通道生产实现：绑定厂商服务后把 ANWBT_Socket* 调用映射为 [AnwVendorChannel]。
 */
fun interface AnwServiceProbe {
    fun queryServiceCount(action: String, packageName: String): Int
}

object AnwPhoneLinkWiring {
    const val SERVICE_ACTION = "com.anwsdk.service.AnwPhoneLink"
    const val SERVICE_PACKAGE = "com.anwsdk.service"

    fun vendorCandidate(
        probe: AnwServiceProbe,
        channelFactory: () -> AnwVendorChannel = { error("bind the IAnwPhoneLink service on the car unit") },
        pairedDevices: () -> List<CarPlayBluetoothDevice> = { emptyList() },
        connectedDevice: () -> CarPlayBluetoothDevice? = { null },
        addressReader: () -> String? = { null },
    ): AnwPhoneLinkBluetoothAdapter = AnwPhoneLinkBluetoothAdapter(
        resolver = { probe.queryServiceCount(SERVICE_ACTION, SERVICE_PACKAGE) > 0 },
        channelFactory = channelFactory,
        pairedDevices = pairedDevices,
        connectedDevice = connectedDevice,
        addressReader = addressReader,
    )
}
