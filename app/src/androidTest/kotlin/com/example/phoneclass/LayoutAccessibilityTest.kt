package com.example.phoneclass

import android.app.Application
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.ClassroomApp
import com.example.phoneclass.ui.theme.PhoneClassTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File

fun captureAcceptance(name: String) {
    require(name.matches(Regex("[a-z0-9-]+")))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
    var captured: Bitmap? = null
    repeat(3) {
        if (captured == null) {
            captured = instrumentation.uiAutomation.takeScreenshot()
            if (captured == null) android.os.SystemClock.sleep(150)
        }
    }
    // Screenshot collection is diagnostic; functional assertions still run if
    // the device compositor temporarily cannot provide a frame during IME animation.
    val bitmap = captured ?: run {
        val descriptor = instrumentation.uiAutomation.executeShellCommand("mkdir -p /data/local/tmp/phoneclass-captures && screencap -p /data/local/tmp/phoneclass-captures/$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        return
    }
    val file = File(dir, "$name.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    // Keep only this app's generated screenshots after the test harness uninstalls it.
    val shell = instrumentation.uiAutomation.executeShellCommand("mkdir -p /data/local/tmp/phoneclass-captures && run-as ${instrumentation.targetContext.packageName} cat ${file.absolutePath} > /data/local/tmp/phoneclass-captures/$name.png")
    ParcelFileDescriptor.AutoCloseInputStream(shell).use { it.readBytes() }
}

class LayoutAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var model: ClassroomModel
    private fun mount() {
        model = ClassroomModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent {
            val state by model.state.collectAsState()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                PhoneClassTheme {
                    Box(Modifier.sizeIn(maxWidth = 360.dp, maxHeight = 640.dp).fillMaxSize()) { ClassroomApp(state.copy(progress = state.progress.copy(fontLevel = 0)), model) }
                }
            }
        }
        rule.waitUntil(10000) { model.state.value.loaded }
    }
    private fun noClippedText() {
        val nodes = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes()
        rule.runOnIdle {
            nodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.forEach { layout ->
                    // A paragraph may reserve the parent's width while Text wraps its actual
                    // glyphs. Check the rendered line widths rather than the blank reservation.
                    val overflows = layout.didOverflowHeight || (0 until layout.lineCount).any { line ->
                        layout.isLineEllipsized(line) || layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1f
                    }
                    assertFalse("Text clipped at 200%: ${layout.layoutInput.text.text}; size=${layout.size}", overflows)
                }
            }
        }
    }
    @Test fun allLessonsFitSmallScreenAt200PercentFont() {
        mount()
        captureAcceptance("large-font-home")
        noClippedText()
        Lessons.all.forEach { lesson ->
            rule.runOnIdle { model.open(lesson.id) }
            (0..2).forEach { index ->
                noClippedText()
                if (index < 2) rule.onNodeWithTag("next_panel").performClick()
            }
            rule.onNodeWithTag("start_guided").performClick()
            noClippedText()
        }
    }
    @Test fun largeTextControlsRemainReachable() {
        mount()
        rule.runOnIdle { model.open("WIFI"); model.start(Stage.SOLO) }
        listOf("app_settings", "route_wlan").forEach { tag -> rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() } }
        rule.onNodeWithTag("sim_toggle").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("sim_network").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("sim_text_input").performScrollTo().performTextReplacement("12345678")
        rule.onNodeWithTag("sim_text_input").performImeAction()
        rule.onNodeWithTag("sim_connect").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithTag("lesson_complete").assertExists()
        captureAcceptance("large-font-complete")
        noClippedText()
    }
    @Test fun wechatTabsAndRemarkStayUsableAt200Percent() {
        mount()
        fun tap(tag: String) { rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() } }
        rule.runOnIdle { model.open("WX_NAV"); model.start(Stage.SOLO) }
        listOf("contacts", "discover", "me", "chats").forEach { tab -> tap("wx_tab_$tab"); noClippedText() }
        rule.onNodeWithTag("lesson_complete").assertExists()
        rule.runOnIdle { model.open("WX_REMARK"); model.start(Stage.SOLO) }
        tap("wx_tab_contacts"); tap("wx_contact_lin"); tap("wx_remark")
        rule.onNodeWithTag("sim_wx_input").performScrollTo().performTextReplacement("家人小林")
        rule.onNodeWithTag("sim_wx_input").performImeAction()
        tap("wx_save_remark"); tap("wx_back")
        noClippedText()
        rule.onNodeWithText("家人小林").assertExists()
    }
}

