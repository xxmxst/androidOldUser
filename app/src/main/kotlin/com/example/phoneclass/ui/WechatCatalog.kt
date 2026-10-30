package com.example.phoneclass.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.theme.*

@Composable
fun WechatCatalog(state: ClassroomState, model: ClassroomModel) {
    val lessons = Lessons.wechat
    val count = lessons.count { it.id in state.progress.completed }
    LazyColumn(Modifier.fillMaxSize().testTag("wechat_catalog"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = model::home, modifier = Modifier.heightIn(min = 56.dp)) { Text("‹ 全部课程") } }
        item { Text("微信常见操作", style = MaterialTheme.typography.headlineMedium) }
        item {
            Surface(color = Color(0xFFE8F5EB), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("把日常小事，学得更明白", style = MaterialTheme.typography.titleLarge, color = Green)
                    Text("先看路径，再动手练，还要知道操作以后有什么变化。", style = MaterialTheme.typography.bodyLarge)
                    Text("微信专题已学 $count / ${lessons.size} 课", style = MaterialTheme.typography.bodyMedium)
                    val recent = state.progress.recent
                    val next = lessons.firstOrNull { it.id == recent && it.id !in state.progress.completed } ?: lessons.firstOrNull { it.id !in state.progress.completed } ?: lessons.first()
                    PrimaryButton(if (count == lessons.size) "再练一课" else "继续学习微信", "wechat_continue") { model.open(next.id, resume = next.id == recent && next.id !in state.progress.completed) }
                }
            }
        }
        itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            Card(onClick = { model.open(lesson.id) }, modifier = Modifier.fillMaxWidth().testTag("lesson_${lesson.id}"), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("微信 ${index + 1}  ${if (lesson.id in state.progress.completed) "· 已学会 ✓" else ""}", style = MaterialTheme.typography.labelMedium, color = Green)
                    Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                    Text(lesson.summary, style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
        }
        item { Text("参考安卓微信常见布局。联系人、群聊、消息和通话均为课堂示例。", style = MaterialTheme.typography.bodySmall, color = Muted) }
    }
}
