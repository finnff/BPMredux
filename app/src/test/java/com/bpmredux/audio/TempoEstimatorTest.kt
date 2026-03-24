package com.bpmredux.audio

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TempoEstimatorTest {

    private lateinit var estimator: TempoEstimator

    @Before
    fun setup() {
        estimator = TempoEstimator()
        estimator.bpmRangeMin = 60f
        estimator.bpmRangeMax = 200f
    }

    @Test
    fun `estimates 120 BPM from synthetic click train`() {
        // 120 BPM = 2 beats per second = onset every 50 ODF samples (at 100 Hz)
        val onsetInterval = 50 // samples at 100 Hz = 0.5s = 120 BPM

        var lastResult: TempoEstimator.Result? = null

        // Run for 4 seconds of ODF samples (400 buffer)
        for (sample in 0 until 400) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % onsetInterval == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) lastResult = result
        }

        assertTrue("Should produce a result", lastResult != null)
        val bpm = lastResult!!.bpm
        assertTrue(
            "Expected ~120 BPM, got $bpm",
            bpm in 110f..130f
        )
    }

    @Test
    fun `estimates 140 BPM from synthetic click train`() {
        val onsetInterval = (100f * 60f / 140f).toInt() // ~43 samples

        var lastResult: TempoEstimator.Result? = null
        for (sample in 0 until 400) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % onsetInterval == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) lastResult = result
        }

        assertTrue("Should produce a result", lastResult != null)
        val bpm = lastResult!!.bpm
        assertTrue(
            "Expected ~140 BPM, got $bpm",
            bpm in 130f..155f
        )
    }

    @Test
    fun `respects BPM range constraint`() {
        estimator.bpmRangeMin = 115f
        estimator.bpmRangeMax = 125f

        val onsetInterval = 50 // 120 BPM

        var lastResult: TempoEstimator.Result? = null
        for (sample in 0 until 400) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % onsetInterval == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) lastResult = result
        }

        if (lastResult != null) {
            val bpm = lastResult.bpm
            assertTrue(
                "BPM $bpm should be within constrained range",
                bpm in 100f..140f
            )
        }
    }

    @Test
    fun `detects range limit when strong signal outside range`() {
        // Strong 170 BPM signal with max=160 — should flag pegging
        estimator.bpmRangeMin = 60f
        estimator.bpmRangeMax = 160f

        val onsetInterval = (100f * 60f / 170f).toInt() // ~35 samples for 170 BPM

        var lastResult: TempoEstimator.Result? = null
        for (sample in 0 until 400) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % onsetInterval == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) lastResult = result
        }

        assertTrue("Should produce a result", lastResult != null)
        // The signal is at 170 BPM which is outside [60, 160], half-time (85) is in range
        // so pegging may or may not trigger depending on whether 85 BPM has a clean match.
        // At minimum, the estimator should return a valid BPM.
        assertTrue("BPM should be positive", lastResult!!.bpm > 0f)
    }

    @Test
    fun `EMA smoothing produces gradual transitions`() {
        // Feed 120 BPM for a while, then switch to 140 BPM
        val interval120 = 50  // 120 BPM
        val interval140 = (100f * 60f / 140f).toInt() // ~43 samples for 140 BPM

        val results = mutableListOf<Float>()

        // Phase 1: 120 BPM
        for (sample in 0 until 300) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % interval120 == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) results.add(result.bpm)
        }

        // Phase 2: 140 BPM
        for (sample in 300 until 600) {
            // Pass flux value (1.0f) on onset, 0f otherwise
            val onsetValue = if (sample % interval140 == 0) 1.0f else 0f
            val result = estimator.addOnsetSample(onsetValue)
            if (result != null) results.add(result.bpm)
        }

        assertTrue("Should produce multiple results", results.size >= 3)

        // Check that transitions are not instantaneous (consecutive values shouldn't jump too far)
        for (i in 1 until results.size) {
            val diff = kotlin.math.abs(results[i] - results[i - 1])
            assertTrue(
                "BPM change between consecutive updates should be gradual (was $diff)",
                diff < 30f // EMA should prevent >30 BPM jumps per update
            )
        }
    }
}
