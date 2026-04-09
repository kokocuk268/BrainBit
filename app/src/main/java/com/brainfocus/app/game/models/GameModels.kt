package com.brainfocus.app.game.models

data class Player(
    var x: Float,
    var y: Float,
    val width: Float = 100f,
    val height: Float = 100f,
    val speed: Float = 15f
)

data class Obstacle(
    var x: Float,
    var y: Float,
    val width: Float = 60f,
    val height: Float = 60f,
    var speed: Float = 8f,
    var speedFactor: Float = 1f
) {
    fun updateSpeed(concentration: Float) {
        speedFactor = when {
            concentration >= 0.7f -> 0.5f
            concentration >= 0.3f -> 0.75f
            else -> 1.0f
        }
    }
}

data class GameState(
    val score: Int = 0,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val concentration: Float = 0.5f
)
