package com.mk.gpstasker

import android.app.Application
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.TriggerDatabase

class App:Application() {
    private val database:TriggerDatabase by lazy { TriggerDatabase.getDatabase(context = this) }
    val triggersRepository by lazy { TriggersRepository(database) }
}