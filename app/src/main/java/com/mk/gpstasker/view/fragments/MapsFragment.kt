package com.mk.gpstasker.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
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
import com.mk.gpstasker.*
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentMapsBinding
import com.mk.gpstasker.view.GPS_UPDATE_INTERVEL
import com.mk.gpstasker.viewmodel.MapsViewModel
import java.util.*

class MapsFragment : Fragment() {

    private val overlaySize: Float
    get() = (viewModel.radius.value?: MinRadius) * 2500F

    private lateinit var mMap: GoogleMap

    private var _binding: FragmentMapsBinding? = null
    private lateinit var viewModel: MapsViewModel

    lateinit var locationCallBack: LocationCallback
    lateinit var locationRequest: LocationRequest
    lateinit var locationManager: LocationManager
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    val binding
        get() = _binding?:throw Exception("Illegal state exception")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater,container,false)
        viewModel = ViewModelProvider(this)[MapsViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        setClickListeners()
        setObservers()
    }

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
        viewModel.latLng.observe(viewLifecycleOwner){
            dropPinOnMap(it,true)
            showNextPageBtn()
        }
        viewModel.radius.observe(viewLifecycleOwner){ radius->
            changeRadiusInUi(radius)
        }
    }


    private fun changeRadiusInUi(rad:Float) {
        "${rad.format(2)} km".also { binding.radiusValueTv.text = it }
        viewModel.latLng.value?.let {
            if (::mMap.isInitialized)
                dropPinOnMap(it,false)
        }
    }
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

        //camera
        if (zoomEnabled)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
        val groundOverlay = GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.circle_black_512))
            .position(LatLng(latLng.latitude,latLng.longitude),overlaySize.toFloat())
        mMap.addGroundOverlay(groundOverlay)
    }

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
        binding.doneBtn.setOnClickListener{
            findNavController().navigate(
                MapsFragmentDirections.actionMapsFragmentToTriggerDetailFragment(
                    //TODO:remove default value
                    viewModel.getLocation()
                )
            )
        }
        binding.fetchLocationBtn.setOnClickListener{
            fetchLocation()
        }
        binding.rangeSlider.setMinSeparationValue(0.01F)
        binding.rangeSlider.addOnChangeListener(RangeSlider.OnChangeListener { slider, value, _ ->
            //TODO:Into a function
            viewModel.radius.value = value * MaxRadius
        })
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


    //location

    private fun fetchLocation() {
        if(locationUsable()){
            Toast.makeText(context, "Getting current location...", Toast.LENGTH_SHORT).show()
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
            viewModel.updateLatLng(LatLng(it.latitude,it.longitude))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationRequest(onSuccess:((location: android.location.Location)->Unit)) {
        if(::locationCallBack.isInitialized.not()){
            locationCallBack = object :LocationCallback(){
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    onSuccess(locationResult.locations[0])
                    stopLocationRequest()
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

    override fun onDestroy() {
        super.onDestroy()
    }
}