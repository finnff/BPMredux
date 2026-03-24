package com.bpmredux.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.sp
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary

@Composable
fun AmplitudeThresholdSlider(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // Vertical text label (letters stacked)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            "THRESHOLD".forEach { ch ->
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Custom vertical slider — large touch target, industrial look
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
                            onThresholdChange(((trackBottom - y) / trackH * 0.5f).coerceIn(0f, 0.5f))

                            // Follow drag
                            drag(down.id) { change ->
                                change.consume()
                                val dy = change.position.y.coerceIn(trackTop, trackBottom)
                                onThresholdChange(((trackBottom - dy) / trackH * 0.5f).coerceIn(0f, 0.5f))
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
            for (i in 0..5) {
                val tickY = trackBottom - (i / 5f) * trackH
                val tickW = if (i == 0 || i == 5) 10f else 6f
                drawLine(
                    color = TextDim,
                    start = Offset(cx - tickW, tickY),
                    end = Offset(cx + tickW, tickY),
                    strokeWidth = 1f
                )
            }

            // Thumb position (0 at bottom, 0.5 at top)
            val thumbY = trackBottom - (threshold / 0.5f) * trackH

            // Active portion of track (below thumb = inactive, above = active)
            drawLine(
                color = AccentDim.copy(alpha = 0.5f),
                start = Offset(cx, thumbY),
                end = Offset(cx, trackBottom),
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

            // Thumb circle
            drawCircle(
                color = Accent,
                radius = 8f,
                center = Offset(cx, thumbY)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(threshold * 200).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
