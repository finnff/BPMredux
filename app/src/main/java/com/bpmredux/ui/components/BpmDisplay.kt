package com.bpmredux.ui.components

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.bpmredux.R
import com.bpmredux.ui.BpmUiState
import com.bpmredux.ui.theme.Accent
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.TextDim
import com.bpmredux.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BpmDisplay(
    uiState: BpmUiState,
    modifier: Modifier = Modifier
) {
    val ringPulse = remember { Animatable(0f) }
    LaunchedEffect(uiState.beatEvent) {
        if (uiState.beatEvent > 0) {
            ringPulse.snapTo(1f)
            ringPulse.animateTo(0f, tween(400))
        }
    }

    val context = LocalContext.current
    val bpmTypeface = remember {
        ResourcesCompat.getFont(context, R.font.share_tech_mono) ?: Typeface.MONOSPACE
    }

    Canvas(modifier = modifier.fillMaxSize().padding(4.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(size.width, size.height) / 2f - 12f

        val arcStart = 135f
        val arcSweep = 270f
        val bpmFloor = 60f
        val bpmCeiling = 240f

        fun bpmToAngle(bpm: Float): Float =
            arcStart + ((bpm - bpmFloor) / (bpmCeiling - bpmFloor)) * arcSweep

        val arcRect = Size(radius * 2, radius * 2)
        val arcOffset = Offset(centerX - radius, centerY - radius)

        // ── Gauge arc with phosphor glow ──

        // Background arc glow (wide, faint)
        drawArc(
            color = TextDim.copy(alpha = 0.08f),
            startAngle = arcStart,
            sweepAngle = arcSweep,
            useCenter = false,
            topLeft = arcOffset,
            size = arcRect,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        // Background arc core
        drawArc(
            color = TextDim,
            startAngle = arcStart,
            sweepAngle = arcSweep,
            useCenter = false,
            topLeft = arcOffset,
            size = arcRect,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )

        // Range highlight glow
        val rangeStartA = bpmToAngle(uiState.bpmRangeMin)
        val rangeSweepA = bpmToAngle(uiState.bpmRangeMax) - rangeStartA
        drawArc(
            color = AccentDim.copy(alpha = 0.1f),
            startAngle = rangeStartA,
            sweepAngle = rangeSweepA,
            useCenter = false,
            topLeft = arcOffset,
            size = arcRect,
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
        // Range highlight core
        drawArc(
            color = AccentDim.copy(alpha = 0.4f),
            startAngle = rangeStartA,
            sweepAngle = rangeSweepA,
            useCenter = false,
            topLeft = arcOffset,
            size = arcRect,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Tick marks every 20 BPM
        val majorTicks = setOf(120, 140, 160, 180)
        for (bpmTick in 60..240 step 20) {
            val angleDeg = bpmToAngle(bpmTick.toFloat())
            val angleRad = angleDeg * PI.toFloat() / 180f
            val isMajor = bpmTick in majorTicks
            val innerR = radius - if (isMajor) 10f else 5f
            val outerR = radius + if (isMajor) 4f else 2f
            val ca = cos(angleRad)
            val sa = sin(angleRad)

            drawLine(
                color = if (isMajor) TextSecondary else TextDim,
                start = Offset(centerX + innerR * ca, centerY + innerR * sa),
                end = Offset(centerX + outerR * ca, centerY + outerR * sa),
                strokeWidth = if (isMajor) 1.5f else 1f
            )
        }

        // ── Current BPM indicator with layered glow ──
        if (uiState.currentBpm > 0f) {
            val clamped = uiState.currentBpm.coerceIn(bpmFloor, bpmCeiling)
            val iRad = bpmToAngle(clamped) * PI.toFloat() / 180f
            val ix = centerX + radius * cos(iRad)
            val iy = centerY + radius * sin(iRad)

            // Outer phosphor bloom
            drawCircle(
                color = Accent.copy(alpha = 0.08f + ringPulse.value * 0.1f),
                radius = 22f,
                center = Offset(ix, iy)
            )
            // Inner glow
            drawCircle(
                color = Accent.copy(alpha = 0.2f + ringPulse.value * 0.35f),
                radius = 12f,
                center = Offset(ix, iy)
            )
            // Core dot
            drawCircle(
                color = Accent,
                radius = 3.5f,
                center = Offset(ix, iy)
            )
        }

        // ── BPM number with phosphor glow ──
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val textSizePx = 64f * density * fontScale
        val textAlpha = if (uiState.currentBpm > 0f) {
            0.4f + uiState.confidence * 0.6f
        } else 0.3f

        val bpmText = if (uiState.currentBpm > 0f) {
            val base = String.format(Locale.US, "%.1f", uiState.currentBpm)
            if (uiState.isAtRangeLimit) "${base}+" else base
        } else "\u2014"

        val textY = centerY + textSizePx * 0.35f

        val mainPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (255 * textAlpha).toInt(), 0x7F, 0xCF, 0xD6
            )
            this.textSize = textSizePx
            typeface = bpmTypeface
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.06f
            setShadowLayer(
                12f * density, 0f, 0f,
                android.graphics.Color.argb((140 * textAlpha).toInt(), 0x00, 0xE5, 0xFF)
            )
        }
        nativeCanvas.drawText(bpmText, centerX, textY, mainPaint)

        // "BPM" label
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(153, 0x2A, 0x6B, 0x6E)
            this.textSize = 13f * density * fontScale
            typeface = bpmTypeface
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.2f
            isAntiAlias = true
        }
        nativeCanvas.drawText("BPM", centerX, textY + 20f * density, labelPaint)

        // Mic indicator
        if (uiState.isDetecting) {
            drawCircle(
                color = Accent.copy(alpha = 0.5f),
                radius = 2.5f * density,
                center = Offset(centerX, textY + 34f * density)
            )
        }
    }
}
