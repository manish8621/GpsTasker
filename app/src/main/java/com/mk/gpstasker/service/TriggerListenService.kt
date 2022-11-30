package com.mk.gpstasker.service

import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.mk.gpstasker.App
import com.mk.gpstasker.MainActivity
import com.mk.gpstasker.R
import com.mk.gpstasker.model.*
import com.mk.gpstasker.model.location.LocationClient
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.view.activities.ActionActivity
import kotlinx.coroutines.*
import kotlin.system.exitProcess

class TriggerListenService: Service() {
    //command is used to track the current status of service
    var command = START_SERVICE

    //to be alive at background
    lateinit var activityPendingIntent: PendingIntent
    lateinit var exitPendingIntent:PendingIntent
    lateinit var notificationBuilder:NotificationCompat.Builder


    //
    lateinit var locationClient: LocationClient
    lateinit var trigger: Trigger
    //TODO:try to remove this
    lateinit var ringtone: Ringtone
    lateinit var repository: TriggersRepository
    //TODO:see will it cause bug
    private var distance  = Double.MAX_VALUE


    override fun onCreate() {
        super.onCreate()
        //init all
        initLocationClient()
        initNotification()
        startInForeground()
        repository = (application as App).triggersRepository
        serviceRunning = true
    }

    private fun initNotification() {
        //init pending intents for notification
        Intent(this,MainActivity::class.java).also {
            activityPendingIntent = PendingIntent.getActivity(this,PENDING_INTENT_REQ_CODE_ACT,it,PendingIntent.FLAG_IMMUTABLE)
        }
        Intent(this,TriggerListenService::class.java).also {
            it.action =  EXIT_PROCESS
            exitPendingIntent = PendingIntent.getService(this, PENDING_INTENT_REQ_CODE_SER,it,PendingIntent.FLAG_IMMUTABLE)
        }

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHN_ID)
            .setSmallIcon(R.drawable.gps_ico)
            .setContentTitle("GPS Tasker")
            .setContentText("listening location in background")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(activityPendingIntent)
    }

    //for every action service will get an intent based on that service will act
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let{
            //TODO:can the default value assign in any scenario ,i mean will service start without bundle
            val newCommand = it.action?: START_SERVICE

            //init trigger
            if (command == START_SERVICE && newCommand == START_SERVICE) {
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
                    STOP_SERVICE -> stopServ()
                    EXIT_PROCESS -> stopServ()
                }
                command = newCommand
            }
        }

        //TODO:try non sticky
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        startForeground(NOTIFICATION_ID,notificationBuilder.build())
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
        if(command == START_GPS) {
            locationClient.stopLocationUpdates()
        }
    }

    //updates trigger status as not running
    private fun updateTriggersAsNotRunning(){
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                repository.updateTriggerState(trigger.id,onGoing = false)
        }
    }



    private fun quitApp(){
        stopServ()
    }

    //stops the current service and all the operations running by it
    private fun stopServ(){
        serviceRunning = false
        stopGPS()
        stopAlert()
        showServiceStoppedNotification()
        stopForeground(true)
        stopSelf()
    }




    //broadcasts the trigger task is done
    private fun sendTriggerSuccess(){
        val intent = Intent("GPSLocationUpdates")
        intent.putExtra("command", TRIGGER_SUCCESS)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    //this function  will handle the triggered situation first
    private fun onTriggerSuccess() {
        updateTriggersAsNotRunning()
        //for safe
        if(command == START_GPS){
            stopGPS()
            Intent(this,ActionActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(it)
            }
            doAction()
            sendTriggerSuccess()
            showTriggerSuccessNotification()
        }
    }

    private fun showTriggerSuccessNotification() {
        notificationBuilder.setContentTitle("GPS Tasker - task completed")
            .setContentText("tap to clear")
            .setContentIntent(exitPendingIntent)
            .clearActions()
            .setAutoCancel(true)

        startForeground(NOTIFICATION_ID,notificationBuilder.build())
    }

    private fun showServiceStoppedNotification() {
        notificationBuilder.setContentTitle("GPS Tasker")
            .setContentText("service stopped")
            .setContentIntent(exitPendingIntent)
            .clearActions()
            .setAutoCancel(true)
        startForeground(NOTIFICATION_ID,notificationBuilder.build())
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
        const val START_SERVICE = "start service"
        const val STOP_SERVICE = "Stop service"
        const val EXIT_PROCESS = "exit process"
        const val START_GPS = "start gps"
        const val STOP_GPS = "stop gps"
        const val LOCATION_SENT = 1
        const val TRIGGER_SUCCESS = 7
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}
