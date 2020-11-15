package com.example.temp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class SingleBarActivity : AppCompatActivity(){


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.singlebar_view)
        val intent = getIntent()
        val barName = intent.getStringExtra("BarName")
        val barRating = intent.getFloatExtra("BarRating", -1F)
        val barNameTextView =findViewById<TextView>(R.id.singlebartextview)
        val barRatingView = findViewById<TextView>(R.id.singlebarratingview)
        barNameTextView.text = barName
        barRatingView.text = barRating.toString()
    }

}