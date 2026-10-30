package com.example.phoneclass.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.theme.*

private val WxGreen = Color(0xFF087E43)
private val WxBubble = Color(0xFF95EC69)
private val WxFocus = staticCompositionLocalOf<String?> { null }
private val WxDiagram = staticCompositionLocalOf { false }
private fun Modifier.wxTag(tag: String, focus: String?) = testTag("sim_$tag").then(if (tag == focus) Modifier.border(3.dp, WxGreen, RoundedCornerShape(12.dp)).background(Color(0xFFE7F4EB), RoundedCornerShape(12.dp)) else Modifier)

@Composable
private fun WxAction(text: String, tag: String, send: (PracticeEvent) -> Unit, danger: Boolean = false) {
    val fm = LocalFocusManager.current
    OutlinedButton(onClick = { fm.clearFocus(); send(PracticeEvent.Click(tag)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).wxTag(tag, WxFocus.current), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (danger) Color(0xFFAD2929) else WxGreen, containerColor = Color.White)) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun WxNote(text: String) {
    Surface(color = Color(0xFFEDF3EF), shape = RoundedCornerShape(12.dp)) { Text(text, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall, color = Ink) }
}

@Composable
private fun WxInput(label: String, value: String, send: (PracticeEvent) -> Unit) {
    val fm = LocalFocusManager.current
    OutlinedTextField(value = value, onValueChange = { send(PracticeEvent.Input(it)) }, modifier = Modifier.fillMaxWidth().wxTag("wx_input", WxFocus.current), label = { Text(label, style = MaterialTheme.typography.bodySmall) }, textStyle = MaterialTheme.typography.bodyLarge, singleLine = true, readOnly = WxDiagram.current, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }), shape = RoundedCornerShape(12.dp))
}

@Composable
private fun WxToggle(label: String, value: Boolean, tag: String, send: (PracticeEvent) -> Unit) {
    Row(Modifier.fillMaxWidth().wxTag(tag, WxFocus.current).clip(RoundedCornerShape(12.dp)).toggleable(value, role = Role.Switch, onValueChange = { send(PracticeEvent.Click(tag)) }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (value) "已开启" else "已关闭", style = MaterialTheme.typography.bodySmall, color = WxGreen)
        }
        Switch(value, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = WxGreen))
    }
}

@Composable
private fun WxAvatar(name: String, group: Boolean = false) {
    Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).background(if (group) Color(0xFFDFE8DA) else Color(0xFFD9E8F2), RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
        Text(if (group) "群" else name.take(1), style = MaterialTheme.typography.titleMedium, color = if (group) Green else Blue)
    }
}

