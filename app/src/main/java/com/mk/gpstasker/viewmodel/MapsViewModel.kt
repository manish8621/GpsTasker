package com.mk.gpstasker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.mk.gpstasker.MinRadius
import com.mk.gpstasker.model.room.Location

class MapsViewModel:ViewModel() {
    //state
    var mapMode = GoogleMap.MAP_TYPE_NORMAL

    val radius = MutableLiveData(MinRadius)

    val latLng = MutableLiveData<LatLng>()

    fun updateLatLng(latLng:LatLng){
        this.latLng.value = latLng
    }
    fun getLattitude():Double{
        return latLng.value?.latitude?:0.0
    }
    fun getLongitude():Double{
        return latLng.value?.longitude?:0.0
    }
    fun requireRadius():Float {
        return radius.value?: MinRadius
    }
    fun getLocation():Location{
        return Location(getLattitude(),getLongitude(),requireRadius())
    }
}