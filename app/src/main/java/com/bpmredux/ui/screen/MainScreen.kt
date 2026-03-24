package com.bpmredux.ui.screen

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bpmredux.audio.Band
import com.bpmredux.ui.BpmUiState
import com.bpmredux.ui.components.AmplitudeThresholdSlider
import com.bpmredux.ui.components.BandToggleRow
import com.bpmredux.ui.components.BpmDisplay
import com.bpmredux.ui.components.BpmRangeSlider
import com.bpmredux.ui.components.Spectrogram
import com.bpmredux.ui.components.TapArea
import com.bpmredux.ui.components.VerticalStabilitySlider
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.Black
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max

@Composable
fun MainScreen(
    uiState: BpmUiState,
    onTap: () -> Unit,
    onBpmRangeChange: (Float, Float) -> Unit,
    onBandToggle: (Band) -> Unit,
    onToggleDetection: () -> Unit,
    onToggleScreenDim: () -> Unit,
    onAmplitudeThresholdChange: (Float) -> Unit,
    onStabilityChange: (Float) -> Unit
) {
    val context = LocalContext.current

    // Keep screen on
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Screen dim
    DisposableEffect(uiState.isScreenDimmed) {
        val window = (context as? android.app.Activity)?.window
        val params = window?.attributes
        if (uiState.isScreenDimmed) {
            params?.screenBrightness = 0.01f
        } else {
            params?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window?.attributes = params
        onDispose { }
    }

    // Secondary readout values
    val peakFreq = uiState.spectrogramColumn?.let { mags ->
        if (mags.size < 2) 0f else {
            val peakBin = mags.indices.maxByOrNull { mags[it] } ?: 0
            peakBin * 44100f / 4096f
        }
    } ?: 0f
    val signalStr = uiState.spectrogramColumn?.let { mags ->
        if (mags.isEmpty()) 0f else mags.max()
    } ?: 0f

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 4.dp)
        ) {
            // BPM Display with threshold slider on left and stability slider on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
            ) {
                AmplitudeThresholdSlider(
                    threshold = uiState.amplitudeThreshold,
                    onThresholdChange = onAmplitudeThresholdChange,
                    modifier = Modifier.fillMaxHeight()
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BpmDisplay(uiState = uiState)
                }

                VerticalStabilitySlider(
                    stability = uiState.stability,
                    onStabilityChange = onStabilityChange,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            // Range slider
            BpmRangeSlider(
                rangeMin = uiState.bpmRangeMin,
                rangeMax = uiState.bpmRangeMax,
                onRangeChange = onBpmRangeChange,
                isAtRangeLimit = uiState.isAtRangeLimit,
                limitSide = uiState.limitSide
            )

            // Spectrogram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .padding(horizontal = 4.dp)
            ) {
                Spectrogram(
                    spectrogramColumn = uiState.spectrogramColumn,
                    activeBands = uiState.activeBands
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(TextDim)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Band toggles
            BandToggleRow(
                activeBands = uiState.activeBands,
                onToggle = onBandToggle
            )

            // Divider
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(TextDim)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Tap area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .padding(horizontal = 20.dp)
            ) {
                TapArea(
                    tapCount = uiState.tapCount,
                    tapBpm = uiState.tapBpm,
                    onTap = onTap
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Secondary readouts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReadoutItem("FREQ", "${peakFreq.toInt()}HZ")
                ReadoutItem("SIG", "${(minOf(1f, signalStr * 4f) * 100).toInt()}%")
                ReadoutItem("CONF", "${(uiState.confidence * 100).toInt()}%")
            }

            // Dim toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = onToggleScreenDim) {
                    Icon(
                        imageVector = if (uiState.isScreenDimmed) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                        contentDescription = "Toggle dim",
                        tint = TextDim
                    )
                }
            }
        }

        // Global scanline overlay + vignette
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Scanlines
            val scanColor = AccentDim.copy(alpha = 0.05f)
            var y = 0f
            while (y < size.height) {
                drawLine(scanColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += 4f
            }

            // Vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Black.copy(alpha = 0.5f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = max(size.width, size.height) * 0.75f
                )
            )
        }
    }
}

@Composable
private fun ReadoutItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}
