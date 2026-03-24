package com.bpmredux.audio

import kotlin.math.sqrt

enum class Band { SUB, MID, HI }

data class BandEnergy(
    val subKick: Float = 0f,
    val snareMid: Float = 0f,
    val highs: Float = 0f
) {
    fun forBand(band: Band): Float = when (band) {
        Band.SUB -> subKick
        Band.MID -> snareMid
        Band.HI -> highs
    }
}

class BandFilter(
    sampleRate: Int = AudioCapture.SAMPLE_RATE,
    fftSize: Int = AudioCapture.FFT_SIZE
) {
    private val binResolution = sampleRate.toFloat() / fftSize

    // Sub/Kick: 40-150 Hz
    private val subLow = (40f / binResolution).toInt()
    private val subHigh = (150f / binResolution).toInt()

    // Snare/Mid: 150-2000 Hz
    private val midLow = subHigh
    private val midHigh = (2000f / binResolution).toInt()

    // Highs: 2000-10000 Hz
    private val hiLow = midHigh
    private val hiHigh = (10000f / binResolution).toInt()

    fun filter(magnitudes: FloatArray): BandEnergy {
        val maxBin = magnitudes.size - 1
        return BandEnergy(
            subKick = rms(magnitudes, subLow, minOf(subHigh, maxBin)),
            snareMid = rms(magnitudes, midLow, minOf(midHigh, maxBin)),
            highs = rms(magnitudes, hiLow, minOf(hiHigh, maxBin))
        )
    }

    private fun rms(data: FloatArray, from: Int, to: Int): Float {
        if (from >= to || from >= data.size) return 0f
        var sum = 0f
        val end = minOf(to, data.size)
        for (i in from until end) {
            sum += data[i] * data[i]
        }
        return sqrt(sum / (end - from))
    }
}
