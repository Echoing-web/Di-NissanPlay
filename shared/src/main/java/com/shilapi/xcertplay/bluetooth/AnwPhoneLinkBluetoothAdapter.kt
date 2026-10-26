package com.shilapi.xcertplay.bluetooth

import com.shilapi.xcertplay.transport.BlockingDuplexByteStream
import java.io.IOException

/**
 * 以厂商 AnwPhoneLink 服务为底座的 [CarPlayBluetooth] 替身（SDD「AnwPhoneLink
 * 蓝牙桥接设计」）。
 *
 * 诚实边界（对齐 T32 实机行为）：车机系统管理厂商蓝牙固件的电源与配对，本
 * 替身不能开启蓝牙、不能发现新手机；服务可达即视为已启用；厂商配对列表不
 * 报告已连接设备。
 *
 * AIDL binder 与服务解析的全部细节经注入隔离在纯 JVM 边界之外；生产装配
 * 见 SDD 接入说明（`CarPlayBluetoothManager` 混合门：原生优先 → 厂商回落）。
 */
class AnwPhoneLinkBluetoothAdapter(
    private val resolver: () -> Boolean,
    private val channelFactory: () -> AnwVendorChannel,
    private val pairedDevices: () -> List<CarPlayBluetoothDevice> = { emptyList() },
    private val connectedDevice: () -> CarPlayBluetoothDevice? = { null },
    private val addressReader: () -> String? = { null },
) : CarPlayBluetooth {

    override fun isSupported(): Boolean = resolver()

    override fun isEnabled(): Boolean = isSupported()

    override fun enable(): Boolean = isSupported()

    override fun getAdapterName(): String = ADAPTER_NAME

    override fun getPairedDevices(): List<CarPlayBluetoothDevice> =
        if (!isSupported()) emptyList() else runCatching(pairedDevices).getOrDefault(emptyList())

    override fun getConnectedDevice(): CarPlayBluetoothDevice? = connectedDevice()

    override fun localAddress(): String? = addressReader()?.takeIf(::isValidMac)

    override fun openRfcommStream(deviceAddress: String, uuid: String): BlockingDuplexByteStream {
        val channel = channelFactory()
        val index = channel.openSocket(deviceAddress, uuidToAnwGuidBytes(uuid))
        return HsaeRfcommStream(channel, index).also { stream ->
            if (index < 0) {
                stream.onVendorWriteError(IOException("AnwPhoneLink socket open failed for $deviceAddress"))
            }
        }
    }

    companion object {
        const val ADAPTER_NAME = "AnwPhoneLink"

        internal fun isValidMac(mac: String): Boolean =
            Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}").matches(mac) &&
                !mac.startsWith("02:00:00:00:00:", ignoreCase = true) &&
                mac != "00:00:00:00:00:00"
    }
}

/**
 * 蓝牙后端混合门：framework 蓝牙可用时永远走原生路径；不可用时回落厂商
 * 替身；两者皆不可用返回 null（调用方必须 fail fast，见
 * [selectVendorWirelessTarget]）。
 */
object CarPlayBluetoothManager {
    fun pick(
        nativeCandidate: () -> CarPlayBluetooth?,
        vendorCandidate: () -> CarPlayBluetooth?,
    ): CarPlayBluetooth? = runCatching(nativeCandidate).getOrNull()?.takeIf { it.isSupported() }
        ?: runCatching(vendorCandidate).getOrNull()?.takeIf { it.isSupported() }
}

/**
 * 在厂商/混合蓝牙后端上为无线 iAP2 引导挑选 iPhone：手动输入地址优先（车主
 * 指名），其次厂商报告的已连接手机，再次唯一配对的 iPhone——镜像 framework
 * 路径的顺序与多 iPhone 失败行为。
 */
fun selectVendorWirelessTarget(hybrid: CarPlayBluetooth?, savedAddress: String?): CarPlayBluetoothDevice {
    if (hybrid == null) {
        throw IOException("Wireless CarPlay needs Bluetooth; use wired USB instead")
    }
    val saved = savedAddress?.takeIf { AnwPhoneLinkBluetoothAdapter.isValidMac(it) }
    if (saved != null) {
        return CarPlayBluetoothDevice(name = "iPhone", address = saved, isConnected = false)
    }
    hybrid.getConnectedDevice()?.let { return it }
    val iPhones = hybrid.getPairedDevices().filter { it.name.contains("iPhone", ignoreCase = true) }
    if (iPhones.size == 1) return iPhones.single()
    if (iPhones.size > 1) {
        throw IOException(
            "Multiple paired iPhones found and none is currently connected; connect one iPhone and retry",
        )
    }
    throw IOException("Wireless CarPlay needs Bluetooth; use wired USB instead")
}
