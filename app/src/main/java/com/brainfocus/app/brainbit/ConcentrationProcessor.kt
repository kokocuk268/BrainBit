package com.brainfocus.app.brainbit

import kotlin.math.PI
import kotlin.math.cos

class ConcentrationProcessor(
    private val samplingRate: Int = 250,
    private val windowSize: Int = samplingRate,
    private val updateInterval: Int = samplingRate / 10
) {
    private val channels = Array(CHANNEL_COUNT) { FloatArray(windowSize) }
    private val concentrationHistory = ArrayDeque<Float>()

    private var writeIndex = 0
    private var sampleCount = 0
    private var samplesSinceUpdate = 0
    private var concentrationSum = 0f
    private var lastConcentration = DEFAULT_CONCENTRATION

    fun processSamples(samples: FloatArray): Float {
        if (samples.size < CHANNEL_COUNT || samples.any { !it.isFinite() }) {
            return lastConcentration
        }

        for (channel in 0 until CHANNEL_COUNT) {
            channels[channel][writeIndex] = samples[channel]
        }
        writeIndex = (writeIndex + 1) % windowSize
        if (sampleCount < windowSize) sampleCount++
        samplesSinceUpdate++

        if (sampleCount < windowSize || samplesSinceUpdate < updateInterval) {
            return lastConcentration
        }
        samplesSinceUpdate = 0

        val concentration = calculateBetaRatio()
        concentrationHistory.addLast(concentration)
        concentrationSum += concentration
        if (concentrationHistory.size > HISTORY_SIZE) {
            concentrationSum -= concentrationHistory.removeFirst()
        }

        lastConcentration = concentrationSum / concentrationHistory.size
        return lastConcentration
    }

    private fun calculateBetaRatio(): Float {
        var ratioSum = 0.0
        var validChannels = 0

        for (channel in channels) {
            val mean = channel.average()
            var betaPower = 0.0
            var totalPower = 0.0

            for (frequency in TOTAL_LOW_HZ..TOTAL_HIGH_HZ) {
                val power = spectralPower(channel, mean, frequency)
                totalPower += power
                if (frequency in BETA_LOW_HZ..BETA_HIGH_HZ) {
                    betaPower += power
                }
            }

            if (totalPower > 0.0 && totalPower.isFinite()) {
                ratioSum += (betaPower / totalPower).coerceIn(0.0, 1.0)
                validChannels++
            }
        }

        return if (validChannels == 0) {
            lastConcentration
        } else {
            (ratioSum / validChannels).toFloat()
        }
    }

    private fun spectralPower(channel: FloatArray, mean: Double, frequency: Int): Double {
        val omega = 2.0 * PI * frequency / samplingRate
        val coefficient = 2.0 * cos(omega)
        var previous = 0.0
        var previousPrevious = 0.0

        for (offset in 0 until windowSize) {
            val index = (writeIndex + offset) % windowSize
            val window = 0.54 - 0.46 * cos(2.0 * PI * offset / (windowSize - 1))
            val value = (channel[index] - mean) * window
            val current = value + coefficient * previous - previousPrevious
            previousPrevious = previous
            previous = current
        }

        return previousPrevious * previousPrevious + previous * previous -
            coefficient * previous * previousPrevious
    }

    fun reset() {
        channels.forEach { it.fill(0f) }
        concentrationHistory.clear()
        writeIndex = 0
        sampleCount = 0
        samplesSinceUpdate = 0
        concentrationSum = 0f
        lastConcentration = DEFAULT_CONCENTRATION
    }

    private companion object {
        const val CHANNEL_COUNT = 4
        const val TOTAL_LOW_HZ = 4
        const val TOTAL_HIGH_HZ = 30
        const val BETA_LOW_HZ = 13
        const val BETA_HIGH_HZ = 30
        const val HISTORY_SIZE = 5
        const val DEFAULT_CONCENTRATION = 0.5f
    }
}
