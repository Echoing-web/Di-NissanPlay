package com.shilapi.xcertplay.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AnwPhoneLink 装配层测试：生产 resolver（服务 action 解析探针）与混合门的
 * 组装行为，全部纯 JVM（探针注入，不触 PackageManager/binder）。
 */
class AnwPhoneLinkWiringTest {

    @Test
    fun serviceProbeHitEnablesVendorCandidate() {
        val probe = FixedProbe(1)

        val vendor = AnwPhoneLinkWiring.vendorCandidate(probe)

        assertTrue(vendor.isSupported())
        assertEquals("AnwPhoneLink", vendor.getAdapterName())
    }

    @Test
    fun serviceProbeMissKeepsVendorUnsupportedAndManagerReturnsNull() {
        val probe = FixedProbe(0)
        val vendor = AnwPhoneLinkWiring.vendorCandidate(probe)

        assertFalse(vendor.isSupported())
        assertFalse("unresolved service must not report enabled", vendor.isEnabled())

        val native = StubNative(supported = false)
        val picked = CarPlayBluetoothManager.pick(
            nativeCandidate = { native },
            vendorCandidate = { vendor },
        )
        assertNull("probe miss must keep the hybrid gate empty", picked)
    }

    @Test
    fun hybridGatePrefersNativeWhenBothResolve() {
        val native = StubNative(supported = true)
        val vendor = AnwPhoneLinkWiring.vendorCandidate(FixedProbe(1))

        val picked = CarPlayBluetoothManager.pick(
            nativeCandidate = { native },
            vendorCandidate = { vendor },
        )

        assertSame(native, picked)
    }

    @Test
    fun probeAsksForTheAnwPhoneLinkServiceIntent() {
        val recorded = mutableListOf<Pair<String, String>>()
        val probe = object : AnwServiceProbe {
            override fun queryServiceCount(action: String, packageName: String): Int {
                recorded.add(action to packageName)
                return 1
            }
        }

        AnwPhoneLinkWiring.vendorCandidate(probe).isSupported()

        assertEquals(
            listOf("com.anwsdk.service.AnwPhoneLink" to "com.anwsdk.service"),
            recorded,
        )
    }

    @Test
    fun uuidGuidBytesPinTheAnwByteOrder() {
        val bytes = uuidToAnwGuidBytes("00000000-deca-fade-deca-deafdecacafe")

        assertEquals(
            "groups 0-3, 4-5, 6-7 of network order are reversed for the GUID order the vendor API expects",
            listOf<Byte>(0, 0, 0, 0, 0xCA.toByte(), 0xDE.toByte(), 0xDE.toByte(), 0xFA.toByte(), 0xDE.toByte(), 0xCA.toByte(), 0xDE.toByte(), 0xAF.toByte(), 0xDE.toByte(), 0xCA.toByte(), 0xCA.toByte(), 0xFE.toByte()),
            bytes.toList(),
        )
    }

    private class FixedProbe(private val count: Int) : AnwServiceProbe {
        override fun queryServiceCount(action: String, packageName: String): Int = count
    }

    private class StubNative(private val supported: Boolean) : CarPlayBluetooth {
        override fun isSupported(): Boolean = supported
        override fun isEnabled(): Boolean = supported
        override fun enable(): Boolean = supported
        override fun getAdapterName(): String = "native-stub"
        override fun getPairedDevices(): List<CarPlayBluetoothDevice> = emptyList()
        override fun getConnectedDevice(): CarPlayBluetoothDevice? = null
        override fun localAddress(): String? = null
        override fun openRfcommStream(deviceAddress: String, uuid: String) =
            throw IllegalStateException("native path only")
    }
}
