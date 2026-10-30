package com.example.phoneclass.learning

object PhoneLessons {
    fun entryScreen(scene: Scene) = when(scene) {
        Scene.TAP, Scene.HORIZONTAL -> PhoneScreen.HOME
        Scene.VERTICAL -> PhoneScreen.COMMUNITY_LIST
        Scene.NAVIGATION, Scene.PHOTO -> PhoneScreen.ALBUMS
        Scene.MENU, Scene.SCAN -> PhoneScreen.COMMUNITY
        Scene.SWITCH -> PhoneScreen.READING
        Scene.WIFI -> PhoneScreen.WLAN
        Scene.DATA -> PhoneScreen.MOBILE_DATA
        Scene.BUDGET -> PhoneScreen.PLAN
        Scene.APP_NETWORK -> PhoneScreen.APP_NETWORK
        Scene.PERMISSION -> PhoneScreen.SAFE
        Scene.NOTIFICATION -> PhoneScreen.APP_NOTIFICATION
        Scene.SOUND -> PhoneScreen.SOUND
        Scene.FONT -> PhoneScreen.FONT
        Scene.CARE -> PhoneScreen.CARE
        Scene.INPUT -> PhoneScreen.NOTE_EDIT
        Scene.SEARCH -> PhoneScreen.SEARCH
        Scene.CHAT, Scene.SEND_PHOTO -> PhoneScreen.CHAT
        Scene.INSTALL -> PhoneScreen.WEATHER_DETAIL
        Scene.WECHAT -> error("WeChat has its own model")
    }
    fun route(scene: Scene) = PhoneCatalog.path(entryScreen(scene))
    fun routeGoals(screen: PhoneScreen) = PhoneCatalog.path(screen).map { page ->
        val instruction = when(page) {
            PhoneScreen.MARKET_RESULTS -> "在应用市场输入“每日天气”，点搜索"
            PhoneScreen.WEATHER_DETAIL -> "点搜索结果“每日天气”，进入应用详情"
            PhoneScreen.ALBUMS -> "在图库里点“家庭相册”"
            else -> "${if (PhoneCatalog.appRoots.containsValue(page)) "从桌面打开" else "在页面中找到"}“${PhoneCatalog.title(page)}”"
        }
        Goal(PhoneCatalog.visit(page), instruction, PhoneCatalog.target(page))
    }
    fun allowed(scene: Scene): Set<PhoneScreen> {
        val extra = when(scene) {
            Scene.TAP, Scene.NAVIGATION, Scene.PHOTO -> PhoneCatalog.path(PhoneScreen.PHOTO)
            Scene.VERTICAL -> listOf(PhoneScreen.COMMUNITY_ALBUM)
            Scene.MENU -> PhoneCatalog.path(PhoneScreen.APP_FONT)
            Scene.DATA -> PhoneCatalog.path(PhoneScreen.WLAN)
            Scene.PERMISSION -> PhoneCatalog.path(PhoneScreen.CAMERA) + listOf(PhoneScreen.SCANNER, PhoneScreen.SCAN_RESULT)
            Scene.NOTIFICATION -> PhoneCatalog.path(PhoneScreen.CHAT)
            Scene.SOUND -> PhoneCatalog.path(PhoneScreen.DISPLAY)
            Scene.CARE -> PhoneCatalog.path(PhoneScreen.APP_CARE)
            Scene.SEARCH -> listOf(PhoneScreen.SEARCH_RESULTS, PhoneScreen.COMMUNITY_ALBUM)
            Scene.SEND_PHOTO -> listOf(PhoneScreen.PICKER, PhoneScreen.PICKER_PREVIEW)
            Scene.SCAN -> listOf(PhoneScreen.SCANNER, PhoneScreen.SCAN_RESULT)
            Scene.INSTALL -> listOf(PhoneScreen.WEATHER)
            else -> emptyList()
        }
        return (route(scene) + extra + listOf(PhoneScreen.HOME, PhoneScreen.RECENTS, PhoneScreen.INFO)).toSet()
    }
    private fun g(key: String, instruction: String, target: String) = Goal(key, instruction, target)
    fun adapt(old: Lesson): Lesson {
        val scene = old.scene
        val basic = when(scene) {
            Scene.TAP -> listOf(g("tap", "点桌面“图库”，打开后回桌面", "app_gallery"), g("long", "回到桌面，长按“图库”图标", "app_gallery"))
            Scene.VERTICAL -> listOf(g("scroll", "在邻里生活的功能列表里向上滑动", "scroll_list"), g("found", "找到并点开“家庭相册”", "family_album"))
            Scene.HORIZONTAL -> listOf(g("swipe", "在模拟桌面的空白处向左滑动", "pager"), g("third", "继续向左滑到第三页“平安到家”", "pager"))
            Scene.NAVIGATION -> listOf(g("opened", "点开山间小屋照片", "open_photo"), g("back", "点手机里的返回键，退回相册", "nav_back"), g("home", "点手机里的圆圈，回到桌面", "nav_home"))
            Scene.MENU -> listOf(g("menu", "点邻里生活右上角的更多菜单", "more"), g("settings", "在菜单里点“设置”", "settings"), g("font", "点设置列表里的“字体大小”", "font_entry"))
            Scene.SWITCH -> listOf(g("on", "打开阅读提醒右边的开关", "toggle"), g("dialog", "点“恢复默认”，阅读确认弹窗", "reset_dialog"), g("cancel", "点“取消”，保留已开启状态", "cancel"))
            Scene.WIFI -> listOf(g("wifi_on", "打开 WLAN 右边的开关", "toggle"), g("network", "点网络列表中的“温暖的家”", "network"), g("connected", "在弹窗输入 12345678，再点连接", "connect"))
            Scene.DATA -> listOf(g("mobile", "打开移动数据，再点教学区“试着上网”", "try_network")) + routeGoals(PhoneScreen.WLAN) + g("wifi", "打开 WLAN 连接已保存的家庭网络，再试着上网", "try_network")
            Scene.BUDGET -> listOf(g("budget", "点套餐限额，输入 2GB 并确定", "quota_row"), g("stop", "打开超额停止移动数据", "toggle"), g("blocked", "在教学区模拟下载 0.3GB，查看超额结果", "download"))
            Scene.APP_NETWORK -> listOf(g("restricted", "关闭乐看视频的“移动数据”勾选", "toggle"), g("mobile_blocked", "在教学区用移动数据试用", "test_mobile"), g("wifi_ok", "保留 WLAN 勾选，在教学区用 WLAN 试用", "test_wifi"))
            Scene.PERMISSION -> listOf(g("denied", "在安心生活打开扫码，在授权弹窗点拒绝", "open_scan")) + routeGoals(PhoneScreen.CAMERA) + listOf(g("allowed", "在相机权限页选择“使用时允许”", "allow"), g("scanned", "回桌面，再打开安心生活的扫码", "open_scan"))
            Scene.NOTIFICATION -> listOf(g("silent", "关闭允许通知，再在教学区模拟来消息", "notify"), g("notified", "重新打开通知，再模拟来一条消息", "notify"))
            Scene.SOUND -> listOf(g("volume", "把媒体音量滑块调到 60% 以上", "volume")) + routeGoals(PhoneScreen.DISPLAY) + g("brightness", "在显示和亮度页，把亮度调到 60% 以上", "brightness")
            Scene.FONT -> listOf(g("font_changed", "拖动字号滑块，把预览字号调到 28 以上", "font_slider"), g("applied", "点返回，观察上一级列表的字体变化", "nav_back"))
            Scene.CARE -> listOf(g("simple", "打开长辈关怀的开关", "toggle"), g("care_home", "回到桌面，看看字体和图标的变化", "nav_home")) + routeGoals(PhoneScreen.APP_CARE) + g("care", "打开软件自己的关怀模式", "care_toggle")
            Scene.INPUT -> listOf(g("typed", "在新建笔记里输入一些文字", "text_input"), g("deleted", "用键盘退格删除，或全选后删除", "text_input"), g("finished", "重新写一句话，点右上角保存", "finish_input"))
            Scene.SEARCH -> listOf(g("searched", "输入“相册”，点搜索", "search"), g("result", "点结果列表中的“家庭相册”", "search_result"))
            Scene.CHAT -> listOf(g("draft", "核对对象是家人，再输入一句问候", "text_input"), g("sent", "确认内容后，点输入栏旁的发送", "send"))
            Scene.PHOTO -> listOf(g("photo_open", "在照片网格中点开山间小屋", "open_photo"), g("zoomed", "在大图上双击或双指张开放大", "photo"), g("photo_back", "点返回，回到相册网格", "nav_back"))
            Scene.SEND_PHOTO -> listOf(g("picker", "点聊天输入栏的加号，再选相册", "add_photo"), g("selected", "在图片网格里勾选一张图，可先预览", "photo_0"), g("photo_sent", "核对对象和图片，点发送图片", "send_photo"))
            Scene.SCAN -> listOf(g("scan_menu", "点右上角更多，在菜单里选择扫一扫", "more"), g("scan_allowed", "在模拟授权弹窗里允许相机", "allow"), g("scan_result", "在教学区点“模拟对准二维码”，读结果", "scan_code"))
            Scene.INSTALL -> listOf(g("installed", "在应用详情里点安装", "install"), g("uninstalled", "回桌面，长按每日天气 → 卸载 → 确认", "uninstall"))
            Scene.WECHAT -> error("WeChat stays unchanged")
        }
        val path = (listOf("桌面") + route(scene).map(PhoneCatalog::title)).joinToString(" → ")
        val extra = when(scene) {
            Scene.DATA -> "本课的练习手机已保存家庭 WLAN；打开后可以自动连接。先体验手机卡，再从设置切换到 WLAN。"
            Scene.PERMISSION -> "先在安心生活里拒绝相机，再回桌面，经设置 → 应用和服务 → 应用管理 → 安心生活 → 权限 → 相机恢复允许。"
            Scene.SOUND -> "媒体音量在声音和振动中调整，屏幕亮度在显示和亮度中调整。它们是两处设置。"
            Scene.FONT -> "进入字体大小与粗细，拖动滑块即可即时预览。返回后会看到列表文字的变化，无需另找应用按钮。"
            Scene.CARE -> "手机长辈关怀从设置 → 辅助功能进入；软件关怀从邻里生活 → 我的 → 设置进入。两个开关分别管理。"
            Scene.INPUT -> "在备忘录新建笔记，输入、退格删除、再重新输入。右上角保存用于完成这次写字练习。"
            Scene.INSTALL -> "先在应用市场搜索每日天气并安装，再回桌面长按新增图标，选择卸载并确认。"
            Scene.HORIZONTAL -> "桌面有三页，小圆点表示当前位置。手指向左看下一页，向右回上一页，最后找到平安到家。"
            else -> old.takeaway
        }
        val operation = when(scene) {
            Scene.FONT, Scene.CARE, Scene.INPUT, Scene.INSTALL, Scene.HORIZONTAL, Scene.PERMISSION, Scene.SOUND -> extra
            else -> old.panels[1].body
        }
        val panels = listOf(
            Panel("从桌面找到入口", "$path。${if (scene in setOf(Scene.FONT, Scene.CARE, Scene.INPUT, Scene.INSTALL, Scene.HORIZONTAL)) extra else old.panels[0].body}", path),
            Panel("看清页面，再动手", operation, "先看页面标题，再找到当前目标"),
            Panel("操作以后，看看变化", "$extra 返回上一级或回桌面会保留本次操作；重新开始才会清空这次模拟。", if (scene == Scene.FONT) "拖动即时预览，返回观察效果" else old.takeaway)
        )
        return old.copy(panels = panels, goals = (routeGoals(entryScreen(scene)) + basic).distinctBy { it.key }, summary = if (scene == Scene.FONT) "从设置里找到字号，拖动就能看变化" else old.summary, takeaway = if (scene in setOf(Scene.FONT, Scene.CARE, Scene.SOUND, Scene.INPUT, Scene.INSTALL)) extra else old.takeaway)
    }
}
