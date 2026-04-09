package com.brainfocus.app.ui.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brainfocus.app.R
import com.brainfocus.app.brainbit.ConnectionState
import com.brainfocus.app.brainbit.ScanState
import com.brainfocus.app.databinding.FragmentConnectionBinding
import com.brainfocus.app.ui.MainActivity
import com.brainfocus.app.utils.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionFragment : Fragment() {
    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionViewModel by activityViewModels { ConnectionViewModelFactory() }
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupTestModeSwitch()
        observeState()
        viewModel.initialize(requireContext())
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            onDeviceSelected(device)
        }
        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }

        binding.startGameButton.setOnClickListener {
            startGame()
        }
    }

    private fun setupTestModeSwitch() {
        binding.testModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.scanButton.isEnabled = false
            } else {
                binding.scanButton.isEnabled = true
            }
            viewModel.setTestMode(isChecked)
            updateUIForTestMode(isChecked)
        }
    }

    private fun updateUIForTestMode(isTestMode: Boolean) {
        if (isTestMode) {
            binding.connectionStatusHint.text = getString(R.string.test_mode_warning)
            binding.connectionStatusHint.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.concentration_medium)
            )
        } else {
            binding.connectionStatusHint.text = getString(R.string.connect_device_hint)
            binding.connectionStatusHint.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
        }
    }

    private fun checkPermissionsAndScan() {
        if (!PermissionHelper.hasBluetoothPermissions(requireContext())) {
            PermissionHelper.requestBluetoothPermissions(requireActivity())
            return
        }

        if (!PermissionHelper.isBluetoothEnabled(requireContext())) {
            Toast.makeText(requireContext(), R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.startScan(requireContext())
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collectLatest { state ->
                updateConnectionUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanState.collectLatest { state ->
                updateScanUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.canStartGame.collectLatest { canStart ->
                binding.startGameButton.isEnabled = canStart
            }
        }
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            is ConnectionState.Disconnected -> {
                binding.connectionStatusText.text = getString(R.string.disconnected)
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.concentration_low)
                )
                binding.scanButton.isVisible = true
                binding.disconnectButton.isVisible = false
            }
            is ConnectionState.Connecting -> {
                binding.connectionStatusText.text = getString(R.string.connecting)
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.concentration_medium)
                )
                binding.startGameButton.isEnabled = false
                binding.scanButton.isVisible = false
            }
            is ConnectionState.Connected -> {
                if (viewModel.isInSimulationMode()) {
                    binding.connectionStatusText.text = getString(R.string.simulation_active)
                    binding.disconnectButton.isVisible = false
                } else {
                    binding.connectionStatusText.text = getString(R.string.connected)
                    binding.disconnectButton.isVisible = true
                }
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.concentration_high)
                )
                binding.scanButton.isVisible = false
            }
            is ConnectionState.Error -> {
                binding.connectionStatusText.text = state.message
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.concentration_low)
                )
                binding.startGameButton.isEnabled = viewModel.isTestMode.value
                binding.scanButton.isVisible = true
                binding.disconnectButton.isVisible = false
            }
        }
    }

    private fun updateScanUI(state: ScanState) {
        when (state) {
            is ScanState.Idle -> {
                binding.scanningProgress.isVisible = false
                binding.noDevicesText.isVisible = false
                binding.scanButton.isEnabled = true
            }
            is ScanState.Scanning -> {
                binding.scanningProgress.isVisible = true
                binding.noDevicesText.isVisible = false
                binding.scanButton.isEnabled = false
            }
            is ScanState.DevicesFound -> {
                binding.scanningProgress.isVisible = false
                binding.deviceRecyclerView.isVisible = state.devices.isNotEmpty()
                binding.noDevicesText.isVisible = state.devices.isEmpty()
                deviceAdapter.submitList(state.devices)
                binding.scanButton.isEnabled = true
            }
            is ScanState.NoDevicesFound -> {
                binding.scanningProgress.isVisible = false
                binding.deviceRecyclerView.isVisible = false
                binding.noDevicesText.isVisible = true
                binding.noDevicesText.text = getString(R.string.no_brainbit_devices)
                binding.scanButton.isEnabled = true
            }
            is ScanState.Error -> {
                binding.scanningProgress.isVisible = false
                binding.noDevicesText.isVisible = true
                binding.noDevicesText.text = state.message
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                binding.scanButton.isEnabled = true
            }
        }
    }

    private fun onDeviceSelected(device: com.brainfocus.app.brainbit.BrainBitDevice) {
        viewModel.stopScan()
        viewModel.connect(requireContext(), device)
    }

    private fun startGame() {
        if (!viewModel.canStartGame.value) {
            Toast.makeText(
                requireContext(),
                R.string.connect_device_hint,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val testMode = viewModel.isTestMode.value
        (activity as? MainActivity)?.navigateToGame(testMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
