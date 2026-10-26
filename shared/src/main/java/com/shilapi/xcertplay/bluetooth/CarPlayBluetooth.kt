package com.shilapi.xcertplay.bluetooth

/**
 * DiPlay 无线 CarPlay 所需的最小蓝牙后端契约。
 *
 * 生产实现有两个：Android framework 蓝牙，以及车机厂商私有栈的桥接替身
 * （见 [AnwPhoneLinkBluetoothAdapter]）。接口刻意不携带 Android 类型，
 * 两个实现与纯 JVM 测试桩共用同一契约。
 */
interface CarPlayBluetooth {
    fun isSupported(): Boolean

    fun isEnabled(): Boolean

    fun enable(): Boolean

    fun getAdapterName(): String

    fun getPairedDevices(): List<CarPlayBluetoothDevice>

    /** 厂商栈可能不报告已连接设备；返回 null 表示未知。 */
    fun getConnectedDevice(): CarPlayBluetoothDevice?

    fun localAddress(): String?

    fun openRfcommStream(deviceAddress: String, uuid: String): com.shilapi.xcertplay.transport.BlockingDuplexByteStream
}

data class CarPlayBluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
)
