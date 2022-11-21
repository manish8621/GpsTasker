package com.mk.gpstasker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TriggerDetailViewModel:ViewModel() {
    val triggerAction = MutableLiveData("")
    val triggerLabel = MutableLiveData("")

    fun addTrigger(){
        //add to db via repo
    }
}