package com.example.phoneclass.learning

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

fun click(target: String) = PracticeEvent.Click(target)
fun input(value: String) = PracticeEvent.Input(value)
fun gesture(kind: String, amount: Float = 1f) = PracticeEvent.Gesture(kind, amount)
fun adjust(target: String, value: Float) = PracticeEvent.Adjust(target, value)
fun phonePath(screen: PhoneScreen): List<PracticeEvent> = PhoneCatalog.path(screen).flatMap { page ->
    if (page == PhoneScreen.MARKET_RESULTS) listOf(input("每日天气"), click("market_search")) else listOf(click(PhoneCatalog.target(page)))
}
fun successfulJourney(scene: Scene): List<PracticeEvent> = phonePath(PhoneLessons.entryScreen(scene)) + when(scene) {
    Scene.WECHAT -> error("Use WeChat IDs")
    Scene.TAP -> listOf(click("app_gallery"), click("nav_home"), gesture("long"))
    Scene.VERTICAL -> listOf(gesture("scroll"), click("family_album"))
    Scene.HORIZONTAL -> listOf(gesture("left"), gesture("left"))
    Scene.NAVIGATION -> listOf(click("open_photo"), click("nav_back"), click("nav_home"))
    Scene.MENU -> listOf(click("more"), click("settings"), click("font_entry"))
    Scene.SWITCH -> listOf(click("toggle"), click("reset_dialog"), click("cancel"))
    Scene.WIFI -> listOf(click("toggle"), click("network"), input("12345678"), click("connect"))
    Scene.DATA -> listOf(click("toggle"), click("try_network"), click("nav_back"), click("nav_back"), click("route_wlan"), click("toggle"), click("try_network"))
    Scene.BUDGET -> listOf(click("quota_row"), input("2"), click("quota_confirm"), click("toggle"), click("download"))
    Scene.APP_NETWORK -> listOf(click("toggle"), click("test_mobile"), click("test_wifi"))
    Scene.PERMISSION -> listOf(click("open_scan"), click("deny"), click("nav_home")) + phonePath(PhoneScreen.CAMERA) + listOf(click("allow"), click("nav_home"), click("app_safe"), click("open_scan"))
    Scene.NOTIFICATION -> listOf(click("toggle"), click("notify"), click("toggle"), click("notify"))
    Scene.SOUND -> listOf(adjust("volume",70f), click("nav_back"), click("route_display"), adjust("brightness",80f))
    Scene.FONT -> listOf(adjust("font_slider",32f), click("nav_back"))
    Scene.CARE -> listOf(click("toggle"), click("nav_home")) + phonePath(PhoneScreen.APP_CARE) + click("care_toggle")
    Scene.INPUT -> listOf(input("写错了"), input(""), input("今天很好"), click("finish_input"))
    Scene.SEARCH -> listOf(input("相册"), click("search"), click("search_result"))
    Scene.CHAT -> listOf(input("平安到家"), click("send"))
    Scene.PHOTO -> listOf(click("open_photo"), gesture("zoom",2f), click("nav_back"))
    Scene.SEND_PHOTO -> listOf(click("add_photo"), click("album"), click("photo_1"), click("preview_photo"), click("send_photo"))
    Scene.SCAN -> listOf(click("more"), click("scan"), click("allow"), click("scan_code"))
    Scene.INSTALL -> listOf(click("install"), click("nav_home"), gesture("long_weather"), click("uninstall"), click("confirm"))
}

