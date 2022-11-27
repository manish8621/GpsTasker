package com.mk.gpstasker.model

import android.location.Location
import android.widget.ImageButton
import android.widget.ImageView
import androidx.navigation.findNavController
import com.google.android.gms.maps.model.LatLng


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
fun ImageButton.setAsNavigationUpBtn(){
    setOnClickListener{
        findNavController().navigateUp()
    }
}
