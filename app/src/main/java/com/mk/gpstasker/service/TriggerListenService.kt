package com.mk.gpstasker.service

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
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
        startForeground(NOTIFICATION_ID,notificationBuilder.build())
        repository = (application as App).triggersRepository
        serviceRunning = true
    }

    private fun initNotification() {
        //init pending intents for notification
        Intent(this,MainActivity::class.java).also {
            activityPendingIntent = PendingIntent.getActivity(this,PENDING_INTENT_REQ_CODE_ACT,it,PendingIntent.FLAG_IMMUTABLE)
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
                }
                command = newCommand
            }
        }
        //TODO:see when dies
        return START_NOT_STICKY
    }

    //init functions
    private fun initLocationClient() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationClient = LocationClient( fusedLocationProviderClient)
    }

    //starts getting current location
    //has the on success callback
    private fun startGPS() {
        if(command == START_SERVICE || command== STOP_GPS){
            locationClient.getCurrentLocationUpdates(applicationContext,oneShot = false) {
                sendLocation(it)
                if (isNearDestination(LatLng(it.latitude,it.longitude))) onTriggerSuccess()
            }
        }
    }


    //stops getting current location
    private fun stopGPS() {
            locationClient.stopLocationUpdates()
    }

    //updates trigger status as not running
    private fun updateTriggersAsNotRunning(){
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                repository.updateTriggerState(trigger.id,onGoing = false)
        }
    }


    //stops the current service and all the operations running by it
    private fun stopServ(){
        serviceRunning = false
        stopGPS()
        stopAlert()
        showServiceStoppedNotification()
        stopForeground(true)
        //make all null

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
            //if trigger action is not silence mode then show a screen to user
            if(trigger.triggerAction != Trigger.ACTION_SILENCE)
                Intent(this,ActionActivity::class.java).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    it.putExtra(Trigger.ACTION_TYPE,trigger.triggerAction)
                    startActivity(it)
                }

            doAction()
            sendTriggerSuccess()
            showTriggerSuccessNotification()
            //no need to keep service
//            if(trigger.triggerAction!= Trigger.ACTION_ALERT) stopServ()
        }
    }

    private fun showTriggerSuccessNotification() {
        Intent(this,TriggerListenService::class.java).also {
            it.action =  STOP_SERVICE
            exitPendingIntent = PendingIntent.getService(this, PENDING_INTENT_REQ_CODE_SER,it,PendingIntent.FLAG_IMMUTABLE)
        }

        notificationBuilder.setContentTitle("GPS Tasker - task completed")
            .setContentText("tap to clear")
            .setContentIntent(exitPendingIntent)
            .clearActions()
            .setAutoCancel(false)


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID,notificationBuilder.build())
//        startForeground(NOTIFICATION_ID,notificationBuilder.build())
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
            Trigger.ACTION_MESSAGE -> sendMessage()
            else -> Toast.makeText(this, "triggered but no action", Toast.LENGTH_SHORT).show()
        }
    }


    //sends message to given number
    private fun sendMessage() {
        if (trigger.mobileNumber.isEmpty()) return
        //send sms
        SmsManager.getDefault().sendTextMessage("+91"+trigger.mobileNumber,null,trigger.message,null,null)
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

    private fun stopAlert(){
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
