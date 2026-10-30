package com.example.phoneclass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.SceneUi
import com.example.phoneclass.ui.theme.PhoneClassTheme

/** QA only. Uses the production phone components without touching learning data. */
class PhonePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = runCatching { Scene.valueOf(intent.getStringExtra("scene") ?: "WIFI") }.getOrDefault(Scene.WIFI).takeUnless { it == Scene.WECHAT } ?: Scene.WIFI
        val panel = intent.getIntExtra("panel", 0).coerceIn(0,2)
        val lesson = Lessons.get(scene.name)
        setContent {
            var state by remember { mutableStateOf(PhonePreviews.state(scene, panel)) }
            PhoneClassTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("手机小课堂 1.2", style = MaterialTheme.typography.headlineMedium)
                        Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
                        SceneUi(scene, state, null, { state = PracticeEngine.reduce(lesson,state,it) })
                        if (state.feedback.isNotBlank()) Text(state.feedback, style = MaterialTheme.typography.bodyLarge)
                        Text("QA 页面核对 · 离线模拟", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
