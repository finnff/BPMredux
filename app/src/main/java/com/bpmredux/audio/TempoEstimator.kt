package com.bpmredux.audio

enum class RangeLimitSide { MIN, MAX }

class TempoEstimator {

    companion object {
        private const val ODF_SAMPLE_RATE = 100 // Hz — onset detection function sample rate
        private const val ODF_BUFFER_SIZE = 400 // 4 seconds at 100 Hz
        private const val UPDATE_INTERVAL_SAMPLES = 20 // ~0.2s
        private const val EMA_ALPHA = 0.3f
        // Extended range for pegging detection (40–250 BPM equivalent)
        private const val EXTENDED_BPM_MIN = 40f
        private const val EXTENDED_BPM_MAX = 250f
        private const val PEGGING_THRESHOLD = 0.3f // out-of-range peak must be >30% stronger
    }

    data class Result(
        val bpm: Float = 0f,
        val confidence: Float = 0f,
        val isAtRangeLimit: Boolean = false,
        val limitSide: RangeLimitSide? = null
    )

    private val odfBuffer = FloatArray(ODF_BUFFER_SIZE)
    private var odfWritePos = 0
    private var odfCount = 0
    private var samplesSinceUpdate = 0

    private var lastSmoothedBpm = 0f
    private var lastResult = Result()

    var bpmRangeMin = 120f
    var bpmRangeMax = 180f

    fun addOnsetSample(isOnset: Boolean): Result? {
        odfBuffer[odfWritePos % ODF_BUFFER_SIZE] = if (isOnset) 1f else 0f
        odfWritePos++
        odfCount = minOf(odfCount + 1, ODF_BUFFER_SIZE)
        samplesSinceUpdate++

        if (samplesSinceUpdate < UPDATE_INTERVAL_SAMPLES || odfCount < ODF_SAMPLE_RATE * 2) {
            return null
        }
        samplesSinceUpdate = 0

        // In-range lags
        val lagMin = (60f * ODF_SAMPLE_RATE / bpmRangeMax).toInt()
        val lagMax = (60f * ODF_SAMPLE_RATE / bpmRangeMin).toInt()

        // Extended lags for pegging detection
        val extLagMin = (60f * ODF_SAMPLE_RATE / EXTENDED_BPM_MAX).toInt()
        val extLagMax = (60f * ODF_SAMPLE_RATE / EXTENDED_BPM_MIN).toInt()

        val effectiveLagMax = maxOf(lagMax, extLagMax)
        if (lagMax <= lagMin || effectiveLagMax >= odfCount) return null

        // Autocorrelation over full extended range
        val acf = FloatArray(effectiveLagMax + 1)
        val bufLen = odfCount
        val start = if (odfWritePos > bufLen) odfWritePos - bufLen else 0

        for (lag in extLagMin..effectiveLagMax) {
            var sum = 0f
            val n = bufLen - lag
            for (i in 0 until n) {
                val idx1 = (start + i) % ODF_BUFFER_SIZE
                val idx2 = (start + i + lag) % ODF_BUFFER_SIZE
                sum += odfBuffer[idx1] * odfBuffer[idx2]
            }
            acf[lag] = sum
        }

        // Find best in-range peak
        var bestLag = lagMin
        var bestVal = acf[lagMin]
        var secondBest = 0f
        for (lag in lagMin + 1..lagMax) {
            if (acf[lag] > bestVal) {
                secondBest = bestVal
                bestVal = acf[lag]
                bestLag = lag
            } else if (acf[lag] > secondBest) {
                secondBest = acf[lag]
            }
        }

        if (bestVal <= 0f) return null

        // Sub-harmonic check: prefer 2x lag if within range and strong
        val doubleLag = bestLag * 2
        if (doubleLag in lagMin..lagMax) {
            val doubleVal = acf[doubleLag]
            if (doubleVal > bestVal * 0.8f) {
                bestLag = doubleLag
                bestVal = doubleVal
            }
        }

        // Half lag check
        val halfLag = bestLag / 2
        if (halfLag in lagMin..lagMax) {
            val halfVal = acf[halfLag]
            if (halfVal > bestVal * 0.9f) {
                bestLag = halfLag
                bestVal = halfVal
            }
        }

        val rawBpm = 60f * ODF_SAMPLE_RATE / bestLag
        val confidence = if (secondBest > 0f) {
            minOf(1f, (bestVal / secondBest - 1f) * 2f)
        } else 1f

        // Range-limit pegging detection
        var isAtRangeLimit = false
        var limitSide: RangeLimitSide? = null

        // Scan out-of-range for stronger peaks
        var bestOutOfRangeLag = -1
        var bestOutOfRangeVal = 0f
        for (lag in extLagMin..effectiveLagMax) {
            if (lag in lagMin..lagMax) continue // skip in-range
            if (acf[lag] > bestOutOfRangeVal) {
                bestOutOfRangeVal = acf[lag]
                bestOutOfRangeLag = lag
            }
        }

        if (bestOutOfRangeLag > 0 && bestOutOfRangeVal > bestVal * (1f + PEGGING_THRESHOLD)) {
            val outBpm = 60f * ODF_SAMPLE_RATE / bestOutOfRangeLag
            // Check if double/half of the out-of-range peak lands in-range
            val doubleBpm = outBpm * 2f
            val halfBpm = outBpm / 2f
            val doubleInRange = doubleBpm in bpmRangeMin..bpmRangeMax
            val halfInRange = halfBpm in bpmRangeMin..bpmRangeMax

            if (!doubleInRange && !halfInRange) {
                isAtRangeLimit = true
                limitSide = if (outBpm > bpmRangeMax) RangeLimitSide.MAX else RangeLimitSide.MIN
            }
        }

        // EMA smoothing
        val smoothedBpm = if (lastSmoothedBpm == 0f) {
            rawBpm
        } else {
            EMA_ALPHA * rawBpm + (1f - EMA_ALPHA) * lastSmoothedBpm
        }
        lastSmoothedBpm = smoothedBpm

        lastResult = Result(smoothedBpm, confidence, isAtRangeLimit, limitSide)
        return lastResult
    }

    fun blendWithTap(tapBpm: Float, tapConfidence: Float): Result {
        if (lastResult.bpm <= 0f) return Result(tapBpm, tapConfidence)
        if (tapBpm <= 0f) return lastResult

        // Weight taps more when algo confidence is low
        val algoWeight = lastResult.confidence
        val tapWeight = tapConfidence * (2f - lastResult.confidence)
        val totalWeight = algoWeight + tapWeight

        val blended = (lastResult.bpm * algoWeight + tapBpm * tapWeight) / totalWeight
        val blendedConfidence = maxOf(lastResult.confidence, tapConfidence)

        return Result(blended, blendedConfidence, lastResult.isAtRangeLimit, lastResult.limitSide)
    }

    fun reset() {
        odfWritePos = 0
        odfCount = 0
        samplesSinceUpdate = 0
        lastSmoothedBpm = 0f
        lastResult = Result()
    }
}
