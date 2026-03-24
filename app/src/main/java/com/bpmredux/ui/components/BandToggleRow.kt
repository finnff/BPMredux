package com.bpmredux.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bpmredux.audio.Band
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.AccentSubtle
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary

@Composable
fun BandToggleRow(
    activeBands: Set<Band>,
    onToggle: (Band) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Band.entries.forEach { band ->
            val selected = band in activeBands
            val label = when (band) {
                Band.SUB -> "SUB"
                Band.MID -> "MID"
                Band.HI -> "HI"
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LED indicator dot
                Canvas(modifier = Modifier.size(4.dp).padding(bottom = 1.dp)) {
                    drawCircle(
                        color = if (selected) Accent else TextDim,
                        radius = size.minDimension / 2f
                    )
                }

                // Sharp rectangle button
                val shape = RoundedCornerShape(2.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .border(
                            BorderStroke(
                                0.5.dp,
                                if (selected) AccentDim else TextDim
                            ),
                            shape
                        )
                        .background(
                            if (selected) AccentSubtle.copy(alpha = 0.15f) else Color.Transparent,
                            shape
                        )
                        .clickable { onToggle(band) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Glowing cyan dot indicator
                    if (selected) {
                        Canvas(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 4.dp)
                                .size(6.dp)
                        ) {
                            // Outer glow
                            drawCircle(
                                color = Accent.copy(alpha = 0.3f),
                                radius = size.minDimension
                            )
                            // Inner glow
                            drawCircle(
                                color = Accent.copy(alpha = 0.6f),
                                radius = size.minDimension * 0.7f
                            )
                            // Core dot
                            drawCircle(
                                color = Accent,
                                radius = size.minDimension * 0.4f
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Accent else TextSecondary
                    )
                }
            }
        }
    }
}
