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


// This class represents every bar
class Bar(var name: String, var pic: Int, var lat: Float, var long: Float, var rat: Float){
    companion object Factory {
        fun create(): Bar = Bar("def",R.drawable.placeholder,1.0F,1.0F, 5F)
    }
}

// This activity will list all of the bars on the page
class BarView : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var _adapterBar: TaskAdapterBar

    var _taskList: MutableList<Bar>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bar_view)

        _taskList = mutableListOf()

        database = FirebaseDatabase.getInstance().reference

        var mListView = findViewById<ListView>(R.id.list_view);

        _adapterBar = TaskAdapterBar(this, _taskList!!)

        mListView.adapter = _adapterBar


       // Adding listener to the database, this will handle reading all of the Bars from Firebase.
        database.orderByKey().addValueEventListener(_taskListener)

        // menu
        bottomNavigationViewbar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> startProfile()
                R.id.bars_list -> startList()
                R.id.bars_map -> startmap()

            }
            true
        }

        // Handling behavior for when one of the bars on tbe list is clicked. This opens the SingleBarActivity.kt,
        // passing the bar name and rating in the intent.
        mListView.setOnItemClickListener{
                parent, view, position, id ->
            val element = _taskList!![position] // The item that was clicked
            val intent = Intent(this, SingleBarActivity::class.java)
            Log.i("TAG", "putthign this in intent "+element.name +" "+ element.rat)
            intent.putExtra("BarName", element.name)
            intent.putExtra("BarRating", element.rat)
            startActivity(intent)
        }

    }

    // This function creates the list of bars that are stored internally.
    private fun loadTaskList(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList")

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


    var _taskListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            loadTaskList(dataSnapshot)
        }
        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Item failed, log a message
            Log.w("TAG", "loadItem:onCancelled", databaseError.toException())
        }
    }


  /*  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        menuInflater.inflate(R.menu.action_bar_listexcluded, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.title
        if(id == "Map"){
            startActivity(Intent(this, MapsActivity::class.java))
        }else if(id == "Profile"){
            startActivity(Intent(this, UserProfile::class.java))
        }
        return super.onOptionsItemSelected(item)
    }*/
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