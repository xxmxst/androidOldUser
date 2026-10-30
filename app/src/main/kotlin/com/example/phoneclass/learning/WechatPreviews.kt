package com.example.phoneclass.learning

/** Read-only picture states; live practice always starts from initial(). */
object WechatPreviews {
    fun state(id: String, panel: Int): PracticeState {
        val base = WechatState(id)
        fun group(muted: Boolean = false, saved: Boolean = false, unread: Int = 0) = base.conversations.map { if (it.id == "neighbor") it.copy(muted = muted, saved = saved, unread = unread) else it }
        val w = when (id) {
            "WX_NAV" -> base.copy(page = listOf(WxPage.CHATS, WxPage.CONTACTS, WxPage.ME)[panel], tab = listOf(WxPage.CHATS, WxPage.CONTACTS, WxPage.ME)[panel])
            "WX_CONTACT" -> when (panel) { 0 -> base.copy(page = WxPage.CONTACTS, tab = WxPage.CONTACTS); 1 -> base.copy(page = WxPage.SEARCH, query = "海阔天空", searched = true); else -> base.copy(page = WxPage.PROFILE) }
            "WX_REMARK" -> when (panel) { 0 -> base.copy(page = WxPage.PROFILE); 1 -> base.copy(page = WxPage.REMARK, remarkDraft = "家人小林"); else -> base.copy(contacts = base.contacts.map { if (it.id == "lin") it.copy(remark = "家人小林") else it }) }
            "WX_TEXT" -> base.copy(page = WxPage.CHAT, drafts = if (panel == 1) mapOf("lin" to "今天很好，放心吧。") else emptyMap(), messages = base.messages + if (panel == 2) listOf(WxMessage(200, "lin", "今天很好，放心吧。", outgoing = true)) else emptyList())
            "WX_SEND_PHOTO" -> when (panel) { 0 -> base.copy(page = WxPage.CHAT, dialog = "add"); 1 -> base.copy(page = WxPage.PHOTOS, selectedPhotos = setOf(0)); else -> base.copy(page = WxPage.PHOTO_PREVIEW, selectedPhotos = setOf(0, 1)) }
            "WX_SAVE_PHOTO" -> when (panel) { 0 -> base.copy(page = WxPage.CHAT); 1 -> base.copy(page = WxPage.RECEIVED_PHOTO, zoom = 1.5f, dialog = "save_photo"); else -> base.copy(page = WxPage.SAVED_ALBUM, savedPhotos = setOf(0)) }
            "WX_VOICE" -> base.copy(page = WxPage.CHAT, voiceMode = true, recording = panel == 1, playingVoice = if (panel == 2) 200 else null, messages = base.messages + if (panel == 2) listOf(WxMessage(200, "lin", "示例语音：今天很好。", WxMessageKind.VOICE, true)) else emptyList())
            "WX_CALL" -> when (panel) { 0 -> base.copy(page = WxPage.CHAT, dialog = "call_types"); 1 -> base.copy(page = WxPage.CALL, call = WxCall(status = WxCallStatus.DIALING)); else -> base.copy(page = WxPage.CALL, call = WxCall(WxCallKind.VIDEO, WxCallStatus.INCOMING, false)) }
            "WX_RECORDS" -> base.copy(page = listOf(WxPage.CHAT, WxPage.RECORD_SEARCH, WxPage.RECORD_RESULTS)[panel], query = if (panel > 0) "周末" else "", searched = panel == 2)
            "WX_MUTE" -> when (panel) {
                0 -> base.copy(alert = WxAlert("邻里群收到了一条普通消息", true))
                1 -> base.copy(page = WxPage.DETAILS, current = "neighbor", conversations = group(muted = true))
                else -> base.copy(conversations = group(muted = true, unread = 1), alert = WxAlert("普通群消息已收到，点击群聊仍可查看。", false))
            }
            "WX_PIN" -> base.copy(page = if (panel == 1) WxPage.DETAILS else WxPage.CHATS, conversations = base.conversations.map { if (it.id == "lin") it.copy(pinned = panel > 0) else it })
            "WX_GROUP" -> base.copy(page = listOf(WxPage.CHATS, WxPage.DETAILS, WxPage.GROUPS)[panel], current = "neighbor", conversations = group(saved = panel > 0))
            else -> base
        }
        return PracticeState(wechat = w)
    }
}
