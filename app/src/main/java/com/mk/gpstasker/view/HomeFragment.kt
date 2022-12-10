package com.mk.gpstasker.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mk.gpstasker.MainActivity
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentHomeBinding
import com.mk.gpstasker.service.TriggerListenService
import com.mk.gpstasker.viewmodel.HomeViewModel


//TODO:Delete this class or mark it as test class
val GPS_UPDATE_INTERVEL= 500L

class HomeFragment : Fragment() {

    lateinit var intent: Intent
    lateinit var binding: FragmentHomeBinding
    lateinit var viewModel : HomeViewModel
    lateinit var locationCallBack:LocationCallback
    lateinit var locationRequest:LocationRequest
    lateinit var locationManager: LocationManager
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = (requireActivity() as MainActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setUpObservers()
        setonClickListeners()
//        fetchLocation()





        return binding.root
    }

    private fun setonClickListeners() {
        binding.okBtn.setOnClickListener{
            binding.radiusEt.text.toString().also {
                if(it.isNotEmpty())
                    viewModel.radius = it.toDouble()
            }
        }
        binding.setBtn.setOnClickListener{
            viewModel.location.value?.let {
                viewModel.lat = it.latitude
                viewModel.lon = it.longitude
            }
            Toast.makeText(context, "current co ord -> target", Toast.LENGTH_SHORT).show()
        }
        binding.serviceBtn.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = Intent(requireContext(),TriggerListenService::class.java)
                requireContext().startForegroundService(intent)
            }
        }
    }

    private fun setUpObservers() {
        viewModel.location.observe(viewLifecycleOwner){


            "now\nlat : ${it.latitude}\nlon : ${it.longitude}".also { text-> binding.locationTv.text = text }
            "target\nlat : ${viewModel.lat}\nlon : ${viewModel.lon}".also { text-> binding.targetLocationTv.text = text }
            "rad: ${viewModel.radius}\nlat diff : ${it.latitude-viewModel.lat}\nlon diff : ${it.longitude-viewModel.lon}".also { text-> binding.diffTv.text = text }

            if(viewModel.isNearDestination())
                Toast.makeText(context, "TRIGGERING [OK]", Toast.LENGTH_SHORT).show()
        }
    }

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
    private fun checkBackgroundLocationPermission(): Boolean {
        return  if(Build.VERSION.SDK_INT >=29)
                    requireActivity().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                else
                    true
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLocationRequest()
        if (::intent.isInitialized)
            requireContext().stopService(intent)
    }

}