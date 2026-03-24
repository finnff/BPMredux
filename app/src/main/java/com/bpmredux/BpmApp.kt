package com.bpmredux

import android.app.Application
import com.bpmredux.audio.AudioCapture
import com.bpmredux.audio.OnsetDetector
import com.bpmredux.audio.TempoEstimator
import com.bpmredux.audio.TapProcessor

class BpmApp : Application() {

    val audioCapture: AudioCapture by lazy { AudioCapture() }
    val onsetDetector: OnsetDetector by lazy { OnsetDetector() }
    val tempoEstimator: TempoEstimator by lazy { TempoEstimator() }
    val tapProcessor: TapProcessor by lazy { TapProcessor() }
}
