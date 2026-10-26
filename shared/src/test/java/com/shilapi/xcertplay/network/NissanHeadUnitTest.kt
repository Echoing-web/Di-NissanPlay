package com.shilapi.xcertplay.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

/** 日产识别只匹配明确的 Nissan 标识，其他车机不得误开日产专用路径。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class NissanHeadUnitTest {

    @Test
    fun defaultRobolectricBuildIsNotDetected() {
        assertFalse(NissanHeadUnit.matchesIdentifiers(emptyList()))
    }

    @Test
    fun nissanManufacturerIsDetected() {
        assertTrue(NissanHeadUnit.matchesIdentifiers(listOf("NISSAN MOTOR CO., LTD.")))
    }

    @Test
    fun nissanBrandIsDetected() {
        assertTrue(NissanHeadUnit.matchesIdentifiers(listOf("nissan")))
    }

    @Test
    fun bydUnitIsNotDetected() {
        assertFalse(NissanHeadUnit.matchesIdentifiers(listOf("BYD AUTO", "DiLink4", "BYD Seal")))
    }

    @Test
    fun geelyUnitIsNotDetected() {
        assertFalse(NissanHeadUnit.matchesIdentifiers(listOf("msmnile_gvmq", "geely_fx11", "CS11_A4", "QUALCOMM")))
    }

    @Test
    fun detectedReadsCurrentBuildFields() {
        ShadowBuild.setManufacturer("NISSAN MOTOR CO., LTD.")
        assertTrue(NissanHeadUnit.detected)
    }
}
