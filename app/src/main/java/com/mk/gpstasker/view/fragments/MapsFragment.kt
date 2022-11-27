package com.mk.gpstasker.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.slider.RangeSlider
import com.google.android.material.snackbar.Snackbar
import com.mk.gpstasker.*
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentMapsBinding
import com.mk.gpstasker.model.*
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.network.checkInternet
import com.mk.gpstasker.viewmodel.MapsViewModel
import java.util.*

//OK
class MapsFragment : Fragment() {

    //the trigger radius size on map
    private val overlaySize: Float
    get() = (viewModel.radius.value?: MinRadius) * 2500F


    private lateinit var binding: FragmentMapsBinding
    private lateinit var viewModel: MapsViewModel

    private lateinit var locationClient: LocationClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>



    private lateinit var mMap: GoogleMap
    //when map is ready
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap.also {  gmap ->

            //show map options interface
            gmap.setOnMapClickListener(){ latLng->

                onLocationSelected(latLng)
                //show next page interface
            }
        }
        showMapModeInterface()
        updateMapMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //permission handle
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted -> run {
            if (isGranted)  fetchLocation()
            else {
                Snackbar.make(binding.root,R.string.permission_denied_msg,Snackbar.LENGTH_SHORT)
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater,container,false)
        viewModel = ViewModelProvider(this)[MapsViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        //location client
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationClient = LocationClient(requireContext(), fusedLocationProviderClient)



        setClickListeners()
        setObservers()
        binding.rangeSlider.setMinSeparationValue(0.01F)
    }

    //check user sent to settings and start the process after user returns
    override fun onStart() {
        super.onStart()
        //TODO : replace with function for ui states
        if(viewModel.uiStates.isSentToSettings) {
            viewModel.uiStates.isSentToSettings = false
            fetchLocation()
        }
    }

    //on pin dropped on map
    private fun onLocationSelected(latLng: LatLng) {
        viewModel.updateLatLng(latLng)
    }

    private fun showNextPageBtn() {
        binding.doneBtn.isVisible = true
    }


    private fun showMapModeInterface() {
        binding.mapTypeGroup.visibility = View.VISIBLE
    }


    private fun setObservers() {
        //when current location is obtained
        viewModel.latLng.observe(viewLifecycleOwner){
            dropPinOnMap(it,true)
            showNextPageBtn()
        }

        //user will drag the radius slider listen and update radius value
        viewModel.radius.observe(viewLifecycleOwner){ radius->
            changeRadiusInUi(radius)
        }


    }


    private fun changeRadiusInUi(rad:Float) {
        //text view update
        "${rad.format(2)} km".also { binding.radiusValueTv.text = it }
        //to change ground overlay size
        viewModel.latLng.value?.let {
            if (::mMap.isInitialized)
                dropPinOnMap(it,false)
        }
    }


    //to mark location on map
    private fun dropPinOnMap(latLng: LatLng,zoomEnabled:Boolean) {
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

        )
        //camera zoom
        if (zoomEnabled)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
        //overlay to show the trigger radius
        val groundOverlay = GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.circle_black_512))
            .position(LatLng(latLng.latitude,latLng.longitude),overlaySize.toFloat())
        mMap.addGroundOverlay(groundOverlay)
    }

    //map mode normal or satellite
    private fun updateMapMode() {
        mMap.mapType = viewModel.mapMode
        when(viewModel.mapMode){
            GoogleMap.MAP_TYPE_NORMAL -> {
                selectOption(binding.defaultViewBtn,binding.defaultTv)
                unSelectOption(binding.satelliteViewBtn,binding.satiliteTv)
            }
            GoogleMap.MAP_TYPE_HYBRID -> {
                unSelectOption(binding.defaultViewBtn,binding.defaultTv)
                selectOption(binding.satelliteViewBtn,binding.satiliteTv)
            }
        }
    }

    private fun setClickListeners() {
        //map mode
        binding.defaultViewBtn.setOnClickListener{
            if (it.tag.toString().isEmpty()){
                viewModel.mapMode = GoogleMap.MAP_TYPE_NORMAL
                updateMapMode()
            }
        }
        binding.satelliteViewBtn.setOnClickListener{
            if (it.tag.toString().isEmpty()){
                viewModel.mapMode = GoogleMap.MAP_TYPE_HYBRID
                updateMapMode()
            }

        }
        //to go next fragment
        binding.doneBtn.setOnClickListener{
            findNavController().navigate(
                MapsFragmentDirections.actionMapsFragmentToTriggerDetailFragment(
                    //TODO:remove default value
                    viewModel.getLocation()
                )
            )
        }
        //to get current location
        binding.fetchLocationBtn.setOnClickListener{
            fetchLocation()
        }
        //to get user defined radius
        binding.rangeSlider.addOnChangeListener(RangeSlider.OnChangeListener { slider, value, _ ->
            //TODO:Into a function
            viewModel.radius.value = value * MaxRadius
        })
        binding.upBtn.setAsNavigationUpBtn()
    }

    //ui utils to select and unselect options
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


    //current location fetch
    //needed permissions will be asked
    private fun fetchLocation() {
        if(requireContext().checkInternet() == INTERNET_AVAILABLE){
            if (locationUsable()) {
                Toast.makeText(context, "Getting current location...", Toast.LENGTH_SHORT).show()
                getLocationUpdates()
            }
        }
        else
        {
            Toast.makeText(context, "No internet", Toast.LENGTH_SHORT).show()
        }
    }

    //user will asked to grant or turn on location related things
    //if they no snack bar will be shown they can use that to go settings
    private fun locationUsable(): Boolean {
        //TODO: network check
        if(true)
        {
            if (locationClient.checkLocationPermission()) {
                if (locationClient.checkLocationEnabled()) {
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
        }
        else{
            Toast.makeText(context, "no internet", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    //location updates starts here
    private fun getLocationUpdates() {
        locationClient.getCurrentLocationUpdates(oneShot = true){
            viewModel.updateLatLng(LatLng(it.latitude,it.longitude))
        }
    }


    //to enable location permissions user will be sent to settings
    private fun goToAppInfo() {
        val settingsIntent = Intent()
        settingsIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package",BuildConfig.APPLICATION_ID,null)
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

    //stop if any location updates
    override fun onDestroy() {
        super.onDestroy()
        locationClient.stopLocationUpdates()
    }
}