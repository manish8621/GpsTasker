package com.mk.gpstasker.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel:ViewModel() {

    //
    var _location = MutableLiveData<Location>()
    var lat:Double = 12.8589
    var lon:Double = 80.0781
    var radius = 0.1
    //
    val location :LiveData<Location>
    get() = _location

    fun storeCurrentLocation(l: Location)
    {
        _location.value = l
    }
    fun isNearDestination():Boolean{
        _location.value?.let {

            if( it.latitude.isInRange(lat,lat+radius) || it.latitude.isInRange(lat-radius,lat))
            {
                if(it.longitude.isInRange(lon,lon+radius) || it.longitude.isInRange(lon-radius,lon)){
                    return true
                }
            }
        }
        return false
    }


    //
    fun Double.isInRange(min:Double,max:Double):Boolean{
        return (this in min..max)
    }
}