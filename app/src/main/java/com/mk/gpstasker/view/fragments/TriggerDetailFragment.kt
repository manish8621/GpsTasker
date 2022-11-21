package com.mk.gpstasker.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentMapsBinding
import com.mk.gpstasker.databinding.FragmentTriggerDetailBinding


class TriggerDetailFragment : Fragment() {
    lateinit var binding: FragmentTriggerDetailBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTriggerDetailBinding.inflate(inflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.alertIv.setOnClickListener{
            selectOption(it as ImageView , binding.alertTv)
            unSelectOption(binding.silentIv, binding.silentTv)
        }
        binding.silentIv.setOnClickListener{
            selectOption(it as ImageView , binding.silentTv)
            unSelectOption(binding.alertIv, binding.alertTv)
        }
    }

    private fun unSelectOption(imageView: ImageView, textView: TextView) {
        imageView.tag =""
        imageView.background = null
        textView.setTextColor(requireContext().getColor(R.color.black))
    }

    private fun selectOption(imageView: ImageView, textView: TextView) {
        imageView.tag ="selected"
        imageView.background = AppCompatResources.getDrawable(requireContext(),R.drawable.highlight_bg_a)
        textView.setTextColor(requireContext().getColor(R.color.highlight_color))
    }


}