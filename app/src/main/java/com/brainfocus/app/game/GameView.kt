package com.brainfocus.app.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.brainfocus.app.R
import com.brainfocus.app.game.models.Obstacle
import com.brainfocus.app.game.models.Player
import com.brainfocus.app.utils.SensorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private var gameThread: GameThread? = null
    private val sensorHelper = SensorHelper(context)
    private var sensorScope: CoroutineScope? = null

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _concentration = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    @Volatile
    private var currentTilt = 0f
    @Volatile
    private var lastObstacleSpawn = 0L
    private var obstacleSpawnInterval = 1500L
    companion object {
        private val TILT_SENSITIVITY_MULTIPLIER = 2.1f // Increased sensitivity
        private val BLOCK_CORNER_RADIUS = 10f // Rounded corners for blocks
    }

    private var gameStartTime = 0L
    private var pauseStartedAt = 0L

    private var player: Player? = null
    private val obstacles = Array<Obstacle?>(32) { null }
    private var obstaclesSize = 0
    private val obstaclePool = ObstaclePool(32)

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8C42")
        style = Paint.Style.FILL
    }

    private val obstaclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    @Volatile
    private var viewWidth = 0
    @Volatile
    private var viewHeight = 0

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewWidth = width
        viewHeight = height
        backgroundPaint.color = ContextCompat.getColor(context, R.color.background)

        player = Player(width / 2f - 50f, height - 200f, width = 100f, height = 100f)

        startSensorListening()
        startGame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
        sensorScope?.cancel()
        sensorScope = null
    }

    private fun startSensorListening() {
        sensorScope?.cancel()
        sensorScope = CoroutineScope(Dispatchers.Main + Job())
        sensorScope?.launch {
            sensorHelper.observeTilt().collect { tilt ->
                currentTilt = tilt
            }
        }
    }

    fun startGame() {
        if (!holder.surface.isValid || gameThread?.isAlive == true) return

        gameStartTime = System.currentTimeMillis()
        pauseStartedAt = 0L
        _score.value = 0
        obstaclesSize = 0
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
        if (_isPaused.value || _isGameOver.value) return
        pauseStartedAt = System.currentTimeMillis()
        _isPaused.value = true
    }

    fun resumeGame() {
        if (!_isPaused.value || _isGameOver.value) return
        if (pauseStartedAt > 0L) {
            gameStartTime += System.currentTimeMillis() - pauseStartedAt
            pauseStartedAt = 0L
        }
        _isPaused.value = false
    }

    fun setConcentration(value: Float) {
        _concentration.value = value.coerceIn(0f, 1f)
    }

    private inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        @Volatile
        var running = false
        private val frameIntervalNanos = 16_666_667L // ~60 FPS

        override fun run() {
            var lastTime = System.nanoTime()

            while (running) {
                val now = System.nanoTime()
                val deltaNanos = now - lastTime
                lastTime = now

                if (_isPaused.value || _isGameOver.value) {
                    val sleepNanos = frameIntervalNanos - deltaNanos
                    if (sleepNanos > 0) {
                        sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
                    }
                    lastTime = System.nanoTime()
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

                val frameNanos = System.nanoTime() - now
                val sleepNanos = frameIntervalNanos - frameNanos
                if (sleepNanos > 0) {
                    sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
                }
            }
        }

        private fun update() {
            // Update player position if player exists
            player?.let { p ->
                p.x = (p.x - currentTilt * p.speed * TILT_SENSITIVITY_MULTIPLIER)
                    .coerceIn(0f, viewWidth - p.width)
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastObstacleSpawn > obstacleSpawnInterval) {
                spawnObstacle()
                lastObstacleSpawn = currentTime
                obstacleSpawnInterval = (800..2000).random().toLong()
            }

            val concentration = _concentration.value
            val speedMultiplier = when {
                concentration >= 0.7f -> 0.5f
                concentration >= 0.3f -> 0.75f
                else -> 1.0f
            }

            var i = 0
            while (i < obstaclesSize) {
                val obstacle = obstacles[i]!!
                obstacle.y += obstacle.speed * speedMultiplier

                if (obstacle.y >= viewHeight + 100) {
                    obstaclePool.free(obstacle)
                    // Remove from array by shifting
                    for (j in i until obstaclesSize - 1) {
                        obstacles[j] = obstacles[j + 1]
                    }
                    obstacles[obstaclesSize - 1] = null
                    obstaclesSize--
                    continue // Do not increment i as we shifted elements
                }

                // Check collision only if player exists
                player?.let { p ->
                    if (checkCollision(p, obstacle)) {
                        _isGameOver.value = true
                        running = false
                        return
                    }
                }

                i++
            }

            if (currentTime - gameStartTime > 0) {
                _score.value = ((currentTime - gameStartTime) / 100).toInt()
            }
        }

        private fun spawnObstacle() {
            if (obstaclesSize >= obstacles.size) return
            val obstacle = obstaclePool.obtain()
            obstacle.x = Random.nextFloat() * (viewWidth - 60f)
            obstacle.y = -60f
            obstacle.speed = (6..12).random().toFloat()
            obstacles[obstaclesSize] = obstacle
            obstaclesSize++
        }

        private fun checkCollision(player: Player, obstacle: Obstacle): Boolean {
            return player.x + 10 < obstacle.x + obstacle.width - 5 &&
                    player.x + player.width - 10 > obstacle.x + 5 &&
                    player.y + 10 < obstacle.y + obstacle.height - 5 &&
                    player.y + player.height - 10 > obstacle.y + 5
        }

        private fun drawGame(canvas: Canvas) {
            canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), backgroundPaint)

            for (i in 0 until obstaclesSize) {
                val obstacle = obstacles[i]!!
                canvas.drawRoundRect(
                    obstacle.x, obstacle.y,
                    obstacle.x + obstacle.width, obstacle.y + obstacle.height,
                    BLOCK_CORNER_RADIUS, BLOCK_CORNER_RADIUS,
                    obstaclePaint
                )
            }

            player?.let { p ->
                canvas.drawRoundRect(
                    p.x, p.y,
                    p.x + p.width, p.y + p.height,
                    BLOCK_CORNER_RADIUS, BLOCK_CORNER_RADIUS,
                    playerPaint
                )
            }
        }
    }

    private class ObstaclePool(private val maxSize: Int) {
        private val pool = arrayOfNulls<Obstacle>(maxSize)
        private var size = 0

        fun obtain(): Obstacle {
            return if (size > 0) {
                pool[--size]!!
            } else {
                Obstacle(0f, 0f)
            }
        }

        fun free(obstacle: Obstacle) {
            if (size < maxSize) {
                pool[size++] = obstacle
            }
        }
    }
}
