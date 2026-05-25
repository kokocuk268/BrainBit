package com.brainfocus.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
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

    // Cached fragments to avoid recreation
    private var connectionFragment: ConnectionFragment? = null
    private var sensorFragment: SensorTestFragment? = null
    private var gamesFragment: GamesFragment? = null

    companion object {
        var pendingScreenshot: Bitmap? = null
        private const val TAG_CONNECTION = "connection"
        private const val TAG_SENSOR = "sensor"
        private const val TAG_GAMES = "games"
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
            navigateToTab(TAG_CONNECTION)
        }

        val screenshot = pendingScreenshot
        if (screenshot != null) {
            pendingScreenshot = null
            binding.themeTransitionOverlay.visibility = View.VISIBLE
            binding.themeTransitionOverlay.setImageBitmap(screenshot)
            binding.themeTransitionOverlay.alpha = 1f

            binding.themeTransitionOverlay.post {
                binding.themeTransitionOverlay.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction {
                        binding.themeTransitionOverlay.visibility = View.GONE
                        binding.themeTransitionOverlay.setImageBitmap(null)
                    }
                    .start()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_connection -> { navigateToTab(TAG_CONNECTION); true }
                R.id.nav_sensor_test -> { navigateToTab(TAG_SENSOR); true }
                R.id.nav_games -> { navigateToTab(TAG_GAMES); true }
                else -> false
            }
        }
    }

    private val cachedTags = setOf(TAG_CONNECTION, TAG_SENSOR, TAG_GAMES)

    private fun navigateToTab(tag: String) {
        val ft = supportFragmentManager.beginTransaction()
            // Removed custom animations to eliminate delay
            .setCustomAnimations(0, 0, 0, 0)

        // Remove any non-cached fragments (e.g., GameFragment, ResultsFragment)
        // to prevent them from lingering in the container
        val fragments = supportFragmentManager.fragments.toList()
        for (fragment in fragments) {
            val fragmentTag = fragment.tag
            if (fragmentTag !in cachedTags) {
                ft.remove(fragment)
            }
        }

        // Hide all cached tab fragments
        connectionFragment?.let { if (it.isAdded) ft.hide(it) }
        sensorFragment?.let { if (it.isAdded) ft.hide(it) }
        gamesFragment?.let { if (it.isAdded) ft.hide(it) }

        // Show the selected fragment
        when (tag) {
            TAG_CONNECTION -> {
                if (connectionFragment == null) {
                    connectionFragment = ConnectionFragment()
                    ft.add(R.id.fragmentContainer, connectionFragment!!, TAG_CONNECTION)
                } else {
                    if (!connectionFragment!!.isAdded) {
                        ft.add(R.id.fragmentContainer, connectionFragment!!, TAG_CONNECTION)
                    } else {
                        ft.show(connectionFragment!!)
                    }
                }
            }
            TAG_SENSOR -> {
                if (sensorFragment == null) {
                    sensorFragment = SensorTestFragment()
                    ft.add(R.id.fragmentContainer, sensorFragment!!, TAG_SENSOR)
                } else {
                    if (!sensorFragment!!.isAdded) {
                        ft.add(R.id.fragmentContainer, sensorFragment!!, TAG_SENSOR)
                    } else {
                        ft.show(sensorFragment!!)
                    }
                }
            }
            TAG_GAMES -> {
                if (gamesFragment == null) {
                    gamesFragment = GamesFragment()
                    ft.add(R.id.fragmentContainer, gamesFragment!!, TAG_GAMES)
                } else {
                    if (!gamesFragment!!.isAdded) {
                        ft.add(R.id.fragmentContainer, gamesFragment!!, TAG_GAMES)
                    } else {
                        ft.show(gamesFragment!!)
                    }
                }
            }
        }

        ft.commit()
    }

    fun navigateToGame() {
        supportFragmentManager.beginTransaction()
            // Removed custom animations to eliminate delay
            .setCustomAnimations(0, 0, 0, 0)
            .replace(R.id.fragmentContainer, com.brainfocus.app.ui.game.GameFragment())
            .addToBackStack(null)
            .commit()
    }

    fun navigateToResults(score: Int, avgConcentration: Float) {
        val fragment = com.brainfocus.app.ui.results.ResultsFragment.newInstance(score, avgConcentration)
        supportFragmentManager.beginTransaction()
            // Removed custom animations to eliminate delay
            .setCustomAnimations(0, 0, 0, 0)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToConnection() {
        navigateToTab(TAG_CONNECTION)
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
