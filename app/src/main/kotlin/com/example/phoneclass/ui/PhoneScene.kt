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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.theme.*
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

private val PhoneBlue = Color(0xFF0A59F7)
private val PhoneGray = Color(0xFFF1F3F5)
private val PhoneFocus = staticCompositionLocalOf<String?> { null }
private val PhoneDiagram = staticCompositionLocalOf { false }
private fun Modifier.phoneTag(tag: String) = testTag("sim_$tag")
private fun Modifier.focusTag(tag: String, focus: String?) = phoneTag(tag).then(if (focus == tag) Modifier.border(3.dp, PhoneBlue, RoundedCornerShape(12.dp)) else Modifier)

@Composable
private fun PhoneAction(label: String, tag: String, send: (PracticeEvent) -> Unit) {
    val fm = LocalFocusManager.current
    Button(onClick = { fm.clearFocus(); send(PracticeEvent.Click(tag)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusTag(tag, PhoneFocus.current), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5EDFF), contentColor = PhoneBlue), contentPadding = PaddingValues(12.dp)) { Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun PhoneRow(label: String, tag: String, send: (PracticeEvent) -> Unit, sub: String = "", icon: Boolean = true, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).focusTag(tag, PhoneFocus.current).background(Color.White, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).clickable { send(PracticeEvent.Click(tag)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon) PhoneGlyph(tag, Modifier.size(32.dp))
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyLarge); if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = Muted) }
        if (trailing != null) trailing() else Text("›", color = Muted, fontSize = 24.sp)
    }
}

@Composable
private fun PhoneSwitch(label: String, checked: Boolean, tag: String = "toggle", send: (PracticeEvent) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).focusTag(tag, PhoneFocus.current).background(Color.White, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).toggleable(checked, role = Role.Switch, onValueChange = { send(PracticeEvent.Click(tag)) }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 6.dp)) { Text(label, style = MaterialTheme.typography.bodyLarge); Text(if (checked) "已开启" else "已关闭", style = MaterialTheme.typography.bodySmall, color = Muted) }
        Switch(checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = PhoneBlue))
    }
}

@Composable
private fun PhoneInput(label: String, s: PracticeState, send: (PracticeEvent) -> Unit, password: Boolean = false, multiline: Boolean = false, modifier: Modifier = Modifier) {
    val fm = LocalFocusManager.current
    OutlinedTextField(value = s.text, onValueChange = { send(PracticeEvent.Input(it)) }, modifier = modifier.fillMaxWidth().focusTag("text_input", PhoneFocus.current), label = { Text(label, style = MaterialTheme.typography.bodySmall) }, textStyle = MaterialTheme.typography.bodyLarge, singleLine = !multiline, minLines = if (multiline) 3 else 1, readOnly = PhoneDiagram.current, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.NumberPassword else KeyboardType.Text, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }), visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None)
}

@Composable
private fun PhoneSlider(label: String, value: Float, tag: String, send: (PracticeEvent) -> Unit, range: ClosedFloatingPointRange<Float> = 0f..100f) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) {
        Text("$label  ${value.toInt()}${if (range.endInclusive == 100f) "%" else ""}", style = MaterialTheme.typography.bodyLarge)
        Slider(value.coerceIn(range), { send(PracticeEvent.Adjust(tag, it)) }, valueRange = range, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusTag(tag, PhoneFocus.current))
    } }
}

@Composable
private fun PhoneNote(text: String) { Text(text, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.fillMaxWidth().padding(8.dp)) }