@Composable
private fun WxEntry(title: String, subtitle: String, tag: String, send: (PracticeEvent) -> Unit, group: Boolean = false, unread: Int = 0, muted: Boolean = false, pinned: Boolean = false) {
    Row(Modifier.fillMaxWidth().wxTag(tag, WxFocus.current).background(if (pinned) Color(0xFFE6EAE6) else Color.White, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { send(PracticeEvent.Click(tag)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WxAvatar(title, group)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
            if (pinned) Text("置顶", style = MaterialTheme.typography.labelSmall, color = Green)
            if (muted) Text("普通消息免打扰", style = MaterialTheme.typography.labelSmall, color = Muted)
            if (unread > 0) Text(if (muted) "● 有未读消息" else "$unread 条未读", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA32626))
        }
        Text("›", color = Muted, style = MaterialTheme.typography.titleMedium)
    }
}

private fun hintTarget(raw: String?, w: WechatState): String? {
    if (raw == null) return null
    if (raw in setOf("wx_mute", "wx_pin", "wx_save_group")) return when (w.page) {
        WxPage.CHATS -> if (raw == "wx_pin") "wx_chat_lin" else "wx_chat_neighbor"
        WxPage.CHAT -> "wx_more"
        WxPage.DETAILS -> raw
        else -> "wx_back"
    }
    if (raw == "wx_incoming_group" || raw == "wx_other_message") return if (w.page == WxPage.CHATS) raw else "wx_back"
    if (raw == "wx_contact_lin") return if (w.page == WxPage.CHATS) "wx_tab_contacts" else raw
    if (raw == "wx_remark") return when (w.page) {
        WxPage.CHATS -> "wx_tab_contacts"; WxPage.CONTACTS -> "wx_contact_lin"; WxPage.PROFILE -> "wx_remark"; WxPage.REMARK -> if (w.remarkDraft == "家人小林") "wx_save_remark" else "wx_input"; else -> "wx_back"
    }
    if (raw == "wx_search") return when (w.page) { WxPage.CHATS, WxPage.CONTACTS -> raw; WxPage.SEARCH -> if (w.query.isBlank()) "wx_input" else "wx_do_search"; else -> "wx_back" }
    if (raw == "wx_add") return when { w.page == WxPage.CALL -> "wx_connect_call"; w.dialog == "call_types" -> "wx_call_voice"; w.dialog == "add" -> if (w.lessonId == "WX_CALL") "wx_call_menu" else "wx_album"; else -> raw }
    if (raw == "wx_more") return when (w.page) { WxPage.CHATS -> "wx_chat_lin"; WxPage.CHAT -> raw; WxPage.DETAILS -> "wx_records"; WxPage.RECORD_SEARCH -> if (w.query.isBlank()) "wx_input" else "wx_do_record_search"; else -> "wx_record_result" }
    if (raw == "wx_saved_album") return when (w.page) { WxPage.CHATS -> "wx_tab_me"; WxPage.ME -> raw; WxPage.SAVED_ALBUM -> "wx_saved_photo_0"; else -> "wx_back" }
    if (raw == "wx_groups") return when (w.page) { WxPage.CHATS -> "wx_tab_contacts"; WxPage.CONTACTS -> raw; else -> "wx_back" }
    if (raw == "wx_photo_surface" && w.dialog == "save_photo") return "wx_save_photo"
    if (raw == "wx_photo_surface" && w.zoom <= 1.2f) return "wx_photo_surface"
    return raw
}

@Composable
fun WechatSceneUi(state: PracticeState, target: String?, send: (PracticeEvent) -> Unit, diagram: Boolean = false) {
    val w = state.wechat ?: return
    val focus = hintTarget(target, w)
    CompositionLocalProvider(WxFocus provides focus, WxDiagram provides diagram) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (diagram) "微信图解示意" else "练习页面 · 微信模拟", style = MaterialTheme.typography.labelMedium, color = WxGreen)
            val root = w.page in setOf(WxPage.CHATS, WxPage.CONTACTS, WxPage.DISCOVER, WxPage.ME)
            Surface(color = Color(0xFFEDEDED), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFB8C6BC))) {
                Column(Modifier.fillMaxWidth().height(560.dp).testTag("wx_frame")) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!root) TextButton(onClick = { send(PracticeEvent.Click("wx_back")) }, modifier = Modifier.heightIn(min = 56.dp).wxTag("wx_back", focus).semantics { contentDescription = "微信页面返回" }) { Text("‹", style = MaterialTheme.typography.titleLarge, color = Ink) }
                        Text(when (w.page) {
                            WxPage.CHATS -> "微信"; WxPage.CONTACTS -> "通讯录"; WxPage.DISCOVER -> "发现"; WxPage.ME -> "我"
                            WxPage.PROFILE -> "联系人资料"; WxPage.REMARK -> "设置备注"; WxPage.SEARCH -> "找联系人"
                            WxPage.CHAT, WxPage.DETAILS, WxPage.CALL -> w.name(); WxPage.PHOTOS -> "相册（示例）"; WxPage.PHOTO_PREVIEW -> "发送前预览"
                            WxPage.RECEIVED_PHOTO -> "查看图片"; WxPage.SAVED_ALBUM -> "模拟相册"; WxPage.RECORD_SEARCH, WxPage.RECORD_RESULTS -> "查找聊天记录"; WxPage.GROUPS -> "群聊"; WxPage.INFO -> "认识这个入口"
                        }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                        if (w.page == WxPage.CHAT || w.page == WxPage.RECEIVED_PHOTO) TextButton(onClick = { send(PracticeEvent.Click(if (w.page == WxPage.CHAT) "wx_more" else "wx_photo_more")) }, modifier = Modifier.heightIn(min = 56.dp).wxTag(if (w.page == WxPage.CHAT) "wx_more" else "wx_photo_more", focus).semantics { contentDescription = "微信右上角更多" }) { Text("···", style = MaterialTheme.typography.titleLarge, color = Ink) }
                    }
                    HorizontalDivider()
                    val bodyScroll = rememberScrollState()
                    LaunchedEffect(w.page) { bodyScroll.scrollTo(0) }
                    LaunchedEffect(w.messages.size) { if (w.page == WxPage.CHAT) bodyScroll.scrollTo(bodyScroll.maxValue) }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(bodyScroll).padding(12.dp).testTag("wx_body"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (w.page) {
                            WxPage.CHATS -> {
                                WxAction("⌕ 搜索联系人", "wx_search", send)
                                w.sortedChats().forEach { conv ->
                                    val last = w.messages.lastOrNull { it.conversation == conv.id }
                                    WxEntry(w.name(conv.id), last?.let { if (it.kind == WxMessageKind.PHOTO) "[图片]" else if (it.kind == WxMessageKind.VOICE) "[语音]" else it.text }.orEmpty(), "wx_chat_${conv.id}", send, conv.group, conv.unread, conv.muted, conv.pinned)
                                }
                            }
                            WxPage.CONTACTS -> {
                                WxAction("⌕ 搜索联系人", "wx_search", send)
                                WxAction("群聊  ›", "wx_groups", send)
                                w.contacts.forEach { WxEntry(it.displayName, "", "wx_contact_${it.id}", send) }
                            }
                            WxPage.DISCOVER -> { WxAction("朋友圈  ›", "wx_info", send); WxAction("扫一扫  ›", "wx_info", send); WxNote("发现常用来找朋友圈、扫一扫等功能。这一课先认识入口位置。") }
                            WxPage.ME -> { WxEntry("学员（示例）", "自己的头像和资料", "wx_info", send); WxAction("设置  ›", "wx_info", send); WxNote("自己的资料、微信设置等通常从这里找。") }
                            WxPage.INFO -> WxNote(w.info)
                            WxPage.SEARCH -> {
                                WxInput("输入昵称或备注", w.query, send)
                                WxAction("搜索", "wx_do_search", send)
                                if (w.searched) {
                                    if (w.contactMatches().isEmpty()) WxNote("没有匹配的联系人。换个昵称或备注试试。")
                                    w.contactMatches().forEach { WxEntry(it.displayName, "昵称：${it.nickname}", "wx_contact_${it.id}", send) }
                                }
                            }
                            WxPage.PROFILE -> {
                                val c = w.contact(w.profile)
                                WxAvatar(c.displayName)
                                Text(c.displayName, style = MaterialTheme.typography.titleLarge)
                                Text("对方原昵称：${c.nickname}\n自己的备注：${c.remark.ifBlank { "未设置" }}\n虚构身份：${c.identity}", style = MaterialTheme.typography.bodyLarge)
                                WxAction("设置备注  ›", "wx_remark", send)
                                WxAction("发消息", "wx_message", send)
                            }
                            WxPage.REMARK -> {
                                WxNote("对方原昵称：${w.contact(w.profile).nickname}\n这里填写自己好认的称呼。")
                                WxInput("填写备注，例如：家人小林", w.remarkDraft, send)
                                WxAction("保存", "wx_save_remark", send)
                                WxAction("取消", "wx_cancel_remark", send)
                                WxNote("清空后保存，会恢复显示昵称。取消会保留原来的备注。")
                            }
                            WxPage.CHAT -> {
                                WxNote("聊天对象：${w.name()}${if (w.conversation().group) "（虚构群）" else "（虚构联系人）"}")
                                val messages = w.messages.filter { it.conversation == w.current }
                                val lastVoice = messages.lastOrNull { it.kind == WxMessageKind.VOICE }?.id
                                messages.forEach { message ->
                                    Surface(color = if (message.outgoing) WxBubble else Color.White, shape = RoundedCornerShape(12.dp), border = if (w.selectedRecord == message.id) BorderStroke(3.dp, WxGreen) else null, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (message.outgoing) "我" else w.name(), style = MaterialTheme.typography.labelSmall, color = Muted)
                                            when (message.kind) {
                                                WxMessageKind.TEXT -> Text(message.text, style = MaterialTheme.typography.bodyLarge)
                                                WxMessageKind.PHOTO -> {
                                                    message.photos.forEach { photo -> PhotoArt(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)).then(if (!message.outgoing) Modifier.wxTag("wx_received_photo", focus).clickable { send(PracticeEvent.Click("wx_received_photo")) } else Modifier), photo) }
                                                    Text(if (message.outgoing) "已发送的图片" else "点图片查看大图", style = MaterialTheme.typography.bodySmall)
                                                }
                                                WxMessageKind.VOICE -> {
                                                    WxAction(if (w.playingVoice == message.id) "▶ 模拟播放中 · 3秒" else "▷ 示例语音 · 3秒", if (message.id == lastVoice) "wx_play_voice" else "wx_voice_${message.id}", { send(PracticeEvent.Click("wx_play_voice")) })
                                                    if (w.playingVoice == message.id) Text(message.text, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                                WxAction(if (w.voiceMode) "切换为文字输入" else "切换为语音输入", "wx_voice_mode", send)
                                if (w.voiceMode) VoiceHoldButton(w.recording, send)
                                else { WxInput("写一句话", w.drafts[w.current].orEmpty(), send); WxAction("发送", "wx_send", send) }
                                WxAction("＋ 添加", "wx_add", send)
                                if (w.dialog == "add") { WxNote("添加内容"); WxAction("相册", "wx_album", send); WxAction("通话", "wx_call_menu", send) }
                                if (w.dialog == "call_types") { WxAction("语音通话", "wx_call_voice", send); WxAction("视频通话", "wx_call_video", send) }
                            }
                            WxPage.DETAILS -> {
                                WxNote("${w.name()}\n${if (w.conversation().group) "你仍然是这个群的成员" else "这是和同一位联系人的聊天设置"}")
                                WxAction("查找聊天记录  ›", "wx_records", send)
                                WxToggle("置顶聊天", w.conversation().pinned, "wx_pin", send)
                                if (w.conversation().group) {
                                    WxToggle("消息免打扰", w.conversation().muted, "wx_mute", send)
                                    WxToggle("保存到通讯录", w.conversation().saved, "wx_save_group", send)
                                }
                            }
                            WxPage.PHOTOS -> {
                                WxNote("已选 ${w.selectedPhotos.size} 张。再点已选图片，可以取消选择。")
                                (0..2).forEach { index ->
                                    Column(Modifier.fillMaxWidth().wxTag("wx_photo_$index", focus).clip(RoundedCornerShape(12.dp)).clickable { send(PracticeEvent.Click("wx_photo_$index")) }.padding(8.dp)) {
                                        PhotoArt(Modifier.fillMaxWidth().height(110.dp), index)
                                        Text("${if (index in w.selectedPhotos) "✓ 已选中" else "○ 未选"} · ${listOf("山间小屋", "花园", "海边")[index]}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                WxAction("预览（${w.selectedPhotos.size}张）", "wx_preview", send)
                            }
                            WxPage.PHOTO_PREVIEW -> {
                                WxNote("发给：${w.name()}\n这次选择 ${w.selectedPhotos.size} 张图片")
                                w.selectedPhotos.sorted().forEach { PhotoArt(Modifier.fillMaxWidth().height(150.dp), it) }
                                WxAction("发送图片", "wx_send_photos", send)
                                WxAction("返回继续选择", "wx_back", send)
                            }
                            WxPage.RECEIVED_PHOTO -> {
                                val dispatch by rememberUpdatedState(send)
                                Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp)).wxTag("wx_photo_surface", focus)
                                    .pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> dispatch(PracticeEvent.Gesture("wx_zoom", zoom)) } }
                                    .pointerInput(w.zoom) { detectTapGestures(onDoubleTap = { dispatch(PracticeEvent.Gesture("wx_zoom", if (w.zoom > 1.2f) .5f else 2f)) }, onLongPress = { dispatch(PracticeEvent.Gesture("wx_photo_menu")) }) }) {
                                    PhotoArt(Modifier.fillMaxSize().graphicsLayer { scaleX = w.zoom; scaleY = w.zoom }, w.previewPhoto)
                                }
                                Text("放大倍数：${"%.1f".format(java.util.Locale.ROOT, w.zoom)}", style = MaterialTheme.typography.bodySmall)
                                WxNote("双击或双指放大。长按图片，可以找到保存。")
                                if (w.dialog == "save_photo") { WxNote("图片操作"); WxAction("保存图片", "wx_save_photo", send); WxAction("取消", "wx_dismiss", send) }
                            }
                            WxPage.SAVED_ALBUM -> {
                                WxNote("这是教学用的模拟相册。真实手机通常从桌面的图库或相册查看。")
                                if (w.savedPhotos.isEmpty()) Text("还没有保存的示例图", style = MaterialTheme.typography.bodyLarge)
                                w.savedPhotos.sorted().forEach { photo ->
                                    Column(Modifier.fillMaxWidth().wxTag("wx_saved_photo_$photo", focus).clickable { send(PracticeEvent.Click("wx_saved_photo_$photo")) }.padding(8.dp)) {
                                        PhotoArt(Modifier.fillMaxWidth().height(130.dp), photo)
                                        Text("已保存 · 点开查看", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            WxPage.RECORD_SEARCH -> { WxNote("只搜索与 ${w.name()} 的聊天记录"); WxInput("输入关键词，例如：周末", w.query, send); WxAction("查找", "wx_do_record_search", send) }
                            WxPage.RECORD_RESULTS -> {
                                WxNote("关键词：${w.query}\n会话：${w.name()}")
                                if (w.recordMatches().isEmpty()) { Text("没有匹配的聊天记录", style = MaterialTheme.typography.bodyLarge); WxAction("换个词再找", "wx_back", send) }
                                w.recordMatches().forEach { message -> WxAction(message.text, if (message.id == 101) "wx_record_result" else "wx_record_result_${message.id}", send) }
                            }
                            WxPage.GROUPS -> {
                                val saved = w.conversations.filter { it.group && it.saved }
                                if (saved.isEmpty()) WxNote("还没有保存到通讯录的群。可以从微信列表进入群设置，把常用群保存下来。")
                                saved.forEach { WxEntry(it.title, "已保存到通讯录", "wx_group_${it.id}", send, group = true) }
                            }
                            WxPage.CALL -> {
                                WxAvatar(w.name())
                                Text(if (w.call.kind == WxCallKind.VOICE) "语音通话（模拟）" else "视频通话（模拟）", style = MaterialTheme.typography.titleMedium)
                                Text(when(w.call.status) { WxCallStatus.DIALING -> "呼叫中 · 等待对方接听"; WxCallStatus.INCOMING -> "对方正在呼叫你"; WxCallStatus.CONNECTED -> "通话中"; else -> "通话已结束" }, style = MaterialTheme.typography.bodyLarge)
                                if (w.call.kind == WxCallKind.VIDEO) Surface(color = Color(0xFFDDE9DF), shape = RoundedCornerShape(12.dp)) { Text(if (w.call.camera) "示例视频画面\n课堂不使用摄像头" else "摄像头已关闭（模拟）", Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.bodyMedium) }
                                if (w.call.status == WxCallStatus.INCOMING) { WxAction("接听", "wx_accept", send); WxAction("拒绝", "wx_reject", send, danger = true) }
                                else {
                                    if (w.call.status == WxCallStatus.CONNECTED) {
                                        WxAction(if (w.call.muted) "麦克风：已静音" else "麦克风：未静音", "wx_call_mic", send)
                                        if (w.call.kind == WxCallKind.VIDEO) WxAction(if (w.call.camera) "关闭摄像头（模拟）" else "打开摄像头（模拟）", "wx_call_camera", send)
                                    }
                                    WxAction("挂断", "wx_hangup", send, danger = true)
                                }
                            }
                        }
                    }
                    if (root) {
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth().background(Color(0xFFF7F7F7))) {
                            listOf(Triple(WxPage.CHATS, "微信", "chats"), Triple(WxPage.CONTACTS, "通讯录", "contacts"), Triple(WxPage.DISCOVER, "发现", "discover"), Triple(WxPage.ME, "我", "me")).forEachIndexed { index, (page, text, tag) ->
                                Column(Modifier.weight(1f).heightIn(min = 64.dp).wxTag("wx_tab_$tag", focus).clickable { send(PracticeEvent.Click("wx_tab_$tag")) }.padding(horizontal = 2.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    TabArt(index, page == w.page)
                                    Text(text, style = MaterialTheme.typography.bodySmall, color = if (page == w.page) WxGreen else Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
            if (w.lessonId == "WX_REMARK") WxNote("备注效果对照\n自己看到：${w.contact("lin").displayName}\n通讯录、聊天标题、搜索使用同一备注。\n对方原昵称：${w.contact("lin").nickname}\n只改变自己的显示。")
            if (w.lessonId == "WX_MUTE") {
                TeachingBox("看看消息提醒的变化") {
                    WxNote("本课假设微信及手机通知允许。先返回微信列表，再模拟收到消息。")
                    WxAction("模拟收到普通群消息", "wx_incoming_group", send)
                    w.alert?.let { alert -> WxNote("提醒效果预览\n消息：已收到\n声音：${if (alert.sound) "会提醒" else "不提醒"}\n振动：${if (alert.vibration) "会提醒" else "不提醒"}\n横幅：${if (alert.banner) "会显示" else "不显示"}\n${alert.message}") }
                    WxNote("这里仅比较普通群消息。@我、@所有人、群公告和通话可能有另外的例外规则或设置，请以本机版本为准。未读提示的样式也可能不同。")
                }
            }
            if (w.lessonId == "WX_PIN" && w.page == WxPage.CHATS) TeachingBox("看看置顶后的位置") { WxAction("模拟其他人来消息", "wx_other_message", send) }
            if (w.lessonId == "WX_CALL") TeachingBox("通话演示工具") {
                WxNote("这些按钮由课堂提供，用来模拟对方的动作。没有真实通话、声音或摄像头画面。")
                if (w.page == WxPage.CALL && w.call.status == WxCallStatus.DIALING) WxAction("模拟对方接听", "wx_connect_call", send)
                if (w.page == WxPage.CHAT) { WxAction("模拟语音来电", "wx_incoming_voice", send); WxAction("模拟视频来电", "wx_incoming_video", send) }
            }
            if (w.lessonId == "WX_SAVE_PHOTO" && w.page == WxPage.ME) TeachingBox("教学相册入口") { WxAction("打开模拟相册", "wx_saved_album", send) }
        }
    }
}

@Composable
private fun TeachingBox(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Warm, shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); content() } }
}

@Composable
private fun VoiceHoldButton(recording: Boolean, send: (PracticeEvent) -> Unit) {
    val dispatch by rememberUpdatedState(send)
    var cancel by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().heightIn(min = 90.dp).wxTag("wx_hold_voice", WxFocus.current).background(if (recording) Color(0xFFDDEBDF) else Color.White, RoundedCornerShape(14.dp))
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume(); cancel = false
                dispatch(PracticeEvent.Gesture("wx_voice_start"))
                var pressed = true
                var elapsed = 0L
                while (pressed) {
                    val e = awaitPointerEvent()
                    val point = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (point.position.y - down.position.y < -48.dp.toPx()) cancel = true
                    elapsed = point.uptimeMillis - down.uptimeMillis
                    pressed = point.pressed; point.consume()
                }
                dispatch(PracticeEvent.Gesture(if (cancel) "wx_voice_cancel" else if (elapsed < 350) "wx_voice_short" else "wx_voice_finish"))
            }
        }.padding(16.dp).semantics { contentDescription = "按住说话，正常松开发送，向上滑后松开取消；模拟录音" }, contentAlignment = Alignment.Center) {
        Text(if (!recording) "按住说话（模拟）" else if (cancel) "松开取消本次录音" else "录音中 · 上滑可取消", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = if (cancel) Color(0xFFAD2929) else WxGreen)
    }
}

