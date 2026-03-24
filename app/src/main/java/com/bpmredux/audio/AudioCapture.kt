package com.bpmredux.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCapture {

    companion object {
        const val SAMPLE_RATE = 44100
        const val FFT_SIZE = 4096
        const val HOP_SIZE = 1024
    }

    @SuppressLint("MissingPermission")
    fun audioFlow(): Flow<ShortArray> = callbackFlow {
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            FFT_SIZE * 2
        )

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val ringBuffer = ShortArray(FFT_SIZE * 2)
        var writePos = 0
        var samplesRead = 0
        val readBuf = ShortArray(HOP_SIZE)

        recorder.startRecording()

        val job = launch(Dispatchers.IO) {
            while (isActive) {
                val read = recorder.read(readBuf, 0, HOP_SIZE)
                if (read > 0) {
                    for (i in 0 until read) {
                        ringBuffer[writePos % ringBuffer.size] = readBuf[i]
                        writePos++
                    }
                    samplesRead += read

                    if (samplesRead >= FFT_SIZE) {
                        val frame = ShortArray(FFT_SIZE)
                        val start = writePos - FFT_SIZE
                        for (i in 0 until FFT_SIZE) {
                            frame[i] = ringBuffer[(start + i + ringBuffer.size) % ringBuffer.size]
                        }
                        trySend(frame)
                    }
                }
            }
        }

        awaitClose {
            job.cancel()
            recorder.stop()
            recorder.release()
        }
    }
}
