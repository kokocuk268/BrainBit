package com.brainfocus.app.brainbit

import kotlin.math.sin
import kotlin.random.Random

class ConcentrationProcessor {
    private val concentrationHistory = mutableListOf<Float>()
    private val historySize = 20

    fun processSamples(): Float {
        val concentration = createSimulatedConcentration()
        
        concentrationHistory.add(concentration)
        if (concentrationHistory.size > historySize) {
            concentrationHistory.removeAt(0)
        }

        return concentrationHistory.average().toFloat()
    }

    private fun createSimulatedConcentration(): Float {
        val time = System.currentTimeMillis() % 10000
        val base = 0.5f + 0.3f * sin((time / 3000.0)).toFloat()
        val noise = Random.nextFloat() * 0.2f - 0.1f
        return (base + noise).coerceIn(0f, 1f)
    }

    fun reset() {
        concentrationHistory.clear()
    }
}
