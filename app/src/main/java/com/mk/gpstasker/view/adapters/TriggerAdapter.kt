package com.mk.gpstasker.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mk.gpstasker.databinding.TriggerItemLayoutBinding
import com.mk.gpstasker.model.room.Trigger

class TriggerAdapter:ListAdapter<Trigger,TriggerAdapter.ItemViewHolder>(DiffutilItemCallback()) {
    class ItemViewHolder(private val binding: TriggerItemLayoutBinding):RecyclerView.ViewHolder(binding.root) {
        companion object{
            fun from(parent: ViewGroup):ItemViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = TriggerItemLayoutBinding.inflate(layoutInflater,parent,false)
                return ItemViewHolder(binding)
            }
        }
        fun bind(trigger:Trigger)
        {
            binding.locationLabelTv.text = trigger.location.name
            binding.locationCoOrdTv.text = "lat:"+trigger.location.latitude +"\nlon:"+ trigger.location.longitude
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        return holder.bind(getItem(position))
    }
}
class DiffutilItemCallback: DiffUtil.ItemCallback<com.mk.gpstasker.model.room.Trigger>() {
    override fun areItemsTheSame(oldItem: Trigger, newItem: Trigger): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Trigger, newItem: Trigger): Boolean {
        return oldItem == newItem
    }

}