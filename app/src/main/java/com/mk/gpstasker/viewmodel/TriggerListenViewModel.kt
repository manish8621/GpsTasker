package com.mk.gpstasker.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.mk.gpstasker.model.room.Trigger
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class TriggerListenViewModel(val trigger: Trigger):ViewModel() {

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation : LiveData<Location>
    get() = _currentLocation

    // kiloMeter
    val radius = 0.010F
    var distance = 0.0
    private val oneDeg = Math.PI / 180


    fun getAction() = trigger.triggerAction

    /**
     * Warning: returns target location while current location is null
     * */
    fun getCurrentLatLng():LatLng{
        _currentLocation.value?.let{ return LatLng(it.latitude,it.longitude) }
        return LatLng(trigger.location.latitude,trigger.location.longitude)
    }

    fun storeCurrentLocation(location: Location)
    {
        _currentLocation.value = location
    }

    fun isNearDestination():Boolean{
        with(trigger.location) {
            _currentLocation.value?.let {
                distance = distanceBetween(LatLng(it.latitude,it.longitude),LatLng(latitude,longitude))
                return (distance <= radius)
            }
        }
        return false
    }

    fun distanceBetween(location1: LatLng,location2: LatLng):Double{
        val l1Lat = toRadian(location1.latitude)
        val l2Lat = toRadian(location2.latitude)
        val l1Lng = toRadian(location1.longitude)
        val l2Lng = toRadian(location2.longitude)
        val dlat = l2Lat - l1Lat
        val dlng = l2Lng - l1Lng

        var d = Math.pow(sin(dlat/2), 2.0) + cos(l1Lat) * cos(l2Lat) * Math.pow(sin(dlng)/2,2.0)
        d = 2 * asin(Math.sqrt(d))

        //multiply with earth radius
        d *= 6371

        return d
    }

    private fun toRadian(dec:Double):Double{
        return dec * oneDeg
    }

    //
    private fun Double.isInRange(min:Double,max:Double):Boolean{
        return (this in min..max)
    }
}
class TriggerListenViewModelFactory(private val trigger: Trigger): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TriggerListenViewModel::class.java)) {
            return TriggerListenViewModel(trigger) as T
        }
        throw IllegalArgumentException("illegal arg in listen factory")
    }
}