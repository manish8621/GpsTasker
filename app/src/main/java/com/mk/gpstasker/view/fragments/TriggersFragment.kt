package com.mk.gpstasker.view.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.mk.gpstasker.App
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggersBinding
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.view.adapters.TriggerAdapter
import com.mk.gpstasker.viewmodel.TriggersDetailViewModelFactory
import com.mk.gpstasker.viewmodel.TriggersViewModel
import com.mk.gpstasker.viewmodel.TriggersViewModelFactory

class TriggersFragment : Fragment() {



    private lateinit var viewModel: TriggersViewModel
    private lateinit var binding: FragmentTriggersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =  FragmentTriggersBinding.inflate(inflater, container, false)
        binding.lifecycleOwner= viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = TriggersViewModelFactory((requireActivity().application as App).triggersRepository)
        viewModel = ViewModelProvider(this,factory)[TriggersViewModel::class.java]

        val adapter = TriggerAdapter()
        adapter.setClickListeners(object :TriggerAdapter.ClickListeners{
            override fun onStartClicked(trigger: Trigger) {
                gotoTriggerListenFragment(trigger)
            }

            override fun onDeleteClicked(trigger: Trigger) {
                viewModel.deleteTrigger(trigger)
                Toast.makeText(context, "trigger removed", Toast.LENGTH_SHORT).show()
            }
        })
        binding.triggersRecyclerView.adapter = adapter
        setObservers(adapter)
        setClickListeners()
    }

    private fun gotoTriggerListenFragment(trigger: Trigger) {
        findNavController().navigate(TriggersFragmentDirections.actionTriggersFragmentToTriggerListenFragment(trigger))
    }

    private fun setClickListeners() {
        binding.newTriggerBtn.setOnClickListener{
            findNavController().navigate(R.id.action_triggersFragment_to_mapsFragment)
        }
        binding.titleTv.setOnClickListener {
            findNavController().navigate(R.id.action_triggersFragment_to_homeFragment)
        }

    }

    private fun setObservers(adapter: TriggerAdapter) {
        viewModel.triggerList.observe(viewLifecycleOwner){
            it?.let{ adapter.submitList(it) }
        }
    }


}