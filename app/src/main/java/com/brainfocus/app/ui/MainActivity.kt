package com.brainfocus.app.ui

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.brainfocus.app.R
import com.brainfocus.app.databinding.ActivityMainBinding
import com.brainfocus.app.ui.connection.ConnectionFragment
import com.brainfocus.app.ui.games.GamesFragment
import com.brainfocus.app.ui.sensortest.SensorTestFragment
import com.brainfocus.app.ui.theme.ThemeManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        var pendingScreenshot: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(ConnectionFragment())
        }

        val screenshot = pendingScreenshot
        if (screenshot != null) {
            pendingScreenshot = null
            binding.diagonalWipeView.visibility = View.VISIBLE
            binding.diagonalWipeView.setScreenshot(screenshot)
            binding.diagonalWipeView.setProgress(0f)

            binding.diagonalWipeView.post {
                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 550
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        binding.diagonalWipeView.setProgress(anim.animatedValue as Float)
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            binding.diagonalWipeView.visibility = View.GONE
                            binding.diagonalWipeView.setScreenshot(null)
                        }
                    })
                }
                animator.start()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_connection -> {
                    loadFragment(ConnectionFragment())
                    true
                }
                R.id.nav_sensor_test -> {
                    loadFragment(SensorTestFragment())
                    true
                }
                R.id.nav_games -> {
                    loadFragment(GamesFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateToGame() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, com.brainfocus.app.ui.game.GameFragment())
            .addToBackStack(null)
            .commit()
    }

    fun navigateToResults(score: Int, avgConcentration: Float) {
        val fragment = com.brainfocus.app.ui.results.ResultsFragment.newInstance(score, avgConcentration)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToConnection() {
        binding.bottomNavigation.selectedItemId = R.id.nav_connection
    }

    fun applyThemeReveal(@Suppress("UNUSED_PARAMETER") startX: Int, @Suppress("UNUSED_PARAMETER") startY: Int) {
        val container = binding.fragmentContainer
        if (container.width > 0 && container.height > 0) {
            try {
                val bitmap = Bitmap.createBitmap(container.width, container.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                container.draw(canvas)
                pendingScreenshot = bitmap
            } catch (e: Exception) {
                pendingScreenshot = null
            }
        }

        ThemeManager.toggleThemeSilent(this)
        recreate()
    }
}
