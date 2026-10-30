package com.example.phoneclass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.phoneclass.learning.*
import com.example.phoneclass.ui.theme.*

/** Teaching pictures and practice pages use the same controls and local artwork. */
@Composable
fun LessonDiagram(lesson: Lesson, index: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.clearAndSetSemantics { contentDescription = "教学图：${lesson.panels[index].caption}" }) {
        if (lesson.scene == Scene.WECHAT) {
            WechatSceneUi(WechatPreviews.state(lesson.id, index), lesson.goals.getOrElse(index) { lesson.goals.last() }.target, {}, diagram = true)
            return@Column
        }
        if (lesson.scene in setOf(Scene.TAP, Scene.VERTICAL, Scene.HORIZONTAL, Scene.PHOTO, Scene.SOUND, Scene.FONT)) GestureGuide(lesson.scene, index)
        SceneUi(lesson.scene, PhonePreviews.state(lesson.scene, index), PhonePreviews.focus(lesson.scene, index), {}, diagram = true)
    }
}

@Composable
private fun GestureGuide(scene: Scene, index: Int) {
    val label = when (scene) {
        Scene.TAP -> if (index == 1) "◎ 按住一会儿" else "◎ 轻轻点一下"
        Scene.VERTICAL -> if (index == 2) "↓ 手指向下，回看上面" else "↑ 手指向上，看下面"
        Scene.HORIZONTAL -> if (index == 2) "→ 向右滑，回上一页" else "← 向左滑，看下一页"
        Scene.PHOTO -> "↔ 两根手指向外分开"
        else -> "→ 向右拖动滑块"
    }
    Surface(color = Color(0xFFEAF0FF), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Blue)
            Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                if (scene == Scene.TAP) {
                    val point = Offset(size.width * .5f, size.height * .5f)
                    drawCircle(Blue.copy(alpha = .16f), 23.dp.toPx(), point)
                    drawCircle(Blue, 18.dp.toPx(), point, style = Stroke(2.dp.toPx()))
                    drawCircle(Color(0xFFE8A449), 9.dp.toPx(), point)
                    return@Canvas
                }
                val horizontal = scene != Scene.VERTICAL
                val start = if (horizontal) Offset(size.width * .24f, size.height * .6f) else Offset(size.width * .5f, size.height * .86f)
                val end = if (horizontal) Offset(size.width * .76f, size.height * .6f) else Offset(size.width * .5f, size.height * .15f)
                val reverse = (scene == Scene.HORIZONTAL && index != 2) || (scene == Scene.VERTICAL && index == 2)
                val a = if (reverse) end else start
                val b = if (reverse) start else end
                drawCircle(Color(0xFFE8A449), 12.dp.toPx(), a)
                drawCircle(Color.White, 5.dp.toPx(), a)
                drawLine(Blue, a, b, 4.dp.toPx(), StrokeCap.Round)
                val d = 9.dp.toPx()
                if (horizontal) {
                    val sign = if (b.x > a.x) -1 else 1
                    drawLine(Blue, b, b + Offset(sign * d, -d), 4.dp.toPx(), StrokeCap.Round)
                    drawLine(Blue, b, b + Offset(sign * d, d), 4.dp.toPx(), StrokeCap.Round)
                } else {
                    val sign = if (b.y < a.y) 1 else -1
                    drawLine(Blue, b, b + Offset(-d, sign * d), 4.dp.toPx(), StrokeCap.Round)
                    drawLine(Blue, b, b + Offset(d, sign * d), 4.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun PhotoArt(modifier: Modifier = Modifier, variant: Int = 0) {
    Canvas(modifier.semantics { contentDescription = listOf("示例照片：山间小屋", "示例照片：花园", "示例照片：海边")[variant.coerceIn(0, 2)] }) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFD9E9E9), Color(0xFFF8EEE0))))
        drawCircle(Color(0xFFF3CF88), h * .11f, Offset(w * .78f, h * .23f))
        if (variant == 2) {
            drawRect(Color(0xFF8BB8BD), Offset(0f, h * .52f), Size(w, h * .48f))
            drawPath(Path().apply { moveTo(0f, h * .8f); quadraticTo(w * .5f, h * .64f, w, h * .85f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFE8D9BA))
            (0..3).forEach { i -> drawLine(Color.White.copy(alpha = .55f), Offset(w * .1f + i * w * .22f, h * .63f), Offset(w * .22f + i * w * .22f, h * .63f), 2.dp.toPx(), StrokeCap.Round) }
        } else {
            drawPath(Path().apply { moveTo(0f, h * .65f); lineTo(w * .28f, h * .26f); lineTo(w * .56f, h * .65f); lineTo(w * .78f, h * .38f); lineTo(w, h * .68f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF92B4AB))
            drawPath(Path().apply { moveTo(0f, h * .82f); quadraticTo(w * .45f, h * .5f, w, h * .86f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF61897B))
            if (variant == 0) {
                drawRoundRect(Color(0xFFF1E7D3), Offset(w * .42f, h * .53f), Size(w * .2f, h * .26f), CornerRadius(3.dp.toPx()))
                drawPath(Path().apply { moveTo(w * .39f, h * .55f); lineTo(w * .52f, h * .38f); lineTo(w * .65f, h * .55f); close() }, Color(0xFFB98264))
                drawRect(Color(0xFF3E655F), Offset(w * .5f, h * .63f), Size(w * .055f, h * .16f))
                drawRect(Color(0xFF90B4AE), Offset(w * .565f, h * .6f), Size(w * .035f, h * .06f))
            } else {
                (0..6).forEach { i ->
                    val x = w * (.1f + i * .13f); val y = h * (.67f + (i % 3) * .06f)
                    drawLine(Color(0xFF375E48), Offset(x, y), Offset(x, h * .96f), 3.dp.toPx())
                    val r = h * .045f
                    (0..4).forEach { p -> val angle = p * Math.PI * 2 / 5; drawCircle(if (i % 2 == 0) Color(0xFFF0CAB9) else Color(0xFFECCE88), r, Offset(x + kotlin.math.cos(angle).toFloat() * r, y + kotlin.math.sin(angle).toFloat() * r)) }
                    drawCircle(Color(0xFFA17D43), r * .5f, Offset(x, y))
                }
            }
        }
    }
}

@Composable
fun QrArt(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "教学示意二维码，不包含可打开的真实链接" }) {
        val side = size.minDimension * .8f
        val start = Offset((size.width - side) / 2, (size.height - side) / 2)
        val unit = side / 21
        drawRect(Color.White)
        fun finder(col: Int, row: Int) {
            drawRect(Ink, start + Offset(col * unit, row * unit), Size(7 * unit, 7 * unit))
            drawRect(Color.White, start + Offset((col + 1) * unit, (row + 1) * unit), Size(5 * unit, 5 * unit))
            drawRect(Ink, start + Offset((col + 2) * unit, (row + 2) * unit), Size(3 * unit, 3 * unit))
        }
        for (x in 0..20) for (y in 0..20) {
            val reserved = (x < 8 && y < 8) || (x > 12 && y < 8) || (x < 8 && y > 12)
            if (!reserved && ((x * 17 + y * 11 + x * y) % 5 < 2)) drawRect(Ink, start + Offset(x * unit, y * unit), Size(unit, unit))
        }
        finder(0, 0); finder(14, 0); finder(0, 14)
        drawRoundRect(Blue, start - Offset(6.dp.toPx(), 6.dp.toPx()), Size(side + 12.dp.toPx(), side + 12.dp.toPx()), CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

