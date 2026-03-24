package com.bpmredux.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bpmredux.audio.AudioCapture
import com.bpmredux.audio.Band
import com.bpmredux.ui.theme.AccentDim
import com.bpmredux.ui.theme.Black
import com.bpmredux.ui.theme.TextDim
import kotlin.math.log10
import kotlin.math.roundToInt

private const val FREQ_BINS = 128
private const val TIME_ROWS = 256
private const val SAMPLE_RATE = AudioCapture.SAMPLE_RATE
private const val MAX_FREQ = SAMPLE_RATE / 2f

private const val DB_FLOOR = -80f
private const val DB_CEILING = -10f

private const val SUB_MID_HZ = 150f
private const val MID_HI_HZ = 2000f

@Composable
fun Spectrogram(
    spectrogramColumn: FloatArray?,
    activeBands: Set<Band>,
    modifier: Modifier = Modifier
) {
    val bitmap = remember {
        Bitmap.createBitmap(FREQ_BINS, TIME_ROWS, Bitmap.Config.ARGB_8888)
    }
    val colorLut = remember { buildTealLut() }
    val melMapping = remember { buildMelMapping(FREQ_BINS) }
    val writePos = remember { intArrayOf(0) }
    val paint = remember { Paint() }

    val subMidBin = remember { melBinForHz(SUB_MID_HZ) }
    val midHiBin = remember { melBinForHz(MID_HI_HZ) }

    // Static noise grain texture
    val noiseBitmap = remember {
        val bmp = Bitmap.createBitmap(FREQ_BINS, TIME_ROWS, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(FREQ_BINS * TIME_ROWS)
        val rng = java.util.Random(42)
        for (i in pixels.indices) {
            val v = rng.nextInt(60)
            pixels[i] = android.graphics.Color.argb(8, 0, v, v)
        }
        bmp.setPixels(pixels, 0, FREQ_BINS, 0, 0, FREQ_BINS, TIME_ROWS)
        bmp
    }

    LaunchedEffect(spectrogramColumn) {
        spectrogramColumn?.let { magnitudes ->
            val y = writePos[0] % TIME_ROWS
            val androidCanvas = AndroidCanvas(bitmap)
            val binned = melBin(magnitudes, melMapping)

            for (x in 0 until FREQ_BINS) {
                val power = binned[x] * binned[x]
                val db = if (power > 0f) (10f * log10(power)).coerceIn(DB_FLOOR, DB_CEILING) else DB_FLOOR
                val normalized = (db - DB_FLOOR) / (DB_CEILING - DB_FLOOR)
                val intensity = (normalized * 255).roundToInt().coerceIn(0, 255)
                paint.color = colorLut[intensity]
                androidCanvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
            writePos[0]++
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pos = writePos[0] % TIME_ROWS
            val imageBitmap = bitmap.asImageBitmap()
            val cw = size.width.toInt()
            val ch = size.height.toInt()

            // Draw spectrogram with vertical wrap
            if (writePos[0] > TIME_ROWS) {
                val oldRows = TIME_ROWS - pos
                if (oldRows > 0) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset(0, pos),
                        srcSize = IntSize(FREQ_BINS, oldRows),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(cw, ch * oldRows / TIME_ROWS)
                    )
                }
                if (pos > 0) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(FREQ_BINS, pos),
                        dstOffset = IntOffset(0, ch * oldRows / TIME_ROWS),
                        dstSize = IntSize(cw, ch * pos / TIME_ROWS)
                    )
                }
            } else if (writePos[0] > 0) {
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(FREQ_BINS, maxOf(1, writePos[0])),
                    dstOffset = IntOffset(0, ch - ch * writePos[0] / TIME_ROWS),
                    dstSize = IntSize(cw, ch * writePos[0] / TIME_ROWS)
                )
            }

            // Noise grain overlay
            drawImage(
                image = noiseBitmap.asImageBitmap(),
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(FREQ_BINS, TIME_ROWS),
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(cw, ch)
            )

            // Scanlines (3px interval, ~12% opacity)
            val scanColor = AccentDim.copy(alpha = 0.12f)
            var sy = 0f
            while (sy < size.height) {
                drawLine(scanColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                sy += 3f
            }

            // Edge fades — all four sides
            val fadeH = size.height * 0.08f
            val fadeW = size.width * 0.05f

            // Top
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Black, Black.copy(alpha = 0f)),
                    startY = 0f, endY = fadeH
                ),
                size = Size(size.width, fadeH)
            )
            // Bottom
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Black.copy(alpha = 0f), Black),
                    startY = size.height - fadeH, endY = size.height
                ),
                topLeft = Offset(0f, size.height - fadeH),
                size = Size(size.width, fadeH)
            )
            // Left
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Black, Black.copy(alpha = 0f)),
                    startX = 0f, endX = fadeW
                ),
                size = Size(fadeW, size.height)
            )
            // Right
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Black.copy(alpha = 0f), Black),
                    startX = size.width - fadeW, endX = size.width
                ),
                topLeft = Offset(size.width - fadeW, 0f),
                size = Size(fadeW, size.height)
            )

            // Inner border (dark teal)
            drawRect(
                color = TextDim,
                style = Stroke(width = 1f),
                size = size
            )

            // Band boundary vertical lines
            val subMidX = size.width * subMidBin / FREQ_BINS
            val midHiX = size.width * midHiBin / FREQ_BINS
            val lineColor = AccentDim.copy(alpha = 0.15f)

            drawLine(lineColor, Offset(subMidX, 0f), Offset(subMidX, size.height), strokeWidth = 1f)
            drawLine(lineColor, Offset(midHiX, 0f), Offset(midHiX, size.height), strokeWidth = 1f)

            // Band labels at bottom
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(50, 0x00, 0xE5, 0xFF)
                textSize = 16f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
                letterSpacing = 0.15f
            }
            drawContext.canvas.nativeCanvas.apply {
                val labelY = size.height - 6f
                drawText("SUB", subMidX / 2f, labelY, labelPaint)
                drawText("MID", (subMidX + midHiX) / 2f, labelY, labelPaint)
                drawText("HI", (midHiX + size.width) / 2f, labelY, labelPaint)
            }
        }
    }
}

