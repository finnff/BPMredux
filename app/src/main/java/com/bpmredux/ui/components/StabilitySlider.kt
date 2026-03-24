package com.bpmredux.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary

@Composable
fun StabilitySlider(
    stability: Float,
    onStabilityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "RESPONSIVE",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "STABILITY",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "STABLE",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
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
                val stabilityRange = 1f - 0f
                for (i in 0..10) {
                    val x = (i / 10f) * size.width
                    val isMajor = i % 2 == 0
                    val tickH = if (isMajor) 8f else 4f
                    val cy = size.height / 2f
                    drawLine(
                        color = if (isMajor) TextSecondary else TextDim,
                        start = Offset(x, cy - tickH),
                        end = Offset(x, cy + tickH),
                        strokeWidth = if (isMajor) 1.5f else 1f
                    )
                }
            }

            Slider(
                value = stability,
                onValueChange = onStabilityChange,
                valueRange = 0f..1f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = AccentDim.copy(alpha = 0.4f),
                    inactiveTrackColor = TextDim.copy(alpha = 0.4f)
                )
            )
        }
    }
}