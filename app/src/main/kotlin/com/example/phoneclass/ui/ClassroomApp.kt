package com.example.phoneclass.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.theme.*

@Composable
fun ClassroomApp(state: ClassroomState, model: ClassroomModel) {
    BackHandler(state.course != null || state.catalog != null) { if (state.course == null) model.home() else model.back() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        if (state.course == null) {
            if (state.loaded) { if (state.catalog == 3) WechatCatalog(state, model) else Home(state, model) } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val lesson = Lessons.get(state.course)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = model::courses, modifier = Modifier.heightIn(min = 56.dp).testTag("home")) { Text("‹ 课程") }
                Text(if (lesson.category == 3) "微信 ${Lessons.wechat.indexOf(lesson) + 1} / ${Lessons.wechat.size}" else "第 ${lesson.number} 课", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            when (state.stage) {
                Stage.INTRO -> Introduction(lesson, state.panel, model)
                Stage.GUIDED, Stage.SOLO -> Practice(lesson, state, model)
                Stage.DONE -> Finished(lesson, model)
            }
        }
    }
}

@Composable
private fun Home(state: ClassroomState, model: ClassroomModel) {
    var category by rememberSaveable { mutableIntStateOf(-1) }
    var fontDialog by remember { mutableStateOf(false) }
    val completed = state.progress.completed.size
    LazyColumn(Modifier.fillMaxSize().testTag("course_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("一起学，慢慢来", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("手机小课堂", style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedButton(onClick = { fontDialog = true }, modifier = Modifier.heightIn(min = 56.dp).testTag("font_settings"), contentPadding = PaddingValues(12.dp)) { Text("字号", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Blue), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("微信常见操作", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("发图片、改备注、找联系人……\n${Lessons.wechat.size} 个生活场景，一起慢慢学。", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE4EDFF))
                    Surface(color = Color(0xFF1C4795), shape = RoundedCornerShape(12.dp)) {
                        Text("离线学习 · 安心模拟", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    OutlinedButton(onClick = model::wechatCatalog, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("wechat_featured"), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Blue)) { Text("进入微信专题  →") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("已经学会 $completed / ${Lessons.all.size} 课", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { completed.toFloat() / Lessons.all.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), trackColor = Color(0xFFEDF1F6), drawStopIndicator = {})
                    val recent = state.progress.recent
                    val suggested = if (recent != null && recent !in state.progress.completed) Lessons.get(recent) else Lessons.all.firstOrNull { it.id !in state.progress.completed } ?: Lessons.all.first()
                    Text(suggested.title, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    PrimaryButton(if (completed == Lessons.all.size) "再练一课" else if (recent != null) "继续学习" else "从第一课开始", "continue") { model.open(suggested.id, resume = suggested.id == recent && recent !in state.progress.completed) }
                }
            }
        }
        item { Text("选一课，慢慢学", style = MaterialTheme.typography.headlineSmall) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(-1) + Lessons.categories.map { it.id }) { index ->
                    FilterChip(selected = category == index, onClick = { category = index }, label = { Text(if (index == -1) "全部" else Lessons.category(index).shortTitle, style = MaterialTheme.typography.bodySmall) }, modifier = Modifier.heightIn(min = 56.dp))
                }
            }
        }
        Lessons.categories.filter { category == -1 || it.id == category }.forEach { group ->
            val lessons = Lessons.all.filter { it.category == group.id }
            item(key = "group_${group.id}") { Text(group.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(lessons, key = { it.id }) { lesson ->
                Card(onClick = { model.open(lesson.id) }, modifier = Modifier.fillMaxWidth().testTag("lesson_${lesson.id}"), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).background(if (lesson.id in state.progress.completed) Color(0xFFE5F3EB) else Color(0xFFEAF0FF), RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(if (lesson.id in state.progress.completed) "✓" else "%02d".format(lesson.number), color = if (lesson.id in state.progress.completed) Green else Blue, style = MaterialTheme.typography.titleSmall)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            Text(if (lesson.id in state.progress.completed) "已学会 · 可以再练" else lesson.summary, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        Text("›", color = Muted, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item {
            Text("界面参考华为 4.x，名称和位置可能因型号不同。练习使用示例数据，可以放心重来。", style = MaterialTheme.typography.bodySmall, color = Muted)
            state.storageMessage?.let { Text(it, color = Blue) }
        }
    }
    if (fontDialog) AlertDialog(onDismissRequest = { fontDialog = false }, title = { Text("选择舒服的字号") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("大字", "更大", "特大").forEachIndexed { index, title ->
                OutlinedButton(onClick = { model.font(index) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (state.progress.fontLevel == index) "✓ $title" else title) }
            }
            Text("系统字号放大也会生效。", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { fontDialog = false }) { Text("选好了") } })
}

@Composable
private fun ColumnScope.Introduction(lesson: Lesson, index: Int, model: ClassroomModel) {
    val scroll = rememberScrollState()
    LaunchedEffect(lesson.id, index) { scroll.scrollTo(0) }
    val panel = lesson.panels[index]
    Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StageLabel("看图学一学", "${index + 1} / ${lesson.panels.size} 张图")
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Text(panel.title, style = MaterialTheme.typography.titleLarge)
        Text(panel.body, style = MaterialTheme.typography.bodyLarge)
        LessonDiagram(lesson, index)
        Surface(color = Warm, shape = RoundedCornerShape(18.dp)) {
            Text("记一记\n${panel.caption}", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Text("这是图解示意。进入跟练页后，就能自己操作。", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(4.dp))
    }
    Surface(shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) OutlinedButton(onClick = { model.panel(-1) }, contentPadding = PaddingValues(14.dp), modifier = Modifier.heightIn(min = 56.dp).semantics { contentDescription = "上一张图" }) { Text("‹") }
                PrimaryButton(if (index < 2) "下一张图  →" else "开始跟练  →", if (index < 2) "next_panel" else "start_guided", Modifier.weight(1f)) { if (index < 2) model.panel(1) else model.start(Stage.GUIDED) }
            }
            TextButton(onClick = { model.start(Stage.SOLO) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("start_solo")) { Text("已经看懂，直接自己试试", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ColumnScope.Practice(lesson: Lesson, state: ClassroomState, model: ClassroomModel) {
    val done = PracticeEngine.complete(lesson, state.practice)
    val goal = PracticeEngine.nextGoal(lesson, state.practice)
    val guided = state.stage == Stage.GUIDED
    val scroll = rememberScrollState()
    LaunchedEffect(lesson.id, state.stage) { scroll.scrollTo(0) }
    Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StageLabel(if (guided) "跟着做一遍" else "自己试一试", "${state.practice.achieved.count { key -> lesson.goals.any { it.key == key } }} / ${lesson.goals.size} 步")
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Surface(color = if (done) Color(0xFFE6F3EB) else Color(0xFFE8F0FF), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (done) "✓ 跟练完成" else "这次的目标", style = MaterialTheme.typography.titleMedium, color = if (done) Green else Blue)
                Text(if (done) "现在可以从头自己做一遍。" else goal?.instruction ?: "看看结果，再试一次。", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("goal"))
                if (!guided && !state.hint) Text("先自己找找。需要时可点下方“给我提示”。", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        SceneUi(lesson.scene, state.practice, if (guided || state.hint) goal?.target else null, model::event)
        if (state.practice.feedback.isNotBlank()) Surface(color = Warm, shape = RoundedCornerShape(18.dp)) {
            Text(state.practice.feedback, Modifier.padding(18.dp).testTag("feedback").semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
        }
        if (guided && done) PrimaryButton("自己试试  →", "guided_done") { model.start(Stage.SOLO) }
        Text("模拟练习只改变这里的示例，不改变手机的真实设置。", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(8.dp))
    }
    Surface(shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = model::showHint, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("hint")) { Text(if (state.hint) "收起提示" else "给我提示", style = MaterialTheme.typography.bodyMedium) }
            OutlinedButton(onClick = model::reset, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("restart")) { Text("重新开始", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ColumnScope.Finished(lesson: Lesson, model: ClassroomModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 24.dp).size(100.dp).background(Color(0xFFE6F3EB), CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = Green, style = MaterialTheme.typography.headlineLarge) }
        Text("这一课，你学会了", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("lesson_complete"))
        Text(lesson.title, style = MaterialTheme.typography.titleLarge, color = Blue)
        Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) { Text(lesson.takeaway, Modifier.padding(22.dp), style = MaterialTheme.typography.bodyLarge) }
        Text("想不起来也没关系，随时可以回来再练。", style = MaterialTheme.typography.bodyMedium, color = Muted)
        val next = Lessons.all.getOrNull(lesson.number)
        if (next != null) PrimaryButton("学下一课", "next_lesson") { model.open(next.id) }
        OutlinedButton(onClick = { model.start(Stage.SOLO) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("再练一遍") }
        TextButton(onClick = model::courses, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("回到课程列表") }
    }
}

@Composable
fun PrimaryButton(label: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) { Text(label) }
}

@Composable
private fun StageLabel(label: String, right: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Blue)
        Text(right, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
