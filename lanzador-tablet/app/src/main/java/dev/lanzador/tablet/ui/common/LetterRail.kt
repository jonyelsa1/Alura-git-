package dev.lanzador.tablet.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

/**
 * Índice A-Z vertical. Al tocar o arrastrar, la letra activa crece y se aparta
 * hacia la izquierda con sus vecinas en forma de onda.
 */
@Composable
fun LetterRail(
    letters: List<String>,
    modifier: Modifier = Modifier,
    idle: Color,
    active: Color,
    style: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    waveWidth: Float = 28f,
    onPick: (String) -> Unit = {},
    onRelease: (String) -> Unit = {},
) {
    var current by remember { mutableStateOf(-1) }
    val list by rememberUpdatedState(letters)
    val pick by rememberUpdatedState(onPick)
    val release by rememberUpdatedState(onRelease)
    val haptics = LocalHapticFeedback.current
    Column(
        modifier.pointerInput(Unit) {
            fun indexAt(y: Float): Int =
                if (list.isEmpty() || size.height == 0) -1 else (y / size.height * list.size).toInt().coerceIn(0, list.lastIndex)

            fun update(y: Float) {
                val i = indexAt(y)
                if (i >= 0 && i != current) {
                    current = i
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pick(list[i])
                }
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                update(down.position.y)
                down.consume()
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    update(change.position.y)
                    change.consume()
                }
                list.getOrNull(current)?.let { release(it) }
                current = -1
            }
        },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { i, letter ->
            val d = if (current < 0) 99f else abs(i - current).toFloat()
            val wave = exp(-(d * d) / 2.2f)
            val shift by animateDpAsState((-waveWidth * wave).dp, label = "shift")
            val grow by animateFloatAsState(1f + 0.7f * wave, label = "grow")
            Text(
                letter,
                style = style.copy(fontWeight = if (d == 0f) FontWeight.Bold else style.fontWeight),
                color = lerp(idle, active, if (d == 0f) 1f else wave * 0.6f),
                modifier = Modifier.offset(x = shift).scale(grow),
            )
        }
    }
}
