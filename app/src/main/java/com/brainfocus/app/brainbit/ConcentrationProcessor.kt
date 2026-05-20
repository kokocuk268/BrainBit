package com.brainfocus.app.brainbit

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

class ConcentrationProcessor {
    private val concentrationHistory = mutableListOf<Float>()
    private val historySize = 20
    private var sampleCount = 0

    fun processSamples(samples: FloatArray): Float {
        if (samples.isEmpty()) {
            return concentrationHistory.lastOrNull() ?: 0.5f
        }

        val concentration = calculateConcentrationFromEEG(samples)

        concentrationHistory.add(concentration)
        if (concentrationHistory.size > historySize) {
            concentrationHistory.removeAt(0)
        }

        return concentrationHistory.average().toFloat()
    }

    private fun calculateConcentrationFromEEG(samples: FloatArray): Float {
        sampleCount += samples.size

        val mean = samples.average().toFloat()
        val variance = samples.map { (it - mean).pow(2) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()

        val normalizedActivity = (stdDev / 100f).coerceIn(0f, 1f)

        val time = System.currentTimeMillis() % 10000
        val base = 0.5f + 0.3f * sin((time / 3000.0)).toFloat()
        val eegInfluence = (normalizedActivity - 0.5f) * 0.2f

        val concentration = (base + eegInfluence).coerceIn(0f, 1f)

        return concentration
    }

    fun reset() {
        concentrationHistory.clear()
        sampleCount = 0
    }
}
