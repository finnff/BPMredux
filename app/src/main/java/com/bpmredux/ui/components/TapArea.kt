package com.bpmredux.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TapArea(
    tapCount: Int,
    tapBpm: Float?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            flashAlpha.snapTo(0.12f)
            flashAlpha.animateTo(0f, tween(250))
        }
    }

    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(0.5.dp, TextDim, shape)
            .background(Accent.copy(alpha = flashAlpha.value), shape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                        if (change != null) {
                            onTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MANUAL INPUT",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            Text(
                text = "TAP",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            if (tapBpm != null && tapBpm > 0f) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", tapBpm)} BPM  \u00D7$tapCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentDim.copy(alpha = 0.6f)
                )
            }
        }
    }
}
