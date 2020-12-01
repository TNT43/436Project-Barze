package com.example.temp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// This activity will list all of the bars on the page
class ReportVisit : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report_visit)

        val user = FirebaseAuth.getInstance().currentUser
        val barName = intent.getStringExtra("BarName")
        val barRating = intent.getFloatExtra("BarRating", -1F)


        database = FirebaseDatabase.getInstance().reference

        val waitingTimeInput = findViewById<EditText>(R.id.WaitingTimeInput)


        // Todo: Consolidate two buttons into one? Handle empty inputs
        val waitSubmitButton = findViewById<Button>(R.id.btnSubmitWaitTime)
        waitSubmitButton.setOnClickListener {
            val user_data = HashMap<String, Float>()
            user_data.put("Waiting Time", waitingTimeInput.text.toString().toFloat())
            database.child("WaitingTime").child(barName).child("Times").push().setValue(user_data)

        }


        val submitButton = findViewById<Button>(R.id.btnSubmit)
        val checkbox  = findViewById<CheckBox>(R.id.FavoriteCheckBox)
        submitButton.setOnClickListener {
            val map = HashMap<String,String>()
            map.put("name",barName)
            map.put("rat",barRating.toString())
            if( checkbox.isChecked ){
                database.child("Favorites").child(user!!.uid).child("Favorites").push().setValue(map)
            }
            val intent = Intent(this, SingleBarActivity::class.java)
            intent.putExtra("BarName", barName)
            intent.putExtra("BarRating", barRating)
            startActivity(intent)
        }


    }

}