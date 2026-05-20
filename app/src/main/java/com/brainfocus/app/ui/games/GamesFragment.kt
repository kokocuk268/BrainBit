package com.brainfocus.app.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.brainfocus.app.R
import com.brainfocus.app.brainbit.ConnectionState
import com.brainfocus.app.databinding.FragmentGamesBinding
import com.brainfocus.app.ui.MainActivity
import com.brainfocus.app.ui.connection.ConnectionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GamesFragment : Fragment() {
    private var _binding: FragmentGamesBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels { com.brainfocus.app.ui.connection.ConnectionViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeConnectionState()
    }

    private fun setupClickListeners() {
        binding.playFocusButton.setOnClickListener {
            if (connectionViewModel.isConnected()) {
                (activity as? MainActivity)?.navigateToGame()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.connect_device_hint,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeConnectionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            connectionViewModel.connectionState.collectLatest { state ->
                binding.playFocusButton.isEnabled = state is ConnectionState.Connected
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