@RunWith(Parameterized::class)
class LessonJourneyTest(private val scene: Scene) {
    companion object { @JvmStatic @Parameterized.Parameters(name="{0}") fun scenes() = Scene.entries.filter { it != Scene.WECHAT }.map { arrayOf(it) } }
    @Test fun completePathAndEffectAreRequired() {
        val lesson = Lessons.get(scene.name)
        var s = PracticeEngine.initial(scene)
        assertEquals(PhoneScreen.HOME,s.phone!!.screen)
        val journey = successfulJourney(scene)
        journey.forEachIndexed { index,event ->
            s = PracticeEngine.reduce(scene,s,event)
            if (index < journey.lastIndex) assertFalse("Premature completion: $scene at $event",PracticeEngine.complete(lesson,s))
        }
        assertTrue("Missing ${lesson.goals.map { it.key }.filterNot { it in s.achieved }} in $scene: $s",PracticeEngine.complete(lesson,s))
    }
    @Test fun restartClearsOnlySessionAndReturnsToDesktop() {
        val final = successfulJourney(scene).fold(PracticeEngine.initial(scene)) { s,e -> PracticeEngine.reduce(scene,s,e) }
        assertTrue(PracticeEngine.complete(Lessons.get(scene.name),final))
        val reset = PracticeEngine.initial(scene)
        assertEquals(PhoneState(), reset.phone)
        assertTrue(reset.achieved.isEmpty()); assertTrue(reset.messages.isEmpty()); assertEquals("",reset.text)
        assertFalse(PracticeEngine.complete(Lessons.get(scene.name),reset))
    }
}

