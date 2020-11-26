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

 class LoginActivity : Activity(){

     private lateinit var database: DatabaseReference  // Firebase DB reference

     val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build())
     val RC_SIGN_IN = 1




    override fun onCreate(savedInstanceState: Bundle?) {
        database = FirebaseDatabase.getInstance().reference

        super.onCreate(savedInstanceState)
        //setContentView(R.layout.login_screen)
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
                 val user_data = HashMap<String,String>()
                 user_data.put("ID",user!!.uid)
                 user!!.email?.let { user_data.put("Email" , it) }
                 database!!.child("Users")!!.push().setValue(user_data)
                 startActivity(Intent(this, BarView::class.java))

                 // ...
             } else {
                 // Sign in failed. If response is null the user canceled the
                 // sign-in flow using the back button. Otherwise check
                 // response.getError().getErrorCode() and handle the error.
                 // ...
                 Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
             }
         }
     }

    fun login(view : View){
        startActivity(Intent(this, MapsActivity::class.java))
    }

    fun viewBars(view : View){
        startActivity(Intent(this, BarView::class.java))
    }
}