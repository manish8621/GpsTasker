package com.mk.gpstasker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_graph)
        navHost?.also{ navFragment ->
            navFragment.childFragmentManager.primaryNavigationFragment?.let {fragment->
                //DO YOUR STUFF
                Toast.makeText(applicationContext, fragment.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }



}