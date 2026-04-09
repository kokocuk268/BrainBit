package com.brainfocus.app.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.brainfocus.app.game.models.Obstacle
import com.brainfocus.app.game.models.Player
import com.brainfocus.app.utils.SensorHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private var gameThread: GameThread? = null
    private val sensorHelper = SensorHelper(context)

    private val _player = MutableStateFlow(Player(0f, 0f))
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
    val obstacles: StateFlow<List<Obstacle>> = _obstacles.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _concentration = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    private var currentTilt = 0f
    private var lastObstacleSpawn = 0L
    private var obstacleSpawnInterval = 1500L
    private var gameStartTime = 0L

    private val playerPaint = Paint().apply {
        color = Color.parseColor("#03DAC6")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val obstaclePaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#121212")
        style = Paint.Style.FILL
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var playerY = 0f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewWidth = width
        viewHeight = height
        playerY = height - 200f

        _player.value = Player(
            x = width / 2f - 50f,
            y = playerY,
            width = 100f,
            height = 100f
        )

        startSensorListening()
        startGame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        playerY = height - 200f
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
    }

    private fun startSensorListening() {
        CoroutineScope(Dispatchers.Main).launch {
            sensorHelper.observeTilt().collect { tilt ->
                currentTilt = tilt
            }
        }
    }

    fun startGame() {
        gameStartTime = System.currentTimeMillis()
        _score.value = 0
        _obstacles.value = emptyList()
        _isGameOver.value = false
        _isPaused.value = false
        lastObstacleSpawn = System.currentTimeMillis()

        gameThread = GameThread(holder)
        gameThread?.running = true
        gameThread?.start()
    }

    fun stopGame() {
        gameThread?.running = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        gameThread = null
    }

    fun pauseGame() {
        _isPaused.value = true
    }

    fun resumeGame() {
        _isPaused.value = false
    }

    fun setConcentration(value: Float) {
        _concentration.value = value.coerceIn(0f, 1f)
    }

    private inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        var running = false

        override fun run() {
            while (running) {
                if (_isPaused.value || _isGameOver.value) {
                    sleep(16)
                    continue
                }

                val canvas: Canvas? = try {
                    surfaceHolder.lockCanvas()
                } catch (e: Exception) {
                    null
                }

                canvas?.let {
                    synchronized(surfaceHolder) {
                        update()
                        drawGame(it)
                    }
                    try {
                        surfaceHolder.unlockCanvasAndPost(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                sleep(16)
            }
        }

        private fun update() {
            val player = _player.value
            val newX = (player.x - currentTilt * player.speed).coerceIn(0f, viewWidth - player.width)
            _player.value = player.copy(x = newX)

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastObstacleSpawn > obstacleSpawnInterval) {
                spawnObstacle()
                lastObstacleSpawn = currentTime
                obstacleSpawnInterval = (800..2000).random().toLong()
            }

            val concentration = _concentration.value
            val updatedObstacles = _obstacles.value
                .map { obstacle ->
                    val obstacleSpeedMultiplier = when {
                        concentration >= 0.7f -> 0.5f
                        concentration >= 0.3f -> 0.75f
                        else -> 1.0f
                    }
                    obstacle.copy(y = obstacle.y + obstacle.speed * obstacleSpeedMultiplier)
                }
                .filter { it.y < viewHeight + 100 }
                .also { remaining ->
                    for (obstacle in remaining) {
                        if (checkCollision(_player.value, obstacle)) {
                            _isGameOver.value = true
                            running = false
                            return
                        }
                    }
                }

            _obstacles.value = updatedObstacles

            if (currentTime - gameStartTime > 0) {
                _score.value = ((currentTime - gameStartTime) / 100).toInt()
            }
        }

        private fun spawnObstacle() {
            val obstacle = Obstacle(
                x = Random.nextFloat() * (viewWidth - 60f),
                y = -60f,
                speed = (6..12).random().toFloat()
            )
            _obstacles.value = _obstacles.value + obstacle
        }

        private fun checkCollision(player: Player, obstacle: Obstacle): Boolean {
            val playerRect = RectF(
                player.x + 10,
                player.y + 10,
                player.x + player.width - 10,
                player.y + player.height - 10
            )
            val obstacleRect = RectF(
                obstacle.x + 5,
                obstacle.y + 5,
                obstacle.x + obstacle.width - 5,
                obstacle.y + obstacle.height - 5
            )
            return RectF.intersects(playerRect, obstacleRect)
        }

        private fun drawGame(canvas: Canvas) {
            canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), backgroundPaint)

            for (obstacle in _obstacles.value) {
                canvas.drawRoundRect(
                    obstacle.x,
                    obstacle.y,
                    obstacle.x + obstacle.width,
                    obstacle.y + obstacle.height,
                    12f, 12f,
                    obstaclePaint
                )
            }

            val player = _player.value
            canvas.drawRoundRect(
                player.x,
                player.y,
                player.x + player.width,
                player.y + player.height,
                16f, 16f,
                playerPaint
            )
        }
    }
}
