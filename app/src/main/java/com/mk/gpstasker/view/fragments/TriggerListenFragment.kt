package com.mk.gpstasker.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.mk.gpstasker.MainActivity
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggerListenBinding
import com.mk.gpstasker.format
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.view.GPS_UPDATE_INTERVEL
import com.mk.gpstasker.viewmodel.TriggerListenViewModel
import com.mk.gpstasker.viewmodel.TriggerListenViewModelFactory
import java.util.*


//TODO:Please Clean and organize the code
class TriggerListenFragment : Fragment() {

    private val overlaySize: Float
        get() = (viewModel.radius) * 2500F


    private lateinit var mMap: GoogleMap
    private lateinit var binding:FragmentTriggerListenBinding
    private lateinit var viewModel:TriggerListenViewModel
    private val args : TriggerListenFragmentArgs by navArgs()

    lateinit var locationCallBack: LocationCallback
    lateinit var locationRequest: LocationRequest
    lateinit var locationManager: LocationManager
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this){
            showAlertDialog("Stop the trigger")
        }
        locationManager = (requireActivity() as MainActivity).getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_UPDATE_INTERVEL).build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted ->run{
            if (isGranted)
            {
                fetchLocation()
            }
            else
            {
                //TODO:SNACKBAR
            }
        }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTriggerListenBinding.inflate(inflater,container,false)
        val factory = TriggerListenViewModelFactory(args.trigger)
        viewModel = ViewModelProvider(this,factory)[TriggerListenViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        setUpObservers()
        setonClickListeners()

    }
    private fun setonClickListeners() {

    }

    private fun setUpObservers() {
        viewModel.currentLocation.observe(viewLifecycleOwner){ loc->
            loc?.let{
                markCurrentLocation(it,false)
                //do trigger action if location reached
                if (viewModel.isNearDestination())
                    doAction()
                //print distance on screen
                viewModel.currentLocation.value?.let {
                         ("distance : " + viewModel.distance.toFloat().format(3)).also { dist->
                             binding.distance.text = dist
                         }
                }
            }
        }
    }


    //map

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
        fetchLocation()
        dropPinOnMap(LatLng(args.trigger.location.latitude,args.trigger.location.longitude),true)
    }

    private fun markCurrentLocation(location: Location ,moveCamera:Boolean) {

        mMap.clear()
        dropPinOnMap(LatLng(args.trigger.location.latitude,args.trigger.location.longitude),false)

        //add ground overlay
        val groundOverlay = GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.circle_black_512))
            .position(LatLng(args.trigger.location.latitude,args.trigger.location.longitude),overlaySize)
        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(location.latitude,location.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.gps_ico))
        )
        mMap.addGroundOverlay(groundOverlay)

        if(moveCamera)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude), 15f))
    }
    private fun dropPinOnMap(latLng: LatLng, moveCamera: Boolean) {
        mMap.clear()
        val snippet = String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Target")
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.flag_64))
        )
        if(moveCamera)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        val groundOverlay = GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.circle_black_512))
            .position(LatLng(latLng.latitude,latLng.longitude),overlaySize)
        mMap.addGroundOverlay(groundOverlay)
    }
    //location
    private fun fetchLocation() {
        if(locationUsable()){
            getLocationUpdates()
        }
    }

    private fun locationUsable(): Boolean {
        if(checkLocationPermission()){
            if(checkLocationEnabled())
            {
                return true
            }
            else
            {
                //TODO:SNACKBAR
            }
        }
        else{
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return false
    }


    private fun getLocationUpdates() {
        startLocationRequest{
            viewModel.storeCurrentLocation(it)
//            Toast.makeText(context, "lat : ${it.latitude} lon : ${it.longitude}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationRequest(onSuccess:((location: Location)->Unit)) {
        if(::locationCallBack.isInitialized.not()){
            locationCallBack = object :LocationCallback(){
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    onSuccess(locationResult.locations[0])
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallBack,null)
    }
    private fun stopLocationRequest(){
        if(::locationCallBack.isInitialized)
            fusedLocationProviderClient.removeLocationUpdates(locationCallBack)
    }

    private fun checkLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkLocationPermission(): Boolean {
        return requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    //TODO:replace with Dialog fragment
    private fun showAlertDialog(text:String) {
        AlertDialog.Builder(
            requireContext()
        ).setTitle(text)
            .setMessage("Are you sure ?")
            .setPositiveButton("Yes"
            ) { _, _ ->
                stopTriggerListening()
                findNavController().navigateUp()
            }
            .setNegativeButton("no") { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    //utils
    private fun doAction() {
        when(viewModel.getAction()){
            Trigger.ACTION_ALERT -> alert()
            Trigger.ACTION_SILENCE -> silentMode()
            else -> Toast.makeText(context, "triggered but no action", Toast.LENGTH_SHORT).show()
        }
    }

    //one shot
    private fun silentMode() {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        Toast.makeText(context, "GPS TASKER : putting device into to Silent mode", Toast.LENGTH_SHORT).show()
        goBack()
    }

    private fun goBack() {
        findNavController().navigateUp()
    }

    private fun alert() {
        val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context,RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context,ringtoneUri)
        ringtone.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationRequest()
    }
    private fun stopTriggerListening() {
        stopLocationRequest()
        Toast.makeText(context, "Trigger stopped", Toast.LENGTH_SHORT).show()
    }


}