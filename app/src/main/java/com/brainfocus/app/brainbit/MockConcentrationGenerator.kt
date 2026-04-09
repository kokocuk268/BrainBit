package com.brainfocus.app.brainbit

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sin
import kotlin.random.Random

object MockConcentrationGenerator {
    private const val MIN_VALUE = 0.2f
    private const val MAX_VALUE = 0.8f
    private const val CYCLE_DURATION_MS = 10000L
    private const val UPDATE_INTERVAL_MS = 100L
    private const val NOISE_AMPLITUDE = 0.1f

    private var lastValue = 0.5f
    private var cyclePosition = 0.0

    val flow: Flow<Float> = flow {
        emit(getInitialValue())
        
        while (true) {
            val normalizedPosition = (cyclePosition % CYCLE_DURATION_MS) / CYCLE_DURATION_MS
            
            val baseValue = MIN_VALUE + (MAX_VALUE - MIN_VALUE) * 
                (0.5f + 0.5f * sin(normalizedPosition * 2 * Math.PI - Math.PI / 2)).toFloat()
            
            val noise = (Random.nextFloat() * 2 - 1) * NOISE_AMPLITUDE
            val value = (baseValue + noise).coerceIn(0f, 1f)
            
            lastValue = value
            emit(value)
            delay(UPDATE_INTERVAL_MS)
            cyclePosition += UPDATE_INTERVAL_MS
        }
    }

    fun getInitialValue(): Float = lastValue

    fun reset() {
        lastValue = 0.5f
        cyclePosition = 0.0
    }
}