/** Small vector icons stay recognisable when the system font is enlarged. */
@Composable
private fun PhoneGlyph(kind: String, modifier: Modifier = Modifier) {
    val color = when { "gallery" in kind || "albums" in kind -> Color(0xFFE56A73); "manager" in kind || "traffic" in kind -> Color(0xFF16A595); "community" in kind || "messages" in kind -> Color(0xFF35A66F); "notes" in kind || "note_" in kind -> Color(0xFFE7AB34); "market" in kind -> Color(0xFFE45C52); "weather" in kind -> Color(0xFF308AE6); else -> Color(0xFF5D83AE) }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .25f))
        when {
            kind == "network" || "wlan" in kind || kind == "other_network" -> {
                drawArc(Color.White, 220f,100f,false,Offset(w*.15f,h*.2f),androidx.compose.ui.geometry.Size(w*.7f,h*.7f),style=Stroke(w*.065f))
                drawArc(Color.White,220f,100f,false,Offset(w*.29f,h*.37f),androidx.compose.ui.geometry.Size(w*.42f,h*.42f),style=Stroke(w*.065f))
                drawCircle(Color.White,w*.055f,Offset(w*.5f,h*.73f))
            }
            "settings" in kind -> {
                drawCircle(Color.White,w*.22f,style=Stroke(w*.075f)); (0..7).forEach { n -> val a=n*Math.PI/4; drawLine(Color.White,Offset(w*.5f+kotlin.math.cos(a).toFloat()*w*.2f,h*.5f+kotlin.math.sin(a).toFloat()*h*.2f),Offset(w*.5f+kotlin.math.cos(a).toFloat()*w*.34f,h*.5f+kotlin.math.sin(a).toFloat()*h*.34f),w*.09f) }
            }
            "market" in kind -> {
                drawRoundRect(Color.White,Offset(w*.22f,h*.38f),androidx.compose.ui.geometry.Size(w*.56f,h*.42f),androidx.compose.ui.geometry.CornerRadius(w*.05f),style=Stroke(w*.055f)); drawArc(Color.White,180f,180f,false,Offset(w*.36f,h*.19f),androidx.compose.ui.geometry.Size(w*.28f,h*.38f),style=Stroke(w*.055f))
            }
            "mobile" in kind || "traffic" in kind || "network" in kind -> (0..2).forEach { n -> drawRoundRect(Color.White,Offset(w*(.22f+n*.21f),h*(.58f-n*.16f)),androidx.compose.ui.geometry.Size(w*.12f,h*(.2f+n*.16f)),androidx.compose.ui.geometry.CornerRadius(w*.025f)) }
            "safe" in kind || "manager" in kind -> drawPath(Path().apply { moveTo(w*.5f,h*.17f); lineTo(w*.8f,h*.3f); lineTo(w*.75f,h*.61f); quadraticTo(w*.7f,h*.76f,w*.5f,h*.85f); quadraticTo(w*.3f,h*.76f,w*.25f,h*.61f); lineTo(w*.2f,h*.3f); close() },Color.White,style=Stroke(w*.055f))
            "camera" in kind || "permissions" in kind -> { drawRoundRect(Color.White,Offset(w*.18f,h*.3f),androidx.compose.ui.geometry.Size(w*.64f,h*.44f),androidx.compose.ui.geometry.CornerRadius(w*.06f),style=Stroke(w*.06f)); drawCircle(Color.White,w*.13f,style=Stroke(w*.05f)) }
            "notification" in kind -> { drawArc(Color.White,180f,180f,false,Offset(w*.28f,h*.2f),androidx.compose.ui.geometry.Size(w*.44f,h*.6f),style=Stroke(w*.06f)); drawLine(Color.White,Offset(w*.28f,h*.5f),Offset(w*.24f,h*.7f),w*.06f); drawLine(Color.White,Offset(w*.72f,h*.5f),Offset(w*.76f,h*.7f),w*.06f); drawLine(Color.White,Offset(w*.24f,h*.7f),Offset(w*.76f,h*.7f),w*.06f); drawCircle(Color.White,w*.055f,Offset(w*.5f,h*.82f)) }
            "sound" in kind -> { drawPath(Path().apply { moveTo(w*.2f,h*.38f); lineTo(w*.36f,h*.38f); lineTo(w*.54f,h*.22f); lineTo(w*.54f,h*.78f); lineTo(w*.36f,h*.62f); lineTo(w*.2f,h*.62f); close() },Color.White); drawArc(Color.White,-60f,120f,false,Offset(w*.42f,h*.25f),androidx.compose.ui.geometry.Size(w*.36f,h*.5f),style=Stroke(w*.05f)) }
            "display" in kind || "font" in kind -> { drawLine(Color.White,Offset(w*.25f,h*.78f),Offset(w*.5f,h*.22f),w*.06f); drawLine(Color.White,Offset(w*.5f,h*.22f),Offset(w*.75f,h*.78f),w*.06f); drawLine(Color.White,Offset(w*.35f,h*.6f),Offset(w*.65f,h*.6f),w*.05f) }
            "gallery" in kind || "albums" in kind -> { (0..5).forEach { n -> val a = n * Math.PI / 3; drawCircle(Color.White.copy(alpha = .9f), w * .13f, Offset(w * .5f + kotlin.math.cos(a).toFloat() * w * .2f, h * .5f + kotlin.math.sin(a).toFloat() * h * .2f)) }; drawCircle(color, w * .12f) }
            "notes" in kind || "note_" in kind -> (0..2).forEach { n -> drawLine(Color.White, Offset(w * .25f, h * (.3f + n * .18f)), Offset(w * .75f, h * (.3f + n * .18f)), w * .06f) }
            "community" in kind || "messages" in kind -> { drawRoundRect(Color.White, Offset(w*.18f,h*.22f), androidx.compose.ui.geometry.Size(w*.64f,h*.48f), androidx.compose.ui.geometry.CornerRadius(w*.12f)); drawPath(Path().apply { moveTo(w*.3f,h*.65f); lineTo(w*.3f,h*.82f); lineTo(w*.5f,h*.65f); close() },Color.White) }
            "weather" in kind -> drawCircle(Color(0xFFFFDD74), w * .24f)
            else -> { drawCircle(Color.White, w*.27f, style = Stroke(w*.07f)); drawCircle(Color.White,w*.07f) }
        }
    }
}

