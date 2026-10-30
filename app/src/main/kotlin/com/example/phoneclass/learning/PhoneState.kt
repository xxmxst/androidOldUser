package com.example.phoneclass.learning

enum class PhoneApp { SETTINGS, MANAGER, GALLERY, COMMUNITY, SAFE, NOTES, MARKET, WEATHER }
enum class PhoneScreen {
    HOME, RECENTS, SETTINGS, WLAN, MOBILE_NETWORK, MOBILE_DATA,
    MANAGER, TRAFFIC, MORE_TRAFFIC, PLAN, APP_NETWORK,
    APP_SERVICES, APP_LIST, APP_DETAILS, PERMISSIONS, CAMERA,
    NOTIFICATIONS, APP_NOTIFICATION, SOUND, DISPLAY, FONT, ACCESSIBILITY, CARE,
    GALLERY, ALBUMS, PHOTO, COMMUNITY, COMMUNITY_LIST, COMMUNITY_MY, COMMUNITY_SETTINGS,
    APP_FONT, READING, APP_CARE, MESSAGES, CHAT, PICKER, PICKER_PREVIEW,
    NOTE_LIST, NOTE_EDIT, SEARCH, SEARCH_RESULTS, COMMUNITY_ALBUM,
    SAFE, SCANNER, SCAN_RESULT, MARKET, MARKET_RESULTS, WEATHER_DETAIL, WEATHER, INFO
}
enum class PhoneOverlay { WIFI_PASSWORD, RESET, PERMISSION, QUOTA, APP_MORE, ATTACHMENTS, ALBUM_MENU, WEATHER_MENU, UNINSTALL }
data class PhoneState(
    val screen: PhoneScreen = PhoneScreen.HOME,
    val stack: List<PhoneScreen> = listOf(PhoneScreen.HOME),
    val activeApp: PhoneApp? = null,
    val recentApps: List<PhoneApp> = emptyList(),
    val appStacks: Map<PhoneApp, List<PhoneScreen>> = emptyMap(),
    val overlay: PhoneOverlay? = null,
    val notificationBanner: Boolean = false,
    val noteSaved: Boolean = false,
    val infoTitle: String = "认识这个入口"
)
data class PhoneLink(val target: String, val label: String, val to: PhoneScreen, val subtitle: String = "")

