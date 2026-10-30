package com.example.phoneclass

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.example.phoneclass.learning.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ClassroomInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun open(scene: Scene, stage: Stage = Stage.SOLO) {
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.open(scene.name); rule.activity.model.start(stage) }
        path(PhoneLessons.entryScreen(scene))
    }
    private fun path(screen: PhoneScreen) { PhoneCatalog.path(screen).forEach { page -> if (page == PhoneScreen.MARKET_RESULTS) input("每日天气"); tap(PhoneCatalog.target(page)) } }
    private fun tap(tag: String) { rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() } }
    private fun input(value: String) {
        rule.onNodeWithTag("sim_text_input").performScrollTo().performTextReplacement(value)
        rule.onNodeWithTag("sim_text_input").performImeAction()
        rule.waitForIdle()
    }
    private fun done() {
        rule.runOnIdle { assertEquals("Unexpected practice state: ${rule.activity.model.state.value}", Stage.DONE, rule.activity.model.state.value.stage) }
        rule.onNodeWithTag("lesson_complete").assertExists()
    }
    @Test fun wifiIncorrectPasswordThenSuccessfulConnection() {
        open(Scene.WIFI); tap("toggle"); tap("network"); input("00000000"); tap("connect")
        rule.onNodeWithTag("feedback").assertTextContains("密码不匹配", substring = true)
        rule.onNodeWithTag("lesson_complete").assertDoesNotExist()
        input("12345678"); tap("connect"); done()
    }
    @Test fun cameraDenialAndRecovery() {
        open(Scene.PERMISSION); tap("open_scan")
        tap("deny")
        tap("open_scan")
        rule.onNodeWithTag("feedback").assertTextContains("不可用", substring = true)
        tap("nav_home"); path(PhoneScreen.CAMERA); tap("allow"); tap("nav_home"); tap("app_safe"); tap("open_scan"); done()
    }
    @Test fun budgetStopsAtLimit() {
        open(Scene.BUDGET); tap("quota_row"); input("2"); tap("quota_confirm"); tap("toggle"); tap("download"); done()
        rule.runOnIdle { assertEquals(2.1f, rule.activity.model.state.value.practice.used, .001f) }
    }
    @Test fun appRestrictionBlocksMobileAndAllowsWifi() {
        open(Scene.APP_NETWORK); tap("toggle"); tap("test_mobile")
        rule.onNodeWithTag("feedback").assertTextContains("访问被阻止", substring = true)
        tap("test_wifi"); done()
    }
    @Test fun fontPreviewAndGuidedToIndependentReset() {
        open(Scene.FONT, Stage.GUIDED)
        rule.onNodeWithTag("sim_font_slider").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(32f) }
        rule.onNodeWithTag("font_preview").assertTextEquals("今天的天气很好，适合出去走走。")
        tap("nav_back")
        rule.onNodeWithTag("guided_done").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(Stage.SOLO, rule.activity.model.state.value.stage); assertEquals(22f, rule.activity.model.state.value.practice.value) }
        path(PhoneScreen.FONT)
        rule.onNodeWithTag("sim_font_slider").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(34f) }
        tap("nav_back"); done()
    }
    @Test fun selectAndSendPhoto() {
        open(Scene.SEND_PHOTO); tap("add_photo"); tap("album"); tap("photo_1")
        rule.onNodeWithTag("lesson_complete").assertDoesNotExist()
        tap("send_photo"); done()
        rule.runOnIdle { assertEquals(1, rule.activity.model.state.value.practice.selected) }
    }
    @Test fun restartRestoresInitialScene() {
        open(Scene.WIFI); tap("toggle"); tap("network"); input("123")
        rule.onNodeWithTag("restart").performClick()
        rule.runOnIdle {
            val s = rule.activity.model.state.value.practice
            assertEquals("", s.text); assertFalse(s.enabled); assertTrue(s.achieved.isEmpty())
        }
    }
    @Test fun simulatedHomeAndRecentTasksPreserveConnectionAndPage() {
        open(Scene.WIFI, Stage.GUIDED); tap("toggle"); tap("network"); input("12345678"); tap("connect")
        tap("nav_home"); tap("nav_recents"); tap("recent_SETTINGS")
        rule.runOnIdle { val s=rule.activity.model.state.value.practice; assertTrue(s.connected); assertEquals(PhoneScreen.WLAN,s.phone!!.screen) }
        rule.onNodeWithText("已连接 · 已加密").assertExists()
        rule.onNodeWithTag("restart").performClick()
        rule.runOnIdle { val s=rule.activity.model.state.value.practice; assertFalse(s.connected); assertEquals(PhoneScreen.HOME,s.phone!!.screen) }
    }
    @Test fun backgroundAndReturnRetainsCurrentPractice() {
        open(Scene.WIFI); tap("toggle"); tap("network"); input("123")
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.onNodeWithTag("sim_text_input").assertTextContains("123")
        rule.runOnIdle { assertEquals(PhoneOverlay.WIFI_PASSWORD, rule.activity.model.state.value.practice.phone!!.overlay) }
    }
    @Test fun all34LessonsAnd102PicturesRender() {
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        Lessons.all.forEach { lesson ->
            rule.runOnIdle { rule.activity.model.open(lesson.id) }
            (0..2).forEach { index ->
                rule.onNodeWithText(lesson.panels[index].title).assertExists()
                if (lesson.scene == Scene.WIFI && index == 0) captureAcceptance("wifi-illustration")
                if (index < 2) rule.onNodeWithTag("next_panel").performClick()
            }
            rule.onNodeWithTag("start_guided").performClick()
            rule.onNodeWithText("练习页面", substring = true).assertExists()
        }
    }
    @Test fun realTouchGesturesDrivePractice() {
        open(Scene.TAP)
        tap("app_gallery"); tap("nav_home")
        rule.onNodeWithTag("phone_frame").performScrollTo()
        rule.onNodeWithTag("sim_app_gallery").performScrollTo().performTouchInput { longClick() }
        done()
        open(Scene.VERTICAL)
        rule.onNodeWithTag("sim_scroll_list").performScrollTo().performTouchInput { swipeUp(durationMillis = 800) }
        rule.onNodeWithTag("sim_family_album").performScrollTo().performClick()
        done()
        open(Scene.HORIZONTAL)
        rule.onNodeWithTag("phone_frame").performScrollTo()
        repeat(2) { rule.onNodeWithTag("sim_pager").performScrollTo().performTouchInput { swipeLeft(durationMillis = 600) } }
        done()
        open(Scene.PHOTO)
        tap("open_photo")
        rule.onNodeWithTag("phone_frame").performScrollTo()
        rule.onNodeWithTag("sim_photo").performScrollTo().performTouchInput { doubleClick() }
        tap("nav_back"); done()
    }
    @Test fun progressAndFontAreSavedAndResumeResetsOnlySimulation() {
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.open("WIFI"); rule.activity.model.panel(1); rule.activity.model.font(1) }
        rule.waitUntil(10000) { rule.activity.model.state.value.progress.recent == "WIFI" && rule.activity.model.state.value.progress.panel == 1 && rule.activity.model.state.value.progress.fontLevel == 1 }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.home(); rule.activity.model.open("WIFI", resume = true) }
        rule.runOnIdle { assertEquals(1, rule.activity.model.state.value.panel); assertEquals(1, rule.activity.model.state.value.progress.fontLevel); assertFalse(rule.activity.model.state.value.practice.connected) }
        rule.runOnIdle { rule.activity.model.font(0) }
    }
}


