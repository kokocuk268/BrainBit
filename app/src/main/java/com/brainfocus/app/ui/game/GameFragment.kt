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
import com.brainfocus.app.brainbit.MockConcentrationGenerator
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
    private var isTestMode: Boolean = false

    companion object {
        private const val ARG_TEST_MODE = "test_mode"

        fun newInstance(testMode: Boolean): GameFragment {
            return GameFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_TEST_MODE, testMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isTestMode = it.getBoolean(ARG_TEST_MODE, false)
        }
    }

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
            val concentrationFlow = if (isTestMode) {
                MockConcentrationGenerator.flow
            } else {
                connectionViewModel.concentration
            }

            concentrationFlow.collectLatest { concentration ->
                binding.gameView.setConcentration(concentration)
                viewModel.addConcentrationSample(concentration)
                updateConcentrationUI(concentration)
            }
        }
    }

    private fun updateConcentrationUI(concentration: Float) {
        val percentage = (concentration * 100).toInt()
        binding.concentrationBar.progress = percentage
        binding.concentrationValue.text = "$percentage%"

        val color = when {
            concentration >= 0.7f -> R.color.concentration_high
            concentration >= 0.3f -> R.color.concentration_medium
            else -> R.color.concentration_low
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
