package com.example.phoneclass.learning

/** A deterministic, offline WeChat teaching model. No real account or system API is used. */
object WechatEngine {
    fun initial(id: String) = PracticeState(wechat = WechatState(id))
    fun complete(lesson: Lesson, s: PracticeState): Boolean {
        val w = s.wechat ?: return false
        return when (lesson.id) {
            "WX_NAV" -> w.page == WxPage.CHATS
            "WX_CONTACT" -> w.page == WxPage.CHAT && w.current == "lin"
            "WX_REMARK" -> w.contact("lin").remark == "家人小林" && w.contact("lin").nickname == "海阔天空" && w.contactMatches().any { it.id == "lin" }
            "WX_TEXT" -> w.messages.any { it.outgoing && it.kind == WxMessageKind.TEXT && it.conversation == "lin" } && w.drafts["lin"].isNullOrBlank()
            "WX_SEND_PHOTO" -> w.messages.any { it.outgoing && it.kind == WxMessageKind.PHOTO && it.photos.isNotEmpty() }
            "WX_SAVE_PHOTO" -> 0 in w.savedPhotos && w.page == WxPage.RECEIVED_PHOTO && w.photoFrom == WxPage.SAVED_ALBUM
            "WX_VOICE" -> !w.recording && w.playingVoice != null && w.messages.any { it.outgoing && it.kind == WxMessageKind.VOICE }
            "WX_CALL" -> w.call.status == WxCallStatus.ENDED
            "WX_RECORDS" -> w.page == WxPage.CHAT && w.selectedRecord == 101
            "WX_MUTE" -> !w.conversation("neighbor").muted && w.conversation("neighbor").joined && w.alert?.notifies == true
            "WX_PIN" -> !w.conversation("lin").pinned && w.sortedChats().first().id == "wang"
            "WX_GROUP" -> w.conversation("neighbor").saved && w.conversation("neighbor").joined && w.current == "neighbor" && w.page == WxPage.CHAT
            else -> false
        }
    }

