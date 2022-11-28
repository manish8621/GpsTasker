package com.mk.gpstasker.model

import android.location.Location
import android.widget.ImageButton
import android.widget.ImageView
import androidx.navigation.findNavController
import com.google.android.gms.maps.model.LatLng
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin


//extension functions
fun Location.toLatLng(): LatLng {
    return LatLng(latitude,longitude)
}
fun com.mk.gpstasker.model.room.Location.toLatLng(): LatLng {
    return LatLng(latitude,longitude)
}
fun Float.format(digits:Int):String{
    return "%.${digits}f".format(this)
}
fun Double.format(digits:Int):String{
    return "%.${digits}f".format(this)
}

fun distanceBetween(location1: LatLng,location2: LatLng):Double{
    val l1Lat = toRadian(location1.latitude)
    val l2Lat = toRadian(location2.latitude)
    val l1Lng = toRadian(location1.longitude)
    val l2Lng = toRadian(location2.longitude)
    val dlat = l2Lat - l1Lat
    val dlng = l2Lng - l1Lng

    var d = Math.pow(sin(dlat/2), 2.0) + cos(l1Lat) * cos(l2Lat) * Math.pow(sin(dlng) /2,2.0)
    d = 2 * asin(Math.sqrt(d))

    //multiply with earth radius
    d *= 6371

    return d
}

private fun toRadian(dec:Double):Double{
    return dec * oneDeg
}

//
private fun Double.isInRange(min:Double,max:Double):Boolean{
    return (this in min..max)
}