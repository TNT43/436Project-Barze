 package com.example.temp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

 // This activity uses the Firebase UI to log users in
 class LoginActivity : Activity(){
     private lateinit var database: DatabaseReference  // Firebase DB reference

     // Providers list, for now enabling only email logins
     val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build())
     val RC_SIGN_IN = 1

     override fun onCreate(savedInstanceState: Bundle?) {
         database = FirebaseDatabase.getInstance().reference
         super.onCreate(savedInstanceState)
         // Starts the UI login activity to authenticate users
         startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)

         if (requestCode == RC_SIGN_IN) {
             val response = IdpResponse.fromResultIntent(data)
             if (resultCode == Activity.RESULT_OK) {
                 // Successfully signed in
                 val user = FirebaseAuth.getInstance().currentUser
                 Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show()
                 // Storing the user data in Firebase Database for later use
                 val user_data = HashMap<String,String>()
                 user_data.put("ID",user!!.uid)
                 user!!.email?.let { user_data.put("Email" , it) }
                 user_data.get("ID")?.let { database!!.child("Users")!!.child(it).setValue(user_data) }

                 // Taking user to profile upon sign in
                 startActivity(Intent(this, UserProfile::class.java))
             } else {
                 // Sign in failed. If response is null the user canceled the
                 // sign-in flow using the back button. Otherwise check
                 // response.getError().getErrorCode() and handle the error.
                 Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
             }
         }
     }
}