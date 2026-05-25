package com.brainfocus.app.ui.sensortest

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.brainfocus.app.R
import com.brainfocus.app.brainbit.ConnectionState
import com.brainfocus.app.databinding.FragmentSensorTestBinding
import com.brainfocus.app.databinding.ItemElectrodeBinding
import com.brainfocus.app.ui.connection.ConnectionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SensorTestFragment : Fragment() {
    private var _binding: FragmentSensorTestBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels { com.brainfocus.app.ui.connection.ConnectionViewModelFactory() }

    private var o1Binding: ItemElectrodeBinding? = null
    private var o2Binding: ItemElectrodeBinding? = null
    private var t3Binding: ItemElectrodeBinding? = null
    private var t4Binding: ItemElectrodeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupElectrodeBindings()
        observeConnectionState()
        observeEEGData()
        observeResistanceData()
    }

    private fun setupElectrodeBindings() {
        o1Binding = ItemElectrodeBinding.bind(binding.electrodeO1.root)
        o2Binding = ItemElectrodeBinding.bind(binding.electrodeO2.root)
        t3Binding = ItemElectrodeBinding.bind(binding.electrodeT3.root)
        t4Binding = ItemElectrodeBinding.bind(binding.electrodeT4.root)

        o1Binding?.electrodeName?.text = "O1"
        o2Binding?.electrodeName?.text = "O2"
        t3Binding?.electrodeName?.text = "T3"
        t4Binding?.electrodeName?.text = "T4"
    }

    private fun observeConnectionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            connectionViewModel.connectionState.collectLatest { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        binding.statusText.text = "Тест активен — данные поступают"
                        binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                    }
                    is ConnectionState.Connecting -> {
                        binding.statusText.text = "Подключение..."
                        binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                    }
                    is ConnectionState.Disconnected -> {
                        binding.statusText.text = "Подключите устройство для начала теста"
                        binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
                        binding.eegGraphView.clear()
                        resetElectrodes()
                    }
                    is ConnectionState.Error -> {
                        binding.statusText.text = state.message
                        binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                    }
                }
            }
        }
    }

    private fun observeEEGData() {
        val manager = connectionViewModel.getManager() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            manager.rawEEGData.collectLatest { sample ->
                binding.eegGraphView.addSample(sample)
            }
        }
    }

    private fun observeResistanceData() {
        val manager = connectionViewModel.getManager() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            manager.resistanceData.collectLatest { sample ->
                sample ?: return@collectLatest

                o1Binding?.let { updateElectrode(it, sample.o1) }
                o2Binding?.let { updateElectrode(it, sample.o2) }
                t3Binding?.let { updateElectrode(it, sample.t3) }
                t4Binding?.let { updateElectrode(it, sample.t4) }
            }
        }
    }

    private fun updateElectrode(binding: ItemElectrodeBinding, resistance: Float) {
        val resistanceKOhm = resistance / 1000f

        binding.resistanceText.text = if (resistanceKOhm > 1000) {
            "∞ кОм"
        } else {
            String.format("%.1f кОм", resistanceKOhm)
        }

        val signalMicroV = 0.0f
        binding.signalText.text = String.format("%.0f мкВ", signalMicroV)

        val qualityColor = when {
            resistanceKOhm < 5 -> ContextCompat.getColor(requireContext(), R.color.success)
            resistanceKOhm < 25 -> ContextCompat.getColor(requireContext(), R.color.warning)
            else -> ContextCompat.getColor(requireContext(), R.color.error)
        }

        binding.qualityIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(qualityColor)
    }

    private fun resetElectrodes() {
        listOf(o1Binding, o2Binding, t3Binding, t4Binding).forEach { b ->
            b?.let {
                it.resistanceText.text = "∞ кОм"
                it.signalText.text = "0 мкВ"
                it.qualityIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