    fun reduce(lesson: Lesson, state: PracticeState, event: PracticeEvent): PracticeState {
        var w = state.wechat ?: WechatState(lesson.id)
        var achieved = state.achieved
        var feedback = ""
        fun mark(key: String) { achieved += key }
        fun say(text: String) { feedback = text }
        fun updateConversation(id: String = w.current, change: (WxConversation) -> WxConversation) { w = w.copy(conversations = w.conversations.map { if (it.id == id) change(it) else it }) }
        fun checkList() {
            if (w.page == WxPage.CONTACTS && w.contact("lin").remark == "家人小林") mark("remark_contacts")
            if (w.page == WxPage.CHATS) {
                if (w.conversation("lin").pinned && w.sortedChats().first().id == "lin") mark("pin_first")
                if ("pin_off" in achieved && !w.conversation("lin").pinned && w.sortedChats().first().id == "wang") mark("pin_restored")
            }
        }
        fun chat(id: String) {
            if (w.conversations.none { it.id == id }) return
            w = w.copy(page = WxPage.CHAT, current = id, tab = WxPage.CHATS, selectedRecord = null, voiceMode = false)
            updateConversation(id) { it.copy(unread = 0) }
            if (id == "lin") {
                mark("chat_open")
                if (w.contact("lin").remark == "家人小林") mark("remark_chat")
            }
            if (id == "neighbor" && "mute_silent" in achieved) { mark("mute_read"); say("群消息仍然在这里，也可以输入文字回复。") }
        }
        fun addMessage(text: String, kind: WxMessageKind = WxMessageKind.TEXT, photos: List<Int> = emptyList()) {
            val id = w.nextId
            w = w.copy(messages = w.messages + WxMessage(id, w.current, text, kind, true, photos), nextId = id + 1)
            updateConversation { it.copy(updated = id) }
        }
        fun incoming(id: String) {
            val conv = w.conversation(id)
            val active = w.page == WxPage.CHAT && w.current == id
            val text = if (id == "neighbor") "邻里新消息 ${w.nextId}：明天上午一起散步。" else "王阿姨：新活动开始报名了。"
            val number = w.nextId
            w = w.copy(messages = w.messages + WxMessage(number, id, text), nextId = number + 1, alert = WxAlert(text, !conv.muted && !active))
            updateConversation(id) { it.copy(unread = if (active) 0 else it.unread + 1, updated = number) }
            if (id == "neighbor") {
                if (w.page != WxPage.CHATS) say("消息已经收到。先返回微信列表，再比较普通消息的提醒效果。")
                else if (conv.muted) { mark("mute_silent"); say("普通群消息已收到，列表有未读提示；本次没有声音、振动和横幅提醒。") }
                else if ("mute_off" in achieved) { mark("mute_restored"); say("后续普通群消息恢复提醒。之前的消息仍然保留。") }
                else { mark("mute_before"); say("未开启免打扰：这条普通消息出现了提醒预览。") }
            } else if (w.conversation("lin").pinned && w.sortedChats().first().id == "lin") { mark("pin_kept"); say("王阿姨有新消息，置顶的家人聊天仍在前面。") }
        }

        when (event) {
            is PracticeEvent.Input -> {
                val text = event.text.take(120)
                w = when (w.page) {
                    WxPage.CHAT -> { if (text.isNotBlank() && w.current == "lin") mark("text_draft"); w.copy(drafts = w.drafts + (w.current to text)) }
                    WxPage.REMARK -> w.copy(remarkDraft = text)
                    WxPage.SEARCH, WxPage.RECORD_SEARCH -> w.copy(query = text, searched = false)
                    else -> w
                }
            }
            is PracticeEvent.Adjust -> Unit
            is PracticeEvent.Gesture -> when (event.kind) {
                "wx_zoom" -> if (w.page == WxPage.RECEIVED_PHOTO) { w = w.copy(zoom = (w.zoom * event.amount).coerceIn(1f, 3f)); if (w.zoom > 1.2f) mark("received_zoom") }
                "wx_photo_menu" -> if (w.page == WxPage.RECEIVED_PHOTO) w = w.copy(dialog = "save_photo")
                "wx_voice_start" -> if (w.page == WxPage.CHAT && w.voiceMode) w = w.copy(recording = true)
                "wx_voice_cancel" -> if (w.recording) { w = w.copy(recording = false); achieved -= setOf("voice_sent", "voice_played"); mark("voice_cancelled"); say("这次录音已取消，没有新增语音消息。") }
                "wx_voice_short" -> if (w.recording) { w = w.copy(recording = false); say("按住的时间有点短。停一小会儿再松开。") }
                "wx_voice_finish" -> if (w.recording) { w = w.copy(recording = false); addMessage("示例语音：今天过得很好，放心吧。", WxMessageKind.VOICE); if ("voice_cancelled" in achieved) mark("voice_sent"); say("已发送一条模拟语音。可以点气泡查看播放示意。") }
            }
            is PracticeEvent.Click -> {
                val target = event.target
                when {
                    target.startsWith("wx_tab_") -> {
                        val page = when (target) { "wx_tab_contacts" -> WxPage.CONTACTS; "wx_tab_discover" -> WxPage.DISCOVER; "wx_tab_me" -> WxPage.ME; else -> WxPage.CHATS }
                        w = w.copy(page = page, tab = page, dialog = null)
                        mark(when (page) { WxPage.CONTACTS -> "contacts_tab"; WxPage.DISCOVER -> "discover_tab"; WxPage.ME -> "me_tab"; else -> "chats_tab" })
                        checkList()
                    }
                    target == "wx_back" -> {
                        val page = when (w.page) {
                            WxPage.PROFILE -> w.profileFrom
                            WxPage.REMARK -> WxPage.PROFILE
                            WxPage.CHAT -> WxPage.CHATS
                            WxPage.DETAILS, WxPage.PHOTOS -> WxPage.CHAT
                            WxPage.PHOTO_PREVIEW -> WxPage.PHOTOS
                            WxPage.RECEIVED_PHOTO -> w.photoFrom
                            WxPage.SAVED_ALBUM -> WxPage.ME
                            WxPage.SEARCH -> w.searchFrom
                            WxPage.RECORD_SEARCH -> WxPage.DETAILS
                            WxPage.RECORD_RESULTS -> WxPage.RECORD_SEARCH
                            WxPage.GROUPS -> WxPage.CONTACTS
                            WxPage.INFO -> w.tab
                            else -> w.page
                        }
                        if (w.page == WxPage.CALL) say("请先接听、拒绝或挂断这次模拟通话。")
                        else { w = w.copy(page = page, dialog = null, recording = false); checkList() }
                    }
                    target == "wx_search" && w.page in setOf(WxPage.CHATS, WxPage.CONTACTS) -> w = w.copy(searchFrom = w.page, page = WxPage.SEARCH, query = "", searched = false)
                    target == "wx_do_search" && w.page == WxPage.SEARCH -> {
                        w = w.copy(searched = true)
                        if (w.contactMatches().any { it.id == "lin" }) { mark("contact_search"); if (w.query.trim() == "家人小林" && w.contact("lin").remark == "家人小林") mark("remark_search") }
                        if (w.contactMatches().isEmpty()) say("没有找到。可以试试昵称“海阔天空”，或已保存的备注。")
                    }
                    target.startsWith("wx_contact_") && w.page in setOf(WxPage.CONTACTS, WxPage.SEARCH) -> {
                        val id = target.removePrefix("wx_contact_")
                        if (w.contacts.any { it.id == id } && (w.page != WxPage.SEARCH || w.contactMatches().any { it.id == id })) {
                            if (id == "lin") { mark("contact_profile"); if (w.page == WxPage.SEARCH) mark("contact_found") }
                            w = w.copy(profile = id, profileFrom = w.page, page = WxPage.PROFILE)
                        }
                    }
                    target == "wx_message" && w.page == WxPage.PROFILE -> { if (w.profile == "lin") mark("contact_chat"); chat(w.profile) }
                    target.startsWith("wx_chat_") && w.page == WxPage.CHATS -> chat(target.removePrefix("wx_chat_"))
                    target == "wx_remark" && w.page == WxPage.PROFILE -> w = w.copy(page = WxPage.REMARK, remarkDraft = w.contact(w.profile).remark)
                    target == "wx_save_remark" && w.page == WxPage.REMARK -> {
                        val remark = w.remarkDraft.trim()
                        w = w.copy(contacts = w.contacts.map { if (it.id == w.profile) it.copy(remark = remark) else it }, page = WxPage.PROFILE)
                        if (w.profile == "lin" && remark == "家人小林") mark("remark_saved")
                        say(if (remark.isEmpty()) "备注已清空，恢复显示对方昵称。" else "已保存自己的备注。对方原昵称保持不变。")
                    }
                    target == "wx_cancel_remark" && w.page == WxPage.REMARK -> { w = w.copy(page = WxPage.PROFILE, remarkDraft = ""); say("已取消，原来的备注保持不变。") }
                    target == "wx_send" && w.page == WxPage.CHAT -> {
                        val draft = w.drafts[w.current].orEmpty().trim()
                        if (draft.isEmpty()) say("先在输入框里写点内容。") else { addMessage(draft); w = w.copy(drafts = w.drafts - w.current); if (w.current == "lin") mark("text_sent"); say("消息已出现在聊天区；这不表示对方已经阅读。") }
                    }
                    target == "wx_add" && w.page == WxPage.CHAT -> w = w.copy(dialog = if (w.dialog == "add") null else "add")
                    target == "wx_album" && w.page == WxPage.CHAT && w.dialog == "add" -> { w = w.copy(page = WxPage.PHOTOS, dialog = null, selectedPhotos = emptySet()); mark("photo_picker") }
                    target.startsWith("wx_photo_") && target.lastOrNull()?.isDigit() == true && w.page == WxPage.PHOTOS -> {
                        val id = target.last().digitToInt()
                        if (id in 0..2) { val selected = if (id in w.selectedPhotos) w.selectedPhotos - id else w.selectedPhotos + id; w = w.copy(selectedPhotos = selected); if (selected.isNotEmpty()) mark("photos_selected") }
                    }
                    target == "wx_preview" && w.page == WxPage.PHOTOS -> if (w.selectedPhotos.isEmpty()) say("先选一张图片，再预览。") else { w = w.copy(page = WxPage.PHOTO_PREVIEW); mark("photo_preview") }
                    target == "wx_send_photos" && w.page == WxPage.PHOTO_PREVIEW -> if (w.selectedPhotos.isNotEmpty()) { addMessage("[图片]", WxMessageKind.PHOTO, w.selectedPhotos.sorted()); w = w.copy(page = WxPage.CHAT, selectedPhotos = emptySet()); mark("photos_sent"); say("图片已发送，原相册里的示例图仍然保留。") }
                    target == "wx_received_photo" && w.page == WxPage.CHAT -> {
                        val image = w.messages.firstOrNull { it.conversation == w.current && !it.outgoing && it.kind == WxMessageKind.PHOTO }
                        if (image != null) { w = w.copy(page = WxPage.RECEIVED_PHOTO, photoFrom = WxPage.CHAT, previewPhoto = image.photos.first(), zoom = 1f); mark("received_open") }
                    }
                    target == "wx_photo_more" && w.page == WxPage.RECEIVED_PHOTO -> w = w.copy(dialog = "save_photo")
                    target == "wx_save_photo" && w.dialog == "save_photo" -> { w = w.copy(savedPhotos = w.savedPhotos + w.previewPhoto, dialog = null); mark("photo_saved"); say("已保存到模拟相册。聊天消息仍然保留。") }
                    target == "wx_saved_album" && w.page == WxPage.ME -> w = w.copy(page = WxPage.SAVED_ALBUM)
                    target.startsWith("wx_saved_photo_") && w.page == WxPage.SAVED_ALBUM -> {
                        val id = target.removePrefix("wx_saved_photo_").toIntOrNull()
                        if (id != null && id in w.savedPhotos) { w = w.copy(page = WxPage.RECEIVED_PHOTO, photoFrom = WxPage.SAVED_ALBUM, previewPhoto = id, zoom = 1f); mark("saved_found"); say("找到了保存的图。真实手机通常在图库或相册里查看。") }
                    }
                    target == "wx_voice_mode" && w.page == WxPage.CHAT -> { w = w.copy(voiceMode = !w.voiceMode, recording = false); if (w.voiceMode) mark("voice_mode") }
                    target == "wx_play_voice" && w.page == WxPage.CHAT -> {
                        val message = w.messages.lastOrNull { it.conversation == w.current && it.kind == WxMessageKind.VOICE }
                        if (message != null) { w = w.copy(playingVoice = message.id); if ("voice_sent" in achieved) mark("voice_played"); say("模拟播放：今天过得很好，放心吧。（本课不播放真实声音）") }
                    }
                    target == "wx_call_menu" && w.dialog == "add" -> w = w.copy(dialog = "call_types")
                    target in setOf("wx_call_voice", "wx_call_video") && w.dialog == "call_types" -> w = w.copy(page = WxPage.CALL, dialog = null, call = WxCall(if (target == "wx_call_video") WxCallKind.VIDEO else WxCallKind.VOICE, WxCallStatus.DIALING))
                    target == "wx_connect_call" && w.call.status == WxCallStatus.DIALING -> { w = w.copy(call = w.call.copy(status = WxCallStatus.CONNECTED)); if (w.call.kind == WxCallKind.VOICE && w.call.outgoing) mark("out_call_connected") }
                    target in setOf("wx_incoming_voice", "wx_incoming_video") && w.page == WxPage.CHAT -> w = w.copy(page = WxPage.CALL, call = WxCall(if (target == "wx_incoming_video") WxCallKind.VIDEO else WxCallKind.VOICE, WxCallStatus.INCOMING, outgoing = false))
                    target == "wx_accept" && w.call.status == WxCallStatus.INCOMING -> { w = w.copy(call = w.call.copy(status = WxCallStatus.CONNECTED)); if (w.call.kind == WxCallKind.VIDEO) mark("video_answered") }
                    target == "wx_reject" && w.call.status == WxCallStatus.INCOMING -> { if (w.call.kind == WxCallKind.VOICE) mark("call_rejected"); w = w.copy(page = WxPage.CHAT, call = w.call.copy(status = WxCallStatus.ENDED)); say("已拒绝这次模拟来电。") }
                    target == "wx_hangup" && w.page == WxPage.CALL -> {
                        if (w.call.status == WxCallStatus.CONNECTED) { if (w.call.outgoing && w.call.kind == WxCallKind.VOICE) mark("out_call_ended"); if (!w.call.outgoing && w.call.kind == WxCallKind.VIDEO) mark("video_ended") }
                        w = w.copy(page = WxPage.CHAT, call = w.call.copy(status = WxCallStatus.ENDED)); say("这次模拟通话已经结束。")
                    }
                    target == "wx_call_mic" && w.page == WxPage.CALL -> w = w.copy(call = w.call.copy(muted = !w.call.muted))
                    target == "wx_call_camera" && w.page == WxPage.CALL -> w = w.copy(call = w.call.copy(camera = !w.call.camera))
                    target == "wx_more" && w.page == WxPage.CHAT -> w = w.copy(page = WxPage.DETAILS)
                    target == "wx_records" && w.page == WxPage.DETAILS -> w = w.copy(page = WxPage.RECORD_SEARCH, query = "", searched = false)
                    target == "wx_do_record_search" && w.page == WxPage.RECORD_SEARCH -> { w = w.copy(page = WxPage.RECORD_RESULTS, searched = true); if (w.recordMatches().any { it.id == 101 }) mark("record_search") else say("没有匹配的记录。本课可以试试“周末”。") }
                    (target == "wx_record_result" || target.startsWith("wx_record_result_")) && w.page == WxPage.RECORD_RESULTS -> {
                        val id = if (target == "wx_record_result") 101 else target.removePrefix("wx_record_result_").toIntOrNull()
                        val message = w.recordMatches().firstOrNull { it.id == id }
                        if (message != null) {
                            w = w.copy(page = WxPage.CHAT, selectedRecord = message.id)
                            if (message.id == 101) mark("record_found")
                            say("已回到原消息：“${message.text}”")
                        }
                    }
                    target == "wx_mute" && w.page == WxPage.DETAILS && w.conversation().group -> {
                        updateConversation { it.copy(muted = !it.muted) }
                        if (w.current == "neighbor") {
                            if (w.conversation().muted) { mark("mute_on"); say("普通群消息免打扰已开启。返回列表，再模拟收到一条消息。") }
                            else if ("mute_read" in achieved) { mark("mute_off"); say("已关闭免打扰。回到列表，再比较后续普通消息。") }
                        }
                    }
                    target == "wx_pin" && w.page == WxPage.DETAILS -> {
                        updateConversation { it.copy(pinned = !it.pinned) }
                        if (w.current == "lin") { if (w.conversation().pinned) mark("pin_on") else if ("pin_kept" in achieved) mark("pin_off") }
                        say("置顶只改变列表排序，通知与免打扰设置保持原样。")
                    }
                    target == "wx_save_group" && w.page == WxPage.DETAILS && w.conversation().group -> { updateConversation { it.copy(saved = !it.saved) }; if (w.current == "neighbor" && w.conversation().saved) mark("group_saved"); say("自己的通讯录入口已更新，群成员身份保持原样。") }
                    target == "wx_groups" && w.page == WxPage.CONTACTS -> { w = w.copy(page = WxPage.GROUPS); if (w.conversation("neighbor").saved) mark("group_list") }
                    target.startsWith("wx_group_") && w.page == WxPage.GROUPS -> {
                        val id = target.removePrefix("wx_group_")
                        if (w.conversations.any { it.id == id && it.saved && it.group }) { chat(id); if (id == "neighbor") mark("group_found") }
                    }
                    target == "wx_incoming_group" -> incoming("neighbor")
                    target == "wx_other_message" -> incoming("wang")
                    target == "wx_dismiss" -> w = w.copy(dialog = null)
                    target == "wx_info" -> w = w.copy(page = WxPage.INFO, info = "这一页先认识位置。实际微信里还有更多功能，可以返回继续本课。")
                    else -> say("先看看上方的本次目标，再试试页面里的入口。")
                }
            }
        }
        return state.copy(wechat = w, achieved = achieved, feedback = feedback)
    }
}
