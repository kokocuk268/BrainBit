package com.brainfocus.app.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.brainfocus.app.databinding.FragmentResultsBinding
import com.brainfocus.app.ui.MainActivity

class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private var finalScore: Int = 0
    private var averageConcentration: Float = 0f

    companion object {
        private const val ARG_SCORE = "score"
        private const val ARG_AVG_CONCENTRATION = "avg_concentration"

        fun newInstance(score: Int, avgConcentration: Float): ResultsFragment {
            return ResultsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SCORE, score)
                    putFloat(ARG_AVG_CONCENTRATION, avgConcentration)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            finalScore = it.getInt(ARG_SCORE, 0)
            averageConcentration = it.getFloat(ARG_AVG_CONCENTRATION, 0f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayResults()
        setupClickListeners()
    }

    private fun displayResults() {
        binding.finalScoreText.text = finalScore.toString()
        binding.avgConcentrationText.text = String.format("%.1f%%", averageConcentration * 100)
    }

    private fun setupClickListeners() {
        binding.playAgainButton.setOnClickListener {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.navigateToGame()
            }
        }

        binding.mainMenuButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToConnection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
