package com.bpmredux.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary

@Composable
fun VerticalStabilitySlider(
    stability: Float,
    onStabilityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "S T A B I L I T Y",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp
            ),
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Custom vertical slider
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            val trackPad = 16f
                            val trackTop = trackPad
                            val trackBottom = size.height - trackPad
                            val trackH = trackBottom - trackTop

                            // Set on initial tap
                            val y = down.position.y.coerceIn(trackTop, trackBottom)
                            onStabilityChange(((trackBottom - y) / trackH).coerceIn(0f, 1f))

                            // Follow drag
                            drag(down.id) { change ->
                                change.consume()
                                val dy = change.position.y.coerceIn(trackTop, trackBottom)
                                onStabilityChange(((trackBottom - dy) / trackH).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
        ) {
            val trackPad = 16f
            val trackTop = trackPad
            val trackBottom = size.height - trackPad
            val trackH = trackBottom - trackTop
            val cx = size.width / 2f

            // Track background
            drawLine(
                color = TextDim,
                start = Offset(cx, trackTop),
                end = Offset(cx, trackBottom),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Tick marks along track
            for (i in 0..10) {
                val tickY = trackBottom - (i / 10f) * trackH
                val tickW = if (i % 2 == 0) 10f else 6f
                drawLine(
                    color = if (i % 2 == 0) TextSecondary else TextDim,
                    start = Offset(cx - tickW, tickY),
                    end = Offset(cx + tickW, tickY),
                    strokeWidth = 1f
                )
            }

            // Thumb position (0 at bottom, 1 at top)
            val thumbY = trackBottom - (stability) * trackH

            // Active portion of track (above thumb = active)
            drawLine(
                color = AccentDim.copy(alpha = 0.5f),
                start = Offset(cx, thumbY),
                end = Offset(cx, trackTop),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Thumb glow
            drawCircle(
                color = Accent.copy(alpha = 0.1f),
                radius = 20f,
                center = Offset(cx, thumbY)
            )
            drawCircle(
                color = Accent.copy(alpha = 0.2f),
                radius = 12f,
                center = Offset(cx, thumbY)
            )

            // Thumb line — wide horizontal bar
            drawLine(
                color = Accent,
                start = Offset(cx - 18f, thumbY),
                end = Offset(cx + 18f, thumbY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (stability < 0.3f) "RESPONSIVE"
            else if (stability > 0.7f) "STABLE"
            else "MID",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}