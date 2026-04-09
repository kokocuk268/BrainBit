package com.brainfocus.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.brainfocus.app.R
import com.brainfocus.app.databinding.ActivityMainBinding
import com.brainfocus.app.ui.connection.ConnectionFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ConnectionFragment())
                .commit()
        }
    }

    fun navigateToGame(testMode: Boolean = false) {
        val fragment = com.brainfocus.app.ui.game.GameFragment.newInstance(testMode)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToResults(score: Int, avgConcentration: Float) {
        val fragment = com.brainfocus.app.ui.results.ResultsFragment.newInstance(score, avgConcentration)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToConnection() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ConnectionFragment())
            .commit()
    }
}
