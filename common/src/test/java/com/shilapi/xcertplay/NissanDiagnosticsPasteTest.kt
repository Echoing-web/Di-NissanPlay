package com.shilapi.xcertplay

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * 日产诊断粘贴转录入口（owner-reported，PRD P0-1）：车主把 PC 上用 ADB 采集的
 * 带分区标签转录粘贴进多行输入框，解析走 NissanDiagnosticsTextParser 并复用既有
 * 三分支 Outcome 对话框。带标签的完整转录 → 15 行报告；裸 dumpsys（无标签）→
 * 字段 unknown 不误采集；空白粘贴 → 不弹报告；detected=false → 拒绝说明。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 34], qualifiers = "en")
class NissanDiagnosticsPasteTest {

    private lateinit var activity: DiPlayActivity

    @Test
    fun pastedFullTranscriptReportsFifteenLinesAsOwnerReported() {
        val controller = prepareDetected()
        try {
            val pasteDialog = pasteAndConfirm(fullTranscript())

            val dialog = latestDialog(after = pasteDialog)
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_report_title),
                shadowOf(dialog).title,
            )
            val reportText = dialogTexts(dialog).first { it.contains("kind=nissan-headunit-diagnostics") }
            assertEquals(15, reportText.split('\n').size)
            assertTrue("pasted transcripts are owner-reported", reportText.contains("source=owner-reported"))
            assertTrue(reportText.contains("ro.product.manufacturer=NISSAN MOTOR CO., LTD."))
            assertTrue(reportText.contains("wm.size=Physical size: 800x1280"))
            assertTrue(reportText.contains("dumpsys.display=DISPLAY MANAGER (dumpsys display)"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun pastedBareDumpsysWithoutLabelsKeepsDumpsysUnknown() {
        val controller = prepareDetected()
        try {
            val pasteDialog =
                pasteAndConfirm("DISPLAY MANAGER (dumpsys display)\n  mGlobalDisplayState=ON")

            val dialog = latestDialog(after = pasteDialog)
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_report_title),
                shadowOf(dialog).title,
            )
            val reportText = dialogTexts(dialog).first { it.contains("kind=nissan-headunit-diagnostics") }
            assertTrue(
                "dumpsys output without a section label line must not be captured",
                reportText.contains("dumpsys.display=unknown"),
            )
            assertFalse(reportText.contains("mGlobalDisplayState"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun blankPasteDoesNotOpenAnyReportDialog() {
        val controller = prepareDetected()
        try {
            pasteAndConfirm("   \n \n ")

            // 空白粘贴不弹报告：输入对话框保留供重试，且没有任何报告文本。
            val latest = ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(latest)
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_paste_dialog_title),
                shadowOf(latest).title,
            )
            assertTrue("the paste input dialog stays open for retry", latest!!.isShowing)
            assertFalse(dialogTexts(latest).any { it.contains("kind=nissan-headunit-diagnostics") })
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun pasteRefusedWhenHeadUnitNotDetected() {
        val controller = prepareDetected()
        try {
            val pasteDialog = pasteAndConfirm(fullTranscript(), beforeConfirm = {
                // 对话框已打开后识别失效：解析时 detected=false → 拒绝说明，不生成报告。
                ShadowBuild.setManufacturer("Google")
            })

            val dialog = latestDialog(after = pasteDialog)
            assertEquals(activity.getString(R.string.settings_nissan_diagnostics), shadowOf(dialog).title)
            val texts = dialogTexts(dialog)
            assertTrue(
                texts.any { it.contains(activity.getString(R.string.settings_nissan_diagnostics_refusal_message)) },
            )
            assertFalse(texts.any { it.contains("kind=nissan-headunit-diagnostics") })
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun pasteButtonHiddenWhenHeadUnitNotDetected() {
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
                buttons.any { it.text == activity.getString(R.string.settings_nissan_diagnostics_paste) },
            )
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun pasteDialogOffersMultiLineMonospaceInputWithLabelGuidance() {
        val controller = prepareDetected()
        try {
            val dialog = openPasteDialog()
            assertEquals(
                activity.getString(R.string.settings_nissan_diagnostics_paste_dialog_title),
                shadowOf(dialog).title,
            )
            val input = inputOf(dialog)
            assertTrue(
                "the transcript box must accept multi-line text",
                input.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0,
            )
            assertEquals(Typeface.MONOSPACE, input.typeface)
            val texts = dialogTexts(dialog)
            assertTrue(
                "the guidance must name the section label lines",
                texts.any { it.contains("wm size:") && it.contains("wm density:") && it.contains("dumpsys display:") },
            )
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun fullTranscript() = listOf(
        "[ro.product.product]: [unit-product]",
        "[ro.product.device]: [unit-device]",
        "[ro.product.model]: [unit-model]",
        "[ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]",
        "[ro.product.brand]: [nissan]",
        "[ro.build.version.release]: [10]",
        "[ro.build.version.sdk]: [29]",
        "wm size:",
        "Physical size: 800x1280",
        "wm density:",
        "Physical density: 320",
        "dumpsys display:",
        "DISPLAY MANAGER (dumpsys display)",
        "  mGlobalDisplayState=ON",
    ).joinToString("\n")

    /** 打开粘贴对话框（可选在确认前改变环境），填入文本并点击确认；返回输入对话框。 */
    private fun pasteAndConfirm(text: String, beforeConfirm: () -> Unit = {}): AlertDialog {
        val pasteDialog = openPasteDialog()
        beforeConfirm()
        inputOf(pasteDialog).setText(text)
        pasteDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        return pasteDialog
    }

    private fun openPasteDialog(): AlertDialog {
        pasteButton().performClick()
        shadowOf(Looper.getMainLooper()).idle()
        return requireNotNull(ShadowAlertDialog.getLatestAlertDialog()) {
            "clicking the paste button must open the transcript input dialog"
        }
    }

    /** 设置日产标识并打开 Diagnostics 分类。 */
    private fun prepareDetected():
        org.robolectric.android.controller.ActivityController<DiPlayActivity> {
        ShadowBuild.setManufacturer("NISSAN MOTOR CO., LTD.")
        val intent = android.content.Intent(
            org.robolectric.RuntimeEnvironment.getApplication(),
            DiPlayActivity::class.java,
        ).putExtra("page", "settings")
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup()
        activity = controller.get()
        ReflectionHelpers.setField(activity, "settingsCategory", SettingsCategory.DIAGNOSTICS)
        ReflectionHelpers.callInstanceMethod<Unit>(activity, "render")
        shadowOf(Looper.getMainLooper()).idle()
        return controller
    }

    private fun pasteButton(): TextView =
        descendants(activity.window.decorView).filterIsInstance<TextView>()
            .firstOrNull { it.text == activity.getString(R.string.settings_nissan_diagnostics_paste) }
            ?: error("Paste transcript button is not rendered on a detected Nissan unit")

    private fun inputOf(dialog: AlertDialog): EditText =
        descendants(dialog.window!!.decorView).filterIsInstance<EditText>().single()

    /** 等待确认后弹出的新对话框（跳过仍为“最新”的粘贴输入框）。 */
    private fun latestDialog(after: AlertDialog?): AlertDialog {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < deadline) {
            val latest = ShadowAlertDialog.getLatestAlertDialog()
            if (latest != null && latest !== after) return latest
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        error("The paste confirmation must open an outcome dialog")
    }

    private fun dialogTexts(dialog: AlertDialog): List<String> =
        descendants(dialog.window!!.decorView).filterIsInstance<TextView>().map { it.text.toString() }.toList()

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }
}
