package com.mk.gpstasker.view.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggersBinding
import com.mk.gpstasker.view.adapters.TriggerAdapter
import com.mk.gpstasker.viewmodel.TriggersViewModel

class TriggersFragment : Fragment() {



    private lateinit var viewModel: TriggersViewModel
    private lateinit var binding: FragmentTriggersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =  FragmentTriggersBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(TriggersViewModel::class.java)
        binding.lifecycleOwner= viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TriggerAdapter()
        binding.triggersRecyclerView.adapter = adapter
        setObservers(adapter)
        setClickListeners()

    }

    private fun setClickListeners() {
        binding.newTriggerBtn.setOnClickListener{
            findNavController().navigate(R.id.action_triggersFragment_to_mapsFragment)
        }
    }

    private fun setObservers(adapter: TriggerAdapter) {
        viewModel.triggerList.observe(viewLifecycleOwner){
            it?.let{ adapter.submitList(it) }
        }
    }


}