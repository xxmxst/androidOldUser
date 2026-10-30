package com.example.phoneclass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.phoneclass.learning.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WechatInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun open(id: String) {
        assertTrue("Automated tests must never target the installed family app", ApplicationProvider.getApplicationContext<Context>().packageName.endsWith(".qa"))
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.open(id); rule.activity.model.start(Stage.SOLO) }
    }
    private fun tap(tag: String) {
        // Accessibility click avoids a coordinate becoming stale during native IME animation.
        // Separate gesture tests below exercise actual touch input.
        rule.onNodeWithTag("sim_$tag").performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
    }
    private fun input(text: String) {
        rule.onNodeWithTag("sim_wx_input").performScrollTo().performTextReplacement(text)
        rule.onNodeWithTag("sim_wx_input").performImeAction()
    }
    private fun back() = tap("wx_back")
    private fun done() { rule.onNodeWithTag("lesson_complete").assertExists() }
    private fun wx() = rule.activity.model.state.value.practice.wechat!!

    @Test fun featuredEntryOpensTwelveLessonCatalogue() {
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.onNodeWithTag("wechat_featured").performScrollTo().performClick()
        rule.onNodeWithTag("wechat_catalog").assertExists()
        rule.onNodeWithTag("wechat_catalog").performScrollToNode(hasTestTag("lesson_WX_MUTE"))
        rule.onNodeWithTag("lesson_WX_MUTE").performClick()
        rule.onNodeWithText("设置群消息免打扰").assertExists()
    }
    @Test fun fourTabsReturnToChatList() {
        open("WX_NAV")
        tap("wx_tab_contacts"); tap("wx_tab_discover"); tap("wx_tab_me"); tap("wx_tab_chats"); done()
    }
    @Test fun searchContactAndVerifyProfile() {
        open("WX_CONTACT"); tap("wx_tab_contacts"); tap("wx_search")
        input("不存在"); tap("wx_do_search")
        rule.runOnIdle { assertTrue(wx().contactMatches().isEmpty()) }
        input("海阔天空"); tap("wx_do_search"); tap("wx_contact_lin")
        rule.onNodeWithText("对方原昵称：海阔天空", substring = true).assertExists()
        tap("wx_message"); done()
    }
    @Test fun remarkCancelSaveAndMultiPageEffects() {
        open("WX_REMARK"); tap("wx_tab_contacts"); tap("wx_contact_lin"); tap("wx_remark")
        input("先不保存"); tap("wx_cancel_remark")
        rule.runOnIdle { assertEquals("海阔天空", wx().name("lin")) }
        tap("wx_remark"); input("家人小林"); tap("wx_save_remark"); back()
        rule.onNodeWithText("家人小林").assertExists()
        tap("wx_tab_chats"); tap("wx_chat_lin")
        rule.runOnIdle { assertEquals("家人小林", wx().name()); assertEquals("海阔天空", wx().contact("lin").nickname) }
        back(); tap("wx_search"); input("家人小林"); tap("wx_do_search"); done()
    }
    @Test fun textDraftOnlySendsOnSend() {
        open("WX_TEXT"); tap("wx_chat_lin"); input("今天过得很好")
        rule.runOnIdle { assertTrue(wx().messages.none { it.outgoing }); assertEquals("今天过得很好", wx().drafts["lin"]) }
        tap("wx_send"); done()
    }
    @Test fun photosSelectDeselectPreviewAndSend() {
        open("WX_SEND_PHOTO"); tap("wx_chat_lin"); tap("wx_add"); tap("wx_album")
        tap("wx_photo_0"); tap("wx_photo_1"); tap("wx_photo_1")
        rule.runOnIdle { assertEquals(setOf(0), wx().selectedPhotos); assertTrue(wx().messages.none { it.outgoing }) }
        tap("wx_preview")
        rule.onNodeWithText("发给：海阔天空", substring = true).assertExists()
        tap("wx_send_photos"); done()
    }
    @Test fun saveReceivedPhotoThroughActualLongPress() {
        open("WX_SAVE_PHOTO"); tap("wx_chat_lin"); tap("wx_received_photo")
        rule.onNodeWithTag("sim_wx_photo_surface").performScrollTo().performTouchInput { doubleClick() }
        rule.runOnIdle { assertTrue(wx().zoom > 1.2f) }
        rule.onNodeWithTag("sim_wx_photo_surface").performScrollTo().performTouchInput { longClick() }
        tap("wx_save_photo"); back(); back(); tap("wx_tab_me"); tap("wx_saved_album"); tap("wx_saved_photo_0"); done()
        rule.runOnIdle { assertTrue(wx().messages.any { it.id == 102 }) }
    }
    @Test fun actualVoiceHoldAndSlideCancel() {
        open("WX_VOICE"); tap("wx_chat_lin"); tap("wx_voice_mode")
        val cancelDistance = rule.activity.resources.displayMetrics.density * 90f
        rule.onNodeWithTag("wx_frame").performScrollTo()
        rule.onNodeWithTag("sim_wx_hold_voice").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("sim_wx_hold_voice").performTouchInput {
            down(center); moveTo(center - Offset(0f, cancelDistance), delayMillis = 600); up()
        }
        rule.runOnIdle { assertTrue("Cancel state: ${rule.activity.model.state.value.practice}", "voice_cancelled" in rule.activity.model.state.value.practice.achieved); assertTrue(wx().messages.none { it.kind == WxMessageKind.VOICE }) }
        rule.onNodeWithTag("wx_frame").performScrollTo()
        rule.onNodeWithTag("sim_wx_hold_voice").performScrollTo().performTouchInput { longClick(durationMillis = 900) }
        tap("wx_play_voice"); done()
    }
    @Test fun outgoingVoiceAndIncomingVideoCalls() {
        open("WX_CALL"); tap("wx_chat_lin"); tap("wx_add"); tap("wx_call_menu"); tap("wx_call_voice")
        rule.runOnIdle { assertEquals(WxCallStatus.DIALING, wx().call.status) }
        tap("wx_connect_call"); tap("wx_hangup"); tap("wx_incoming_voice"); tap("wx_reject")
        tap("wx_incoming_video"); tap("wx_accept"); tap("wx_call_camera")
        rule.runOnIdle { assertFalse(wx().call.camera) }
        tap("wx_hangup"); done()
    }
    @Test fun chatRecordsFindOriginalMessage() {
        open("WX_RECORDS"); tap("wx_chat_lin"); tap("wx_more"); tap("wx_records"); input("周末"); tap("wx_do_record_search"); tap("wx_record_result"); done()
        rule.runOnIdle { assertEquals(101, wx().selectedRecord) }
    }
    @Test fun muteChangesAlertsButRetainsMessagesAndMembership() {
        open("WX_MUTE"); tap("wx_incoming_group")
        rule.runOnIdle { assertTrue(wx().alert!!.notifies) }
        tap("wx_chat_neighbor"); tap("wx_more"); tap("wx_mute"); back(); back(); tap("wx_incoming_group")
        rule.runOnIdle { assertFalse(wx().alert!!.notifies); assertTrue(wx().conversation("neighbor").unread > 0); assertTrue(wx().conversation("neighbor").joined) }
        rule.onNodeWithText("声音：不提醒", substring = true).assertExists()
        rule.onNodeWithText("声音：不提醒", substring = true).performScrollTo()
        captureAcceptance("wechat-mute-effects")
        tap("wx_chat_neighbor")
        rule.runOnIdle { assertEquals(3, wx().messages.count { it.conversation == "neighbor" }); assertEquals(0, wx().conversation("neighbor").unread) }
        tap("wx_more"); tap("wx_mute"); back(); back(); tap("wx_incoming_group"); done()
    }
    @Test fun pinOrderingAndRestore() {
        open("WX_PIN"); tap("wx_chat_lin"); tap("wx_more"); tap("wx_pin"); back(); back(); tap("wx_other_message")
        rule.runOnIdle { assertEquals("lin", wx().sortedChats().first().id); assertFalse(wx().conversation("lin").muted) }
        tap("wx_chat_lin"); tap("wx_more"); tap("wx_pin"); back(); back(); done()
    }
    @Test fun savedGroupAppearsInAddressBook() {
        open("WX_GROUP"); tap("wx_chat_neighbor"); tap("wx_more"); tap("wx_save_group"); back(); back(); tap("wx_tab_contacts"); tap("wx_groups"); tap("wx_group_neighbor"); done()
    }
    @Test fun resetAndNewIdsPreserveLegacyProgress() {
        open("WX_REMARK")
        val repository = ProgressRepository(ApplicationProvider.getApplicationContext())
        runBlocking { repository.complete("TAP"); repository.remember("WIFI", Stage.INTRO, 1); repository.setFont(1) }
        rule.waitUntil(10000) { "TAP" in rule.activity.model.state.value.progress.completed && rule.activity.model.state.value.progress.fontLevel == 1 }
        rule.runOnIdle { rule.activity.model.open("WX_MUTE"); rule.activity.model.start(Stage.SOLO) }
        tap("wx_chat_neighbor"); tap("wx_more"); tap("wx_mute")
        rule.onNodeWithTag("restart").performClick()
        rule.runOnIdle { assertFalse(wx().conversation("neighbor").muted); assertEquals(WxPage.CHATS, wx().page); assertTrue("TAP" in rule.activity.model.state.value.progress.completed) }
        rule.waitUntil(10000) { rule.activity.model.state.value.progress.recent == "WX_MUTE" }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10000) { rule.activity.model.state.value.loaded }
        rule.runOnIdle { rule.activity.model.open("WX_MUTE", resume = true) }
        rule.runOnIdle { assertEquals(Stage.SOLO, rule.activity.model.state.value.stage); assertFalse(wx().conversation("neighbor").muted); rule.activity.model.font(0) }
    }
}
