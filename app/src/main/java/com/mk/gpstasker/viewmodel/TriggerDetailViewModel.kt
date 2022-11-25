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

class TriggerDetailViewModel(private val triggersRepository: TriggersRepository):ViewModel() {
    val triggerAction = MutableLiveData(Trigger.ACTION_SILENCE)
    val triggerLabel = MutableLiveData("")

    fun addTrigger(location: Location){
        //add to db via repo
        viewModelScope.launch {
            withContext(Dispatchers.IO)
            {
                triggersRepository.saveTrigger(Trigger(
                    location = location,
                    triggerAction = triggerAction.value?:Trigger.ACTION_OTHER,
                    label = requireLabel()
                ))
            }
        }
    }

    private fun requireLabel(): String {
        return triggerLabel.value?:"unlabeled"
    }
}