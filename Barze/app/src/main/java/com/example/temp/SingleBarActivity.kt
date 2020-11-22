package com.example.temp

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

// This class represents a Review object. It currently stores the user who created the review, and the review itself.
class Review(var name: String, var review: String){
    companion object Factory {
        fun create(): Review = Review("default_user", "default_review")
    }
}

// This activity displays details on a single given bar
class SingleBarActivity : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var _adapterBar: TaskAdapterReview

    var _taskList: MutableList<Review>? = null

    // This variable will store the barname this page is supposed to be displaying.
    // This will usually be passed in by whatever intent is starting this activity.
    lateinit var barName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.singlebar_view)

        _taskList = mutableListOf()
        database = FirebaseDatabase.getInstance().reference

        var mListView = findViewById<ListView>(R.id.review_list_view);

        _adapterBar = TaskAdapterReview(this, _taskList!!)

        mListView.adapter = _adapterBar


        // Get bar name and rating from intent // this is where information specific to the bar should be extracted
        val intent = getIntent()
        barName = intent.getStringExtra("BarName")
        val barRating = intent.getFloatExtra("BarRating", -1F)

        // Setting the text views to display the above data
        val barNameTextView =findViewById<TextView>(R.id.singlebartextview)
        val barRatingView = findViewById<TextView>(R.id.singlebarratingview)
        barNameTextView.text = barName
        barRatingView.text = barRating.toString()

        // This part is to add Reviews to the bar
        database.child("Bars").child(barName).addValueEventListener(_taskListener)
        val reviewButton = findViewById<Button>(R.id.ReviewAddButton)
        reviewButton.setOnClickListener {
            val reviewText = findViewById<EditText>(R.id.ReviewInput).text.toString()
            if (reviewText.length > 0) {
                var reviewHolder = HashMap<String, String>()
                reviewHolder.put("User", "Default User")  // Replace by pulling in details of logged in user
                // TODO - Push this review under the user in the users db for firebase.
                reviewHolder.put("Review",reviewText)
                database.child("Bars").child(barName).child("Reviews").push().setValue(reviewHolder)
                val reviewTextField = findViewById<EditText>(R.id.ReviewInput)
                Toast.makeText(this, "Your review has been posted!", Toast.LENGTH_LONG).show()
                reviewTextField.setText("")
            }
        }
    }

    private fun loadTaskList(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList for reviews")

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
                val task = Review.create()

                //get current data in a map
                val key = currentItem.value as HashMap<*,*>
                Log.i("TAG", "printing key")
                Log.i("TAG", key.toString())



                //key will return the Firebase ID
                task.name = key.get("User") as String
                task.review =key.get("Review") as String
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

}