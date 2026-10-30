package com.example.phoneclass.learning

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.learningStore by preferencesDataStore("learning_progress")
enum class Stage { INTRO, GUIDED, SOLO, DONE }
data class Progress(val completed: Set<String> = emptySet(), val recent: String? = null, val stage: Stage = Stage.INTRO, val panel: Int = 0, val fontLevel: Int = 0)
class ProgressRepository(context: Context) {
    private val store = context.applicationContext.learningStore
    private val completed = stringSetPreferencesKey("completed")
    private val recent = stringPreferencesKey("recent")
    private val stage = stringPreferencesKey("stage")
    private val panel = intPreferencesKey("panel")
    private val font = intPreferencesKey("font")
    val progress: Flow<Progress> = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { p -> Progress(
        completed = (p[completed] ?: emptySet()).filter { id -> Lessons.all.any { it.id == id } }.toSet(),
        recent = p[recent]?.takeIf { id -> Lessons.all.any { it.id == id } },
        stage = runCatching { Stage.valueOf(p[stage] ?: "INTRO") }.getOrDefault(Stage.INTRO),
        panel = (p[panel] ?: 0).coerceIn(0, 2), fontLevel = (p[font] ?: 0).coerceIn(0, 2)
    ) }
    suspend fun remember(id: String, currentStage: Stage, currentPanel: Int) {
        store.edit { it[recent] = id; it[stage] = currentStage.name; it[panel] = currentPanel }
    }
    suspend fun complete(id: String) { store.edit { it[completed] = (it[completed] ?: emptySet()) + id } }
    suspend fun setFont(level: Int) { store.edit { it[font] = level.coerceIn(0, 2) } }
}
