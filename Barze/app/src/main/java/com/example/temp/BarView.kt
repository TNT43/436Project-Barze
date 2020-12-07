package com.example.temp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.bar_view.*


// This class represents every individual bar stored in Firebase, with names, ratings and locations
class Bar(var name: String, var pic: Int, var lat: Float, var long: Float, var rat: Float){
    companion object Factory {
        fun create(): Bar = Bar("def",R.drawable.placeholder,1.0F,1.0F, 5F)
    }
}

// This activity will list all of the bars stored in Firebase, that represent CP bars
class BarView : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var _adapterBar: TaskAdapterBar  // Adapter for List view reference

    var _taskList: MutableList<Bar>? = null // list that will process each bar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bar_view)

        // initializing references
        _taskList = mutableListOf()
        database = FirebaseDatabase.getInstance().reference
        _adapterBar = TaskAdapterBar(this, _taskList!!)

        // This list view will contain all of the bars
        var mListView = findViewById<ListView>(R.id.list_view);
        mListView.adapter = _adapterBar

        // menu for navigation at bottom
        bottomNavigationViewbar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> startProfile()
                R.id.bars_list -> startList()
                R.id.bars_map -> startmap()

            }
            true
        }

        // Adding listener to the database, this will handle reading all of the Bars from Firebase.
        database.orderByKey().addValueEventListener(_taskListener)


        // Handling behavior for when one of the bars on tbe list is clicked. This opens the SingleBarActivity.kt,
        // passing the bar name and rating in the intent.
        mListView.setOnItemClickListener{
                parent, view, position, id ->
            val element = _taskList!![position] // The item that was clicked
            val intent = Intent(this, SingleBarActivity::class.java)
            Log.i("TAG", "Putting  this in intent "+element.name +" "+ element.rat)
            intent.putExtra("BarName", element.name)
            intent.putExtra("BarRating", element.rat)
            startActivity(intent)
        }

    }

    // This listener is bound to Firebase and listens for events to display Bars
    var _taskListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            loadTaskList(dataSnapshot)
        }
        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Item failed, log a message
            Log.w("TAG", "loadItem:onCancelled", databaseError.toException())
        }
    }

    // This function creates the list of bars that are stored internally and is called by the Firebase listener
    private fun loadTaskList(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList for Bar list")

        val tasks = dataSnapshot.children.iterator()

        //Check if current database contains any collection
        if (tasks.hasNext()) {

            _taskList!!.clear()


            val listIndex = tasks.next()
            val itemsIterator = listIndex.children.iterator()

            //check if the collection has any task or not
            while (itemsIterator.hasNext()) {

                //get current task
                val currentItem = itemsIterator.next()
                val task = Bar.create()

                //get current data in a map
                val map = currentItem.getValue() as HashMap<*, *>

                // Create and popultate the Bar object with the name and rating pulled from firebase.
                task.name = map.get("name") as String
                val matcher = BarMatch()
                task.pic = matcher.MatchImageWithName(task.name)
                task.rat   =map.get("rat").toString().toFloat() as Float
                _taskList!!.add(task)
            }
        }

        //alert adapter that has changed
        _adapterBar.notifyDataSetChanged()

    }

    // Following methods are for the bottom navigation bar
    private fun startList(){
        //startActivity(Intent(this, BarView::class.java))
    }

    private fun startmap(){
        startActivity(Intent(this, MapsActivity::class.java))
    }

    private fun startProfile(){
        startActivity(Intent(this, UserProfile::class.java))
    }
}