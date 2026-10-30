package com.example.phoneclass.learning

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

fun wxJourney(id: String): List<PracticeEvent> = when (id) {
    "WX_NAV" -> listOf(click("wx_tab_contacts"), click("wx_tab_discover"), click("wx_tab_me"), click("wx_tab_chats"))
    "WX_CONTACT" -> listOf(click("wx_tab_contacts"), click("wx_search"), input("海阔天空"), click("wx_do_search"), click("wx_contact_lin"), click("wx_message"))
    "WX_REMARK" -> listOf(click("wx_tab_contacts"), click("wx_contact_lin"), click("wx_remark"), input("家人小林"), click("wx_save_remark"), click("wx_back"), click("wx_tab_chats"), click("wx_chat_lin"), click("wx_back"), click("wx_search"), input("家人小林"), click("wx_do_search"))
    "WX_TEXT" -> listOf(click("wx_chat_lin"), input("今天很好，放心吧。"), click("wx_send"))
    "WX_SEND_PHOTO" -> listOf(click("wx_chat_lin"), click("wx_add"), click("wx_album"), click("wx_photo_0"), click("wx_photo_1"), click("wx_preview"), click("wx_send_photos"))
    "WX_SAVE_PHOTO" -> listOf(click("wx_chat_lin"), click("wx_received_photo"), gesture("wx_zoom", 2f), gesture("wx_photo_menu"), click("wx_save_photo"), click("wx_back"), click("wx_back"), click("wx_tab_me"), click("wx_saved_album"), click("wx_saved_photo_0"))
    "WX_VOICE" -> listOf(click("wx_chat_lin"), click("wx_voice_mode"), gesture("wx_voice_start"), gesture("wx_voice_cancel"), gesture("wx_voice_start"), gesture("wx_voice_finish"), click("wx_play_voice"))
    "WX_CALL" -> listOf(click("wx_chat_lin"), click("wx_add"), click("wx_call_menu"), click("wx_call_voice"), click("wx_connect_call"), click("wx_hangup"), click("wx_incoming_voice"), click("wx_reject"), click("wx_incoming_video"), click("wx_accept"), click("wx_hangup"))
    "WX_RECORDS" -> listOf(click("wx_chat_lin"), click("wx_more"), click("wx_records"), input("周末"), click("wx_do_record_search"), click("wx_record_result"))
    "WX_MUTE" -> listOf(click("wx_incoming_group"), click("wx_chat_neighbor"), click("wx_more"), click("wx_mute"), click("wx_back"), click("wx_back"), click("wx_incoming_group"), click("wx_chat_neighbor"), click("wx_more"), click("wx_mute"), click("wx_back"), click("wx_back"), click("wx_incoming_group"))
    "WX_PIN" -> listOf(click("wx_chat_lin"), click("wx_more"), click("wx_pin"), click("wx_back"), click("wx_back"), click("wx_other_message"), click("wx_chat_lin"), click("wx_more"), click("wx_pin"), click("wx_back"), click("wx_back"))
    "WX_GROUP" -> listOf(click("wx_chat_neighbor"), click("wx_more"), click("wx_save_group"), click("wx_back"), click("wx_back"), click("wx_tab_contacts"), click("wx_groups"), click("wx_group_neighbor"))
    else -> error(id)
}

fun wxRun(id: String, vararg events: PracticeEvent): PracticeState {
    val lesson = Lessons.get(id)
    return events.fold(PracticeEngine.initial(lesson)) { state, event -> PracticeEngine.reduce(lesson, state, event) }
}

@RunWith(Parameterized::class)
class WechatJourneyTest(private val id: String) {
    companion object { @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases() = WechatLessons.all.map { arrayOf(it.id) } }
    @Test fun completeJourneyRequiresAllEffects() {
        val lesson = Lessons.get(id)
        var state = PracticeEngine.initial(lesson)
        assertFalse(PracticeEngine.complete(lesson, state))
        val journey = wxJourney(id)
        journey.forEachIndexed { index, event ->
            state = PracticeEngine.reduce(lesson, state, event)
            if (index < journey.lastIndex) assertFalse("Completed early: $id at $event", PracticeEngine.complete(lesson, state))
        }
        assertTrue("Cannot finish $id: ${state.achieved}", PracticeEngine.complete(lesson, state))
    }
    @Test fun restartResetsContactsPhotosAndGroupSettings() {
        val lesson = Lessons.get(id)
        val final = wxJourney(id).fold(PracticeEngine.initial(lesson)) { state, event -> PracticeEngine.reduce(lesson, state, event) }
        assertTrue(PracticeEngine.complete(lesson, final))
        val fresh = PracticeEngine.initial(lesson)
        assertTrue(fresh.achieved.isEmpty()); assertEquals("", fresh.wechat!!.contact("lin").remark)
        assertTrue(fresh.wechat.savedPhotos.isEmpty()); assertTrue(fresh.wechat.selectedPhotos.isEmpty())
        assertFalse(fresh.wechat.conversation("neighbor").muted); assertFalse(fresh.wechat.conversation("neighbor").saved)
        assertFalse(fresh.wechat.conversation("lin").pinned); assertEquals(WxPage.CHATS, fresh.wechat.page)
    }
}

