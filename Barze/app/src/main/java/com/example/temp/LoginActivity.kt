package com.example.temp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View

class LoginActivity : Activity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)
    }

    fun login(view : View){
        startActivity(Intent(this, MapsActivity::class.java))
    }
}