private fun hzToMel(hz: Float): Float = 2595f * log10(1f + hz / 700f)

private fun melBinForHz(hz: Float): Float {
    val melMax = hzToMel(MAX_FREQ)
    return (hzToMel(hz) / melMax) * FREQ_BINS
}

private fun buildMelMapping(numMelBins: Int): Array<IntArray> {
    val melMax = hzToMel(MAX_FREQ)
    val fftSize = AudioCapture.FFT_SIZE
    val numFftBins = fftSize / 2 + 1
    val hzPerBin = SAMPLE_RATE.toFloat() / fftSize

    return Array(numMelBins) { i ->
        val melLow = melMax * i.toFloat() / numMelBins
        val melHigh = melMax * (i + 1).toFloat() / numMelBins
        val hzLow = 700f * (Math.pow(10.0, (melLow / 2595f).toDouble()).toFloat() - 1f)
        val hzHigh = 700f * (Math.pow(10.0, (melHigh / 2595f).toDouble()).toFloat() - 1f)
        val binLow = (hzLow / hzPerBin).roundToInt().coerceIn(0, numFftBins - 1)
        val binHigh = (hzHigh / hzPerBin).roundToInt().coerceIn(binLow, numFftBins - 1)
        intArrayOf(binLow, binHigh)
    }
}

private fun melBin(magnitudes: FloatArray, mapping: Array<IntArray>): FloatArray {
    val result = FloatArray(mapping.size)
    for (i in mapping.indices) {
        val (low, high) = mapping[i]
        if (low > magnitudes.lastIndex) continue
        val effectiveHigh = minOf(high, magnitudes.lastIndex)
        var sum = 0f
        var count = 0
        for (j in low..effectiveHigh) {
            sum += magnitudes[j]
            count++
        }
        result[i] = if (count > 0) sum / count else 0f
    }
    return result
}

/** Black → teal → bright cyan colormap (no white) */
private fun buildTealLut(): IntArray {
    val lut = IntArray(256)
    for (i in 0..255) {
        val t = i / 255f
        val s = t * t // gamma — more detail in darks
        val r = 0
        val g = (s * 229).roundToInt().coerceIn(0, 229)
        val b = (s * 255).roundToInt().coerceIn(0, 255)
        lut[i] = android.graphics.Color.rgb(r, g, b)
    }
    return lut
}
