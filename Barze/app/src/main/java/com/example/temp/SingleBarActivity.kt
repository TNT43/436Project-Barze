package com.example.temp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

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

    lateinit var imageView:ImageView
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var storageReference: StorageReference? = null

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

        // This part handles image uploading by user for reviews
        val btnUpload = findViewById<Button>(R.id.btnUpload)
        val btnChoose = findViewById<Button>(R.id.btnChoose)
        imageView = findViewById<ImageView>(R.id.imgView)

        storageReference = FirebaseStorage.getInstance().reference

        btnChoose.setOnClickListener {
            Log.i("Tag", "Choose cliekd")
            launchGallery()
        }

        btnUpload.setOnClickListener {
            uploadImage()
        }

    }


    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    private fun uploadImage(){
        if(filePath != null){
            val ref = storageReference?.child(barName+"/" + UUID.randomUUID().toString())
            val uploadTask = ref?.putFile(filePath!!)

            val urlTask = uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation ref.downloadUrl
            })?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("Tag", "Succesful ")
                    val downloadUri = task.result
                    val data = HashMap<String, Any>()
                    data["imageUrl"] =   (downloadUri.toString())

                    Log.i("tag", "Entered adding record, data is:" + data)
                    Log.i("tag", "Barname is"+barName)

                    //database.child("Bars").child(barName).child("Images").push().setValue(data)


                } else {
                    // Handle failures
                    Toast.makeText(this, "Upload wasn't successful, please try again", Toast.LENGTH_SHORT).show()
                }
            }?.addOnFailureListener{
                Toast.makeText(this, "Upload wasn't successful, please try again", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this, "Please Upload an Image", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data == null || data.data == null){
                return
            }

            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
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