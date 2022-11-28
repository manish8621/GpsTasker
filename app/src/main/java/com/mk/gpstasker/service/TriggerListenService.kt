package com.mk.gpstasker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.mk.gpstasker.model.*
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.room.Trigger

class TriggerListenService: Service() {
    var command = START_SERVICE


    lateinit var locationClient: LocationClient
    lateinit var trigger: Trigger
    lateinit var ringtone: Ringtone
    private var distance  = Double.MAX_VALUE

    override fun onCreate() {
        super.onCreate()
        //init all
        initLocationClient()

        serviceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let{
            //TODO:can the default value assign in any scenario ,i mean will service start without bundle
            val newCommand = it.getIntExtra(SERVICE_COMMAND, START_SERVICE)

            //init trigger
            if (newCommand == START_SERVICE) {
                it.getSerializableExtra(TRIGGER_SERIALIZABLE)?.let { trigger->
                    this.trigger = trigger as Trigger
                }
            }


            //check if command is duplicated
            if(newCommand != command)
            {
                when(newCommand){
                    START_GPS -> startGPS()
                    STOP_GPS -> stopGPS()
                    STOP_SERVICE -> stopAll()
                }
                command = newCommand
            }
        }

        //TODO:try non sticky
        return super.onStartCommand(intent, flags, startId)
    }

    //init functions
    private fun initLocationClient() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationClient = LocationClient(this, fusedLocationProviderClient)
    }

    //starts getting current location
    //has the on success callback
    private fun startGPS() {
        if(command == START_SERVICE || command== STOP_GPS){
            locationClient.getCurrentLocationUpdates(oneShot = false) {
                sendLocation(it)
                if (isNearDestination(LatLng(it.latitude,it.longitude))) onTriggerSuccess()
            }
        }
    }


    //stops getting current location
    private fun stopGPS() {
        if(command == START_GPS) locationClient.stopLocationUpdates()
    }

    //stops all ongoing ops
    private fun stopAll() {
        serviceRunning = false
        stopGPS()
        stopAlert()
        //TODO:dsf
        stopSelf()
    }

    //broadcasts the trigger task is done
    fun sendTriggerSuccess(){
        val intent = Intent("GPSLocationUpdates")
        intent.putExtra("command", TRIGGER_SUCCESS)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    //this function  will handle the triggered situation first
    private fun onTriggerSuccess() {
        //for safe
        if(command == START_GPS){
            stopGPS()
            doAction()
            sendTriggerSuccess()
            //TODO:notification change
        }
    }

    //broadcasts the location
    private fun sendLocation(location:Location){
        val intent = Intent("GPSLocationUpdates")
        intent.putExtra("command", LOCATION_SENT)
        intent.putExtra(LATITUDE, location.latitude.toString())
        intent.putExtra(LONGITUDE, location.longitude.toString())
        intent.putExtra(DISTANCE, distance.toFloat())

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    //computations
    fun isNearDestination(currentLocation: LatLng):Boolean{
        //TODO:naming conv
        if(::trigger.isInitialized){
            with(trigger.location) {
                    distance = distanceBetween(
                        currentLocation,
                        LatLng(latitude, longitude)
                    )
                    return (distance <= radius)
            }
        }
        return false
    }


    //trigger action will be done here
    private fun doAction() {
        when(trigger.triggerAction){
            Trigger.ACTION_ALERT -> alert()
            Trigger.ACTION_SILENCE -> silentMode()
            else -> Toast.makeText(this, "triggered but no action", Toast.LENGTH_SHORT).show()
        }
    }

    //one shot
    //puts mobile to silent mode
    private fun silentMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        Toast.makeText(this, "GPS TASKER : putting device into to Silent mode", Toast.LENGTH_SHORT).show()
//        TODO:show notification and close
    }

    private fun alert() {
        Toast.makeText(this, "alert", Toast.LENGTH_SHORT).show()
        val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this,ringtoneUri)
        ringtone.play()
    }

    fun stopAlert(){
        if(::ringtone.isInitialized && ringtone.isPlaying)
            ringtone.stop()
    }



    companion object{

        //state
        private var serviceRunning = false
        val isRunning:Boolean
        get() = serviceRunning


        //command
        const val LOCATION_SENT = 4
        const val START_SERVICE = 0
        const val STOP_SERVICE = 3
        const val START_GPS = 1
        const val STOP_GPS = 2
        const val TRIGGER_SUCCESS = 5
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlert()
        stopGPS()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}
