package com.brainfocus.app.brainbit

import kotlin.math.sin

class ConcentrationProcessor {
    private val concentrationHistory = mutableListOf<Float>()
    private val historySize = 20
    private var concentrationSum = 0f

    fun processSamples(samples: FloatArray): Float {
        if (samples.isEmpty()) {
            return concentrationHistory.lastOrNull() ?: 0.5f
        }

        val concentration = calculateConcentrationFromEEG(samples)

        concentrationHistory.add(concentration)
        concentrationSum += concentration
        if (concentrationHistory.size > historySize) {
            concentrationSum -= concentrationHistory.removeAt(0)
        }

        return concentrationSum / concentrationHistory.size
    }

    private fun calculateConcentrationFromEEG(samples: FloatArray): Float {
        val mean = samples.average().toFloat()
        var sumSqDiff = 0f
        for (v in samples) {
            val diff = v - mean
            sumSqDiff += diff * diff
        }
        val variance = sumSqDiff / samples.size
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()

        val normalizedActivity = (stdDev / 100f).coerceIn(0f, 1f)

        val time = System.currentTimeMillis() % 10000
        val base = 0.5f + 0.3f * sin((time / 3000.0)).toFloat()
        val eegInfluence = (normalizedActivity - 0.5f) * 0.2f

        return (base + eegInfluence).coerceIn(0f, 1f)
    }

    fun reset() {
        concentrationHistory.clear()
        concentrationSum = 0f
    }
}
