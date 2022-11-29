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

    //this is used to (save the trigger that user clicked) start listening after
    // user gone to settings ,given permissions and returned
    var lastSelectedTrigger:Trigger? = null
    //updates the trigger's onGoing column as true
    fun updateTriggersAsRunning(triggerId:Long){
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO)
            {
                repository.updateTriggerState(triggerId,onGoing = true)
            }
        }
    }

    fun deleteTrigger(trigger: Trigger){
       viewModelScope.launch {
           withContext(Dispatchers.IO){
               repository.deleteTrigger(trigger)
           }
       }
    }

    class UiStates{
        var  isSentToSettings = false
        var  isSentToTriggerListenFragment = false
    }
}