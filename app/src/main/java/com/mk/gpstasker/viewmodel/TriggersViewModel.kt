package com.mk.gpstasker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.Location
import com.mk.gpstasker.model.room.Trigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TriggersViewModel(val repository: TriggersRepository) : ViewModel() {

    val uiStates=UiStates()
    val triggerList = repository.getTriggers()
//    fun addTrigger(lat:Double,lng:Double){
//        //add to db via repo
//        viewModelScope.launch {
//            withContext(Dispatchers.IO)
//            {
//                repository.saveTrigger(Trigger(location = Location(latitude =lat, longitude = lng, "unlabeled")
//                    , triggerAction = Trigger.ACTION_SILENCE
//                ))
//            }
//        }
//    }
    var lastSelectedTrigger:Trigger? = null
    fun deleteTrigger(trigger: Trigger){
       viewModelScope.launch {
           withContext(Dispatchers.IO){
               repository.deleteTrigger(trigger)
           }
       }
    }
    class UiStates{
        var  isSentToSettings = false
    }
}