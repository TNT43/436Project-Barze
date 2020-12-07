package com.example.temp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// This activity will allow a user to report their visit to a given bar
class ReportVisit : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report_visit)

        // Initializing data passed from intent
        val user = FirebaseAuth.getInstance().currentUser
        val barName = intent.getStringExtra("BarName")
        val barRating = intent.getFloatExtra("BarRating", -1F)


        database = FirebaseDatabase.getInstance().reference

       // Input for a a user's waiting time
        val waitingTimeInput = findViewById<EditText>(R.id.WaitingTimeInput)

        // Input for whether the given bar was a user's favorite
        val checkbox  = findViewById<CheckBox>(R.id.FavoriteCheckBox)

        // Submission button
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            // Storing the waiting time for a this user
            val user_data = HashMap<String, Float>()
            if(waitingTimeInput.text.toString().length > 0) {
                user_data.put("Waiting Time", waitingTimeInput.text.toString().toFloat())
                // Pushing the waiting time to firebase
                database.child("WaitingTime").child(barName).child("Times").push().setValue(user_data)
            }
            // Storing information about the favorite bar
            val map = HashMap<String,String>()
            map.put("name",barName)
            map.put("rat",barRating.toString())
            // If checked pushing back to Firebase
            if( checkbox.isChecked ){
                database.child("Favorites").child(user!!.uid).child("Favorites").push().setValue(map)
            }
            // Taking user back to the Single Bar view they'd come from
            val intent = Intent(this, SingleBarActivity::class.java)
            intent.putExtra("BarName", barName)
            intent.putExtra("BarRating", barRating)
            startActivity(intent)
        }


    }

}