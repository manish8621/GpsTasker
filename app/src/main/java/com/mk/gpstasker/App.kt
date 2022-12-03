package com.mk.gpstasker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.mk.gpstasker.model.NOTIFICATION_CHN_ID
import com.mk.gpstasker.model.NOTIFICATION_CHN_NAME
import com.mk.gpstasker.model.repository.TriggersRepository
import com.mk.gpstasker.model.room.TriggerDatabase
import com.mk.gpstasker.service.TriggerListenService

class App:Application() {
    private val database:TriggerDatabase by lazy { TriggerDatabase.getDatabase(context = this) }
    val triggersRepository by lazy { TriggersRepository(database) }
    override fun onCreate() {
        super.onCreate()
        createAndRegisterNChannel()
    }

    private fun createAndRegisterNChannel() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            getSystemService(NotificationManager::class.java).run {
                val nChannel = NotificationChannel(NOTIFICATION_CHN_ID
                    , NOTIFICATION_CHN_NAME
                    ,NotificationManager.IMPORTANCE_HIGH)
                createNotificationChannel(nChannel)
            }
        }
    }

}