@Composable
private fun TabArt(index: Int, selected: Boolean) {
    val color = if (selected) WxGreen else Muted
    Canvas(Modifier.size(28.dp)) {
        val u = size.width / 28
        if (index == 0) {
            drawRoundRect(color, Offset(3*u, 4*u), Size(22*u, 16*u), androidx.compose.ui.geometry.CornerRadius(5*u), style = Stroke(2*u))
            drawPath(Path().apply { moveTo(8*u, 19*u); lineTo(6*u, 24*u); lineTo(13*u, 20*u) }, color, style = Stroke(2*u))
            drawCircle(color, u, Offset(10*u, 12*u)); drawCircle(color, u, Offset(18*u, 12*u))
        } else if (index == 2) {
            drawCircle(color, 11*u, center, style = Stroke(2*u))
            drawPath(Path().apply { moveTo(18*u, 9*u); lineTo(15*u, 16*u); lineTo(9*u, 19*u); lineTo(12*u, 12*u); close() }, color)
        } else {
            drawCircle(color, 5*u, Offset(14*u, 8*u), style = Stroke(2*u))
            drawArc(color, 180f, 180f, false, Offset(4*u, 15*u), Size(20*u, 15*u), style = Stroke(2*u))
            if (index == 1) drawLine(color, Offset(23*u, 6*u), Offset(27*u, 6*u), 2*u)
        }
    }
}
