package com.mk.gpstasker.view.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentMapsBinding
import com.mk.gpstasker.viewmodel.MapsViewModel
import java.util.*

class MapsFragment : Fragment() {

    private lateinit var mMap: GoogleMap

    private var _binding: FragmentMapsBinding? = null
    private lateinit var viewModel: MapsViewModel

    val binding
        get() = _binding?:throw Exception("Illegal state exception")


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

    private fun setObservers() {
        viewModel.latLng.observe(viewLifecycleOwner){
            dropPinOnMap(it)
            showNextPageBtn()
        }
    }

    private fun dropPinOnMap(latLng: LatLng) {
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
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
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
            findNavController().navigate(R.id.action_mapsFragment_to_triggerDetailFragment)
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