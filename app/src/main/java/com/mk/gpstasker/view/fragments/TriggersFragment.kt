package com.mk.gpstasker.view.fragments

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.mk.gpstasker.App
import com.mk.gpstasker.BuildConfig
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggersBinding
import com.mk.gpstasker.model.INTERNET_AVAILABLE
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.network.checkInternet
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.view.adapters.TriggerAdapter
import com.mk.gpstasker.viewmodel.TriggersViewModel
import com.mk.gpstasker.viewmodel.TriggersViewModelFactory
import java.security.acl.Permission
import java.util.jar.Manifest

class TriggersFragment : Fragment() {



    private lateinit var viewModel: TriggersViewModel
    private lateinit var binding: FragmentTriggersBinding
    private lateinit var locationClient: LocationClient
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //permission handle
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted -> run {
            if (isGranted) {
                viewModel.lastSelectedTrigger?.let { startTriggerListener(it) }
            }
            else {
                Snackbar.make(binding.root,R.string.permission_denied_msg, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.grant){
                        viewModel.uiStates.isSentToSettings = true
                        Toast.makeText(context, "Grant location permission", Toast.LENGTH_SHORT).show()
                        goToAppInfo()
                    }
                    .show()
            }
        }
        }
    }

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


        //location
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationClient = LocationClient(requireContext(),fusedLocationProviderClient)

        //recycler view
        val adapter = TriggerAdapter()
        adapter.setClickListeners(object :TriggerAdapter.ClickListeners{
            override fun onStartClicked(trigger: Trigger) {
                viewModel.lastSelectedTrigger = trigger
                startTriggerListener(trigger)
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

    //check if user sent to settings and do the pending work
    override fun onStart() {
        super.onStart()
        if(viewModel.uiStates.isSentToSettings)
        {
            viewModel.uiStates.isSentToSettings = false
            viewModel.lastSelectedTrigger?.let { startTriggerListener(it) }
        }
    }


    //check before continue
    private fun startTriggerListener(trigger: Trigger) {
        if(requireContext().checkInternet()== INTERNET_AVAILABLE) {
            if(locationClient.checkLocationPermission()) {
                if(locationClient.checkLocationEnabled()){ gotoTriggerListenFragment(trigger,isAlreadyRunning = false) }
                else {
                    Snackbar.make(
                        binding.root,
                        R.string.turn_location_on_msg,
                        Snackbar.LENGTH_SHORT
                    )
                        .setAction(R.string.turn_on) {
                            viewModel.uiStates.isSentToSettings = true
                            Toast.makeText(context, "turn on location", Toast.LENGTH_SHORT).show()
                            goToLocationSettings()
                        }
                        .show()
                }
            }
            else
            {
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        else Toast.makeText(context, "No Internet", Toast.LENGTH_SHORT).show()
    }

    private fun gotoTriggerListenFragment(trigger: Trigger,isAlreadyRunning:Boolean) {
        if(isAlreadyRunning.not()) viewModel.updateTriggersAsRunning(trigger.id)
        viewModel.uiStates.isSentToTriggerListenFragment = true
        findNavController().navigate(TriggersFragmentDirections.actionTriggersFragmentToTriggerListenFragment(trigger))
    }

    private fun setClickListeners() {
        binding.newTriggerBtn.setOnClickListener{
            if(requireContext().checkInternet() == INTERNET_AVAILABLE)
                findNavController().navigate(R.id.action_triggersFragment_to_mapsFragment)
            else
                Toast.makeText(context, "Internet is needed for loading map\nconsider turning on internet and Try again !", Toast.LENGTH_SHORT).show()
        }
        binding.titleTv.setOnClickListener {
            findNavController().navigate(R.id.action_triggersFragment_to_homeFragment)
        }

    }

    //the trigger list will be ordered by onGoing (field) in db and sent here
    //so just checking if the first item is if onGoing we could easily say there is a trigger already running
    //so navigate to the listening fragment
    //there can only one ongoing triggerListener
    private fun setObservers(adapter: TriggerAdapter) {
        viewModel.triggerList.observe(viewLifecycleOwner){
            it?.let{
                if(it.isEmpty()) showStatus("No triggers added\nTry adding new triggers")
                else {
                    //checks if trigger is already in background
                    if(viewModel.uiStates.isSentToTriggerListenFragment.not() && it.first().onGoing)
                        gotoTriggerListenFragment(trigger = it.first(), isAlreadyRunning = true)
                    hideStatus()
                }
                adapter.submitList(it)
            }
        }
    }

    private fun hideStatus(){
        with(binding.statusTv) {
            visibility = View.GONE
        }
    }
    private fun showStatus(statusText: String) {

        with(binding.statusTv) {
            visibility = View.VISIBLE
            text = statusText
        }
    }


    override fun onStop() {
        super.onStop()

    }


    //TODO:add viewUtil
    private fun goToAppInfo() {
        val settingsIntent = Intent()
        settingsIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID,null)
        settingsIntent.data =uri
        settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(settingsIntent)
    }

    //to turn on location permissions user will be sent to settings
    private fun goToLocationSettings() {
        val settingsIntent = Intent()
        settingsIntent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(settingsIntent)
    }

}