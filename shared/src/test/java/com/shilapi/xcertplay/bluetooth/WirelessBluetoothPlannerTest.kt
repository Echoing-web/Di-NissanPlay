package com.shilapi.xcertplay.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 无线蓝牙混合门规划器测试（纯 JVM）：原生在场永远优先；原生缺失且厂商
 * 服务可达走桥接；两者皆无返回 None（调用方 fail fast——NTDA3 家族无系统
 * 蓝牙时的核心场景）。
 */
class WirelessBluetoothPlannerTest {

    @Test
    fun nativeAdapterPresentAlwaysWins() {
        val vendor = supportedVendor()

        val plan = WirelessBluetoothPlanner.plan(nativeAdapterPresent = true, vendorBridge = vendor)

        assertTrue(plan is WirelessBluetoothPlan.Native)
    }

    @Test
    fun missingNativeWithVendorSupportBridges() {
        val vendor = supportedVendor()

        val plan = WirelessBluetoothPlanner.plan(nativeAdapterPresent = false, vendorBridge = vendor)

        assertTrue(plan is WirelessBluetoothPlan.Vendor)
        assertEquals(vendor, (plan as WirelessBluetoothPlan.Vendor).bridge)
    }

    @Test
    fun missingNativeWithoutVendorSupportIsNone() {
        val vendor = AnwPhoneLinkBluetoothAdapter(resolver = { false }, channelFactory = { error("no bridge") })

        val plan = WirelessBluetoothPlanner.plan(nativeAdapterPresent = false, vendorBridge = vendor)

        assertEquals(WirelessBluetoothPlan.None, plan)
    }

    @Test
    fun missingNativeAndMissingBridgeIsNone() {
        val plan = WirelessBluetoothPlanner.plan(nativeAdapterPresent = false, vendorBridge = null)

        assertEquals(WirelessBluetoothPlan.None, plan)
    }

    @Test
    fun vendorProbeFailureDegradesToNone() {
        val throwing = AnwPhoneLinkBluetoothAdapter(
            resolver = { error("binder probe crashed") },
            channelFactory = { error("no bridge") },
        )

        val plan = WirelessBluetoothPlanner.plan(nativeAdapterPresent = false, vendorBridge = throwing)

        assertEquals("a crashing vendor probe must not break the gate", WirelessBluetoothPlan.None, plan)
    }

    private fun supportedVendor(): AnwPhoneLinkBluetoothAdapter =
        AnwPhoneLinkBluetoothAdapter(resolver = { true }, channelFactory = { error("not opened here") })
}
