package com.mk.gpstasker.model.repository

import com.mk.gpstasker.model.room.Location
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.model.room.TriggerDatabase

class TriggersRepository(private val database: TriggerDatabase) {


    fun getTriggers() = database.triggersDao.getTriggers()

    suspend fun saveTrigger(trigger: Trigger){
        database.triggersDao.insertTrigger(trigger)
    }

    suspend fun deleteTrigger(trigger: Trigger){
        database.triggersDao.deleteTrigger(trigger)
    }
    fun updateTriggerState(triggerId:Long,onGoing:Boolean){

        database.triggersDao.updateTriggerState(triggerId,onGoing)
    }
}