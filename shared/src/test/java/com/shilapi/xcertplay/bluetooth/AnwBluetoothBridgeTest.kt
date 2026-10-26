package com.shilapi.xcertplay.bluetooth

import com.shilapi.xcertplay.transport.BlockingDuplexByteStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.UUID

/**
 * AnwPhoneLink 蓝牙桥接测试（纯 JVM，厂商通道全部注入桩，不绑定真实 AIDL）。
 *
 * 对应 BDD「规划：AnwPhoneLink 蓝牙桥接场景」四场景：
 * 原生可用走原生 / 原生缺失且 Anw 可达走桥接 / 皆无时明确报错 / 桥接失败清理并报错。
 */
class AnwBluetoothBridgeTest {

    @Test
    fun nativeBluetoothPresentKeepsNativePath() {
        val native = StubBluetooth(supported = true)
        val anw = AnwPhoneLinkBluetoothAdapter(resolver = { true }, channelFactory = { error("must not bridge") })

        val picked = CarPlayBluetoothManager.pick(nativeCandidate = { native }, vendorCandidate = { anw })

        assertSame("native path must win whenever it is supported", native, picked)
    }

    @Test
    fun anwServiceReachableBridgesWhenNativeAbsent() {
        val paired = listOf(CarPlayBluetoothDevice(name = "iPhone 15 Pro", address = "AA:BB:CC:DD:EE:FF", isConnected = false))
        val channel = FakeAnwChannel()
        val adapter = AnwPhoneLinkBluetoothAdapter(
            resolver = { true },
            channelFactory = { channel },
            pairedDevices = { paired },
        )

        assertTrue(adapter.isSupported())
        assertTrue("service reachable means the stack reports enabled", adapter.isEnabled())
        assertEquals("AnwPhoneLink", adapter.getAdapterName())
        assertEquals(paired, adapter.getPairedDevices())

        val stream = adapter.openRfcommStream("AA:BB:CC:DD:EE:FF", IAP2_UUID)
        assertTrue(stream is HsaeRfcommStream)
        assertEquals("AA:BB:CC:DD:EE:FF", channel.lastOpenedAddress)
        assertEquals(IAP2_UUID_BYTES.toList(), channel.lastOpenedUuid.toList())
    }

    @Test
    fun noBluetoothPathFailsFastWithUsbGuidance() {
        val native = StubBluetooth(supported = false)
        val anw = AnwPhoneLinkBluetoothAdapter(resolver = { false }, channelFactory = { error("must not bridge") })

        val picked = CarPlayBluetoothManager.pick(nativeCandidate = { native }, vendorCandidate = { anw })

        assertNull("no usable bluetooth backend must not return an adapter", picked)
        val error = runCatching { selectVendorWirelessTarget(picked, savedAddress = null) }.exceptionOrNull()
        assertTrue(
            "fail fast must tell the driver to use wired USB: $error",
            error is IOException && error.message!!.contains("wired USB"),
        )
    }

    @Test
    fun bridgeFailureCleansUpAndReports() {
        val channel = FakeAnwChannel()
        channel.writeResult = -1
        val adapter = AnwPhoneLinkBluetoothAdapter(resolver = { true }, channelFactory = { channel })

        val stream = adapter.openRfcommStream("AA:BB:CC:DD:EE:FF", IAP2_UUID)
        val sendError = runCatching { stream.send(byteArrayOf(1, 2, 3)) }.exceptionOrNull()
        assertTrue("vendor write failure must surface as IOException", sendError is IOException)
        val recvError = runCatching { stream.recv(16, 0) }.exceptionOrNull()
        assertTrue("failure must persist into recv", recvError is IOException)

        val opened = channel.openedIndexes
        assertTrue("bridge must open exactly one vendor socket", opened.size == 1)
        stream.close()
        assertTrue("close must disconnect the owned vendor socket", channel.closedIndexes.containsAll(opened))
        val afterClose = runCatching { stream.send(byteArrayOf(9)) }.exceptionOrNull()
        assertTrue(afterClose is IOException)
    }

    @Test
    fun bridgedStreamKeepsVendorByteStreamSemantics() {
        val channel = FakeAnwChannel()
        val adapter = AnwPhoneLinkBluetoothAdapter(resolver = { true }, channelFactory = { channel })
        val stream = adapter.openRfcommStream("AA:BB:CC:DD:EE:FF", IAP2_UUID)

        channel.deliver(byteArrayOf(1, 2, 3, 4, 5))
        channel.deliver(byteArrayOf(6, 7))

        assertEquals(byteListOf(1, 2), stream.recv(2, 100)!!.toByteList())
        assertEquals(
            "partial reads keep order; one internal buffer per recv like a syscall read",
            byteListOf(3, 4, 5),
            stream.recv(4, 100)!!.toByteList(),
        )
        assertEquals(byteListOf(6, 7), stream.recv(8, 100)!!.toByteList())
        assertNull("timeout with empty buffer returns null", stream.recv(8, 0))

        stream.send(byteArrayOf(9, 9))
        assertEquals(byteListOf(9, 9), channel.written)

        channel.disconnect()
        assertEquals("peer ended: recv drains then reports end-of-stream", 0, stream.recv(8, 100)!!.size)
        stream.close()
    }

