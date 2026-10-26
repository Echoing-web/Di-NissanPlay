package com.shilapi.xcertplay

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.network.NissanDiagnosticsCapture
import com.shilapi.xcertplay.network.NissanDiagnosticsInput
import com.shilapi.xcertplay.network.NissanDiagnosticsReport
import com.shilapi.xcertplay.network.NissanDiagnosticsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers

/**
 * 日产诊断 Outcome UI 分支测试：注入桩采集链路（不执行真实命令），验证
 * CaptureFailed → 重采提示、Undetected → 拒绝说明、Reported → 报告渲染。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34], qualifiers = "en")
class NissanDiagnosticsUiTest {

    @Test
    fun captureFailedShowsRecapturePromptWithSignals() {
        val controller = prepare(
            NissanDiagnosticsCapture.Outcome.CaptureFailed(
                listOf(
                    "Security exception: Must hold permission android.permission.WRITE_SECURE_SETTINGS",
                    "Permission Denial: can't dump DisplayManagerService from pid=1234",
                ),
            ),
        )
        try {
            val dialog = latestDialog()
            val shadow = shadowOf(dialog)
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_recapture_title),
                shadow.title,
            )
            val texts = dialogTexts(dialog)
            assertTrue(texts.any { it.contains("Security exception: Must hold permission") })
            assertTrue(texts.any { it.contains("Permission Denial: can't dump DisplayManagerService") })
            assertFalse(texts.any { it.contains("kind=nissan-headunit-diagnostics") })
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun undetectedShowsRefusalWithoutRecapture() {
        val controller = prepare(NissanDiagnosticsCapture.Outcome.Undetected)
        try {
            val dialog = latestDialog()
            val shadow = shadowOf(dialog)
            assertEquals(activity.getString(R.string.settings_nissan_diagnostics), shadow.title)
            val texts = dialogTexts(dialog)
            assertTrue(
                texts.any { it.contains(activity.getString(R.string.settings_nissan_diagnostics_refusal_message)) },
            )
            assertFalse(
                texts.any { it.contains(activity.getString(R.string.settings_nissan_diagnostics_recapture_title)) },
            )
            assertFalse(texts.any { it.contains("kind=nissan-headunit-diagnostics") })
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun reportedRendersFifteenLines() {
        val report = NissanDiagnosticsReport.from(
            NissanDiagnosticsInput(
                detected = true,
                source = NissanDiagnosticsSource.ON_CAR,
                product = "unit-product",
                device = "unit-device",
                model = "unit-model",
                manufacturer = "NISSAN MOTOR CO., LTD.",
                brand = "nissan",
                buildVersionRelease = "10",
                buildVersionSdk = "29",
                wmSize = "Physical size: 800x1280",
                wmDensity = "Physical density: 320",
                dumpsysDisplay = "DISPLAY MANAGER (dumpsys display)",
            ),
        )!!
        val controller = prepare(NissanDiagnosticsCapture.Outcome.Reported(report))
        try {
            val dialog = latestDialog()
            val shadow = shadowOf(dialog)
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_report_title),
                shadow.title,
            )
            val reportText = dialogTexts(dialog).first { it.contains("kind=nissan-headunit-diagnostics") }
            assertEquals(15, reportText.split('\n').size)
            assertTrue(reportText.contains("ro.product.product=unit-product"))
            assertTrue(reportText.contains("wm.size=Physical size: 800x1280"))
            assertTrue(reportText.contains("dumpsys.display=DISPLAY MANAGER (dumpsys display)"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun nissanButtonHiddenWhenHeadUnitNotDetected() {
        ShadowBuild.setManufacturer("Google")
        val intent = android.content.Intent(
            org.robolectric.RuntimeEnvironment.getApplication(),
            DiPlayActivity::class.java,
        ).putExtra("page", "settings")
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup()
        try {
            activity = controller.get()
            ReflectionHelpers.setField(activity, "settingsCategory", SettingsCategory.DIAGNOSTICS)
            ReflectionHelpers.callInstanceMethod<Unit>(activity, "render")
            shadowOf(Looper.getMainLooper()).idle()
            val buttons = descendants(activity.window.decorView).filterIsInstance<TextView>()
            assertFalse(
                buttons.any { it.text == activity.getString(R.string.settings_nissan_diagnostics) },
            )
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private lateinit var activity: DiPlayActivity

    /** 设置日产标识、注入桩采集链路，打开 Diagnostics 分类并点击日产诊断按钮。 */
    private fun prepare(outcome: NissanDiagnosticsCapture.Outcome):
        org.robolectric.android.controller.ActivityController<DiPlayActivity> {
        ShadowBuild.setManufacturer("NISSAN MOTOR CO., LTD.")
        val intent = android.content.Intent(
            org.robolectric.RuntimeEnvironment.getApplication(),
            DiPlayActivity::class.java,
        ).putExtra("page", "settings")
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup()
        activity = controller.get()
        ReflectionHelpers.setField(
            activity,
            "nissanDiagnosticsRunner",
            { outcome } as () -> NissanDiagnosticsCapture.Outcome,
        )
        ReflectionHelpers.setField(activity, "settingsCategory", SettingsCategory.DIAGNOSTICS)
        ReflectionHelpers.callInstanceMethod<Unit>(activity, "render")
        shadowOf(Looper.getMainLooper()).idle()
        val runButton = descendants(activity.window.decorView).filterIsInstance<TextView>()
            .firstOrNull { it.text == activity.getString(R.string.settings_nissan_diagnostics) }
            ?: error("Nissan diagnostics button is not rendered on a detected Nissan unit")
        runButton.performClick()
        return controller
    }

    private fun latestDialog(): android.app.AlertDialog {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (ShadowAlertDialog.getLatestAlertDialog() == null && System.nanoTime() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        return requireNotNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    private fun dialogTexts(dialog: android.app.AlertDialog): List<String> =
        descendants(dialog.window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }.toList()

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }
}
