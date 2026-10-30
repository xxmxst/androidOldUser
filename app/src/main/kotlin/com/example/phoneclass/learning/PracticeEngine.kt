package com.example.phoneclass.learning

data class PracticeState(
    val page: String = "main", val enabled: Boolean = false, val secondary: Boolean = false,
    val permission: Boolean? = null, val text: String = "", val value: Float = 30f,
    val brightness: Float = 30f, val budget: Int = 0, val used: Float = 1.8f,
    val index: Int = 0, val selected: Int? = null, val zoom: Float = 1f,
    val connected: Boolean = false, val installed: Boolean = false,
    val messages: List<String> = emptyList(), val achieved: Set<String> = emptySet(),
    val feedback: String = "", val dialog: String? = null, val wechat: WechatState? = null, val phone: PhoneState? = null
)
sealed interface PracticeEvent {
    data class Click(val target: String) : PracticeEvent
    data class Input(val text: String) : PracticeEvent
    data class Adjust(val target: String, val value: Float) : PracticeEvent
    data class Gesture(val kind: String, val amount: Float = 1f) : PracticeEvent
}

/** Pure local reducers; neither simulator reads or changes real phone settings. */
object PracticeEngine {
    fun initial(lesson: Lesson): PracticeState = if (lesson.scene == Scene.WECHAT) WechatEngine.initial(lesson.id) else PhoneEngine.initial(lesson.scene)
    fun initial(scene: Scene): PracticeState = if (scene == Scene.WECHAT) WechatEngine.initial("WX_NAV") else PhoneEngine.initial(scene)
    fun reduce(lesson: Lesson, state: PracticeState, event: PracticeEvent): PracticeState = if (lesson.scene == Scene.WECHAT) WechatEngine.reduce(lesson, state, event) else PhoneEngine.reduce(lesson, state, event)
    fun reduce(scene: Scene, state: PracticeState, event: PracticeEvent): PracticeState = reduce(Lessons.all.first { it.scene == scene }, state, event)
    fun complete(lesson: Lesson, state: PracticeState): Boolean = lesson.goals.all { it.key in state.achieved } && if (lesson.scene == Scene.WECHAT) WechatEngine.complete(lesson, state) else PhoneEngine.complete(lesson.scene, state)
    fun nextGoal(lesson: Lesson, state: PracticeState) = lesson.goals.firstOrNull { it.key !in state.achieved }
}
