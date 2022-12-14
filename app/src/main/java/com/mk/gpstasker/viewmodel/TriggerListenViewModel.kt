package com.mk.gpstasker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TriggerListenViewModel(val trigger: Trigger, val repository: TriggersRepository):ViewModel() {

    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation : LiveData<LatLng>
    get() = _currentLocation

    val uiStates = UiStates()

    // kiloMeter
    var distance = 0.0f
    private val oneDeg = Math.PI / 180


    //TODO:REMOVE
    fun getAction() = trigger.triggerAction

    /**
     * Warning: returns target location while current location is null
     * */
    fun getCurrentLatLng():LatLng{
        _currentLocation.value?.let{ return LatLng(it.latitude,it.longitude) }
        return LatLng(trigger.location.latitude,trigger.location.longitude)
    }

    fun storeCurrentLocation(location: LatLng)
    {
        _currentLocation.value = location
    }

    //updates trigger status as not running
    fun updateTriggersAsNotRunning(){
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            repository.updateTriggerState(trigger.id,onGoing = false)
        }
    }

    class UiStates{
        var isSentToSettings = false
        var taskCompleted = false
    }
}
//TODO:REMOVE
class TriggerListenViewModelFactory(private val trigger: Trigger,private val repository: TriggersRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TriggerListenViewModel::class.java)) {
            return TriggerListenViewModel(trigger,repository) as T
        }
        throw IllegalArgumentException("illegal arg in listen factory")
    }
}