class PracticeEngineTest {
    private fun run(scene: Scene, vararg events: PracticeEvent): PracticeState = (phonePath(PhoneLessons.entryScreen(scene)) + events).fold(PracticeEngine.initial(scene)) { s,e -> PracticeEngine.reduce(scene,s,e) }
    private fun apply(scene: Scene, s: PracticeState, vararg events: PracticeEvent) = events.fold(s) { state,e -> PracticeEngine.reduce(scene,state,e) }
    @Test fun cataloguePreservesIdsAnd102Pictures() {
        assertEquals(34,Lessons.all.size); assertEquals(102,Lessons.all.sumOf { it.panels.size }); assertEquals(34,Lessons.all.map { it.id }.toSet().size)
        Scene.entries.filter { it != Scene.WECHAT }.forEach { assertEquals(it,Lessons.get(it.name).scene) }
        Lessons.all.forEach { assertEquals(it.goals.size,it.goals.map { g -> g.key }.toSet().size); assertTrue(it.panels.all { p -> p.body.length > 15 }) }
    }
    @Test fun wrongPageCannotChangeSettingsOrInventAchievements() {
        listOf(Scene.WIFI,Scene.PERMISSION,Scene.FONT,Scene.INSTALL,Scene.BUDGET).forEach { scene ->
            val before=PracticeEngine.initial(scene)
            val after=apply(scene,before,click("toggle"),click("allow"),click("install"),adjust("font_slider",32f),click("connect"),input("12345678"))
            assertTrue(after.achieved.isEmpty()); assertEquals(before.enabled,after.enabled); assertEquals(before.value,after.value); assertNull(after.permission); assertFalse(after.installed); assertEquals("",after.text)
        }
    }
    @Test fun wrongWifiPasswordAndCancelCanRetry() {
        var s=run(Scene.WIFI,click("toggle"),click("network"),input("000"),click("connect"))
        assertFalse(s.connected); assertEquals(PhoneOverlay.WIFI_PASSWORD,s.phone!!.overlay); assertTrue(s.feedback.contains("密码不匹配"))
        s=apply(Scene.WIFI,s,click("cancel"),click("network"),input("12345678"),click("connect"))
        assertTrue(PracticeEngine.complete(Lessons.get("WIFI"),s)); assertEquals("",s.text)
        s=apply(Scene.WIFI,s,click("toggle")); assertFalse(s.connected); assertFalse(PracticeEngine.complete(Lessons.get("WIFI"),s))
    }
    @Test fun cannotConnectWhileWlanOff() { val s=run(Scene.WIFI,click("network"),input("12345678"),click("connect")); assertFalse(s.connected); assertNull(s.phone!!.overlay) }
    @Test fun overlayBlocksUnderlyingOperations() {
        val s=run(Scene.WIFI,click("toggle"),click("network"),click("toggle"),click("route_display"))
        assertTrue(s.enabled); assertEquals(PhoneScreen.WLAN,s.phone!!.screen); assertEquals(PhoneOverlay.WIFI_PASSWORD,s.phone.overlay)
    }
    @Test fun homeAndRecentsPreserveConnectionAndPage() {
        var s=run(Scene.WIFI,click("toggle"),click("network"),input("12345678"),click("connect"),click("nav_home"),click("nav_recents"))
        assertTrue(s.connected); assertEquals(listOf(PhoneApp.SETTINGS),s.phone!!.recentApps)
        s=apply(Scene.WIFI,s,click("recent_SETTINGS")); assertEquals(PhoneScreen.WLAN,s.phone!!.screen); assertTrue(s.connected)
        s=apply(Scene.WIFI,s,click("nav_back")); assertEquals(PhoneScreen.SETTINGS,s.phone!!.screen); assertTrue(s.connected)
    }
    @Test fun deniedCameraCannotScanAndFullSettingsPathRestores() {
        var s=run(Scene.PERMISSION,click("open_scan"),click("deny"),click("open_scan"))
        assertEquals(false,s.permission); assertEquals(PhoneScreen.SAFE,s.phone!!.screen); assertTrue(s.feedback.contains("不可用"))
        s=apply(Scene.PERMISSION,s,click("nav_home"))
        s=phonePath(PhoneScreen.CAMERA).fold(s) { state,e -> PracticeEngine.reduce(Scene.PERMISSION,state,e) }
        s=apply(Scene.PERMISSION,s,click("allow"),click("nav_home"),click("app_safe"),click("open_scan"))
        assertTrue(PracticeEngine.complete(Lessons.get("PERMISSION"),s))
    }
    @Test fun wrongAllowFirstCanRecoverByDenialInSettings() {
        var s=run(Scene.PERMISSION,click("open_scan"),click("allow"),click("nav_home"))
        s=phonePath(PhoneScreen.CAMERA).fold(s) { state,e -> PracticeEngine.reduce(Scene.PERMISSION,state,e) }
        s=apply(Scene.PERMISSION,s,click("deny"),click("allow"),click("nav_home"),click("app_safe"),click("nav_back"),click("open_scan"))
        assertTrue(PracticeEngine.complete(Lessons.get("PERMISSION"),s))
    }
    @Test fun appRestrictionLeavesWlanIndependent() { val s=run(Scene.APP_NETWORK,click("toggle"),click("test_mobile"),click("test_wifi")); assertFalse(s.enabled); assertTrue(s.secondary); assertTrue(PracticeEngine.complete(Lessons.get("APP_NETWORK"),s)) }
    @Test fun quotaNeedsBothSwitchAndLimitAndBlocksFurtherDownload() {
        val without=run(Scene.BUDGET,click("quota_row"),input("2"),click("quota_confirm"),click("download")); assertFalse("blocked" in without.achieved)
        val s=run(Scene.BUDGET,click("quota_row"),input("bad"),click("quota_confirm")); assertEquals(0,s.budget); assertEquals(PhoneOverlay.QUOTA,s.phone!!.overlay)
        val limited=apply(Scene.BUDGET,s,input("2"),click("quota_confirm"),click("toggle"),click("download"),click("download")); assertEquals(2.1f,limited.used,.001f); assertTrue(limited.feedback.contains("停止"))
    }
    @Test fun notificationOffReceivesMessagesWithoutBannerAndOnRestores() {
        val off=run(Scene.NOTIFICATION,click("toggle"),click("notify")); assertEquals(1,off.messages.size); assertFalse(off.phone!!.notificationBanner)
        val on=apply(Scene.NOTIFICATION,off,click("toggle"),click("notify")); assertEquals(2,on.messages.size); assertTrue(on.phone!!.notificationBanner); assertTrue(PracticeEngine.complete(Lessons.get("NOTIFICATION"),on))
    }
    @Test fun overLimitMistakeCanRecoverByEnablingStopAfterDownload() {
        val s=run(Scene.BUDGET,click("quota_row"),input("2"),click("quota_confirm"),click("download"),click("toggle"),click("download"))
        assertEquals(2.1f,s.used,.001f); assertTrue(PracticeEngine.complete(Lessons.get("BUDGET"),s))
    }
    @Test fun fontIsImmediateAndBackObservesEffectWithoutApplyButton() {
        val s=run(Scene.FONT,adjust("font_slider",32f)); assertEquals(32f,s.value); assertFalse(PracticeEngine.complete(Lessons.get("FONT"),s))
        assertTrue(PracticeEngine.complete(Lessons.get("FONT"),apply(Scene.FONT,s,click("nav_back"))))
    }
    @Test fun draftIsSeparateFromSentMessage() { val draft=run(Scene.CHAT,input("平安到家")); assertTrue(draft.messages.isEmpty()); val sent=apply(Scene.CHAT,draft,click("send")); assertEquals(listOf("平安到家"),sent.messages); assertEquals("",sent.text) }
    @Test fun photoSelectionPreviewCancelAndSend() {
        var s=run(Scene.SEND_PHOTO,click("add_photo"),click("album"),click("send_photo")); assertFalse(s.secondary)
        s=apply(Scene.SEND_PHOTO,s,click("photo_2"),click("preview_photo"),click("nav_back")); assertEquals(2,s.selected); assertFalse(s.secondary)
        s=apply(Scene.SEND_PHOTO,s,click("photo_2")); assertNull(s.selected)
        s=apply(Scene.SEND_PHOTO,s,click("photo_1"),click("preview_photo"),click("send_photo")); assertTrue(PracticeEngine.complete(Lessons.get("SEND_PHOTO"),s))
    }
    @Test fun installationRequiresDesktopLongPressAndCancelRetainsIcon() {
        var s=run(Scene.INSTALL,click("install"),click("uninstall")); assertTrue(s.installed); assertNull(s.phone!!.overlay)
        s=apply(Scene.INSTALL,s,click("nav_home"),gesture("long_weather"),click("uninstall"),click("cancel")); assertTrue(s.installed)
        s=apply(Scene.INSTALL,s,gesture("long_weather"),click("uninstall"),click("confirm")); assertFalse(s.installed); assertTrue(PracticeEngine.complete(Lessons.get("INSTALL"),s))
    }
    @Test fun wrongSearchReturnsToEditAndCanRetry() { var s=run(Scene.SEARCH,input("不存在"),click("search")); assertFalse(s.secondary); s=apply(Scene.SEARCH,s,click("nav_back"),input("相册"),click("search"),click("search_result")); assertTrue(PracticeEngine.complete(Lessons.get("SEARCH"),s)) }
    @Test fun zoomAndDraftSurviveHomeAndRecentTask() {
        val s=run(Scene.PHOTO,click("open_photo"),gesture("zoom",2f),click("nav_home"),click("nav_recents"),click("recent_GALLERY")); assertEquals(2f,s.zoom); assertEquals(PhoneScreen.PHOTO,s.phone!!.screen)
        val draft=run(Scene.CHAT,input("未发出的字"),click("nav_home"),click("app_community")); assertEquals("未发出的字",draft.text); assertTrue(draft.messages.isEmpty()); assertEquals(PhoneScreen.CHAT,draft.phone!!.screen)
    }
    @Test fun allowedPagesAndPreviewStatesCoverAllNonWechatLessons() {
        Scene.entries.filter { it != Scene.WECHAT }.forEach { scene -> (0..2).forEach { i -> val s=PhonePreviews.state(scene,i); assertNotNull(s.phone); assertTrue("$scene $i",s.phone!!.screen in PhoneLessons.allowed(scene)) } }
    }
}

