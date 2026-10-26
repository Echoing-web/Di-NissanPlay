package com.shilapi.xcertplay.bluetooth

/**
 * 厂商 AnwPhoneLink 通道抽象：把 AIDL binder 细节隔离在这层之外。
 *
 * 生产实现负责绑定 `com.anwsdk.service` 并把 ANWBT_Socket* 调用映射到这里；
 * 纯 JVM 测试注入桩通道。接口按 socket 子系统建模（NTDA3 家族实机验证的
 * 路线，SDD「AnwPhoneLink 蓝牙桥接设计」）。
 */
interface AnwVendorChannel {

    /** 打开厂商 socket；返回连接索引，失败返回负值。 */
    fun openSocket(address: String, uuidBytes: ByteArray): Int

    /** 写入数据；返回写入字节数，失败返回负值。 */
    fun write(index: Int, data: ByteArray): Int

    fun closeSocket(index: Int)

    /** 注册/反注册该连接的数据与断开回调；null 表示反注册。 */
    fun setListener(index: Int, listener: Listener?)

    interface Listener {
        fun onData(data: ByteArray)

        fun onDisconnected()
    }
}

/**
 * iAP2 service UUID 按厂商 API 要求的 GUID 字节序编码：相对网络序反转
 * 0–3、4–5、6–7 字节组（上游 #880 实机对勘结论）。
 */
fun uuidToAnwGuidBytes(uuid: String): ByteArray {
    val hex = uuid.filter { it != '-' }.lowercase()
    require(hex.length == 32) { "not a UUID: $uuid" }
    val networkOrder = ByteArray(16) { index -> hex.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    fun reverseRange(from: Int, until: Int) {
        var left = from
        var right = until - 1
        while (left < right) {
            val tmp = networkOrder[left]
            networkOrder[left] = networkOrder[right]
            networkOrder[right] = tmp
            left++
            right--
        }
    }
    reverseRange(0, 4)
    reverseRange(4, 6)
    reverseRange(6, 8)
    return networkOrder
}
