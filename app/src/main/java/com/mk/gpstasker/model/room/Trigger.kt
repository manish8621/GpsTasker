package com.mk.gpstasker.model.room

import androidx.navigation.NavType
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "trigger_table")
data class Trigger(
    @PrimaryKey(autoGenerate = true)
    val id:Long=0L,

    @Embedded
    val location:Location,

    val triggerAction:String,
    val mobileNumber:String="",
    val message:String,

    val label:String,

    var onGoing:Boolean = false

    ):java.io.Serializable
{
    companion object{

        val ACTION_TYPE = "action type"
        val ACTION_SILENCE = "ACTION_SILENCE"
        val ACTION_ALERT = "ACTION_ALERT"
        val ACTION_MESSAGE = "ACTION_MESSAGE"
        //TODO:Add extra features
        val ACTION_OTHER = "ACTION_?"

    }
}


data class Location(
    val latitude:Double,
    val longitude:Double,
    val radius:Float
    ):java.io.Serializable
