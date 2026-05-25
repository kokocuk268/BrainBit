package com.brainfocus.app.ui.connection

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
import com.brainfocus.app.ui.theme.ThemeManager
import com.brainfocus.app.utils.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionFragment : Fragment() {
    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionViewModel by activityViewModels { ConnectionViewModelFactory() }
    private lateinit var deviceAdapter: DeviceAdapter
    private var pulseAnimator: ObjectAnimator? = null
    private var pulseAnimatorScaleY: ObjectAnimator? = null

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
        observeState()
        observeDeviceInfo()
        updateThemeIcon()
        viewModel.initialize(requireContext())
    }

    private fun updateThemeIcon() {
        val isDark = ThemeManager.isDarkTheme(requireContext())
        binding.themeToggleBtn.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
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

        binding.themeToggleBtn.setOnClickListener { view ->
            val isDark = ThemeManager.isDarkTheme(requireContext())
            binding.themeToggleBtn.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val x = location[0] + view.width / 2
            val y = location[1] + view.height / 2
            (activity as? MainActivity)?.applyThemeReveal(x, y)
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
            viewModel.batteryLevel.collectLatest { level ->
                updateBatteryUI(level)
            }
        }
    }

    private fun observeDeviceInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deviceInfo.collectLatest { info ->
                if (info != null) {
                    binding.deviceInfoText.isVisible = true
                    binding.deviceInfoText.text = info.address
                } else {
                    binding.deviceInfoText.isVisible = false
                }
            }
        }
    }

    private fun updateBatteryUI(level: Int?) {
        if (level != null) {
            binding.batteryIndicator.isVisible = true
            binding.batteryText.text = getString(R.string.battery_level, level)

            val iconRes = when {
                level >= 50 -> R.drawable.ic_battery_full
                level >= 20 -> R.drawable.ic_battery_mid
                else -> R.drawable.ic_battery_low
            }
            binding.batteryIcon.setImageResource(iconRes)
        } else {
            binding.batteryIndicator.isVisible = false
        }
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            is ConnectionState.Disconnected -> {
                binding.connectionStatusText.text = getString(R.string.disconnected)
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                binding.statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                stopPulseAnimation()
                binding.scanButton.isVisible = true
                binding.disconnectButton.isVisible = false
            }
            is ConnectionState.Connecting -> {
                binding.connectionStatusText.text = getString(R.string.connecting)
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.warning)
                )
                binding.statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.warning)
                )
                startPulseAnimation()
                binding.scanButton.isVisible = false
            }
            is ConnectionState.Connected -> {
                binding.connectionStatusText.text = getString(R.string.connected)
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
                binding.statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
                stopPulseAnimation()
                binding.scanButton.isVisible = false
                binding.disconnectButton.isVisible = true
            }
            is ConnectionState.Error -> {
                binding.connectionStatusText.text = state.message
                binding.connectionStatusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                binding.statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                stopPulseAnimation()
                binding.scanButton.isVisible = true
                binding.disconnectButton.isVisible = false
            }
        }
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimatorScaleY?.cancel()
        pulseAnimator = ObjectAnimator.ofFloat(binding.statusDot, "scaleX", 1f, 1.5f, 1f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        pulseAnimatorScaleY = ObjectAnimator.ofFloat(binding.statusDot, "scaleY", 1f, 1.5f, 1f).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseAnimatorScaleY?.cancel()
        pulseAnimatorScaleY = null
        binding.statusDot.scaleX = 1f
        binding.statusDot.scaleY = 1f
    }

    private fun updateScanUI(state: ScanState) {
        when (state) {
            is ScanState.Idle -> {
                binding.scanningTextContainer.isVisible = false
                binding.noDevicesText.isVisible = false
                binding.scanButton.isEnabled = true
            }
            is ScanState.Scanning -> {
                binding.scanningTextContainer.isVisible = true
                binding.noDevicesText.isVisible = false
                binding.deviceRecyclerView.isVisible = false
                binding.scanButton.isEnabled = false
            }
            is ScanState.DevicesFound -> {
                binding.scanningTextContainer.isVisible = false
                binding.deviceRecyclerView.isVisible = state.devices.isNotEmpty()
                binding.noDevicesText.isVisible = state.devices.isEmpty()
                deviceAdapter.submitList(state.devices)
                binding.scanButton.isEnabled = true
            }
            is ScanState.NoDevicesFound -> {
                binding.scanningTextContainer.isVisible = false
                binding.deviceRecyclerView.isVisible = false
                binding.noDevicesText.isVisible = true
                binding.noDevicesText.text = getString(R.string.no_brainbit_devices)
                binding.scanButton.isEnabled = true
            }
            is ScanState.Error -> {
                binding.scanningTextContainer.isVisible = false
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

    override fun onDestroyView() {
        super.onDestroyView()
        stopPulseAnimation()
        _binding = null
    }
}
