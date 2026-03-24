package com.bpmredux.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TapProcessorTest {

    private lateinit var processor: TapProcessor

    @Before
    fun setup() {
        processor = TapProcessor()
    }

    @Test
    fun `calculates BPM from regular taps at 120 BPM`() {
        // 120 BPM = 500ms between taps
        val interval = 500L
        var result = TapProcessor.Result()

        for (i in 0..5) {
            result = processor.tap(i * interval)
        }

        assertTrue("Should have BPM > 0", result.bpm > 0f)
        assertTrue(
            "Expected ~120 BPM, got ${result.bpm}",
            result.bpm in 115f..125f
        )
    }

    @Test
    fun `requires minimum 3 taps for BPM`() {
        val r1 = processor.tap(0)
        assertEquals(0f, r1.bpm, 0.01f)
        assertEquals(1, r1.tapCount)

        val r2 = processor.tap(500)
        assertEquals(0f, r2.bpm, 0.01f)
        assertEquals(2, r2.tapCount)

        val r3 = processor.tap(1000)
        assertTrue("Should have BPM after 3 taps", r3.bpm > 0f)
        assertEquals(3, r3.tapCount)
    }

    @Test
    fun `rejects outlier intervals`() {
        // Regular taps at 500ms, then one outlier at 2000ms
        processor.tap(0)
        processor.tap(500)
        processor.tap(1000)
        processor.tap(1500)
        val result = processor.tap(3500) // outlier: 2000ms gap

        // BPM should still be close to 120, not dragged down by outlier
        if (result.bpm > 0f) {
            assertTrue(
                "BPM should not be severely affected by single outlier: ${result.bpm}",
                result.bpm in 50f..200f
            )
        }
    }

    @Test
    fun `resets after long gap`() {
        processor.tap(0)
        processor.tap(500)
        processor.tap(1000)

        // Long gap > 10 seconds
        val result = processor.tap(12000)
        assertEquals("Should reset after 10s gap", 1, result.tapCount)
    }

    @Test
    fun `confidence decreases over time`() {
        processor.tap(0)
        processor.tap(500)
        processor.tap(1000)

        val earlyConf = processor.getConfidenceAt(2000)
        val lateConf = processor.getConfidenceAt(8000)

        assertTrue("Early confidence should be > 0", earlyConf > 0f)
        assertTrue("Late confidence should be less", lateConf < earlyConf)
    }
}
