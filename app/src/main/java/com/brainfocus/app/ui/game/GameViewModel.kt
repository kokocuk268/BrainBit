package com.brainfocus.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameScore = MutableStateFlow(0)
    val gameScore: StateFlow<Int> = _gameScore.asStateFlow()

    private val _averageConcentration = MutableStateFlow(0f)
    val averageConcentration: StateFlow<Float> = _averageConcentration.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val concentrationHistory = mutableListOf<Float>()

    fun updateScore(score: Int) {
        _gameScore.value = score
    }

    fun addConcentrationSample(concentration: Float) {
        concentrationHistory.add(concentration)
        if (concentrationHistory.isNotEmpty()) {
            _averageConcentration.value = concentrationHistory.average().toFloat()
        }
    }

    fun setGameOver() {
        _isGameOver.value = true
    }

    fun reset() {
        _gameScore.value = 0
        _isGameOver.value = false
        concentrationHistory.clear()
        _averageConcentration.value = 0f
    }

    fun getFinalScore(): Int = _gameScore.value

    fun getFinalAverageConcentration(): Float = _averageConcentration.value
}