private fun routeFocus(desired: PhoneScreen, p: PhoneState): String {
    val path = PhoneCatalog.path(desired)
    if (p.screen == PhoneScreen.HOME) return path.firstOrNull()?.let(PhoneCatalog::target) ?: "nav_home"
    val index = path.indexOf(p.screen)
    return if (index >= 0 && index < path.lastIndex) PhoneCatalog.target(path[index + 1]) else "nav_back"
}

private fun phoneHint(scene: Scene, s: PracticeState, raw: String?): String? {
    if (raw == null) return null
    val p = s.phone ?: return raw
    if (p.overlay != null) return when(p.overlay) {
        PhoneOverlay.WIFI_PASSWORD -> if (s.text.isEmpty()) "text_input" else "connect"
        PhoneOverlay.QUOTA -> if (s.text != "2") "text_input" else "quota_confirm"
        PhoneOverlay.PERMISSION -> if (scene == Scene.PERMISSION) "deny" else "allow"
        PhoneOverlay.APP_MORE -> if (scene == Scene.SCAN) "scan" else "settings"
        PhoneOverlay.ATTACHMENTS -> "album"
        PhoneOverlay.WEATHER_MENU -> "uninstall"
        PhoneOverlay.UNINSTALL -> "confirm"
        else -> raw
    }
    if (raw.startsWith("app_") || raw.startsWith("route_") || raw in setOf("market_search", "font_entry", "settings")) {
        val desired = PhoneScreen.entries.firstOrNull { PhoneCatalog.target(it) == raw }
        if (desired != null) {
            if (p.screen == PhoneScreen.MARKET && desired == PhoneScreen.MARKET_RESULTS) return if (s.text.isBlank()) "text_input" else raw
            return routeFocus(desired, p)
        }
    }
    if (raw in setOf("nav_home", "nav_back", "try_network", "download", "test_mobile", "test_wifi", "notify", "scan_code")) return raw
    val desired = when {
        raw == "photo" -> PhoneScreen.PHOTO
        raw == "uninstall" -> PhoneScreen.HOME
        scene == Scene.PERMISSION && raw == "allow" -> PhoneScreen.CAMERA
        scene == Scene.PERMISSION -> PhoneScreen.SAFE
        scene == Scene.SOUND && raw == "brightness" -> PhoneScreen.DISPLAY
        scene == Scene.CARE && raw == "care_toggle" -> PhoneScreen.APP_CARE
        scene == Scene.TAP && raw == "app_gallery" -> PhoneScreen.HOME
        raw in setOf("photo_0", "send_photo") -> PhoneScreen.PICKER
        else -> PhoneLessons.entryScreen(scene)
    }
    if (raw == "uninstall") return if (p.screen == PhoneScreen.HOME) "app_weather" else "nav_home"
    return if (p.screen == desired || (raw == "send_photo" && p.screen == PhoneScreen.PICKER_PREVIEW)) raw else routeFocus(desired, p)
}

@Composable
fun SceneUi(scene: Scene, state: PracticeState, focus: String?, onEvent: (PracticeEvent) -> Unit, diagram: Boolean = false) {
    if (scene == Scene.WECHAT) { WechatSceneUi(state, focus, onEvent, diagram); return }
    PhoneSceneUi(scene, state, focus, onEvent, diagram)
}

@Composable
fun PhoneSceneUi(scene: Scene, s: PracticeState, focus: String?, send: (PracticeEvent) -> Unit, diagram: Boolean = false) {
    val p = s.phone ?: PhoneState()
    val actualFocus = phoneHint(scene, s, focus)
    CompositionLocalProvider(PhoneFocus provides actualFocus, PhoneDiagram provides diagram) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (diagram) "手机图解示意" else "练习页面 · 练习手机", style = MaterialTheme.typography.labelMedium, color = PhoneBlue)
            Surface(modifier = Modifier.fillMaxWidth().height(580.dp).testTag("phone_frame"), shape = RoundedCornerShape(26.dp), border = BorderStroke(2.dp, Color(0xFFCDD2DC)), color = PhoneGray) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))) {
                    Column(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 18.dp).semantics { contentDescription = "模拟状态栏，9点41分，电量百分之86" }) {
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(32,45,60); textSize = 12.dp.toPx() }
                            drawContext.canvas.nativeCanvas.drawText("9:41", 0f, 19.dp.toPx(), paint)
                            val status = if (s.connected) "WLAN   ▰  86%" else "▰  86%"
                            drawContext.canvas.nativeCanvas.drawText(status, size.width - paint.measureText(status), 19.dp.toPx(), paint)
                        }
                        if (p.screen != PhoneScreen.HOME) Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            PhoneNav("header_back", send, Modifier.size(56.dp))
                            Text(if (p.screen == PhoneScreen.INFO) p.infoTitle else PhoneCatalog.title(p.screen), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (p.screen == PhoneScreen.COMMUNITY) Box(Modifier.size(56.dp).focusTag("more", actualFocus).semantics { contentDescription = "更多菜单" }.clickable { send(PracticeEvent.Click("more")) }, contentAlignment = Alignment.Center) { Canvas(Modifier.size(24.dp)) { (0..2).forEach { i -> drawCircle(Ink, 2.dp.toPx(), Offset(size.width*.5f,size.height*(.2f+i*.3f))) } } }
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (p.screen == PhoneScreen.HOME) PhoneDesktop(scene, s, send)
                            else key(p.screen) {
                                val type = MaterialTheme.typography
                                MaterialTheme(typography = if (scene == Scene.FONT && p.screen == PhoneScreen.DISPLAY) type.copy(bodyLarge = type.bodyLarge.copy(fontSize = s.value.sp, lineHeight = (s.value * 1.4f).sp)) else type) { PhoneContent(scene, s, send) }
                            }
                            if (p.notificationBanner) Surface(Modifier.align(Alignment.TopCenter).padding(8.dp), shadowElevation = 8.dp, shape = RoundedCornerShape(16.dp), color = Color.White) { PhoneRow("社区消息", "dismiss_banner", send, "明天上午有活动。点此收起", icon = false) }
                        }
                        Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .8f)).height(56.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            listOf("nav_back", "nav_home", "nav_recents").forEach { PhoneNav(it, send, Modifier.weight(1f).fillMaxHeight()) }
                        }
                    }
                    if (p.overlay != null) PhoneOverlayUi(p.overlay, s, send)
                }
            }
            if (!diagram) PhoneTools(scene, s, send)
        }
    }
}