    @Test
    fun targetSelectionMirrorsFrameworkOrder() {
        val saved = CarPlayBluetoothDevice(name = "iPhone", address = "11:11:11:11:11:11", isConnected = false)
        val connected = CarPlayBluetoothDevice(name = "iPhone", address = "22:22:22:22:22:22", isConnected = true)
        val other = CarPlayBluetoothDevice(name = "iPhone", address = "33:33:33:33:33:33", isConnected = false)
        val onePaired = AnwPhoneLinkBluetoothAdapter(
            resolver = { true },
            channelFactory = { FakeAnwChannel() },
            pairedDevices = { listOf(other) },
            connectedDevice = { connected },
        )
        assertEquals(
            "connected phone wins over the paired list",
            connected,
            selectVendorWirelessTarget(onePaired, savedAddress = null),
        )
        val manual = AnwPhoneLinkBluetoothAdapter(
            resolver = { true },
            channelFactory = { FakeAnwChannel() },
            pairedDevices = { listOf(other) },
            connectedDevice = { connected },
        )
        assertEquals(
            "manually entered address wins over everything",
            saved,
            selectVendorWirelessTarget(manual, savedAddress = saved.address),
        )
        val single = AnwPhoneLinkBluetoothAdapter(
            resolver = { true },
            channelFactory = { FakeAnwChannel() },
            pairedDevices = { listOf(CarPlayBluetoothDevice(name = "iPad", address = "44:44:44:44:44:44", isConnected = false), other) },
            connectedDevice = { null },
        )
        assertEquals("single paired iPhone is chosen", other, selectVendorWirelessTarget(single, savedAddress = null))
        val multi = AnwPhoneLinkBluetoothAdapter(
            resolver = { true },
            channelFactory = { FakeAnwChannel() },
            pairedDevices = {
                listOf(
                    CarPlayBluetoothDevice(name = "iPhone A", address = "55:55:55:55:55:55", isConnected = false),
                    CarPlayBluetoothDevice(name = "iPhone B", address = "66:66:66:66:66:66", isConnected = false),
                )
            },
            connectedDevice = { null },
        )
        val error = runCatching { selectVendorWirelessTarget(multi, savedAddress = null) }.exceptionOrNull()
        assertTrue("multiple paired iPhones must fail like the framework path: $error", error is IOException)
    }

    @Test
    fun closedStreamReleasesItsVendorListener() {
        val channel = FakeAnwChannel()
        val adapter = AnwPhoneLinkBluetoothAdapter(resolver = { true }, channelFactory = { channel })
        val stream = adapter.openRfcommStream("AA:BB:CC:DD:EE:FF", IAP2_UUID)

        channel.deliver(byteArrayOf(1))
        assertEquals(byteListOf(1), stream.recv(4, 100)!!.toByteList())
        stream.close()
        channel.deliver(byteArrayOf(2))

        val afterClose = stream.recv(4, 50)
        assertTrue(
            "data delivered after close must not accumulate: $afterClose",
            afterClose == null || afterClose.isEmpty(),
        )
    }

    private companion object {
        val IAP2_UUID: String = "00000000-deca-fade-deca-deafdecacafe"
        val IAP2_UUID_BYTES: ByteArray = uuidToAnwGuidBytes(IAP2_UUID)
    }
}

/** 桩原生蓝牙后端：只回答“是否可用”，其余不可达。 */
private class StubBluetooth(private val supported: Boolean) : CarPlayBluetooth {
    override fun isSupported(): Boolean = supported
    override fun isEnabled(): Boolean = supported
    override fun enable(): Boolean = supported
    override fun getAdapterName(): String = "stub"
    override fun getPairedDevices(): List<CarPlayBluetoothDevice> = throw IllegalStateException("native path only")
    override fun getConnectedDevice(): CarPlayBluetoothDevice? = null
    override fun localAddress(): String? = null
    override fun openRfcommStream(deviceAddress: String, uuid: String): BlockingDuplexByteStream =
        throw IllegalStateException("native path only")
}

/** 桩厂商通道：记录开/关/写，支持注入失败与数据下发。 */
private class FakeAnwChannel : AnwVendorChannel {
    var writeResult: Int = 0
    val openedIndexes = mutableListOf<Int>()
    val closedIndexes = mutableListOf<Int>()
    val written = mutableListOf<Byte>()
    var lastOpenedAddress: String? = null
    var lastOpenedUuid: ByteArray = ByteArray(0)
    private var listener: AnwVendorChannel.Listener? = null
    private var nextIndex = 1

    override fun openSocket(address: String, uuidBytes: ByteArray): Int {
        lastOpenedAddress = address
        lastOpenedUuid = uuidBytes
        val index = nextIndex++
        openedIndexes.add(index)
        return index
    }

    override fun write(index: Int, data: ByteArray): Int {
        if (writeResult < 0) return writeResult
        written.addAll(data.toList())
        return data.size
    }

    override fun closeSocket(index: Int) {
        closedIndexes.add(index)
    }

    override fun setListener(index: Int, listener: AnwVendorChannel.Listener?) {
        this.listener = listener
    }

    fun deliver(data: ByteArray) {
        listener?.onData(data)
    }

    fun disconnect() {
        listener?.onDisconnected()
    }
}

private fun byteListOf(vararg values: Int): List<Byte> = values.map { it.toByte() }

private fun ByteArray.toByteList(): List<Byte> = toList()
