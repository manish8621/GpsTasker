package com.mk.gpstasker.model.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TriggersDao {
    @Insert
    fun insertTrigger(trigger: Trigger)

    @Query("SELECT * FROM trigger_table")
    fun getTriggers():LiveData<List<Trigger>>

    @Query("SELECT * FROM trigger_table where id = :id")
    fun getTrigger(id:Long):Trigger

    @Delete
    fun deleteTrigger(trigger: Trigger)

    @Update
    fun updateTrigger(trigger: Trigger)
}