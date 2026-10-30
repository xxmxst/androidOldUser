package com.example.phoneclass.learning

enum class WxPage { CHATS, CONTACTS, DISCOVER, ME, SEARCH, PROFILE, REMARK, CHAT, DETAILS, PHOTOS, PHOTO_PREVIEW, RECEIVED_PHOTO, SAVED_ALBUM, RECORD_SEARCH, RECORD_RESULTS, CALL, GROUPS, INFO }
enum class WxMessageKind { TEXT, PHOTO, VOICE }
enum class WxCallStatus { IDLE, DIALING, INCOMING, CONNECTED, ENDED }
enum class WxCallKind { VOICE, VIDEO }
data class WxContact(val id: String, val nickname: String, val identity: String, val remark: String = "") {
    val displayName get() = remark.ifBlank { nickname }
}
data class WxConversation(val id: String, val title: String = "", val group: Boolean = false, val muted: Boolean = false, val pinned: Boolean = false, val saved: Boolean = false, val joined: Boolean = true, val unread: Int = 0, val updated: Int = 0)
data class WxMessage(val id: Int, val conversation: String, val text: String, val kind: WxMessageKind = WxMessageKind.TEXT, val outgoing: Boolean = false, val photos: List<Int> = emptyList())
data class WxAlert(val message: String, val notifies: Boolean, val sound: Boolean = notifies, val vibration: Boolean = notifies, val banner: Boolean = notifies)
data class WxCall(val kind: WxCallKind = WxCallKind.VOICE, val status: WxCallStatus = WxCallStatus.IDLE, val outgoing: Boolean = true, val muted: Boolean = false, val camera: Boolean = true)
data class WechatState(
    val lessonId: String, val page: WxPage = WxPage.CHATS, val tab: WxPage = WxPage.CHATS,
    val contacts: List<WxContact> = listOf(WxContact("lin", "海阔天空", "家人小林"), WxContact("wang", "王阿姨", "邻居王阿姨"), WxContact("zhou", "周老师", "社区老师")),
    val conversations: List<WxConversation> = listOf(WxConversation("lin", updated = 2), WxConversation("wang", updated = 4), WxConversation("family", "幸福一家人", group = true, updated = 3), WxConversation("neighbor", "邻里交流群", group = true, updated = 1)),
    val messages: List<WxMessage> = listOf(WxMessage(101, "lin", "周末去公园走走，好吗？"), WxMessage(102, "lin", "给你看今天拍的照片", WxMessageKind.PHOTO, photos = listOf(0)), WxMessage(103, "wang", "今天社区有活动。"), WxMessage(104, "neighbor", "欢迎邻居们交流。"), WxMessage(105, "family", "一家人平安就好。")),
    val current: String = "lin", val profile: String = "lin", val profileFrom: WxPage = WxPage.CONTACTS,
    val searchFrom: WxPage = WxPage.CONTACTS, val query: String = "", val searched: Boolean = false,
    val remarkDraft: String = "", val drafts: Map<String, String> = emptyMap(),
    val selectedPhotos: Set<Int> = emptySet(), val previewPhoto: Int = 0, val zoom: Float = 1f,
    val photoFrom: WxPage = WxPage.CHAT, val savedPhotos: Set<Int> = emptySet(),
    val voiceMode: Boolean = false, val recording: Boolean = false, val playingVoice: Int? = null,
    val call: WxCall = WxCall(), val selectedRecord: Int? = null, val nextId: Int = 200,
    val dialog: String? = null, val alert: WxAlert? = null, val info: String = ""
) {
    fun contact(id: String) = contacts.first { it.id == id }
    fun conversation(id: String = current) = conversations.first { it.id == id }
    fun name(id: String = current): String = conversations.firstOrNull { it.id == id }?.let { if (it.group) it.title else contact(id).displayName } ?: contact(id).displayName
    fun sortedChats() = conversations.sortedWith(compareByDescending<WxConversation> { it.pinned }.thenByDescending { it.updated })
    fun contactMatches() = if (searched && query.isNotBlank()) contacts.filter { it.nickname.contains(query.trim(), ignoreCase = true) || it.remark.contains(query.trim(), ignoreCase = true) } else emptyList()
    fun recordMatches() = if (query.isNotBlank()) messages.filter { it.conversation == current && it.kind == WxMessageKind.TEXT && it.text.contains(query.trim(), ignoreCase = true) } else emptyList()
}
