package com.example.phoneclass

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.phoneclass.learning.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Every non-WeChat lesson traverses real Compose controls from the desktop. */
@RunWith(Parameterized::class)
class PhoneJourneyInteractionTest(private val scene: Scene) {
    companion object { @JvmStatic @Parameterized.Parameters(name="{0}") fun scenes() = Scene.entries.filter { it != Scene.WECHAT }.map { arrayOf(it) } }
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun tap(tag: String) { rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() } }
    private fun input(text: String) { rule.onNodeWithTag("sim_text_input").performScrollTo().performTextReplacement(text); rule.onNodeWithTag("sim_text_input").performImeAction() }
    private fun path(page: PhoneScreen) { PhoneCatalog.path(page).forEach { p -> if (p == PhoneScreen.MARKET_RESULTS) input("每日天气"); tap(PhoneCatalog.target(p)) } }
    private fun slider(tag: String,value: Float) { rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(value) } }
    private fun gesture(tag: String, kind: String) {
        rule.onNodeWithTag("phone_frame").performScrollTo()
        val node=rule.onNodeWithTag("sim_$tag").performScrollTo()
        when(kind) {
            "long" -> node.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
            "left" -> node.performTouchInput { swipeLeft(durationMillis=600) }
            "up" -> node.performTouchInput { swipeUp(durationMillis=700) }
            "zoom" -> node.performTouchInput { doubleClick() }
        }
    }
    @Test fun fullDesktopPathPracticeAndEffect() {
        assertTrue(ApplicationProvider.getApplicationContext<Context>().packageName.endsWith(".qa"))
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.open(scene.name); rule.activity.model.start(Stage.SOLO) }
        path(PhoneLessons.entryScreen(scene))
        when(scene) {
            Scene.TAP -> { tap("app_gallery"); tap("nav_home"); gesture("app_gallery","long") }
            Scene.VERTICAL -> { gesture("scroll_list","up"); tap("family_album") }
            Scene.HORIZONTAL -> { gesture("pager","left"); gesture("pager","left") }
            Scene.NAVIGATION -> { tap("open_photo"); tap("nav_back"); tap("nav_home") }
            Scene.MENU -> { tap("more"); tap("settings"); tap("font_entry") }
            Scene.SWITCH -> { tap("toggle"); tap("reset_dialog"); tap("cancel") }
            Scene.WIFI -> { tap("toggle"); tap("network"); input("12345678"); tap("connect") }
            Scene.DATA -> { tap("toggle"); tap("try_network"); tap("nav_back"); tap("nav_back"); tap("route_wlan"); tap("toggle"); tap("try_network") }
            Scene.BUDGET -> { tap("quota_row"); input("2"); tap("quota_confirm"); tap("toggle"); tap("download") }
            Scene.APP_NETWORK -> { tap("toggle"); tap("test_mobile"); tap("test_wifi") }
            Scene.PERMISSION -> { tap("open_scan"); tap("deny"); tap("nav_home"); path(PhoneScreen.CAMERA); tap("allow"); tap("nav_home"); tap("app_safe"); tap("open_scan") }
            Scene.NOTIFICATION -> { tap("toggle"); tap("notify"); tap("toggle"); tap("notify") }
            Scene.SOUND -> { slider("volume",70f); tap("nav_back"); tap("route_display"); slider("brightness",80f) }
            Scene.FONT -> { slider("font_slider",32f); tap("nav_back") }
            Scene.CARE -> { tap("toggle"); tap("nav_home"); path(PhoneScreen.APP_CARE); tap("care_toggle") }
            Scene.INPUT -> { input("写错了"); input(""); input("今天很好"); tap("finish_input") }
            Scene.SEARCH -> { input("相册"); tap("search"); tap("search_result") }
            Scene.CHAT -> { input("平安到家"); tap("send") }
            Scene.PHOTO -> { tap("open_photo"); gesture("photo","zoom"); tap("nav_back") }
            Scene.SEND_PHOTO -> { tap("add_photo"); tap("album"); tap("photo_1"); tap("preview_photo"); tap("nav_back"); tap("send_photo") }
            Scene.SCAN -> { tap("more"); tap("scan"); tap("allow"); tap("scan_code") }
            Scene.INSTALL -> { tap("install"); tap("nav_home"); gesture("app_weather","long"); tap("uninstall"); tap("cancel"); gesture("app_weather","long"); tap("uninstall"); tap("confirm") }
            Scene.WECHAT -> error("Not a phone lesson")
        }
        rule.onNodeWithTag("lesson_complete").assertExists()
    }
}
