package com.mk.gpstasker.view.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.mk.gpstasker.App
import com.mk.gpstasker.BuildConfig
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggersBinding
import com.mk.gpstasker.databinding.PermissionRequestLayoutBinding
import com.mk.gpstasker.model.INTERNET_AVAILABLE
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.network.checkInternet
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.service.TriggerListenService
import com.mk.gpstasker.view.adapters.TriggerAdapter
import com.mk.gpstasker.viewmodel.TriggersViewModel
import com.mk.gpstasker.viewmodel.TriggersViewModelFactory
import java.security.acl.Permission
import java.util.jar.Manifest

class TriggersFragment : Fragment() {



    private lateinit var viewModel: TriggersViewModel
    private lateinit var binding: FragmentTriggersBinding
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
                        Toast.makeText(context, "Grant permissions", Toast.LENGTH_SHORT).show()
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


    @SuppressLint("InlinedApi")
    //api version was checked already in checked in LocationClient.checkBackgroundLocationPermission fun
    //check before continue
    private fun startTriggerListener(trigger: Trigger) {
        //check for trigger special action permission
        when(trigger.triggerAction)
        {
            Trigger.ACTION_MESSAGE-> {
                if (requireContext().checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    showPermissionAlert(android.Manifest.permission.SEND_SMS)
                    return
                }
            }
        }


        //check necessary permissions
            if(!LocationClient.checkLocationPermission(requireContext())) {
                showPermissionAlert(android.Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }


                if(LocationClient.checkLocationEnabled(requireContext())){
                    if(!LocationClient.checkBackgroundLocationPermission(requireContext())){
                        showPermissionAlert(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        return
                    }
                    gotoTriggerListenFragment(trigger,isAlreadyRunning = false)
                }
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


    /**
     * shows a alert dialog and explains the message that why app needs the permission
     * */
    private fun showPermissionAlert(permission:String) {

        //get message related for that permission
        var msg =""
        var title =""
        var drawableId = R.drawable.gps_ico
        when(permission){
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                msg = getString(R.string.background_location_permission_msg)
                drawableId = R.drawable.antena
                title = "Background location permission"
            }
            android.Manifest.permission.ACCESS_FINE_LOCATION -> {
                msg = getString(R.string.location_permission_msg)
                title = "Location permission"
                drawableId = R.drawable.ic_baseline_my_location
            }
            android.Manifest.permission.SEND_SMS -> {
                msg = getString(R.string.message_permission_msg)
                title = "Sms permission"
                drawableId = R.drawable.msg_with_bg
            }
            else-> throw IllegalArgumentException("invalid permission check the permission string value")
        }


        val permissionReqView = PermissionRequestLayoutBinding.inflate(layoutInflater)
        permissionReqView.bannerIv.setImageResource(drawableId)
        permissionReqView.messageTv.text = msg

        AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.gps_ico)
            .setTitle(title)
            .setView(permissionReqView.root)
            .setPositiveButton("Allow") { _, _ ->
                if(requireActivity().shouldShowRequestPermissionRationale(permission)){
                    requestPermissionLauncher.launch(permission)
                }
                else {
                    //if prompt wont show
                    viewModel.uiStates.isSentToSettings = true
                    goToAppInfo()
                }
            }
            .setNegativeButton("Ignore") { dialog, _ ->
                dialog.cancel()
                //app can work without this permission so allow the user to continue
                if (permission == android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    gotoTriggerListenFragment(viewModel.lastSelectedTrigger!!, false)
            }.show()
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
                    else
                        //check if service running
                         if (TriggerListenService.isRunning)requireContext().stopService(Intent(requireContext(), TriggerListenService::class.java))
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