object PhoneCatalog {
    fun visit(screen: PhoneScreen) = "visit_${screen.name}"
    val appRoots = mapOf(PhoneApp.SETTINGS to PhoneScreen.SETTINGS, PhoneApp.MANAGER to PhoneScreen.MANAGER, PhoneApp.GALLERY to PhoneScreen.GALLERY, PhoneApp.COMMUNITY to PhoneScreen.COMMUNITY, PhoneApp.SAFE to PhoneScreen.SAFE, PhoneApp.NOTES to PhoneScreen.NOTE_LIST, PhoneApp.MARKET to PhoneScreen.MARKET, PhoneApp.WEATHER to PhoneScreen.WEATHER)
    val appNames = mapOf(PhoneApp.SETTINGS to "设置", PhoneApp.MANAGER to "手机管家", PhoneApp.GALLERY to "图库", PhoneApp.COMMUNITY to "邻里生活", PhoneApp.SAFE to "安心生活", PhoneApp.NOTES to "备忘录", PhoneApp.MARKET to "应用市场", PhoneApp.WEATHER to "每日天气")
    fun appTarget(app: PhoneApp) = "app_${app.name.lowercase()}"
    fun target(screen: PhoneScreen): String = when(screen) {
        PhoneScreen.MARKET_RESULTS -> "market_search"
        PhoneScreen.PHOTO -> "open_photo"
        PhoneScreen.COMMUNITY_SETTINGS -> "settings"
        PhoneScreen.APP_FONT -> "font_entry"
        else -> appRoots.entries.firstOrNull { it.value == screen }?.let { appTarget(it.key) } ?: "route_${screen.name.lowercase()}"
    }
    fun path(screen: PhoneScreen): List<PhoneScreen> = when (screen) {
        PhoneScreen.HOME, PhoneScreen.RECENTS, PhoneScreen.INFO -> emptyList()
        PhoneScreen.SETTINGS, PhoneScreen.MANAGER, PhoneScreen.GALLERY, PhoneScreen.COMMUNITY, PhoneScreen.SAFE, PhoneScreen.NOTE_LIST, PhoneScreen.MARKET, PhoneScreen.WEATHER -> listOf(screen)
        PhoneScreen.WLAN, PhoneScreen.MOBILE_NETWORK, PhoneScreen.APP_SERVICES, PhoneScreen.NOTIFICATIONS, PhoneScreen.SOUND, PhoneScreen.DISPLAY, PhoneScreen.ACCESSIBILITY -> listOf(PhoneScreen.SETTINGS, screen)
        PhoneScreen.MOBILE_DATA -> path(PhoneScreen.MOBILE_NETWORK) + screen
        PhoneScreen.TRAFFIC -> listOf(PhoneScreen.MANAGER, screen)
        PhoneScreen.MORE_TRAFFIC, PhoneScreen.APP_NETWORK -> path(PhoneScreen.TRAFFIC) + screen
        PhoneScreen.PLAN -> path(PhoneScreen.MORE_TRAFFIC) + screen
        PhoneScreen.APP_LIST -> path(PhoneScreen.APP_SERVICES) + screen
        PhoneScreen.APP_DETAILS -> path(PhoneScreen.APP_LIST) + screen
        PhoneScreen.PERMISSIONS -> path(PhoneScreen.APP_DETAILS) + screen
        PhoneScreen.CAMERA -> path(PhoneScreen.PERMISSIONS) + screen
        PhoneScreen.APP_NOTIFICATION -> path(PhoneScreen.NOTIFICATIONS) + screen
        PhoneScreen.FONT -> path(PhoneScreen.DISPLAY) + screen
        PhoneScreen.CARE -> path(PhoneScreen.ACCESSIBILITY) + screen
        PhoneScreen.ALBUMS -> listOf(PhoneScreen.GALLERY, screen)
        PhoneScreen.PHOTO -> path(PhoneScreen.ALBUMS) + screen
        PhoneScreen.COMMUNITY_LIST, PhoneScreen.COMMUNITY_MY, PhoneScreen.MESSAGES, PhoneScreen.SEARCH -> listOf(PhoneScreen.COMMUNITY, screen)
        PhoneScreen.COMMUNITY_SETTINGS -> path(PhoneScreen.COMMUNITY_MY) + screen
        PhoneScreen.APP_FONT, PhoneScreen.READING, PhoneScreen.APP_CARE -> path(PhoneScreen.COMMUNITY_SETTINGS) + screen
        PhoneScreen.CHAT -> path(PhoneScreen.MESSAGES) + screen
        PhoneScreen.PICKER -> path(PhoneScreen.CHAT) + screen
        PhoneScreen.PICKER_PREVIEW -> path(PhoneScreen.PICKER) + screen
        PhoneScreen.NOTE_EDIT -> listOf(PhoneScreen.NOTE_LIST, screen)
        PhoneScreen.SEARCH_RESULTS -> path(PhoneScreen.SEARCH) + screen
        PhoneScreen.COMMUNITY_ALBUM -> path(PhoneScreen.SEARCH_RESULTS) + screen
        PhoneScreen.SCANNER -> listOf(PhoneScreen.SAFE, screen)
        PhoneScreen.SCAN_RESULT -> path(PhoneScreen.SCANNER) + screen
        PhoneScreen.MARKET_RESULTS -> listOf(PhoneScreen.MARKET, screen)
        PhoneScreen.WEATHER_DETAIL -> path(PhoneScreen.MARKET_RESULTS) + screen
    }
    fun title(screen: PhoneScreen) = when(screen) {
        PhoneScreen.HOME -> "桌面"; PhoneScreen.RECENTS -> "最近任务"; PhoneScreen.SETTINGS -> "设置"; PhoneScreen.WLAN -> "WLAN"
        PhoneScreen.MANAGER -> "手机管家"; PhoneScreen.MOBILE_NETWORK -> "移动网络"; PhoneScreen.MOBILE_DATA -> "移动数据"; PhoneScreen.TRAFFIC -> "流量管理"; PhoneScreen.MORE_TRAFFIC -> "更多流量设置"; PhoneScreen.PLAN -> "套餐设置"; PhoneScreen.APP_NETWORK -> "应用联网"
        PhoneScreen.APP_SERVICES -> "应用和服务"; PhoneScreen.APP_LIST -> "应用管理"; PhoneScreen.APP_DETAILS -> "安心生活"; PhoneScreen.PERMISSIONS -> "应用权限"; PhoneScreen.CAMERA -> "相机权限"
        PhoneScreen.NOTIFICATIONS -> "通知和状态栏"; PhoneScreen.APP_NOTIFICATION -> "社区消息通知"; PhoneScreen.SOUND -> "声音和振动"; PhoneScreen.DISPLAY -> "显示和亮度"; PhoneScreen.FONT -> "字体大小与粗细"; PhoneScreen.ACCESSIBILITY -> "辅助功能"; PhoneScreen.CARE -> "长辈关怀"
        PhoneScreen.GALLERY -> "图库"; PhoneScreen.ALBUMS -> "家庭相册"; PhoneScreen.PHOTO -> "山间小屋"; PhoneScreen.COMMUNITY -> "邻里生活"; PhoneScreen.COMMUNITY_LIST -> "全部功能"; PhoneScreen.COMMUNITY_MY -> "我的"; PhoneScreen.COMMUNITY_SETTINGS -> "邻里生活设置"
        PhoneScreen.APP_FONT -> "软件字体大小"; PhoneScreen.READING -> "阅读提醒"; PhoneScreen.APP_CARE -> "软件关怀模式"; PhoneScreen.MESSAGES -> "消息"; PhoneScreen.CHAT -> "家人"; PhoneScreen.PICKER -> "选择图片"; PhoneScreen.PICKER_PREVIEW -> "图片预览"
        PhoneScreen.NOTE_LIST -> "备忘录"; PhoneScreen.NOTE_EDIT -> "新建笔记"; PhoneScreen.SEARCH -> "搜索"; PhoneScreen.SEARCH_RESULTS -> "搜索结果"; PhoneScreen.COMMUNITY_ALBUM -> "家庭相册"; PhoneScreen.SAFE -> "安心生活"; PhoneScreen.SCANNER -> "扫一扫"; PhoneScreen.SCAN_RESULT -> "识别结果"
        PhoneScreen.MARKET -> "应用市场"; PhoneScreen.MARKET_RESULTS -> "应用搜索结果"; PhoneScreen.WEATHER_DETAIL -> "应用详情"; PhoneScreen.WEATHER -> "每日天气"; PhoneScreen.INFO -> "认识这个入口"
    }
    private fun entriesOf(vararg pages: PhoneScreen) = pages.map { PhoneLink(target(it), title(it), it) }
    fun links(screen: PhoneScreen): List<PhoneLink> = when(screen) {
        PhoneScreen.SETTINGS -> entriesOf(PhoneScreen.WLAN, PhoneScreen.MOBILE_NETWORK, PhoneScreen.APP_SERVICES, PhoneScreen.NOTIFICATIONS, PhoneScreen.SOUND, PhoneScreen.DISPLAY, PhoneScreen.ACCESSIBILITY)
        PhoneScreen.MOBILE_NETWORK -> entriesOf(PhoneScreen.MOBILE_DATA)
        PhoneScreen.MANAGER -> entriesOf(PhoneScreen.TRAFFIC)
        PhoneScreen.TRAFFIC -> entriesOf(PhoneScreen.APP_NETWORK, PhoneScreen.MORE_TRAFFIC)
        PhoneScreen.MORE_TRAFFIC -> entriesOf(PhoneScreen.PLAN)
        PhoneScreen.APP_SERVICES -> entriesOf(PhoneScreen.APP_LIST)
        PhoneScreen.APP_LIST -> listOf(PhoneLink(target(PhoneScreen.APP_DETAILS), "安心生活", PhoneScreen.APP_DETAILS, "示例软件 · 扫码工具"))
        PhoneScreen.APP_DETAILS -> entriesOf(PhoneScreen.PERMISSIONS)
        PhoneScreen.PERMISSIONS -> entriesOf(PhoneScreen.CAMERA)
        PhoneScreen.NOTIFICATIONS -> listOf(PhoneLink(target(PhoneScreen.APP_NOTIFICATION), "社区消息", PhoneScreen.APP_NOTIFICATION))
        PhoneScreen.DISPLAY -> entriesOf(PhoneScreen.FONT)
        PhoneScreen.ACCESSIBILITY -> entriesOf(PhoneScreen.CARE)
        PhoneScreen.GALLERY -> listOf(PhoneLink(target(PhoneScreen.ALBUMS), "家庭相册", PhoneScreen.ALBUMS, "3 张示例照片"))
        PhoneScreen.COMMUNITY -> entriesOf(PhoneScreen.COMMUNITY_LIST, PhoneScreen.MESSAGES, PhoneScreen.COMMUNITY_MY)
        PhoneScreen.COMMUNITY_MY -> entriesOf(PhoneScreen.COMMUNITY_SETTINGS)
        PhoneScreen.COMMUNITY_SETTINGS -> entriesOf(PhoneScreen.APP_FONT, PhoneScreen.READING, PhoneScreen.APP_CARE)
        PhoneScreen.MESSAGES -> listOf(PhoneLink(target(PhoneScreen.CHAT), "家人", PhoneScreen.CHAT, "今天过得好吗？"))
        PhoneScreen.NOTE_LIST -> listOf(PhoneLink(target(PhoneScreen.NOTE_EDIT), "＋ 新建笔记", PhoneScreen.NOTE_EDIT))
        else -> emptyList()
    }
}


