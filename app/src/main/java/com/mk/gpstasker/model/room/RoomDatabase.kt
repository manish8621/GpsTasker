package com.mk.gpstasker.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trigger::class], version = 2)
abstract class TriggerDatabase: RoomDatabase() {
    abstract val triggersDao:TriggersDao
    companion object{
        private var INSTANCE:TriggerDatabase? = null
        fun getDatabase(context:Context):TriggerDatabase{
            return synchronized(this){
                if(INSTANCE == null)
                    INSTANCE = Room.databaseBuilder(
                        context,
                        TriggerDatabase::class.java,
                        "triggers_db"
                    ).fallbackToDestructiveMigration().build()
                return@synchronized INSTANCE as TriggerDatabase
            }
        }
    }
}