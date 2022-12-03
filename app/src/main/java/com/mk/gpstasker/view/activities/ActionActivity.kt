package com.mk.gpstasker.view.activities

import android.content.Intent
import android.graphics.drawable.Animatable2
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.ActivityActionBinding
import com.mk.gpstasker.databinding.FragmentTriggerListenBinding
import com.mk.gpstasker.model.SERVICE_COMMAND
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.service.TriggerListenService
import kotlin.system.exitProcess

class ActionActivity : AppCompatActivity() {
    lateinit var binding: ActivityActionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setOnclickListeners()
        setInfo()
    }

    private fun setInfo() {
        intent?.let {
            it.getStringExtra(Trigger.ACTION_TYPE)?.let { actionType->
                when(actionType){
                    Trigger.ACTION_MESSAGE -> {
                        binding.msgTv.text = "Message sent"
                    }
                }
            }
        }
    }


    private fun setOnclickListeners() {
        binding.stopBtn.setOnClickListener{
            stopTriggerListenService()
            finish()
        }
    }

    private fun stopTriggerListenService(){
//        val intent = Intent(this,TriggerListenService::class.java)
//        intent.action = TriggerListenService.STOP_SERVICE
//        startService(intent)
        stopService(Intent(this,TriggerListenService::class.java))
    }
}