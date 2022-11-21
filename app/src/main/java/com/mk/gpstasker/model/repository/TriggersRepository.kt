package com.mk.gpstasker.model.repository

import com.mk.gpstasker.model.room.Location
import com.mk.gpstasker.model.room.Trigger

class TriggersRepository() {
    //TODO:DB
    //fake db
    fun getTriggers():List<Trigger>{
        return listOf(
            Trigger(id = 1, location = Location(12.4566778,22.4566778,"Zoho"),Trigger.ACTION_ALERT),
            Trigger(id = 2,location = Location(12.4566778,22.4566778,"Chennai"),Trigger.ACTION_SILENCE),
            Trigger(id = 3,location = Location(12.4566778,22.4566778,"Karaikudi"),Trigger.ACTION_SILENCE),
            Trigger(id = 4,location = Location(12.4566778,22.4566778,"Erode"),Trigger.ACTION_SILENCE),
        )
    }
}