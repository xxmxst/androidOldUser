package com.example.phoneclass.learning

/** Illustrations render the exact same phone components as live practice. */
object PhonePreviews {
    fun state(scene: Scene, index: Int): PracticeState {
        var s = PhoneEngine.initial(scene)
        val entry = PhoneLessons.entryScreen(scene)
        val page = if (index == 0) PhoneScreen.HOME else when(scene) {
            Scene.TAP, Scene.HORIZONTAL, Scene.INSTALL -> PhoneScreen.HOME
            Scene.NAVIGATION -> if (index == 1) PhoneScreen.PHOTO else PhoneScreen.ALBUMS
            Scene.MENU -> if (index == 1) PhoneScreen.COMMUNITY else PhoneScreen.APP_FONT
            Scene.PERMISSION -> if (index == 1) PhoneScreen.SAFE else PhoneScreen.CAMERA
            Scene.SOUND -> if (index == 1) PhoneScreen.SOUND else PhoneScreen.DISPLAY
            Scene.FONT -> if (index == 1) PhoneScreen.FONT else PhoneScreen.DISPLAY
            Scene.CARE -> if (index == 1) PhoneScreen.CARE else PhoneScreen.APP_CARE
            Scene.SEARCH -> if (index == 1) PhoneScreen.SEARCH else PhoneScreen.SEARCH_RESULTS
            Scene.PHOTO -> if (index == 1) PhoneScreen.PHOTO else PhoneScreen.ALBUMS
            Scene.SEND_PHOTO -> if (index == 1) PhoneScreen.PICKER else PhoneScreen.CHAT
            Scene.SCAN -> if (index == 1) PhoneScreen.COMMUNITY else PhoneScreen.SCAN_RESULT
            else -> entry
        }
        val overlay = if (index == 1) when(scene) {
            Scene.TAP -> PhoneOverlay.ALBUM_MENU
            Scene.MENU, Scene.SCAN -> PhoneOverlay.APP_MORE
            Scene.SWITCH -> PhoneOverlay.RESET
            Scene.WIFI -> PhoneOverlay.WIFI_PASSWORD
            Scene.PERMISSION -> PhoneOverlay.PERMISSION
            else -> null
        } else null
        if (index > 0) s = s.copy(
            enabled = scene in setOf(Scene.WIFI, Scene.DATA, Scene.BUDGET, Scene.SWITCH, Scene.CARE) || (scene == Scene.NOTIFICATION && index == 2),
            secondary = scene in setOf(Scene.APP_NETWORK, Scene.SEARCH) || (index == 2 && scene in setOf(Scene.DATA, Scene.CARE, Scene.SEND_PHOTO, Scene.NOTIFICATION)),
            permission = if (index == 2 && scene in setOf(Scene.PERMISSION, Scene.SCAN)) true else null,
            connected = index == 2 && scene in setOf(Scene.WIFI, Scene.DATA),
            value = if (scene == Scene.FONT) 32f else 70f, brightness = 75f,
            budget = if (scene == Scene.BUDGET) 2 else 0, used = if (index == 2 && scene == Scene.BUDGET) 2.1f else 1.8f,
            installed = scene == Scene.INSTALL && index == 1,
            text = when(scene) { Scene.INPUT -> "今天心情很好"; Scene.SEARCH -> "相册"; Scene.WIFI -> if (index == 1) "12345678" else ""; Scene.CHAT -> if (index == 1) "今天很好，放心吧" else ""; else -> "" },
            messages = if (scene == Scene.CHAT && index == 2) listOf("今天很好，放心吧") else emptyList(),
            selected = if (scene == Scene.SEND_PHOTO) 0 else null, zoom = if (scene == Scene.PHOTO && index == 1) 1.7f else 1f,
            index = if (scene == Scene.HORIZONTAL) index else 0
        )
        val path = PhoneCatalog.path(page)
        val app = PhoneCatalog.appRoots.entries.firstOrNull { it.value == path.firstOrNull() }?.key
        return s.copy(phone = PhoneState(screen = page, stack = listOf(PhoneScreen.HOME) + path, activeApp = app, overlay = overlay, notificationBanner = scene == Scene.NOTIFICATION && index == 2))
    }
    fun focus(scene: Scene, index: Int): String? = if (index == 0) PhoneLessons.route(scene).firstOrNull()?.let(PhoneCatalog::target) else when(scene) {
        Scene.WIFI -> if (index == 1) "connect" else "network"
        Scene.FONT -> if (index == 1) "font_slider" else null
        Scene.PERMISSION -> if (index == 1) "deny" else "allow"
        Scene.INSTALL -> if (index == 1) "app_weather" else null
        Scene.SEND_PHOTO -> if (index == 1) "photo_0" else null
        else -> null
    }
}
