package com.shilapi.xcertplay.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

/**
 * The Nissan Connect detection matches only identifiers that say Nissan. The default
 * Robolectric build, BYD units and the measured Geely unit stay undetected, so no Nissan
 * route can turn on for them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class NissanHeadUnitTest {

    @Test
    fun defaultRobolectricBuildIsNotDetected() {
        assertFalse(NissanHeadUnit.detected)
    }

    @Test
    fun nissanManufacturerIsDetected() {
        ShadowBuild.setManufacturer("NISSAN MOTOR CO., LTD.")
        assertTrue(NissanHeadUnit.detected)
    }

    @Test
    fun nissanBrandIsDetected() {
        ShadowBuild.setBrand("nissan")
        assertTrue(NissanHeadUnit.detected)
    }

    @Test
    fun bydUnitIsNotDetected() {
        ShadowBuild.setManufacturer("BYD AUTO")
        ShadowBuild.setProduct("DiLink4")
        ShadowBuild.setModel("BYD Seal")
        assertFalse(NissanHeadUnit.detected)
    }

    @Test
    fun geelyUnitIsNotDetected() {
        ShadowBuild.setDevice("msmnile_gvmq")
        ShadowBuild.setProduct("geely_fx11")
        ShadowBuild.setModel("CS11_A4")
        ShadowBuild.setManufacturer("QUALCOMM")
        assertFalse(NissanHeadUnit.detected)
    }
}
