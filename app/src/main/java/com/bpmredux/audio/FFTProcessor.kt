package com.bpmredux.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

class FFTProcessor(private val size: Int = AudioCapture.FFT_SIZE) {

    private val hannWindow = FloatArray(size) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
    }

    private val bitReversalTable = IntArray(size).also { table ->
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) {
            var reversed = 0
            var value = i
            for (b in 0 until bits) {
                reversed = (reversed shl 1) or (value and 1)
                value = value shr 1
            }
            table[i] = reversed
        }
    }

    fun applyWindow(samples: ShortArray): FloatArray {
        val result = FloatArray(size)
        val len = minOf(samples.size, size)
        for (i in 0 until len) {
            result[i] = samples[i].toFloat() / 32768f * hannWindow[i]
        }
        return result
    }

    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size

        // Bit-reversal permutation
        for (i in 0 until n) {
            val j = bitReversalTable[i]
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }

        // Cooley-Tukey radix-2 DIT
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wReal = cos(angle).toFloat()
            val wImag = kotlin.math.sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curReal = 1f
                var curImag = 0f
                for (j in 0 until halfLen) {
                    val u = i + j
                    val v = u + halfLen
                    val tReal = curReal * real[v] - curImag * imag[v]
                    val tImag = curReal * imag[v] + curImag * real[v]
                    real[v] = real[u] - tReal
                    imag[v] = imag[u] - tImag
                    real[u] = real[u] + tReal
                    imag[u] = imag[u] + tImag
                    val newReal = curReal * wReal - curImag * wImag
                    curImag = curReal * wImag + curImag * wReal
                    curReal = newReal
                }
                i += len
            }
            len *= 2
        }
    }

    fun magnitudeSpectrum(real: FloatArray, imag: FloatArray): FloatArray {
        val n = real.size / 2 + 1
        return FloatArray(n) { i -> sqrt(real[i] * real[i] + imag[i] * imag[i]) }
    }

    fun process(samples: ShortArray): FloatArray {
        val real = applyWindow(samples)
        val imag = FloatArray(size)
        fft(real, imag)
        return magnitudeSpectrum(real, imag)
    }
}