@Composable
private fun PhoneNav(tag: String, send: (PracticeEvent) -> Unit, modifier: Modifier) {
    val label = when(tag) { "nav_home" -> "回到模拟桌面"; "nav_recents" -> "模拟最近任务"; else -> "返回上一级" }
    Box(modifier.focusTag(tag, PhoneFocus.current).semantics { contentDescription = label }.clickable { send(PracticeEvent.Click(tag)) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) { val w = size.width; val h = size.height
            when(tag) {
                "nav_home" -> drawCircle(Ink, w*.36f, style = Stroke(2.dp.toPx()))
                "nav_recents" -> drawRoundRect(Ink, Offset(w*.15f,h*.15f), androidx.compose.ui.geometry.Size(w*.7f,h*.7f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(2.dp.toPx()))
                else -> drawPath(Path().apply { moveTo(w*.72f,h*.12f); lineTo(w*.22f,h*.5f); lineTo(w*.72f,h*.88f) },Ink, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun PhoneDesktop(scene: Scene, s: PracticeState, send: (PracticeEvent) -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFD5E7F5), Color(0xFFC9DDD5), Color(0xFFF4E8D5)))).focusTag("pager", PhoneFocus.current).pointerInput(scene) { detectHorizontalDragGestures(onDragStart = { drag = 0f }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount }, onDragEnd = { if (abs(drag) > 40) send(PracticeEvent.Gesture(if (drag < 0) "left" else "right")) }) }) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=14.dp,vertical=8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listOf("早上好", "出门走一走", "平安到家")[s.index.coerceIn(0,2)], style = MaterialTheme.typography.headlineMedium, color = Color(0xFF253D54))
            Text("10月28日  星期三", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3C566B))
            val columns = if (scene == Scene.CARE && s.enabled) 2 else 3
            val apps = listOf(PhoneApp.SETTINGS, PhoneApp.MANAGER, PhoneApp.GALLERY, PhoneApp.COMMUNITY, PhoneApp.SAFE, PhoneApp.NOTES, PhoneApp.MARKET) + if (s.installed) listOf(PhoneApp.WEATHER) else emptyList()
            apps.chunked(columns).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { app ->
                    val tag = PhoneCatalog.appTarget(app)
                    Column(Modifier.weight(1f).focusTag(tag, PhoneFocus.current).clip(RoundedCornerShape(14.dp)).combinedClickable(onClick = { send(PracticeEvent.Click(tag)) }, onLongClick = { send(PracticeEvent.Gesture(if (app == PhoneApp.WEATHER) "long_weather" else if (app == PhoneApp.GALLERY) "long" else "unrelated_long")) }).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PhoneGlyph(tag, Modifier.size(if (scene == Scene.CARE && s.enabled) 64.dp else 46.dp))
                        Text(PhoneCatalog.appNames.getValue(app), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
                repeat(columns-row.size) { Spacer(Modifier.weight(1f)) }
            } }
        }
        Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment=Alignment.CenterVertically) { (0..2).forEach { i -> Box(Modifier.padding(5.dp).size(if (i == s.index) 9.dp else 6.dp).background(if (i == s.index) Color(0xFF335475) else Color.White, RoundedCornerShape(50))) } }
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=6.dp).background(Color.White.copy(alpha = .45f), RoundedCornerShape(24.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(PhoneApp.COMMUNITY, PhoneApp.GALLERY).forEach { app -> Box(Modifier.size(56.dp).clickable { send(PracticeEvent.Click(PhoneCatalog.appTarget(app))) }.semantics { contentDescription = "常用入口：${PhoneCatalog.appNames[app]}" }.padding(5.dp)) { PhoneGlyph(PhoneCatalog.appTarget(app), Modifier.fillMaxSize()) } }
        }
    }
}
@Composable
private fun PhoneContent(scene: Scene, s: PracticeState, send: (PracticeEvent) -> Unit) {
    val p = s.phone ?: return
    val currentZoom by rememberUpdatedState(s.zoom)
    val scroll = rememberScrollState()
    if (p.screen == PhoneScreen.COMMUNITY_LIST) LaunchedEffect(scroll) { snapshotFlow { scroll.value }.distinctUntilChanged().collect { if (it > 20) send(PracticeEvent.Gesture("scroll")) } }
    val dark = p.screen == PhoneScreen.PHOTO
    Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF18202A) else PhoneGray).verticalScroll(scroll).then(if (p.screen == PhoneScreen.COMMUNITY_LIST) Modifier.focusTag("scroll_list", PhoneFocus.current) else Modifier).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (scene == Scene.FONT && p.screen == PhoneScreen.DISPLAY) Text("文字已按新字号显示", fontSize = s.value.sp, color = Ink)
        when(p.screen) {
            PhoneScreen.RECENTS -> {
                PhoneNote("点一张卡片，回到刚才打开的页面。")
                if (p.recentApps.isEmpty()) PhoneNote("还没有打开的模拟应用。")
                p.recentApps.forEach { app -> Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp) { PhoneRow(PhoneCatalog.appNames.getValue(app), "recent_${app.name}", send, PhoneCatalog.title(p.appStacks[app]?.lastOrNull() ?: PhoneCatalog.appRoots.getValue(app))) } }
            }
            PhoneScreen.SETTINGS -> { PhoneNote("练习手机 · HarmonyOS 风格示意"); PhoneNote("账号与设备信息") }
            PhoneScreen.WLAN -> {
                val on = if (scene == Scene.DATA) s.secondary else s.enabled
                PhoneSwitch("WLAN", on, send = send)
                PhoneNote(if (on) "可用网络" else "开启后查看附近的无线网络")
                if (on) {
                    PhoneRow("温暖的家", "network", send, if (s.connected) "已连接 · 已加密" else "需要密码 · 信号强")
                    PhoneRow("社区阅览室", "other_network", send, "已加密 · 示例网络")
                }
            }
            PhoneScreen.MOBILE_DATA -> { PhoneSwitch("移动数据", s.enabled, send = send); PhoneNote("使用手机卡套餐中的流量。没有 WLAN 时也能上网，可能产生套餐费用。") }
            PhoneScreen.MANAGER -> { PhoneGlyph("app_manager", Modifier.size(80.dp)); Text("手机管家", style = MaterialTheme.typography.headlineMedium); PhoneNote("管理手机的流量与应用联网") }
            PhoneScreen.TRAFFIC -> {
                Surface(color = Color(0xFFE6ECF8), shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text("本月已用", style = MaterialTheme.typography.bodySmall); Text("${"%.1f".format(s.used)} GB", style = MaterialTheme.typography.headlineMedium); Text("手机卡 1 · 示例数据", style = MaterialTheme.typography.bodySmall) } }
            }
            PhoneScreen.PLAN -> {
                PhoneRow("套餐限额", "quota_row", send, if (s.budget == 0) "尚未设置" else "每月 ${s.budget} GB", icon = false)
                PhoneSwitch("超额停止移动数据", s.enabled, send = send)
                PhoneNote("已用 ${"%.1f".format(s.used)} GB。限额只管理手机卡移动数据，不影响 WLAN。")
                if (s.enabled && s.budget > 0 && s.used > s.budget) PhoneNote("已超过限额，模拟移动数据已停止。")
            }
            PhoneScreen.APP_NETWORK -> {
                PhoneNote("应用可以分别使用移动数据和 WLAN。勾选表示允许。")
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) {
                    Text("乐看视频", style = MaterialTheme.typography.titleMedium)
                    PhoneCheck("移动数据", s.enabled, "toggle", send)
                    PhoneCheck("WLAN", s.secondary, "wifi_toggle", send)
                } }
                PhoneNote("社区消息：移动数据与 WLAN 均允许")
            }
            PhoneScreen.APP_DETAILS -> { PhoneGlyph("app_safe", Modifier.size(64.dp)); PhoneNote("安心生活 · 虚构示例软件") }
            PhoneScreen.CAMERA -> {
                PhoneNote("安心生活需要使用相机来扫码。这里的权限只影响模拟软件。")
                PhoneRow("使用时允许", "allow", send, if (s.permission == true) "已选择" else "仅在使用软件时允许", icon = false, trailing = { RadioButton(s.permission == true, null) })
                PhoneRow("不允许", "deny", send, if (s.permission == false) "已选择" else "扫码将暂不可用", icon = false, trailing = { RadioButton(s.permission == false, null) })
            }
            PhoneScreen.APP_NOTIFICATION -> { PhoneSwitch("允许通知", s.enabled, send = send); PhoneNote("关闭后不再显示通知横幅。消息仍会收到，可以打开软件查看。") }
            PhoneScreen.SOUND -> { PhoneSlider("媒体音量", s.value, "volume", send); PhoneNote("影响音乐、视频等媒体播放的声音大小。本练习用文字表示，不播放声音。") }
            PhoneScreen.DISPLAY -> if (scene == Scene.SOUND) PhoneSlider("屏幕亮度", s.brightness, "brightness", send)
            PhoneScreen.FONT -> {
                Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("字体预览", fontSize = s.value.sp); Text("今天的天气很好，适合出去走走。", fontSize = s.value.sp, modifier = Modifier.testTag("font_preview")) } }
                PhoneSlider("字号", s.value, "font_slider", send, 20f..36f)
                PhoneNote("拖动立即预览。返回后，上一级列表也会显示调整后的文字。")
            }
            PhoneScreen.CARE -> { PhoneSwitch("长辈关怀", s.enabled, send = send); PhoneNote("开启后，练习桌面的图标和文字更大、排列更疏。软件内部的关怀模式需单独设置。") }
            PhoneScreen.GALLERY -> PhoneNote("相册")
            PhoneScreen.ALBUMS, PhoneScreen.COMMUNITY_ALBUM -> { PhoneNote("3 张照片 · 示例图片"); PhonePhotoGrid(s, false, send) }
            PhoneScreen.PHOTO -> {
                Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(10.dp)).focusTag("photo", PhoneFocus.current).pointerInput(Unit) { detectTapGestures(onDoubleTap = { send(PracticeEvent.Gesture("zoom", if (currentZoom > 1.2f) .5f else 1.7f)) }) }.pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> send(PracticeEvent.Gesture("zoom", zoom)) } }, contentAlignment = Alignment.Center) { PhotoArt(Modifier.fillMaxSize().graphicsLayer(scaleX = s.zoom, scaleY = s.zoom)) }
                Text("山间小屋 · 放大 ${"%.1f".format(s.zoom)} 倍", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("双击或双指张开可以放大", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            PhoneScreen.COMMUNITY -> {
                Surface(color = Color(0xFFE0EEDC), shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text("邻里相伴", style = MaterialTheme.typography.headlineMedium); Text("把日子过得有声有色", style = MaterialTheme.typography.bodySmall) } }
                PhoneRow("搜索功能", "route_search", send, "输入关键词查找", icon = false)
            }
            PhoneScreen.COMMUNITY_LIST -> {
                PhoneNote("向上滑动，看看下面的功能")
                listOf("社区活动", "健康小知识", "便民服务", "每日读报", "邻里分享", "兴趣小组").forEachIndexed { i, label -> PhoneRow(label, "static_$i", send, "认识社区生活", icon = false) }
                PhoneRow("家庭相册", "family_album", send, "家人分享的照片")
            }
            PhoneScreen.COMMUNITY_MY -> { PhoneGlyph("app_community", Modifier.size(64.dp)); Text("快乐的我", style = MaterialTheme.typography.titleMedium) }
            PhoneScreen.APP_FONT -> { PhoneSlider("软件字号", s.value, "app_font", send, 20f..36f); Text("邻里生活字体预览", fontSize = s.value.sp) }
            PhoneScreen.READING -> { PhoneSwitch("阅读提醒", s.enabled, send = send); PhoneNote("开启后，软件会提醒阅读社区消息。") ; PhoneRow("恢复默认", "reset_dialog", send, icon = false) }
            PhoneScreen.APP_CARE -> { PhoneSwitch("软件关怀模式", s.secondary, "care_toggle", send); Text("常用功能", fontSize = if (s.secondary) 28.sp else 22.sp); PhoneNote("这里只改变邻里生活自己的文字和布局。手机的长辈关怀开关独立管理。") }
            PhoneScreen.MESSAGES -> if (scene == Scene.NOTIFICATION) s.messages.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            PhoneScreen.CHAT -> {
                PhoneNote("聊天对象：家人 · 虚构人物")
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp)) { Text("今天过得好吗？", Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge) }
                s.messages.forEach { message -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Surface(color = Color(0xFFD9EDC8), shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(max = 240.dp)) { Text(message, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge) } } }
                if (s.secondary && scene == Scene.SEND_PHOTO) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { PhotoArt(Modifier.size(140.dp), s.selected ?: 0) }
                HorizontalDivider()
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PhoneInput("消息", s, send, modifier = Modifier.weight(1f))
                    Box(Modifier.size(56.dp).focusTag("add_photo", PhoneFocus.current).semantics { contentDescription = "添加照片等内容" }.clickable { send(PracticeEvent.Click("add_photo")) }, contentAlignment = Alignment.Center) { Canvas(Modifier.size(30.dp)) { drawCircle(Muted,size.width*.43f,style=Stroke(1.5.dp.toPx())); drawLine(Muted,Offset(size.width*.25f,size.height*.5f),Offset(size.width*.75f,size.height*.5f),2.dp.toPx()); drawLine(Muted,Offset(size.width*.5f,size.height*.25f),Offset(size.width*.5f,size.height*.75f),2.dp.toPx()) } }
                } }
                if (s.text.isNotBlank()) PhoneAction("发送", "send", send)
            }
            PhoneScreen.PICKER -> { PhoneNote("发送给：家人。勾选照片后，还需要点发送。") ; PhonePhotoGrid(s, true, send); PhoneAction("预览", "preview_photo", send); PhoneAction("发送图片${if (s.selected != null) "（1张）" else ""}", "send_photo", send) }
            PhoneScreen.PICKER_PREVIEW -> { PhoneNote("发送给：家人"); PhotoArt(Modifier.fillMaxWidth().height(230.dp), s.selected ?: 0); PhoneAction("发送图片", "send_photo", send) }
            PhoneScreen.NOTE_LIST -> if (p.noteSaved) PhoneNote("已保存笔记：${s.text}")
            PhoneScreen.NOTE_EDIT -> { PhoneNote("正文"); PhoneInput("写一句话", s, send, multiline = true); PhoneAction("保存", "finish_input", send) }
            PhoneScreen.SEARCH, PhoneScreen.MARKET -> { PhoneInput(if (p.screen == PhoneScreen.MARKET) "搜索应用" else "搜索功能", s, send); PhoneAction("搜索", if (p.screen == PhoneScreen.MARKET) "market_search" else "search", send); if (p.screen == PhoneScreen.MARKET) PhoneNote("应用市场 · 示例软件均在本地模拟") }
            PhoneScreen.SEARCH_RESULTS -> if (s.secondary) PhoneRow("家庭相册", "search_result", send, "找到 1 个匹配功能") else PhoneNote("没有找到，点返回修改关键词。")
            PhoneScreen.SAFE -> { PhoneGlyph("app_safe", Modifier.size(64.dp)); Text("安心生活", style = MaterialTheme.typography.headlineMedium); PhoneRow("扫一扫", "open_scan", send, if (s.permission == false) "相机未允许，扫码不可用" else "扫描生活中的二维码") }
            PhoneScreen.SCANNER -> { PhoneNote("相机已允许 · 模拟取景框"); Box(Modifier.fillMaxWidth().height(240.dp).background(Color(0xFF293441), RoundedCornerShape(18.dp)).padding(28.dp)) { QrArt(Modifier.fillMaxSize()) }; PhoneNote("将二维码放入框内。下方教学工具可以模拟识别。") }
            PhoneScreen.SCAN_RESULT -> { Text("识别成功", style = MaterialTheme.typography.headlineMedium); PhoneNote("欢迎来到社区图书角。这是一段示例文字。") }
            PhoneScreen.MARKET_RESULTS -> PhoneRow("每日天气", "route_weather_detail", send, "天气工具 · 虚构软件 · 12MB")
            PhoneScreen.WEATHER_DETAIL -> { PhoneGlyph("app_weather", Modifier.size(72.dp)); Text("每日天气", style = MaterialTheme.typography.headlineMedium); PhoneNote("查看天气和生活提示。示例软件仅在练习手机中安装。") ; PhoneAction(if (s.installed) "打开" else "安装", if (s.installed) "open_weather" else "install", send); if (s.installed) PhoneNote("已安装 · 桌面已添加图标") }
            PhoneScreen.WEATHER -> { Text("晴  23℃", style = MaterialTheme.typography.headlineMedium); PhoneNote("每日天气 · 示例应用。回桌面长按图标可以卸载。") }
            PhoneScreen.INFO -> PhoneNote("这是${p.infoTitle}的入口。当前课程只需认识它，点返回继续练习。")
            else -> Unit
        }
        PhoneCatalog.links(p.screen).forEach { link -> PhoneRow(link.label, link.target, send, link.subtitle) }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun PhoneCheck(label: String, checked: Boolean, tag: String, send: (PracticeEvent) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).focusTag(tag, PhoneFocus.current).toggleable(checked, role = Role.Checkbox, onValueChange = { send(PracticeEvent.Click(tag)) }), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, null); Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyLarge); Text(if (checked) "允许" else "不允许", style = MaterialTheme.typography.bodySmall, color = Muted) } }
}

