package com.mk.gpstasker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mk.gpstasker.model.repository.TriggersRepository

class TriggersViewModelFactory(private val repository: TriggersRepository):ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TriggersViewModel::class.java))
        {
            return TriggersViewModel(repository = repository) as T
        }
        throw IllegalArgumentException("in factory of triggers")
    }
}