package com.brainfocus.app.ui.connection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brainfocus.app.databinding.ItemDeviceBinding
import com.brainfocus.app.brainbit.BrainBitDevice

class DeviceAdapter(
    private val onDeviceClick: (BrainBitDevice) -> Unit
) : ListAdapter<BrainBitDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BrainBitDevice) {
            binding.deviceName.text = device.name
            binding.deviceAddress.text = device.address
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<BrainBitDevice>() {
        override fun areItemsTheSame(oldItem: BrainBitDevice, newItem: BrainBitDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BrainBitDevice, newItem: BrainBitDevice): Boolean {
            return oldItem == newItem
        }
    }
}
