package com.mk.gpstasker.view.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.DiscretePathEffect
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.mk.gpstasker.App
import com.mk.gpstasker.BuildConfig
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggerListenBinding
import com.mk.gpstasker.model.*
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.network.NetworkUtil
import com.mk.gpstasker.model.network.checkInternet
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.service.TriggerListenService
import com.mk.gpstasker.viewmodel.TriggerListenViewModel
import com.mk.gpstasker.viewmodel.TriggerListenViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ln


//cleaned
//TODO:add more actions
class TriggerListenFragment : Fragment() {

    private val overlaySize: Float
        get() = (viewModel.trigger.location.radius) * 2500F

    private lateinit var mMap: GoogleMap
    private lateinit var binding:FragmentTriggerListenBinding
    private lateinit var viewModel:TriggerListenViewModel
    private val args : TriggerListenFragmentArgs by navArgs()


    private lateinit var locationClient: LocationClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var networkUtil: NetworkUtil
    private lateinit var ringtone:Ringtone


    //get current location when map is loaded
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap.also { gmap->
            //show long press on map to goto current location msg
            gmap.setOnMapClickListener {
                Toast.makeText(context, "long press on map to \ngo to current location", Toast.LENGTH_SHORT).show()
            }
            //when long pressed on map goto current location
            gmap.setOnMapLongClickListener {
                gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(viewModel.getCurrentLatLng(),18F))
            }
        }
        startTriggerListening()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //permission handle
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted -> run {
            if (isGranted)  fetchLocation()
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

        requireActivity().onBackPressedDispatcher.addCallback(this){
            if(viewModel.uiStates.taskCompleted.not()) showAlertDialog("Stop the trigger")
            else goBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTriggerListenBinding.inflate(inflater,container,false)

        val repository = (requireActivity().application as App).triggersRepository
        val factory = TriggerListenViewModelFactory(args.trigger,repository)
        viewModel = ViewModelProvider(this,factory)[TriggerListenViewModel::class.java]

        //init net mon
        val connectivityManager=requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkUtil = NetworkUtil(connectivityManager)




        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        setUpObservers()
        setonClickListeners()
        showTriggerAction()

    }

    private fun showTriggerAction() {
        var triggerDetail = args.trigger.triggerAction
        binding.triggerActionIv.setImageResource(
            when(args.trigger.triggerAction){
                Trigger.ACTION_ALERT -> {
                    triggerDetail = "alerts when location is reached"
                    R.drawable.alert
                }
                Trigger.ACTION_SILENCE -> {
                    triggerDetail = "turns silent mode on \nwhen location is reached"
                    R.drawable.ic_baseline_vibration_24
                }
                Trigger.ACTION_MESSAGE -> {
                    triggerDetail = "sends ${args.trigger.message} to ${args.trigger.mobileNumber} \n when location is reached"
                    R.drawable.sms
                }
                else -> androidx.core.R.drawable.notification_bg_normal
            }
        )
        binding.triggerActionIv.setOnClickListener{
            Toast.makeText(context, triggerDetail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerLocationReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver( localBroadcastReceiver, IntentFilter(
            GPS_BROADCAST_INTENT_FILTER))

    }
    private fun unRegisterLocationReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(localBroadcastReceiver)
    }

    override fun onStart() {
        super.onStart()

        //start net monitor
        networkUtil.registerNetworkCallback()
        registerLocationReceiver()
        //TODO : replace with function for ui states
        if(viewModel.uiStates.isSentToSettings) {
            viewModel.uiStates.isSentToSettings = false
            fetchLocation()
        }
    }


    private fun setonClickListeners() {
        binding.upBtn.setOnClickListener{
            if(viewModel.uiStates.taskCompleted.not()) showAlertDialog("Stop the trigger")
            else goBack()
        }
        binding.noInternetCl.setOnClickListener{
            if(requireContext().checkInternet().not())
                Toast.makeText(context, "no internet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpObservers() {
        //observe current location to update ui
        viewModel.currentLocation.observe(viewLifecycleOwner){ location->
            location?.let{
                markCurrentAndTargetLocations(currentLatLng = it)
                if(!viewModel.uiStates.taskCompleted && viewModel.distance.isFinite()) showDistance()
            }
        }

        networkUtil.internetAvailable.observe(viewLifecycleOwner){
            if(viewModel.uiStates.taskCompleted.not()){
                if (it == INTERNET_AVAILABLE)
                    hideNoInternetLayout()
                else
                    showNoInternetLayout()
            }
        }
        binding.taskCompletedTv.setOnClickListener{
            goBack()
        }
    }

    private fun showNoInternetLayout() {
        binding.noInternetCl.visibility = View.VISIBLE
    }

    private fun hideNoInternetLayout() {
        binding.noInternetCl.visibility = View.GONE

    }


    //starts when map is ready
    //fetches location which will observed and other actions will be invoked when it change
    private fun startTriggerListening() {
        //start fetching location
        fetchLocation()
        //drop pin on target location
        markTargetLocation(moveCamera = true)
    }

    //mark current location + target location
    private fun markCurrentAndTargetLocations(currentLatLng: LatLng) {
        //clears markers by default
        markTargetLocation(moveCamera = false)
        dropPinOnMap(currentLatLng, clearMarkers = false, moveCamera = false, addGroundOverlay = false, CURRENT_LOCATION)
    }

    private fun markTargetLocation(moveCamera: Boolean){
        dropPinOnMap(args.trigger.location.toLatLng(), clearMarkers = true, moveCamera = moveCamera, addGroundOverlay = true, TARGET_LOCATION)
    }

    //mark location on map
    private fun dropPinOnMap(latLng: LatLng,clearMarkers:Boolean, moveCamera: Boolean,addGroundOverlay:Boolean,locationType:Int) {

        val snippet = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )

        val markerTitle = if(locationType== CURRENT_LOCATION) "currnt location" else "Target"


        if (clearMarkers)
            mMap.clear()
        //place marker
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(markerTitle)
                .snippet(snippet)
                .also { if(locationType == CURRENT_LOCATION) it.icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location_blue)) }
        )

        if(moveCamera)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        //show location trigger radius
        if(addGroundOverlay) {
            val groundOverlay =
                GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.circle_black_512))
                    .position(LatLng(latLng.latitude, latLng.longitude), overlaySize)
            mMap.addGroundOverlay(groundOverlay)
        }
    }

    //current location fetch
    //needed permissions will be asked
    private fun fetchLocation() {
        if(locationUsable()){
            getLocationUpdates()
        }
    }

    //user will asked to grant or turn on location related things
    //if they no snack bar will be shown they can use that to go settings
    private fun locationUsable(): Boolean {

            if (LocationClient.checkLocationPermission(requireContext())) {
                if (LocationClient.checkLocationEnabled(requireContext())) {
                    return true
                } else {
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
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        return false
    }




    //location update starts here
    private fun getLocationUpdates() {
        if(TriggerListenService.isRunning.not())
            startTriggerListenService()
        startGPSService()
    }


    //after location reached do stop all the operations
    private fun onTaskCompleted() {
        viewModel.uiStates.taskCompleted = true
        stopGPSService()
        binding.taskCompletedTv.visibility = View.VISIBLE
    }
    //stops all operations
    private fun stopTriggerListening() {
        stopTriggerListenService()
    }





    //shows distance b/w current location and the target location
    private fun showDistance() {
        //print distance on screen
        viewModel.currentLocation.value?.let {
            (viewModel.distance.format(3)+" km").also { dist->
                binding.distanceValueTv.text = dist
            }
        }
    }

    //replace with Dialog fragment
    //alert dialog when user presses back button
    private fun showAlertDialog(text:String) {
        AlertDialog.Builder(
            requireContext()
        ).setTitle(text)
            .setMessage("Are you sure ?")
            .setPositiveButton("Yes"
            ) { _, _ ->
                goBack()
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.cancel()
            }.show()
    }






    //to enable location permissions user will be sent to settings
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
    //navigate up
    private fun goBack() {
        //TODO:REMOVE
        if(viewModel.uiStates.taskCompleted.not()) viewModel.updateTriggersAsNotRunning()
        stopTriggerListenService()
        unRegisterLocationReceiver()
        Toast.makeText(context, "Trigger stopped", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }





    private fun startTriggerListenService(){
        val intent = Intent(requireContext(),TriggerListenService::class.java)
        intent.action = TriggerListenService.START_SERVICE
        intent.putExtra(TRIGGER_SERIALIZABLE,args.trigger)
        requireContext().startService(intent)
    }
    private fun startGPSService(){
        val intent = Intent(requireContext(),TriggerListenService::class.java)
        intent.action = TriggerListenService.START_GPS
        requireContext().startService(intent)
    }
    private fun stopGPSService(){
        val intent = Intent(requireContext(),TriggerListenService::class.java)
        intent.action = TriggerListenService.STOP_GPS
        requireContext().startService(intent)
    }

    private fun stopTriggerListenService(){
        val intent = Intent(requireContext(),TriggerListenService::class.java)
        intent.action = TriggerListenService.STOP_SERVICE
        requireContext().startService(intent)
    }

    private val localBroadcastReceiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let{
                val command = intent.getIntExtra("command",-1)
                if(command == TriggerListenService.LOCATION_SENT)
                {
                    val lat = intent.getStringExtra(LATITUDE)?:return
                    val lng = intent.getStringExtra(LONGITUDE)?:return
                    val distance = intent.getFloatExtra(DISTANCE,0.0f)

                    if (::viewModel.isInitialized){
                        viewModel.distance = distance
                        viewModel.storeCurrentLocation(LatLng(lat.toDouble(), lng.toDouble()))
                    }
                }
                else if(command == TriggerListenService.TRIGGER_SUCCESS)
                {
                    onTaskCompleted()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        networkUtil.unregisterNetworkCallback()
        unRegisterLocationReceiver()
    }

    //stop if any location updates
    override fun onDestroy() {
        super.onDestroy()
    }
}