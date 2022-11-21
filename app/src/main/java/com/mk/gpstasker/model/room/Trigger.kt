package com.mk.gpstasker.model.room

data class Trigger(val id:Int,val location:Location,val triggerAction:String){
    companion object{
        val ACTION_SILENCE = "ACTION_SILENCE"
        val ACTION_ALERT = "ACTION_SILENCE"
        //TODO:Add extra features
        val ACTION_OTHER = "ACTION_?"

    }
}
data class Location(val latitude:Double, val longitude:Double, val name:String)
