package com.example.phoneclass.learning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassroomState(
    val course: String? = null, val stage: Stage = Stage.INTRO, val panel: Int = 0, val catalog: Int? = null,
    val practice: PracticeState = PracticeState(), val hint: Boolean = false,
    val progress: Progress = Progress(), val loaded: Boolean = false, val storageMessage: String? = null
)
class ClassroomModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProgressRepository(application)
    private val mutable = MutableStateFlow(ClassroomState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch { repository.progress.collect { progress -> mutable.update { it.copy(progress = progress, loaded = true) } } } }
    private fun persist(action: suspend () -> Unit) { viewModelScope.launch {
        runCatching { action() }.onFailure { mutable.update { it.copy(storageMessage = "这次进度暂未保存，仍可继续练习。") } }
    } }
    private fun remember() { val s = mutable.value; s.course?.let { id -> persist { repository.remember(id, s.stage, s.panel) } } }
    fun open(id: String, resume: Boolean = false) {
        val lesson = Lessons.get(id)
        val p = mutable.value.progress
        mutable.update { it.copy(course = id, panel = if (resume) p.panel else 0,
            stage = if (resume && p.stage != Stage.DONE) p.stage else Stage.INTRO,
            practice = PracticeEngine.initial(lesson), hint = false) }
        remember()
    }
    fun home() { mutable.update { it.copy(course = null, catalog = null, practice = PracticeState(), hint = false) } }
    fun wechatCatalog() { mutable.update { it.copy(course = null, catalog = 3, practice = PracticeState(), hint = false) } }
    fun courses() { if (mutable.value.course?.let(Lessons::get)?.category == 3) wechatCatalog() else home() }
    fun panel(delta: Int) { mutable.update { it.copy(panel = (it.panel + delta).coerceIn(0, 2)) }; remember() }
    fun start(stage: Stage) {
        val lesson = Lessons.get(mutable.value.course ?: return)
        mutable.update { it.copy(stage = stage, practice = PracticeEngine.initial(lesson), hint = false) }
        remember()
    }
    fun showHint() { mutable.update { it.copy(hint = !it.hint) } }
    fun reset() {
        val lesson = Lessons.get(mutable.value.course ?: return)
        mutable.update { it.copy(practice = PracticeEngine.initial(lesson), hint = false) }
    }
    fun font(level: Int) { mutable.update { it.copy(progress = it.progress.copy(fontLevel = level.coerceIn(0, 2))) }; persist { repository.setFont(level) } }
    fun event(event: PracticeEvent) {
        val before = mutable.value
        val lesson = before.course?.let(Lessons::get) ?: return
        if (before.stage !in setOf(Stage.GUIDED, Stage.SOLO)) return
        val next = PracticeEngine.reduce(lesson, before.practice, event)
        val done = before.stage == Stage.SOLO && PracticeEngine.complete(lesson, next)
        mutable.update { it.copy(practice = next, stage = if (done) Stage.DONE else it.stage) }
        if (done) {
            mutable.update { it.copy(progress = it.progress.copy(completed = it.progress.completed + lesson.id)) }
            persist { repository.complete(lesson.id) }
            remember()
        }
    }
    fun back() {
        val s = mutable.value
        if (s.stage != Stage.INTRO) { mutable.update { it.copy(stage = Stage.INTRO, practice = PracticeState()) }; remember() }
        else if (s.panel > 0) panel(-1) else courses()
    }
}