@Composable
private fun PhonePhotoGrid(s: PracticeState, select: Boolean, send: (PracticeEvent) -> Unit) {
    listOf(0,1,2).chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { i -> val tag = if (select) "photo_$i" else if (i == 0) "open_photo" else "other_photo_$i"
            Column(Modifier.weight(1f).focusTag(tag, PhoneFocus.current).clip(RoundedCornerShape(12.dp)).clickable { send(PracticeEvent.Click(tag)) }) { Box { PhotoArt(Modifier.fillMaxWidth().height(112.dp), i); if (select) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(30.dp).background(if (s.selected == i) PhoneBlue else Color.White, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Canvas(Modifier.size(20.dp)) { if (s.selected == i) drawPath(Path().apply { moveTo(size.width*.15f,size.height*.5f); lineTo(size.width*.4f,size.height*.75f); lineTo(size.width*.85f,size.height*.25f) },Color.White,style=Stroke(2.dp.toPx())) else drawCircle(Ink,size.width*.35f,style=Stroke(1.dp.toPx())) } } }; Text(listOf("山间小屋", "花园", "海边")[i], style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(6.dp)); if (select) Text(if (s.selected == i) "已选中" else "未选中", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(6.dp)) }
        }; if (row.size == 1) Spacer(Modifier.weight(1f))
    } }
}

