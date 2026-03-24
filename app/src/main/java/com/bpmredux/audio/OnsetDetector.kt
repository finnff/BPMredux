package com.bpmredux.audio

class OnsetDetector {

    companion object {
        private const val MIN_ONSET_INTERVAL_MS = 100L
        private const val AUTO_TARGET_MIN = 1f // onsets per second
        private const val AUTO_TARGET_MAX = 8f
    }

    enum class Sensitivity(val baseThreshold: Float) {
        LOW(2.0f), MEDIUM(1.5f), HIGH(1.0f), AUTO(1.5f)
    }

    // Stability-adjustable history size (default = 22 at level 0.5)
    private var historySize = 22
    private var fluxHistory = FloatArray(historySize)
    private var historyPos = 0
    private var historyCount = 0
    private var previousMagnitudes: FloatArray? = null
    private var lastOnsetTime = 0L
    private var sensitivity = Sensitivity.AUTO
    private var autoThreshold = 1.5f
    private var recentOnsetCount = 0
    private var onsetWindowStart = 0L

    var activeBands: Set<Band> = setOf(Band.SUB, Band.MID, Band.HI)

    /**
     * Set stability level (0-1).
     * 0 = most responsive (smaller history, faster adaptation)
     * 1 = most stable (larger history, slower adaptation)
     */
    fun setStability(level: Float) {
        // HISTORY_SIZE: 10 (responsive) to 40 (stable), default 22 at level 0.5
        val newSize = (10 + level * 30).toInt()

        if (newSize != historySize) {
            // Reallocate buffer if size changed (preserve existing data if possible)
            val newBuffer = FloatArray(newSize)
            val copyCount = minOf(historyCount, newSize)
            for (i in 0 until copyCount) {
                val srcIdx = (historyPos - historyCount + i + historySize) % historySize
                newBuffer[i] = fluxHistory[srcIdx]
            }
            fluxHistory = newBuffer
            historySize = newSize
            historyPos = copyCount % newSize
            historyCount = minOf(historyCount, newSize)
        }
    }

    fun setSensitivity(s: Sensitivity) {
        sensitivity = s
        if (s != Sensitivity.AUTO) autoThreshold = s.baseThreshold
    }

    fun process(magnitudes: FloatArray, bandEnergy: BandEnergy, timeMs: Long): Float {
        if (previousMagnitudes == null) {
            previousMagnitudes = magnitudes.copyOf()
            return 0f
        }

        // Enforce minimum onset interval
        if (timeMs - lastOnsetTime < MIN_ONSET_INTERVAL_MS) {
            previousMagnitudes = magnitudes.copyOf()
            return 0f
        }

        // Compute spectral flux for active bands only
        var flux = 0f
        if (Band.SUB in activeBands) flux += bandFlux(previousMagnitudes!!, magnitudes, 4, 14)
        if (Band.MID in activeBands) flux += bandFlux(previousMagnitudes!!, magnitudes, 14, 186)
        if (Band.HI in activeBands) flux += bandFlux(previousMagnitudes!!, magnitudes, 186, minOf(929, magnitudes.size))

        // Update history
        fluxHistory[historyPos % historySize] = flux
        historyPos++
        historyCount = minOf(historyCount + 1, historySize)

        if (historyCount < 4) {
            previousMagnitudes = magnitudes.copyOf()
            return 0f
        }

        // Compute mean and stddev
        var sum = 0f
        var sumSq = 0f
        val count = historyCount
        for (i in 0 until count) {
            val v = fluxHistory[i]
            sum += v
            sumSq += v * v
        }
        val mean = sum / count
        val variance = sumSq / count - mean * mean
        val stddev = if (variance > 0f) kotlin.math.sqrt(variance) else 0.001f

        val threshold = if (sensitivity == Sensitivity.AUTO) autoThreshold else sensitivity.baseThreshold
        val isOnset = flux > mean + threshold * stddev

        // Minimum inter-onset interval
        if (isOnset && timeMs - lastOnsetTime < MIN_ONSET_INTERVAL_MS) {
            return 0f
        }

        if (isOnset) {
            lastOnsetTime = timeMs

            // Auto-adapt threshold
            if (sensitivity == Sensitivity.AUTO) {
                recentOnsetCount++
                if (onsetWindowStart == 0L) onsetWindowStart = timeMs
                val elapsed = (timeMs - onsetWindowStart) / 1000f
                if (elapsed >= 1f) {
                    val rate = recentOnsetCount / elapsed
                    if (rate > AUTO_TARGET_MAX) autoThreshold += 0.1f
                    else if (rate < AUTO_TARGET_MIN) autoThreshold = maxOf(0.5f, autoThreshold - 0.1f)
                    recentOnsetCount = 0
                    onsetWindowStart = timeMs
                }
            }
        }

        return if (isOnset) flux else 0f
    }

    private fun bandFlux(prev: FloatArray, curr: FloatArray, from: Int, to: Int): Float {
        var flux = 0f
        val end = minOf(to, prev.size, curr.size)
        for (i in from until end) {
            val diff = curr[i] - prev[i]
            if (diff > 0f) flux += diff
        }
        return flux
    }

    fun reset() {
        previousMagnitudes = null
        fluxHistory.fill(0f)
        historyPos = 0
        historyCount = 0
        lastOnsetTime = 0L
        recentOnsetCount = 0
        onsetWindowStart = 0L
        autoThreshold = 1.5f
    }
}
