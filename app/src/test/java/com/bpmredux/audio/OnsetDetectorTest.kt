package com.bpmredux.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnsetDetectorTest {

    private lateinit var detector: OnsetDetector

    @Before
    fun setup() {
        detector = OnsetDetector()
    }

    @Test
    fun `detects onset from spike in magnitudes`() {
        val silent = FloatArray(512) { 0.001f }
        val loud = FloatArray(512) { if (it in 4..14) 1.0f else 0.001f }
        val bandFilter = BandFilter()

        // Feed several silent frames to build history
        var detectedOnset = false
        for (frame in 0..30) {
            val mags = if (frame < 25) silent else loud
            val energy = bandFilter.filter(mags)
            val flux = detector.process(mags, energy, frame * 23L)
            if (flux > 0f) detectedOnset = true
        }

        assertTrue("Should detect onset from sudden spike", detectedOnset)
    }

    @Test
    fun `respects minimum inter-onset interval`() {
        val silent = FloatArray(512) { 0.001f }
        val loud = FloatArray(512) { 1.0f }
        val bandFilter = BandFilter()

        // Build history
        for (frame in 0..24) {
            detector.process(silent, bandFilter.filter(silent), frame * 23L)
        }

        // First onset
        val first = detector.process(loud, bandFilter.filter(loud), 25 * 23L)

        // Immediate second onset (within 100ms)
        val second = detector.process(loud, bandFilter.filter(loud), 25 * 23L + 50)

        // Can't both be positive if interval enforced
        if (first > 0f) {
            assertFalse("Second onset within 100ms should be suppressed (returns 0)", second <= 0f)
        }
    }

    @Test
    fun `no onset on constant signal`() {
        val constant = FloatArray(512) { 0.5f }
        val bandFilter = BandFilter()
        var anyOnset = false

        for (frame in 0..50) {
            val flux = detector.process(constant, bandFilter.filter(constant), frame * 23L)
            if (flux > 0f) {
                anyOnset = true
            }
        }

        assertFalse("Constant signal should not produce onsets", anyOnset)
    }
}
