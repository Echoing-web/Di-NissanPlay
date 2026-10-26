package com.shilapi.xcertplay

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.network.NissanDiagnosticsCapture
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 日产诊断入口的负载与生命周期守卫（低配车机资源约束，SDD「低负载与资源约束设计」）：
 * 重复点击只跑一次采集（防 N×4 并发子进程）；Activity 销毁后完成的采集不再弹
 * 对话框（防 WindowManager 泄漏）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34], qualifiers = "en")
class NissanDiagnosticsLifecycleTest {

    private lateinit var activity: DiPlayActivity

    @Test
    fun repeatedTapsRunTheCaptureOnlyOnce() {
        val started = AtomicInteger(0)
        val release = CountDownLatch(1)
        val controller = prepare { started.incrementAndGet(); release.await(10, TimeUnit.SECONDS); NissanDiagnosticsCapture.Outcome.Undetected }
        try {
            val button = nissanButton()
            button.performClick()
            shadowOf(Looper.getMainLooper()).idle()
            button.performClick()
            shadowOf(Looper.getMainLooper()).idle()
            button.performClick()
            shadowOf(Looper.getMainLooper()).idle()

            release.countDown()
            awaitDialog()
            assertEquals("repeated taps must not spawn concurrent captures", 1, started.get())
        } finally {
            release.countDown()
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun outcomeAfterActivityDestroyedIsDropped() {
        val release = CountDownLatch(1)
        val controller = prepare { release.await(10, TimeUnit.SECONDS); NissanDiagnosticsCapture.Outcome.Undetected }
        nissanButton().performClick()
        shadowOf(Looper.getMainLooper()).idle()

        controller.pause().stop().destroy()
        release.countDown()
        val deadline = System.nanoTime() + 2_000_000_000L
        while (System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        val latest = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(
            "no dialog may be shown from a destroyed activity",
            latest == null || !latest.isShowing,
        )
    }

    @Test
    fun captureInProgressDisablesTheButtonUntilItFinishes() {
        val release = CountDownLatch(1)
        val controller = prepare { release.await(10, TimeUnit.SECONDS); NissanDiagnosticsCapture.Outcome.Undetected }
        try {
            val button = nissanButton()
            button.performClick()
            shadowOf(Looper.getMainLooper()).idle()

            assertFalse("the button must show progress by disabling during capture", button.isEnabled)

            release.countDown()
            awaitDialog()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue("the button must be re-enabled after the outcome", button.isEnabled)
        } finally {
            release.countDown()
            controller.pause().stop().destroy()
        }
    }

    private fun prepare(runner: () -> NissanDiagnosticsCapture.Outcome):
        org.robolectric.android.controller.ActivityController<DiPlayActivity> {
        ShadowBuild.setManufacturer("NISSAN MOTOR CO., LTD.")
        val intent = android.content.Intent(
            org.robolectric.RuntimeEnvironment.getApplication(),
            DiPlayActivity::class.java,
        ).putExtra("page", "settings")
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup()
        activity = controller.get()
        ReflectionHelpers.setField(activity, "nissanDiagnosticsRunner", runner as Any)
        ReflectionHelpers.setField(activity, "settingsCategory", SettingsCategory.DIAGNOSTICS)
        ReflectionHelpers.callInstanceMethod<Unit>(activity, "render")
        shadowOf(Looper.getMainLooper()).idle()
        return controller
    }

    private fun nissanButton(): TextView =
        descendants(activity.window.decorView).filterIsInstance<TextView>()
            .firstOrNull { it.text == activity.getString(R.string.settings_nissan_diagnostics) }
            ?: error("Nissan diagnostics button is not rendered on a detected Nissan unit")

    private fun awaitDialog() {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (ShadowAlertDialog.getLatestAlertDialog() == null && System.nanoTime() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }
}
