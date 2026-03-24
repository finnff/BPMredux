package com.bpmredux.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FFTProcessorTest {

    private val fftSize = 4096
    private val sampleRate = 44100
    private val processor = FFTProcessor(fftSize)

    @Test
    fun `detects 440 Hz sinusoid`() {
        val samples = ShortArray(fftSize) { i ->
            (sin(2.0 * PI * 440.0 * i / sampleRate) * 16000).toInt().toShort()
        }

        val magnitudes = processor.process(samples)
        val expectedBin = (440.0 * fftSize / sampleRate).toInt()

        // Find peak bin
        var peakBin = 0
        var peakVal = 0f
        for (i in 1 until magnitudes.size) {
            if (magnitudes[i] > peakVal) {
                peakVal = magnitudes[i]
                peakBin = i
            }
        }

        // Peak should be within 1 bin of expected
        assertTrue(
            "Peak at bin $peakBin, expected near $expectedBin",
            kotlin.math.abs(peakBin - expectedBin) <= 1
        )
    }

    @Test
    fun `detects 1000 Hz sinusoid`() {
        val freq = 1000.0
        val samples = ShortArray(fftSize) { i ->
            (sin(2.0 * PI * freq * i / sampleRate) * 16000).toInt().toShort()
        }

        val magnitudes = processor.process(samples)
        val expectedBin = (freq * fftSize / sampleRate).toInt()

        var peakBin = 0
        var peakVal = 0f
        for (i in 1 until magnitudes.size) {
            if (magnitudes[i] > peakVal) {
                peakVal = magnitudes[i]
                peakBin = i
            }
        }

        assertTrue(
            "Peak at bin $peakBin, expected near $expectedBin",
            kotlin.math.abs(peakBin - expectedBin) <= 1
        )
    }

    @Test
    fun `magnitude spectrum has correct size`() {
        val samples = ShortArray(fftSize)
        val magnitudes = processor.process(samples)
        assertEquals(fftSize / 2 + 1, magnitudes.size)
    }

    @Test
    fun `silence produces near-zero magnitudes`() {
        val samples = ShortArray(fftSize)
        val magnitudes = processor.process(samples)
        val maxMag = magnitudes.max()
        assertTrue("Silence max magnitude should be near zero: $maxMag", maxMag < 0.001f)
    }
}
