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
    var speed: Float = 8f
) {
    fun reset() {
        x = 0f
        y = 0f
        speed = 8f
    }
}

data class GameState(
    val score: Int = 0,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val concentration: Float = 0.5f
)
