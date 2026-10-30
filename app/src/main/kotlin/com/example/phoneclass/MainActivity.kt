package com.example.phoneclass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.phoneclass.ui.theme.PhoneClassTheme
import com.example.phoneclass.learning.ClassroomModel
import com.example.phoneclass.ui.ClassroomApp

class MainActivity : ComponentActivity() {
  val model: ClassroomModel by viewModels()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val state by model.state.collectAsStateWithLifecycle()
      PhoneClassTheme(state.progress.fontLevel) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { ClassroomApp(state, model) }
      }
    }
  }
}
