package com.example.temp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*


class Bar(var name: String, var lat: Float, var long: Float, var rat: Float){
    companion object Factory {
        fun create(): Bar = Bar("def",1.0F,1.0F, 5F)
    }
}
// Add a chile event listener to make this view more dynamic
// https://firebase.google.com/docs/database/android/lists-of-data
class BarView : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var _adapter: TaskAdapter

    var _taskList: MutableList<Bar>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bar_view)

        _taskList = mutableListOf()

        database = FirebaseDatabase.getInstance().reference
        var mListView = findViewById<ListView>(R.id.list_view);

        _adapter = TaskAdapter(this, _taskList!!)

        mListView.adapter = _adapter

        database.orderByKey().addValueEventListener(_taskListener)

    }

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

                //key will return the Firebase ID
                task.name = map.get("name") as String
                _taskList!!.add(task)
            }
        }

        //alert adapter that has changed
        _adapter.notifyDataSetChanged()

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
}