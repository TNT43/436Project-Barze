package com.example.temp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.userprofile.*
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap


// This activity displays details for a single authenticated user
class UserProfile : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    // All the #2 variables are for the list view of favorite bars for the user, while the others are for reviews from the users

    // Adapters for favorite bar lists and user reviews
    lateinit var _adapterBar: TaskAdapterReview
    lateinit var _adapterBar2: TaskAdapterBar

    // Lists for favorite bars and reviews
    var _taskList: MutableList<Review>? = null
    var _taskList2: MutableList<Bar>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userprofile)

        // Setting the text view to display the logged in user's email
        val userEmailView =findViewById<TextView>(R.id.useremailtextview)
        userEmailView.text = FirebaseAuth.getInstance().currentUser!!.email

        // Initializing above variables
        _taskList = mutableListOf()
        _taskList2 = mutableListOf()
        database = FirebaseDatabase.getInstance().reference
        _adapterBar = TaskAdapterReview(this, _taskList!!)
        _adapterBar2 = TaskAdapterBar(this, _taskList2!!)


        // Storing list views for user reviews and favorite bar lists
        var mListView = findViewById<ListView>(R.id.user_review_list_view);
        var mListView2 = findViewById<ListView>(R.id.favorites_list_view);

        mListView.adapter = _adapterBar
        mListView2.adapter = _adapterBar2

        // Setting listeners in Firebase DB for Reviews and Favorite Bars. These listeners will display the lists from Firebase
        database.child("Reviews").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(_taskListener)
        database.child("Favorites").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(_taskListener2)

        // Allowing the favorites bar list to be clickable
        mListView2.setOnItemClickListener{
                parent, view, position, id ->
            val element = _taskList2!![position] // The item that was clicked
            val intent = Intent(this, SingleBarActivity::class.java)
            Log.i("TAG", "Putting  this in intent "+element.name +" "+ element.rat)
            intent.putExtra("BarName", element.name)
            intent.putExtra("BarRating", element.rat)
            startActivity(intent)
        }


        //Menu for navigation at the bottom of screen
        bottomNavigationViewprofile.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> startProfile()
                R.id.bars_list -> startList()
                R.id.bars_map -> startmap()

            }
            true
        }
    }

    // Implementing the value event listeners for favorite bars and reviews
    var _taskListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            loadTaskList(dataSnapshot)
        }
        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Item failed, log a message
            Log.w("TAG", "loadItem:onCancelled", databaseError.toException())
        }
    }
    var _taskListener2: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            loadTaskList2(dataSnapshot)
        }
        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Item failed, log a message
            Log.w("TAG", "loadItem:onCancelled", databaseError.toException())
        }
    }

    // Handles loading reviews stored in Firebase
    private fun loadTaskList(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList for user reviews")
        val tasks = dataSnapshot.children.iterator()
        //Check if current database contains any collection
        if (tasks.hasNext()) {
            _taskList!!.clear()
            val listIndex = tasks.next()
            val itemsIterator = listIndex.children.iterator()
            //check if the collection has any reviews or not
            while (itemsIterator.hasNext()) {
                //get current review
                val currentItem = itemsIterator.next()
                val task = Review.create()
                //get current data in a map
                val key = currentItem.value as HashMap<*,*>
                Log.i("TAG", "printing key")
                Log.i("TAG", key.toString())
                //key will contain the review
                task.name = key.get("User") as String
                task.review =key.get("Review") as String
                _taskList!!.add(task)
            }
        }
        //alert adapter that has changed
        _adapterBar.notifyDataSetChanged()
    }

    // Handles loading favorite bars stored in Firebase
    private fun loadTaskList2(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList for user favorite bars")
        val tasks = dataSnapshot.children.iterator()
        //Check if current database contains any collection
        if (tasks.hasNext()) {
            _taskList2!!.clear()
            val listIndex = tasks.next()
            val itemsIterator = listIndex.children.iterator()
            //check if the collection has any bar or not
            while (itemsIterator.hasNext()) {
                //get current task
                val currentItem = itemsIterator.next()
                val task = Bar.create()
                //get current data in a map
                val key = currentItem.value as HashMap<*,*>
                Log.i("TAG", "printing key")
                Log.i("TAG", key.toString())
                //key will return a Bar
                val matcher = BarMatch()
                task.name = key.get("name") as String
                task.pic = matcher.MatchImageWithName(task.name)
                task.rat =key.get("rat").toString().toFloat()
                _taskList2!!.add(task)
            }
        }
        //alert adapter that has changed
        _adapterBar2.notifyDataSetChanged()
    }

    // Functions for bottom navigation menu
    private fun startList(){
        startActivity(Intent(this, BarView::class.java))
    }

    private fun startmap(){
        startActivity(Intent(this, MapsActivity::class.java))
    }

    private fun startProfile(){
        //startActivity(Intent(this, UserProfile::class.java))
    }
}