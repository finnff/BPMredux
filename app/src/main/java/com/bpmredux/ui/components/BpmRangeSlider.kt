package com.bpmredux.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.bpmredux.audio.RangeLimitSide
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary

@Composable
fun BpmRangeSlider(
    rangeMin: Float,
    rangeMax: Float,
    onRangeChange: (Float, Float) -> Unit,
    isAtRangeLimit: Boolean = false,
    limitSide: RangeLimitSide? = null,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "pegging_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${rangeMin.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = AccentDim.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "RANGE",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "${rangeMax.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = AccentDim.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Box {
            // Tick marks behind slider
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 10.dp)
            ) {
                val range = 240f - 60f
                for (bpm in 60..240 step 20) {
                    val x = (bpm - 60f) / range * size.width
                    val isMajor = bpm % 60 == 0
                    val tickH = if (isMajor) 8f else 4f
                    val cy = size.height / 2f
                    drawLine(
                        color = if (isMajor) TextSecondary else TextDim,
                        start = Offset(x, cy - tickH),
                        end = Offset(x, cy + tickH),
                        strokeWidth = 1f
                    )
                }
            }

            RangeSlider(
                value = rangeMin..rangeMax,
                onValueChange = { range ->
                    onRangeChange(range.start, range.endInclusive)
                },
                valueRange = 60f..240f,
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = AccentDim.copy(alpha = 0.4f),
                    inactiveTrackColor = TextDim.copy(alpha = 0.4f)
                )
            )

            // Pulsing glow on limiting thumb
            if (isAtRangeLimit && limitSide != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 10.dp)
                ) {
                    val range = 240f - 60f
                    val thumbX = when (limitSide) {
                        RangeLimitSide.MIN -> (rangeMin - 60f) / range * size.width
                        RangeLimitSide.MAX -> (rangeMax - 60f) / range * size.width
                    }
                    drawCircle(
                        color = Accent.copy(alpha = pulseAlpha),
                        radius = 16f,
                        center = Offset(thumbX, size.height / 2f)
                    )
                }
            }
        }
    }
}