class WechatEngineTest {
    @Test fun legacyIdsAndNumbersRemainStableWhileWechatSharesOneScene() {
        val legacy = Scene.entries.filter { it != Scene.WECHAT }
        assertEquals(legacy.map { it.name }, Lessons.all.take(22).map { it.id })
        assertEquals((1..34).toList(), Lessons.all.map { it.number })
        assertEquals(12, Lessons.wechat.size); assertTrue(Lessons.wechat.all { it.scene == Scene.WECHAT && it.id.startsWith("WX_") })
        assertEquals(3, Lessons.categories.first().id)
    }
    @Test fun cancellingAndClearingRemarkHasExpectedDisplayEffect() {
        val id = "WX_REMARK"
        var s = wxRun(id, click("wx_tab_contacts"), click("wx_contact_lin"), click("wx_remark"), input("误写的名字"), click("wx_cancel_remark"))
        assertEquals("海阔天空", s.wechat!!.name("lin"))
        s = PracticeEngine.reduce(Lessons.get(id), s, click("wx_remark"))
        s = PracticeEngine.reduce(Lessons.get(id), s, input("家人小林"))
        s = PracticeEngine.reduce(Lessons.get(id), s, click("wx_save_remark"))
        assertEquals("家人小林", s.wechat!!.name("lin")); assertEquals("海阔天空", s.wechat.contact("lin").nickname)
        s = PracticeEngine.reduce(Lessons.get(id), s, click("wx_remark"))
        s = PracticeEngine.reduce(Lessons.get(id), s, input(""))
        s = PracticeEngine.reduce(Lessons.get(id), s, click("wx_save_remark"))
        assertEquals("海阔天空", s.wechat!!.name("lin")); assertFalse(PracticeEngine.complete(Lessons.get(id), s))
    }
    @Test fun blankAndIncorrectContactSearchCanBeRetried() {
        var s = wxRun("WX_CONTACT", click("wx_tab_contacts"), click("wx_search"), click("wx_do_search"), click("wx_contact_lin"))
        assertEquals(WxPage.SEARCH, s.wechat!!.page); assertTrue(s.wechat.contactMatches().isEmpty())
        s = PracticeEngine.reduce(Lessons.get("WX_CONTACT"), s, input("不存在"))
        s = PracticeEngine.reduce(Lessons.get("WX_CONTACT"), s, click("wx_do_search"))
        assertTrue(s.wechat!!.contactMatches().isEmpty())
        s = PracticeEngine.reduce(Lessons.get("WX_CONTACT"), s, input("海阔天空"))
        s = PracticeEngine.reduce(Lessons.get("WX_CONTACT"), s, click("wx_do_search"))
        assertEquals(listOf("lin"), s.wechat!!.contactMatches().map { it.id })
    }
    @Test fun draftsBelongToTheirOwnConversation() {
        val s = wxRun("WX_TEXT", click("wx_chat_lin"), input("给家人的草稿"), click("wx_back"), click("wx_chat_wang"))
        assertEquals("给家人的草稿", s.wechat!!.drafts["lin"]); assertTrue(s.wechat.drafts["wang"].isNullOrBlank())
        assertTrue(s.wechat.messages.none { it.outgoing })
    }
    @Test fun deselectingAllPhotosPreventsPreviewAndSend() {
        val s = wxRun("WX_SEND_PHOTO", click("wx_chat_lin"), click("wx_add"), click("wx_album"), click("wx_photo_0"), click("wx_photo_0"), click("wx_preview"), click("wx_send_photos"))
        assertEquals(WxPage.PHOTOS, s.wechat!!.page); assertTrue(s.wechat.messages.none { it.outgoing && it.kind == WxMessageKind.PHOTO })
    }
    @Test fun sentImagesRetainOriginalExamples() {
        val s = wxRun("WX_SEND_PHOTO", *wxJourney("WX_SEND_PHOTO").toTypedArray())
        assertEquals(listOf(0, 1), s.wechat!!.messages.last().photos)
        assertEquals(listOf(0), s.wechat.messages.first { it.id == 102 }.photos)
    }
    @Test fun savingPhotoDoesNotDeleteChatMessage() {
        val s = wxRun("WX_SAVE_PHOTO", *wxJourney("WX_SAVE_PHOTO").toTypedArray())
        assertTrue(0 in s.wechat!!.savedPhotos); assertTrue(s.wechat.messages.any { it.id == 102 })
    }
    @Test fun cancellingAndTooShortVoiceDoNotSend() {
        val s = wxRun("WX_VOICE", click("wx_chat_lin"), click("wx_voice_mode"), gesture("wx_voice_start"), gesture("wx_voice_cancel"), gesture("wx_voice_start"), gesture("wx_voice_short"))
        assertFalse(s.wechat!!.recording); assertTrue(s.wechat.messages.none { it.kind == WxMessageKind.VOICE })
    }
    @Test fun rejectionAndHangupEndOnlyTheSimulatedCall() {
        val s = wxRun("WX_CALL", click("wx_chat_lin"), click("wx_incoming_voice"), click("wx_reject"))
        assertEquals(WxPage.CHAT, s.wechat!!.page); assertEquals(WxCallStatus.ENDED, s.wechat.call.status)
        assertTrue("call_rejected" in s.achieved); assertFalse("video_answered" in s.achieved)
    }
    @Test fun chatRecordSearchIsLimitedToSelectedConversation() {
        val s = wxRun("WX_RECORDS", click("wx_chat_wang"), click("wx_more"), click("wx_records"), input("周末"), click("wx_do_record_search"), click("wx_record_result"))
        assertTrue(s.wechat!!.recordMatches().isEmpty()); assertFalse("record_found" in s.achieved)
    }
    @Test fun selectingAnotherSearchResultReturnsItsOwnMessage() {
        val s = wxRun("WX_RECORDS", click("wx_chat_lin"), input("周末我带水果"), click("wx_send"), click("wx_more"), click("wx_records"), input("周末"), click("wx_do_record_search"), click("wx_record_result_200"))
        assertEquals(200, s.wechat!!.selectedRecord)
        assertFalse("record_found" in s.achieved)
    }
    @Test fun muteKeepsMessagesMembershipAndUnreadButChangesAlerts() {
        val before = wxRun("WX_MUTE", click("wx_incoming_group"))
        assertTrue(before.wechat!!.alert!!.notifies)
        val muted = wxRun("WX_MUTE", *wxJourney("WX_MUTE").take(7).toTypedArray())
        assertFalse(muted.wechat!!.alert!!.notifies); assertTrue(muted.wechat.conversation("neighbor").joined)
        assertTrue(muted.wechat.conversation("neighbor").unread > 0); assertEquals(3, muted.wechat.messages.count { it.conversation == "neighbor" })
        val restored = wxRun("WX_MUTE", *wxJourney("WX_MUTE").toTypedArray())
        assertTrue(restored.wechat!!.alert!!.notifies); assertEquals(4, restored.wechat.messages.count { it.conversation == "neighbor" })
    }
    @Test fun readingMutedGroupClearsUnreadWithoutDeletingMessages() {
        val s = wxRun("WX_MUTE", *wxJourney("WX_MUTE").take(8).toTypedArray())
        assertEquals(0, s.wechat!!.conversation("neighbor").unread); assertEquals(3, s.wechat.messages.count { it.conversation == "neighbor" })
        assertTrue("mute_read" in s.achieved)
    }
    @Test fun pinnedConversationStaysFirstAndNotificationsRemainIndependent() {
        val s = wxRun("WX_PIN", *wxJourney("WX_PIN").take(6).toTypedArray())
        assertEquals("lin", s.wechat!!.sortedChats().first().id); assertTrue(s.wechat.alert!!.notifies)
        assertFalse(s.wechat.conversation("lin").muted)
        val off = wxRun("WX_PIN", *wxJourney("WX_PIN").toTypedArray())
        assertEquals("wang", off.wechat!!.sortedChats().first().id)
    }
    @Test fun savingAndUnsavingGroupOnlyChangesAddressBookEntry() {
        var s = wxRun("WX_GROUP", click("wx_chat_neighbor"), click("wx_more"), click("wx_save_group"))
        assertTrue(s.wechat!!.conversation("neighbor").saved)
        s = PracticeEngine.reduce(Lessons.get("WX_GROUP"), s, click("wx_save_group"))
        assertFalse(s.wechat!!.conversation("neighbor").saved); assertTrue(s.wechat.conversation("neighbor").joined)
    }
}
