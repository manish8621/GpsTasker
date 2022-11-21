package com.mk.gpstasker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class MapsViewModel:ViewModel() {
    //state
    var mapMode = GoogleMap.MAP_TYPE_NORMAL
    val latLng = MutableLiveData<LatLng>()
    fun updateLatLng(latLng:LatLng){
        this.latLng.value = latLng
    }
}