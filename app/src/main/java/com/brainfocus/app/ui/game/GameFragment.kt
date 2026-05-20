package com.brainfocus.app.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.brainfocus.app.R
import com.brainfocus.app.databinding.FragmentGameBinding
import com.brainfocus.app.ui.MainActivity
import com.brainfocus.app.ui.connection.ConnectionViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GameFragment : Fragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    private var concentrationUpdateJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGameView()
        setupClickListeners()
        observeGameState()
        observeConcentration()
        observeBattery()
    }

    private fun setupGameView() {
        binding.gameView.startGame()
    }

    private fun setupClickListeners() {
        binding.pauseButton.setOnClickListener {
            if (binding.gameView.isPaused.value) {
                binding.gameView.resumeGame()
                binding.pauseButton.text = getString(R.string.pause)
            } else {
                binding.gameView.pauseGame()
                binding.pauseButton.text = getString(R.string.resume)
            }
        }
    }

    private fun observeGameState() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.gameView.score.collectLatest { score ->
                viewModel.updateScore(score)
                binding.scoreText.text = getString(R.string.score, score)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.gameView.isGameOver.collectLatest { isGameOver ->
                if (isGameOver) {
                    viewModel.setGameOver()
                    navigateToResults()
                }
            }
        }
    }

    private fun observeConcentration() {
        concentrationUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            connectionViewModel.concentration.collectLatest { concentration ->
                binding.gameView.setConcentration(concentration)
                viewModel.addConcentrationSample(concentration)
                updateConcentrationUI(concentration)
            }
        }
    }

    private fun observeBattery() {
        viewLifecycleOwner.lifecycleScope.launch {
            connectionViewModel.batteryLevel.collectLatest { level ->
                updateBatteryUI(level)
            }
        }
    }

    private fun updateBatteryUI(level: Int?) {
        if (level != null) {
            binding.batteryIndicator.visibility = View.VISIBLE
            binding.batteryText.text = getString(R.string.battery_level, level)

            val colorRes = when {
                level >= 50 -> R.color.success
                level >= 20 -> R.color.warning
                else -> R.color.error
            }
            binding.batteryIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), colorRes)
            )
        } else {
            binding.batteryIndicator.visibility = View.GONE
        }
    }

    private fun updateConcentrationUI(concentration: Float) {
        val percentage = (concentration * 100).toInt()
        binding.concentrationBar.progress = percentage
        binding.concentrationValue.text = "$percentage%"

        val color = when {
            concentration >= 0.7f -> R.color.success
            concentration >= 0.3f -> R.color.warning
            else -> R.color.error
        }

        val colorInt = ContextCompat.getColor(requireContext(), color)
        binding.concentrationBar.progressTintList = android.content.res.ColorStateList.valueOf(colorInt)
        binding.concentrationValue.setTextColor(colorInt)
    }

    private fun navigateToResults() {
        binding.gameView.stopGame()
        concentrationUpdateJob?.cancel()

        (activity as? MainActivity)?.navigateToResults(
            viewModel.getFinalScore(),
            viewModel.getFinalAverageConcentration()
        )
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pauseGame()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        concentrationUpdateJob?.cancel()
        binding.gameView.stopGame()
        _binding = null
    }
}
