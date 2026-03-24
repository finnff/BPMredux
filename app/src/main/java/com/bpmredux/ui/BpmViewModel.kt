package com.bpmredux.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpmredux.audio.AudioCapture
import com.bpmredux.audio.Band
import com.bpmredux.audio.BandEnergy
import com.bpmredux.audio.BandFilter
import com.bpmredux.audio.FFTProcessor
import com.bpmredux.audio.OnsetDetector
import com.bpmredux.audio.RangeLimitSide
import com.bpmredux.audio.TapProcessor
import com.bpmredux.audio.TempoEstimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class BpmUiState(
    val currentBpm: Float = 0f,
    val confidence: Float = 0f,
    val isDetecting: Boolean = false,
    val beatEvent: Long = 0L,
    val spectrogramColumn: FloatArray? = null,
    val bandEnergies: BandEnergy = BandEnergy(),
    val bpmRangeMin: Float = 120f,
    val bpmRangeMax: Float = 180f,
    val activeBands: Set<Band> = setOf(Band.SUB, Band.MID, Band.HI),
    val tapBpm: Float? = null,
    val tapCount: Int = 0,
    val isAtRangeLimit: Boolean = false,
    val limitSide: RangeLimitSide? = null,
    val amplitudeThreshold: Float = 0.1f,
    val hasPermission: Boolean = false,
    val isScreenDimmed: Boolean = false
)

class BpmViewModel(
    private val audioCapture: AudioCapture,
    private val onsetDetector: OnsetDetector,
    private val tempoEstimator: TempoEstimator,
    private val tapProcessor: TapProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(BpmUiState())
    val uiState: StateFlow<BpmUiState> = _uiState.asStateFlow()

    private val fftProcessor = FFTProcessor()
    private val bandFilter = BandFilter()

    private var detectionJob: Job? = null
    private val odfSampleInterval = AudioCapture.SAMPLE_RATE / 100
    private var samplesSinceOdf = 0

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted && !_uiState.value.isDetecting) {
            startDetection()
        }
    }

    fun toggleDetection() {
        if (_uiState.value.isDetecting) stopDetection() else startDetection()
    }

    private fun startDetection() {
        if (detectionJob != null) return
        _uiState.update { it.copy(isDetecting = true) }

        onsetDetector.reset()
        tempoEstimator.reset()
        tempoEstimator.bpmRangeMin = _uiState.value.bpmRangeMin
        tempoEstimator.bpmRangeMax = _uiState.value.bpmRangeMax

        detectionJob = viewModelScope.launch(Dispatchers.Default) {
            audioCapture.audioFlow().collect { frame ->
                processFrame(frame)
            }
        }
    }

    private fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null
        _uiState.update { it.copy(isDetecting = false) }
    }

    private fun processFrame(frame: ShortArray) {
        val magnitudes = fftProcessor.process(frame)
        val bandEnergy = bandFilter.filter(magnitudes)
        val timeMs = System.currentTimeMillis()

        // Compute frame RMS for amplitude threshold gating
        var sumSquares = 0.0
        for (sample in frame) {
            val normalized = sample.toFloat() / Short.MAX_VALUE
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / frame.size).toFloat()
        val aboveThreshold = rms >= _uiState.value.amplitudeThreshold

        onsetDetector.activeBands = _uiState.value.activeBands

        // Only run onset detection if above amplitude threshold
        val isOnset = if (aboveThreshold) {
            onsetDetector.process(magnitudes, bandEnergy, timeMs)
        } else {
            false
        }

        samplesSinceOdf += AudioCapture.HOP_SIZE
        while (samplesSinceOdf >= odfSampleInterval) {
            samplesSinceOdf -= odfSampleInterval

            // Only feed ODF if above threshold
            if (aboveThreshold) {
                val result = tempoEstimator.addOnsetSample(isOnset)

                if (result != null) {
                    val tapConfidence = tapProcessor.getConfidenceAt(timeMs)
                    val final = if (tapConfidence > 0f && _uiState.value.tapBpm != null) {
                        tempoEstimator.blendWithTap(_uiState.value.tapBpm!!, tapConfidence)
                    } else {
                        result
                    }

                    // Round to 1 decimal
                    val bpmRounded = (final.bpm * 10).toInt() / 10f

                    _uiState.update {
                        it.copy(
                            currentBpm = bpmRounded,
                            confidence = final.confidence,
                            isAtRangeLimit = final.isAtRangeLimit,
                            limitSide = final.limitSide
                        )
                    }
                }
            }
        }

        // Always emit spectrogram data regardless of threshold
        _uiState.update {
            it.copy(
                spectrogramColumn = magnitudes,
                bandEnergies = bandEnergy,
                beatEvent = if (isOnset) timeMs else it.beatEvent
            )
        }
    }

    fun onTap() {
        val timeMs = System.currentTimeMillis()
        val result = tapProcessor.tap(timeMs)
        _uiState.update {
            it.copy(
                tapBpm = if (result.bpm > 0f) result.bpm else it.tapBpm,
                tapCount = result.tapCount
            )
        }
    }

    fun onBpmRangeChange(min: Float, max: Float) {
        tempoEstimator.bpmRangeMin = min
        tempoEstimator.bpmRangeMax = max
        _uiState.update { it.copy(bpmRangeMin = min, bpmRangeMax = max) }
    }

    fun onBandToggle(band: Band) {
        _uiState.update {
            val current = it.activeBands.toMutableSet()
            if (band in current && current.size > 1) current.remove(band) else current.add(band)
            it.copy(activeBands = current)
        }
    }

    fun onAmplitudeThresholdChange(threshold: Float) {
        _uiState.update { it.copy(amplitudeThreshold = threshold) }
    }

    fun toggleScreenDim() {
        _uiState.update { it.copy(isScreenDimmed = !it.isScreenDimmed) }
    }

    override fun onCleared() {
        super.onCleared()
        stopDetection()
    }
}
