package com.brainfocus.app.brainbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ConcentrationProcessorTest {
    @Test
    fun betaSignalProducesHigherConcentrationThanAlphaSignal() {
        val beta = concentrationForFrequency(18.0)
        val alpha = concentrationForFrequency(8.0)

        assertTrue("beta=$beta alpha=$alpha", beta > alpha + 0.5f)
        assertTrue(beta > 0.8f)
        assertTrue(alpha < 0.2f)
    }

    @Test
    fun resetReturnsProcessorToDefaultUntilWindowIsFilled() {
        val processor = ConcentrationProcessor()
        repeat(300) { index -> processor.processSamples(sample(18.0, index)) }

        processor.reset()

        assertEquals(0.5f, processor.processSamples(sample(18.0, 0)), 0.0001f)
    }

    private fun concentrationForFrequency(frequency: Double): Float {
        val processor = ConcentrationProcessor()
        var result = 0.5f
        repeat(500) { index ->
            result = processor.processSamples(sample(frequency, index))
        }
        return result
    }

    private fun sample(frequency: Double, index: Int): FloatArray {
        val value = sin(2.0 * PI * frequency * index / 250.0).toFloat()
        return floatArrayOf(value, value, value, value)
    }
}