@Composable
private fun BoxScope.PhoneOverlayUi(overlay: PhoneOverlay, s: PracticeState, send: (PracticeEvent) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)).clickable(onClick = {}))
    val menu = overlay in setOf(PhoneOverlay.APP_MORE, PhoneOverlay.ALBUM_MENU, PhoneOverlay.WEATHER_MENU)
    Surface(modifier = Modifier.align(if (menu) Alignment.TopEnd else Alignment.Center).padding(top = if (menu) 80.dp else 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth(if (menu) .88f else 1f).heightIn(max = 440.dp).testTag("phone_overlay"), color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 12.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when(overlay) {
                PhoneOverlay.WIFI_PASSWORD -> { Text("温暖的家", style = MaterialTheme.typography.titleMedium); PhoneInput("密码", s, send, password = true); PhoneNote("练习密码：12345678"); if (s.feedback.isNotBlank()) PhoneNote(s.feedback); PhoneAction("连接", "connect", send) }
                PhoneOverlay.QUOTA -> { Text("设置套餐限额", style = MaterialTheme.typography.titleMedium); PhoneInput("每月流量（GB）", s, send); PhoneNote("本课请输入 2，单位为 GB。"); PhoneAction("确定", "quota_confirm", send) }
                PhoneOverlay.PERMISSION -> { Text("允许使用相机？", style = MaterialTheme.typography.titleMedium); PhoneNote("允许后可以扫码；拒绝后扫码暂不可用。这只是练习，不会使用真实相机。"); PhoneAction("使用时允许", "allow", send); PhoneAction("拒绝", "deny", send) }
                PhoneOverlay.RESET -> { Text("恢复默认设置？", style = MaterialTheme.typography.titleMedium); PhoneNote("确认会关闭阅读提醒。取消会保留原来的设置。"); PhoneAction("恢复默认", "confirm", send) }
                PhoneOverlay.APP_MORE -> { PhoneRow("扫一扫", "scan", send, icon = false); PhoneRow("设置", "settings", send, icon = false) }
                PhoneOverlay.ATTACHMENTS -> { Text("更多内容", style = MaterialTheme.typography.titleMedium); PhoneRow("相册", "album", send, "选择一张照片") }
                PhoneOverlay.ALBUM_MENU -> { Text("图库", style = MaterialTheme.typography.titleMedium); PhoneNote("长按可以看到图标附近的操作菜单。点一下打开软件，长按会打开更多操作。") }
                PhoneOverlay.WEATHER_MENU -> { Text("每日天气", style = MaterialTheme.typography.titleMedium); PhoneRow("卸载", "uninstall", send, icon = false) }
                PhoneOverlay.UNINSTALL -> { Text("卸载每日天气？", style = MaterialTheme.typography.titleMedium); PhoneNote("确认后，示例软件和桌面图标会移除。"); PhoneAction("确认卸载", "confirm", send) }
            }
            PhoneAction(if (overlay == PhoneOverlay.ALBUM_MENU) "关闭菜单" else "取消", "cancel", send)
        }
    }
}

