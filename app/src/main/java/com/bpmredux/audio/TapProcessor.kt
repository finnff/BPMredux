package com.bpmredux.audio

class TapProcessor {

    companion object {
        private const val MAX_TAPS = 8
        private const val MIN_TAPS_FOR_BPM = 3
        private const val DECAY_START_MS = 5000L
        private const val RESET_AFTER_MS = 10000L
    }

    data class Result(
        val bpm: Float = 0f,
        val confidence: Float = 0f,
        val tapCount: Int = 0
    )

    private val tapTimestamps = ArrayDeque<Long>(MAX_TAPS)
    private var lastTapTime = 0L
    private var lastTapConfidence = 0f

    fun tap(timeMs: Long): Result {
        // Reset if too long since last tap
        if (lastTapTime > 0 && timeMs - lastTapTime > RESET_AFTER_MS) {
            tapTimestamps.clear()
        }

        tapTimestamps.addLast(timeMs)
        if (tapTimestamps.size > MAX_TAPS) tapTimestamps.removeFirst()
        lastTapTime = timeMs

        if (tapTimestamps.size < MIN_TAPS_FOR_BPM) {
            return Result(tapCount = tapTimestamps.size)
        }

        // Compute intervals
        val intervals = mutableListOf<Long>()
        for (i in 1 until tapTimestamps.size) {
            intervals.add(tapTimestamps[i] - tapTimestamps[i - 1])
        }

        // Outlier rejection: remove intervals > 2x median
        val sortedIntervals = intervals.sorted()
        val median = sortedIntervals[sortedIntervals.size / 2]
        val filtered = intervals.filter { it <= median * 2 && it >= median / 2 }

        if (filtered.isEmpty()) return Result(tapCount = tapTimestamps.size)

        val avgInterval = filtered.average()
        val bpm = (60000.0 / avgInterval).toFloat()

        // Confidence: 1 - stddev/mean
        val mean = filtered.average()
        val variance = filtered.map { (it - mean) * (it - mean) }.average()
        val stddev = kotlin.math.sqrt(variance)
        val confidence = maxOf(0f, minOf(1f, (1.0 - stddev / mean).toFloat()))

        lastTapConfidence = confidence
        return Result(bpm, confidence, tapTimestamps.size)
    }

    fun getConfidenceAt(timeMs: Long): Float {
        if (lastTapTime == 0L || tapTimestamps.size < MIN_TAPS_FOR_BPM) return 0f

        val elapsed = timeMs - lastTapTime
        return when {
            elapsed > RESET_AFTER_MS -> 0f
            elapsed > DECAY_START_MS -> {
                val decayDuration = RESET_AFTER_MS - DECAY_START_MS
                val decayElapsed = elapsed - DECAY_START_MS
                val decayFactor = 1f - (decayElapsed.toFloat() / decayDuration.toFloat())
                maxOf(0f, lastTapConfidence * decayFactor)
            }
            else -> lastTapConfidence
        }
    }

    fun reset() {
        tapTimestamps.clear()
        lastTapTime = 0L
    }
}
