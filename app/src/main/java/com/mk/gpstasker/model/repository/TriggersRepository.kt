package com.mk.gpstasker.model.repository

import com.mk.gpstasker.model.room.Location
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.model.room.TriggerDatabase

class TriggersRepository(val database: TriggerDatabase) {
    //TODO:DB
    //fake db
//    fun getTriggers():List<Trigger>{
//        return listOf(
//            Trigger(id = 1, location = Location(12.4566778,22.4566778,"Zoho"),Trigger.ACTION_ALERT),
//            Trigger(id = 2,location = Location(12.4566778,22.4566778,"Chennai"),Trigger.ACTION_SILENCE),
//            Trigger(id = 3,location = Location(12.4566778,22.4566778,"Karaikudi"),Trigger.ACTION_SILENCE),
//            Trigger(id = 4,location = Location(12.4566778,22.4566778,"Erode"),Trigger.ACTION_SILENCE),
//        )
//    }

    fun getTriggers() = database.triggersDao.getTriggers()

    suspend fun saveTrigger(trigger: Trigger){
        database.triggersDao.insertTrigger(trigger)
    }

    suspend fun deleteTrigger(trigger: Trigger){
        database.triggersDao.deleteTrigger(trigger)
    }
}