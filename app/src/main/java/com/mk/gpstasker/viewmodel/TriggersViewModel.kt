package com.mk.gpstasker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.Trigger

class TriggersViewModel : ViewModel() {
    val repository = TriggersRepository()

    val triggerList = MutableLiveData<List<Trigger>>()
    init {
        triggerList.value = repository.getTriggers()
    }

}