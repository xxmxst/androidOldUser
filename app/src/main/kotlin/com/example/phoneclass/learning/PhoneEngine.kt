package com.example.phoneclass.learning

/** Local phone navigation and lesson effects. All settings and apps are fictional. */
object PhoneEngine {
    fun initial(scene: Scene) = PracticeState(
        enabled = scene in setOf(Scene.APP_NETWORK, Scene.NOTIFICATION),
        secondary = scene == Scene.APP_NETWORK,
        value = if (scene == Scene.FONT) 22f else 30f,
        phone = PhoneState()
    )
    fun complete(scene: Scene, s: PracticeState): Boolean {
        val p = s.phone ?: return false
        return when(scene) {
            Scene.TAP -> p.overlay == PhoneOverlay.ALBUM_MENU
            Scene.VERTICAL, Scene.SEARCH -> p.screen == PhoneScreen.COMMUNITY_ALBUM
            Scene.HORIZONTAL -> p.screen == PhoneScreen.HOME && s.index == 2
            Scene.NAVIGATION -> p.screen == PhoneScreen.HOME
            Scene.MENU -> p.screen == PhoneScreen.APP_FONT
            Scene.SWITCH -> s.enabled && p.overlay == null
            Scene.WIFI -> s.enabled && s.connected
            Scene.DATA -> s.secondary && s.connected
            Scene.BUDGET -> s.budget == 2 && s.enabled && s.used > 2
            Scene.APP_NETWORK -> !s.enabled && s.secondary
            Scene.PERMISSION -> s.permission == true && p.screen == PhoneScreen.SCANNER
            Scene.NOTIFICATION -> s.enabled && s.secondary
            Scene.SOUND -> s.value >= 60 && s.brightness >= 60
            Scene.FONT -> s.value >= 28 && p.screen == PhoneScreen.DISPLAY
            Scene.CARE -> s.enabled && s.secondary
            Scene.INPUT -> p.noteSaved && s.text.isNotBlank()
            Scene.CHAT -> s.messages.isNotEmpty() && s.text.isEmpty()
            Scene.PHOTO -> p.screen == PhoneScreen.ALBUMS
            Scene.SEND_PHOTO -> s.secondary && p.screen == PhoneScreen.CHAT
            Scene.SCAN -> s.permission == true && p.screen == PhoneScreen.SCAN_RESULT
            Scene.INSTALL -> !s.installed && p.screen == PhoneScreen.HOME
            Scene.WECHAT -> false
        }
    }
    fun reduce(lesson: Lesson, before: PracticeState, event: PracticeEvent): PracticeState {
        val scene = lesson.scene
        var s = before.copy(feedback = "")
        var p = s.phone ?: PhoneState()
        fun mark(key: String) { s = s.copy(achieved = s.achieved + key) }
        fun say(text: String) { s = s.copy(feedback = text) }
        fun cache() { p.activeApp?.let { app ->
            p = p.copy(appStacks = p.appStacks + (app to p.stack.filter { it !in setOf(PhoneScreen.HOME, PhoneScreen.RECENTS) }), recentApps = listOf(app) + p.recentApps.filter { it != app })
        } }
        fun enter(screen: PhoneScreen, label: String = PhoneCatalog.title(screen)) {
            if (screen !in PhoneLessons.allowed(scene)) {
                p = p.copy(screen = PhoneScreen.INFO, stack = p.stack + PhoneScreen.INFO, infoTitle = label, overlay = null)
                say("这里先认识入口位置，可以点返回继续当前课程。")
                return
            }
            p = p.copy(screen = screen, stack = p.stack + screen, overlay = null)
            mark(PhoneCatalog.visit(screen))
            if (screen == PhoneScreen.APP_FONT && scene == Scene.MENU) mark("font")
            cache()
        }
        fun home() {
            cache()
            p = p.copy(screen = PhoneScreen.HOME, stack = listOf(PhoneScreen.HOME), activeApp = null, overlay = null, notificationBanner = false)
            if (scene == Scene.NAVIGATION && "back" in s.achieved) mark("home")
            if (scene == Scene.CARE && s.enabled) mark("care_home")
        }
        fun back() {
            if (p.overlay != null) { p = p.copy(overlay = null); return }
            val previous = p.screen
            if (p.stack.size > 1) {
                val stack = p.stack.dropLast(1)
                p = p.copy(screen = stack.last(), stack = stack)
                if (p.screen == PhoneScreen.HOME) p = p.copy(activeApp = null)
            }
            if (previous == PhoneScreen.PHOTO) {
                if (scene == Scene.NAVIGATION) mark("back")
                if (scene == Scene.PHOTO && "zoomed" in s.achieved) mark("photo_back")
            }
            if (previous == PhoneScreen.FONT && scene == Scene.FONT && s.value >= 28) mark("applied")
            cache()
        }
        fun openApp(app: PhoneApp) {
            cache()
            val root = PhoneCatalog.appRoots.getValue(app)
            if (root !in PhoneLessons.allowed(scene) || (app == PhoneApp.WEATHER && !s.installed)) {
                enter(PhoneScreen.INFO, PhoneCatalog.appNames.getValue(app)); return
            }
            val stack = p.appStacks[app].orEmpty().ifEmpty { listOf(root) }
            p = p.copy(screen = stack.last(), stack = listOf(PhoneScreen.HOME) + stack, activeApp = app, overlay = null)
            mark(PhoneCatalog.visit(root))
            if (scene == Scene.TAP && app == PhoneApp.GALLERY) mark("tap")
            cache()
        }
        fun scan() {
            when(s.permission) {
                null -> p = p.copy(overlay = PhoneOverlay.PERMISSION)
                false -> if (scene == Scene.SCAN) p = p.copy(overlay = PhoneOverlay.PERMISSION) else say("相机未允许，扫码暂不可用。回桌面，到设置里找到安心生活的相机权限。")
                true -> { enter(PhoneScreen.SCANNER); if (scene == Scene.PERMISSION) mark("scanned") }
            }
        }
        fun finish() = s.copy(phone = p, page = "main", dialog = null)
        val click = (event as? PracticeEvent.Click)?.target
        if (click == "nav_home") { home(); return finish() }
        if (click == "nav_back" || click == "header_back" || click == "back" || click == "photo_back") { back(); return finish() }
        if (click == "nav_recents") { cache(); p = p.copy(screen = PhoneScreen.RECENTS, stack = listOf(PhoneScreen.HOME, PhoneScreen.RECENTS), activeApp = null, overlay = null); return finish() }
        if (click?.startsWith("recent_") == true && p.screen == PhoneScreen.RECENTS) {
            val app = runCatching { PhoneApp.valueOf(click.removePrefix("recent_")) }.getOrNull()
            if (app != null && app in p.recentApps) openApp(app)
            return finish()
        }

        if (event is PracticeEvent.Input) {
            val editable = p.overlay in setOf(PhoneOverlay.WIFI_PASSWORD, PhoneOverlay.QUOTA) || (p.overlay == null && p.screen in setOf(PhoneScreen.NOTE_EDIT, PhoneScreen.CHAT, PhoneScreen.SEARCH, PhoneScreen.MARKET))
            if (editable) {
                val text = event.text.take(120)
                if (scene == Scene.INPUT && p.screen == PhoneScreen.NOTE_EDIT) {
                    if (text.isNotBlank()) mark("typed")
                    if (text.length < s.text.length) mark("deleted")
                }
                if (scene == Scene.CHAT && text.isNotBlank()) mark("draft")
                s = s.copy(text = text)
            }
            return finish()
        }

        val overlay = p.overlay
        if (overlay != null) {
            when(overlay) {
                PhoneOverlay.WIFI_PASSWORD -> when(click) {
                    "connect" -> if (s.text == "12345678") { s = s.copy(connected = true, text = ""); p = p.copy(overlay = null); mark("connected"); say("温暖的家：已连接。") } else say("密码不匹配。练习密码是 12345678，请修改后重试。")
                    "cancel", "dismiss" -> { p = p.copy(overlay = null); s = s.copy(text = "") }
                }
                PhoneOverlay.QUOTA -> when(click) {
                    "quota_confirm" -> {
                        val value = s.text.trim().toIntOrNull()
                        if (value != null && value in 1..999) {
                            s = s.copy(budget = value, text = ""); p = p.copy(overlay = null)
                            if (value == 2) mark("budget") else say("已保存 ${value}GB；本课请设为 2GB，再比较超额效果。")
                        } else say("请输入 1 到 999 之间的数字，单位为 GB。")
                    }
                    "cancel", "dismiss" -> p = p.copy(overlay = null)
                }
                PhoneOverlay.RESET -> when(click) {
                    "cancel" -> { p = p.copy(overlay = null); if (s.enabled) mark("cancel"); say("已取消，阅读提醒仍然保持开启。") }
                    "confirm" -> { s = s.copy(enabled = false, achieved = s.achieved - setOf("on", "dialog", "cancel")); p = p.copy(overlay = null); say("已恢复默认，可以再打开并练习取消。") }
                }
                PhoneOverlay.PERMISSION -> when(click) {
                    "deny" -> { s = s.copy(permission = false, achieved = s.achieved - "scanned"); p = p.copy(overlay = null); if (scene == Scene.PERMISSION) mark("denied"); say("已拒绝相机权限，扫码暂不可用。") }
                    "allow" -> { s = s.copy(permission = true); p = p.copy(overlay = null); if (scene == Scene.SCAN) mark("scan_allowed"); scan() }
                    "cancel", "dismiss" -> p = p.copy(overlay = null)
                }
                PhoneOverlay.APP_MORE -> when(click) {
                    "settings" -> { p = p.copy(overlay = null); enter(PhoneScreen.COMMUNITY_SETTINGS); if (scene == Scene.MENU) mark("settings") }
                    "scan" -> { p = p.copy(overlay = null); if (scene == Scene.SCAN) mark("scan_menu"); scan() }
                    "cancel", "dismiss" -> p = p.copy(overlay = null)
                }
                PhoneOverlay.ATTACHMENTS -> when(click) {
                    "album" -> { p = p.copy(overlay = null); enter(PhoneScreen.PICKER); mark("picker") }
                    "cancel", "dismiss" -> p = p.copy(overlay = null)
                }
                PhoneOverlay.ALBUM_MENU -> if (click in setOf("cancel", "dismiss", "confirm")) p = p.copy(overlay = null)
                PhoneOverlay.WEATHER_MENU -> when(click) { "uninstall" -> p = p.copy(overlay = PhoneOverlay.UNINSTALL); "cancel", "dismiss" -> p = p.copy(overlay = null) }
                PhoneOverlay.UNINSTALL -> when(click) {
                    "confirm" -> { s = s.copy(installed = false); mark("uninstalled"); p = p.copy(overlay = null, appStacks = p.appStacks - PhoneApp.WEATHER, recentApps = p.recentApps - PhoneApp.WEATHER); say("已卸载示例软件，桌面图标已经移除。") }
                    "cancel", "dismiss" -> { p = p.copy(overlay = null); say("已取消卸载，软件和图标仍然保留。") }
                }
            }
            return finish()
        }

        if (event is PracticeEvent.Adjust) {
            when {
                event.target == "volume" && p.screen == PhoneScreen.SOUND && scene == Scene.SOUND -> { s = s.copy(value = event.value.coerceIn(0f, 100f)); if (s.value >= 60) mark("volume") }
                event.target == "brightness" && p.screen == PhoneScreen.DISPLAY && scene == Scene.SOUND -> { s = s.copy(brightness = event.value.coerceIn(0f, 100f)); if (s.brightness >= 60) mark("brightness") }
                event.target == "font_slider" && p.screen == PhoneScreen.FONT && scene == Scene.FONT -> { s = s.copy(value = event.value.coerceIn(20f, 36f)); if (s.value >= 28) mark("font_changed") }
                event.target == "app_font" && p.screen == PhoneScreen.APP_FONT -> s = s.copy(value = event.value.coerceIn(20f, 36f))
            }
            return finish()
        }
        if (event is PracticeEvent.Gesture) {
            when {
                event.kind == "long" && scene == Scene.TAP && p.screen == PhoneScreen.HOME -> { p = p.copy(overlay = PhoneOverlay.ALBUM_MENU); mark("long") }
                event.kind == "long_weather" && scene == Scene.INSTALL && p.screen == PhoneScreen.HOME && s.installed -> p = p.copy(overlay = PhoneOverlay.WEATHER_MENU)
                event.kind == "scroll" && scene == Scene.VERTICAL && p.screen == PhoneScreen.COMMUNITY_LIST -> mark("scroll")
                event.kind in setOf("left", "right") && p.screen == PhoneScreen.HOME && scene == Scene.HORIZONTAL -> {
                    s = s.copy(index = (s.index + if (event.kind == "left") 1 else -1).coerceIn(0, 2)); if (event.kind == "left") mark("swipe"); if (s.index == 2) mark("third")
                }
                event.kind == "zoom" && p.screen == PhoneScreen.PHOTO -> { s = s.copy(zoom = (s.zoom * event.amount).coerceIn(1f, 3f)); if (s.zoom > 1.2f) mark("zoomed") }
            }
            return finish()
        }

        if (click?.startsWith("app_") == true && p.screen == PhoneScreen.HOME) {
            val app = runCatching { PhoneApp.valueOf(click.removePrefix("app_").uppercase()) }.getOrNull()
            if (app != null) openApp(app)
            return finish()
        }
        val link = PhoneCatalog.links(p.screen).firstOrNull { it.target == click }
        if (link != null) { enter(link.to, link.label); return finish() }
        when {
            click == "more" && p.screen == PhoneScreen.COMMUNITY -> { p = p.copy(overlay = PhoneOverlay.APP_MORE); if (scene == Scene.MENU) mark("menu") }
            click == "route_search" && p.screen == PhoneScreen.COMMUNITY -> { enter(PhoneScreen.SEARCH); s = s.copy(text = "") }
            click == "family_album" && p.screen == PhoneScreen.COMMUNITY_LIST -> { mark("found"); enter(PhoneScreen.COMMUNITY_ALBUM) }
            click == "open_photo" && p.screen == PhoneScreen.ALBUMS -> { enter(PhoneScreen.PHOTO); if (scene == Scene.NAVIGATION) mark("opened"); if (scene == Scene.PHOTO) mark("photo_open") }
            click == "toggle" && p.screen == PhoneScreen.WLAN && scene == Scene.WIFI -> { s = s.copy(enabled = !s.enabled, connected = false); if (s.enabled) mark("wifi_on") }
            click in setOf("toggle", "wifi_toggle") && p.screen == PhoneScreen.WLAN && scene == Scene.DATA -> s = s.copy(secondary = !s.secondary, connected = !s.secondary)
            click == "network" && p.screen == PhoneScreen.WLAN -> when(scene) {
                Scene.WIFI -> if (s.enabled) { p = p.copy(overlay = PhoneOverlay.WIFI_PASSWORD); s = s.copy(text = ""); mark("network") } else say("先打开 WLAN，再选择网络。")
                Scene.DATA -> if (s.secondary) { s = s.copy(connected = true); say("已连接预先保存的家庭网络。") }
                else -> Unit
            }
            click == "toggle" && p.screen == PhoneScreen.MOBILE_DATA && scene == Scene.DATA -> s = s.copy(enabled = !s.enabled)
            click == "try_network" && scene == Scene.DATA && PhoneCatalog.visit(PhoneScreen.MOBILE_DATA) in s.achieved -> when {
                s.secondary && s.connected -> { mark("wifi"); say("已通过家庭 WLAN 上网。当前没有使用手机卡的移动数据。") }
                s.enabled -> { mark("mobile"); say("已通过手机卡移动数据上网，会使用套餐流量。") }
                else -> say("两种连接都关闭了，暂时无法上网。")
            }
            click == "quota_row" && p.screen == PhoneScreen.PLAN -> { p = p.copy(overlay = PhoneOverlay.QUOTA); s = s.copy(text = if (s.budget > 0) s.budget.toString() else "") }
            click == "toggle" && p.screen == PhoneScreen.PLAN && scene == Scene.BUDGET -> { s = s.copy(enabled = !s.enabled); if (s.enabled) mark("stop") }
            click == "download" && scene == Scene.BUDGET && PhoneCatalog.visit(PhoneScreen.PLAN) in s.achieved -> {
                if (s.enabled && s.budget > 0 && s.used > s.budget) { if (s.budget == 2) mark("blocked"); say("移动数据已停止，不能继续下载。") }
                else { s = s.copy(used = s.used + .3f); if (s.enabled && s.budget == 2 && s.used > 2) { mark("blocked"); say("用量 2.1GB，超过 2GB 限额：模拟移动数据已停止。") } else say("下载完成。请比较限额和已用流量。") }
            }
            click == "toggle" && p.screen == PhoneScreen.APP_NETWORK && scene == Scene.APP_NETWORK -> { s = s.copy(enabled = !s.enabled); if (!s.enabled) mark("restricted") }
            click == "wifi_toggle" && p.screen == PhoneScreen.APP_NETWORK && scene == Scene.APP_NETWORK -> s = s.copy(secondary = !s.secondary)
            click == "test_mobile" && scene == Scene.APP_NETWORK && PhoneCatalog.visit(PhoneScreen.APP_NETWORK) in s.achieved -> if (!s.enabled) { mark("mobile_blocked"); say("访问被阻止：乐看视频不允许使用移动数据。") } else say("当前仍允许这个软件使用移动数据。")
            click == "test_wifi" && scene == Scene.APP_NETWORK && PhoneCatalog.visit(PhoneScreen.APP_NETWORK) in s.achieved -> if (s.secondary) { mark("wifi_ok"); say("通过 WLAN 可以访问，其他开关保持原样。") } else say("WLAN 也被关闭，请保留它后重试。")
            click == "open_scan" && p.screen == PhoneScreen.SAFE -> scan()
            click == "allow" && p.screen == PhoneScreen.CAMERA && scene == Scene.PERMISSION -> { s = s.copy(permission = true); mark("allowed"); say("相机已设为使用时允许。回桌面，再打开安心生活扫码。") }
            click == "deny" && p.screen == PhoneScreen.CAMERA && scene == Scene.PERMISSION -> { s = s.copy(permission = false, achieved = s.achieved - "scanned"); mark("denied"); say("相机已设为不允许。") }
            click == "toggle" && p.screen == PhoneScreen.APP_NOTIFICATION && scene == Scene.NOTIFICATION -> s = s.copy(enabled = !s.enabled)
            click == "notify" && scene == Scene.NOTIFICATION && PhoneCatalog.visit(PhoneScreen.APP_NOTIFICATION) in s.achieved -> {
                s = s.copy(messages = s.messages + "社区消息：明天上午有活动。", secondary = s.enabled)
                p = p.copy(notificationBanner = s.enabled)
                if (s.enabled) { mark("notified"); say("消息已收到，模拟通知横幅出现了。") } else { mark("silent"); say("消息已收到，通知已关闭，所以没有横幅提醒。仍可打开软件查看。") }
            }
            click == "dismiss_banner" -> p = p.copy(notificationBanner = false)
            click == "toggle" && p.screen == PhoneScreen.CARE && scene == Scene.CARE -> { s = s.copy(enabled = !s.enabled); if (s.enabled) mark("simple") }
            click == "care_toggle" && p.screen == PhoneScreen.APP_CARE && scene == Scene.CARE -> { s = s.copy(secondary = !s.secondary); if (s.secondary) mark("care") }
            click == "toggle" && p.screen == PhoneScreen.READING && scene == Scene.SWITCH -> { s = s.copy(enabled = !s.enabled); if (s.enabled) mark("on") }
            click == "reset_dialog" && p.screen == PhoneScreen.READING && scene == Scene.SWITCH -> { p = p.copy(overlay = PhoneOverlay.RESET); mark("dialog") }
            click == "finish_input" && p.screen == PhoneScreen.NOTE_EDIT && scene == Scene.INPUT -> if (s.text.isNotBlank() && "deleted" in s.achieved) { p = p.copy(noteSaved = true); mark("finished"); back(); say("笔记已保存，输入和删除都练习过了。") } else say("先写字、删除，再重新写一句话。")
            click == "search" && p.screen == PhoneScreen.SEARCH -> { val found = s.text.isNotBlank() && "家庭相册".contains(s.text.trim()); s = s.copy(secondary = found); mark("searched"); enter(PhoneScreen.SEARCH_RESULTS); if (!found) say("没有找到。试试输入“相册”。") }
            click == "search_result" && p.screen == PhoneScreen.SEARCH_RESULTS && s.secondary -> { mark("result"); enter(PhoneScreen.COMMUNITY_ALBUM) }
            click == "send" && p.screen == PhoneScreen.CHAT -> if (s.text.isNotBlank()) { s = s.copy(messages = s.messages + s.text.trim(), text = ""); mark("sent"); say("消息已发送，输入框已经清空。") } else say("输入框还是空的，先写一句话。")
            click == "add_photo" && p.screen == PhoneScreen.CHAT -> p = p.copy(overlay = PhoneOverlay.ATTACHMENTS)
            click in setOf("photo_0", "photo_1", "photo_2") && p.screen == PhoneScreen.PICKER -> { val id = click!!.last().digitToInt(); s = s.copy(selected = if (s.selected == id) null else id); if (s.selected != null) mark("selected") }
            click == "preview_photo" && p.screen == PhoneScreen.PICKER -> if (s.selected != null) enter(PhoneScreen.PICKER_PREVIEW) else say("先选一张照片，再预览。")
            click == "send_photo" && p.screen in setOf(PhoneScreen.PICKER, PhoneScreen.PICKER_PREVIEW) -> if (s.selected != null) {
                s = s.copy(secondary = true); mark("photo_sent")
                val stack = p.stack.takeWhile { it != PhoneScreen.PICKER }
                p = p.copy(screen = PhoneScreen.CHAT, stack = stack); cache(); say("照片已发送给虚构的家人，原相册图片仍然保留。")
            } else say("先选择一张照片，再点发送。")
            click == "scan_code" && p.screen == PhoneScreen.SCANNER && s.permission == true -> { enter(PhoneScreen.SCAN_RESULT); mark("scan_result"); say("扫码结果：欢迎来到社区图书角。这是一段示例文字。") }
            click == "market_search" && p.screen == PhoneScreen.MARKET -> if (s.text.isNotBlank() && "每日天气".contains(s.text.trim())) enter(PhoneScreen.MARKET_RESULTS) else say("没有找到。请搜索“每日天气”。")
            click == "route_weather_detail" && p.screen == PhoneScreen.MARKET_RESULTS -> enter(PhoneScreen.WEATHER_DETAIL)
            click == "install" && p.screen == PhoneScreen.WEATHER_DETAIL && scene == Scene.INSTALL -> { s = s.copy(installed = true); mark("installed"); say("安装完成，桌面新增了每日天气图标。回桌面后可以长按卸载。") }
            click == "open_weather" && p.screen == PhoneScreen.WEATHER_DETAIL && s.installed -> { home(); openApp(PhoneApp.WEATHER) }
            else -> say("先看看上方目标，再试试页面里的入口。")
        }
        return finish()
    }
}