@Composable
private fun PhoneTools(scene: Scene, s: PracticeState, send: (PracticeEvent) -> Unit) {
    val tools = when(scene) {
        Scene.DATA -> if (PhoneCatalog.visit(PhoneScreen.MOBILE_DATA) in s.achieved) listOf("试着上网" to "try_network") else emptyList()
        Scene.BUDGET -> if (PhoneCatalog.visit(PhoneScreen.PLAN) in s.achieved) listOf("模拟下载 0.3GB" to "download") else emptyList()
        Scene.APP_NETWORK -> if (PhoneCatalog.visit(PhoneScreen.APP_NETWORK) in s.achieved) listOf("用移动数据试用" to "test_mobile", "用 WLAN 试用" to "test_wifi") else emptyList()
        Scene.NOTIFICATION -> if (PhoneCatalog.visit(PhoneScreen.APP_NOTIFICATION) in s.achieved) listOf("模拟来消息" to "notify") else emptyList()
        Scene.SCAN -> if (s.phone?.screen == PhoneScreen.SCANNER) listOf("模拟对准二维码" to "scan_code") else emptyList()
        else -> emptyList()
    }
    if (tools.isNotEmpty()) Surface(color = Color(0xFFEEF2F8), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("教学演示工具", style = MaterialTheme.typography.titleMedium, color = PhoneBlue); Text("以下按钮用于比较效果，属于课堂演示。", style = MaterialTheme.typography.bodySmall); tools.forEach { PhoneAction(it.first, it.second, send) } } }
}



