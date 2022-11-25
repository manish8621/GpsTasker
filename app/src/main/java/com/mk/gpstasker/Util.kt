package com.mk.gpstasker



fun Float.format(digits:Int):String{
    return "%.${digits}f".format(this)
}
fun Double.format(digits:Int):String{
    return "%.${digits}f".format(this)
}