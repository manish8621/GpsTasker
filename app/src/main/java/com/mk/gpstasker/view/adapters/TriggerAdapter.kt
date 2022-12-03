package com.mk.gpstasker.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.TriggerItemLayoutBinding
import com.mk.gpstasker.model.format
import com.mk.gpstasker.model.room.Trigger

class TriggerAdapter:ListAdapter<Trigger,TriggerAdapter.ItemViewHolder>(DiffutilItemCallback()) {
    private var clickListeners:ClickListeners? = null

    class ItemViewHolder(private val binding: TriggerItemLayoutBinding):RecyclerView.ViewHolder(binding.root) {
        companion object{
            fun from(parent: ViewGroup,clickListeners: ClickListeners?):ItemViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = TriggerItemLayoutBinding.inflate(layoutInflater,parent,false)

                //add click listeners
                binding.root.setOnClickListener{root->
                    binding.trigger?.let {

                        when(it.triggerAction){
                            Trigger.ACTION_MESSAGE -> "sends '${it.message}' to \n ${it.mobileNumber}"
                            Trigger.ACTION_SILENCE -> "puts mobile into silent mode"
                            Trigger.ACTION_ALERT -> "alerts when the location reached"
                            else -> "error unknown trigger"
                        }.also { text ->
                            Toast.makeText(root.context, text, Toast.LENGTH_SHORT).show()
                        }

                    }
                }

                binding.actionBtn.setOnClickListener{
                    binding.trigger?.let {
                        clickListeners?.onStartClicked(it)
                    }
                }
                binding.deleteBtn.setOnClickListener{
                    binding.trigger?.let {
                        clickListeners?.onDeleteClicked(it)
                    }
                }

                return ItemViewHolder(binding)
            }
        }
        fun bind(trigger:Trigger)
        {
            binding.trigger = trigger
            binding.latitudeTv.text = trigger.location.latitude.format(3)
            binding.longitudeTv.text = trigger.location.longitude.format(3)

            when(trigger.triggerAction){
                Trigger.ACTION_SILENCE -> binding.triggerIv.setImageResource(R.drawable.vibration_white)
                Trigger.ACTION_ALERT -> binding.triggerIv.setImageResource(R.drawable.alert_white)
                Trigger.ACTION_MESSAGE -> binding.triggerIv.setImageResource(R.drawable.sms)
                else -> binding.triggerIv.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent,clickListeners)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        return holder.bind(getItem(position))
    }
    interface ClickListeners{
        fun onStartClicked(trigger: Trigger)
        fun onDeleteClicked(trigger: Trigger)
    }

    fun setClickListeners(clickListeners: ClickListeners){
        this.clickListeners = clickListeners
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