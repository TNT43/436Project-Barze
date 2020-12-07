package com.example.temp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
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
import kotlinx.android.synthetic.main.singlebar_view.*
import org.w3c.dom.Text
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.round

// This class represents a Review object. It currently stores the user who created the review, and the review itself.
class Review(var name: String, var review: String){
    companion object Factory {
        fun create(): Review = Review("default_user", "default_review")
    }
}

// This activity displays details for a single Bar
class SingleBarActivity : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    // Variables to handle upload and display of images for a given bar
    val imagelist = arrayListOf<String>()
    lateinit var imageView:ImageView
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var storageReference: StorageReference? = null

    // Variables to manage listview and display of reviews for a bar
    lateinit var _adapterBar: TaskAdapterReview
    var _taskList: MutableList<Review>? = null

    // This variable will store the barname this page is supposed to be displaying.
    // This will usually be passed in by whatever intent is starting this activity.
    lateinit var barName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.singlebar_view)

        // Initializing above variables
        _taskList = mutableListOf()
        database = FirebaseDatabase.getInstance().reference
        _adapterBar = TaskAdapterReview(this, _taskList!!)

        // Get bar name and rating from intent // this is where information specific to the bar should be extracted
        val intent = getIntent()
        barName = intent.getStringExtra("BarName")
        val barRating = intent.getFloatExtra("BarRating", -1F)

        // Setting the text views to display the above data from the intent
        val barNameTextView =findViewById<TextView>(R.id.singlebartextview)
        val barRatingView = findViewById<TextView>(R.id.singlebarratingview)
        barNameTextView.text = barName
        barRatingView.text = barRating.toString()


        // This listview  will store all of the reviews for a bar
        var mListView = findViewById<ListView>(R.id.review_list_view);
        mListView.adapter = _adapterBar

        // This button creates and uploads a user review to Firebase
        val reviewButton = findViewById<Button>(R.id.ReviewAddButton)
        reviewButton.setOnClickListener {
            val reviewText = findViewById<EditText>(R.id.ReviewInput).text.toString()
            if (reviewText.length > 0) {
                val user = FirebaseAuth.getInstance().currentUser
                // reviewHoldre represents a single review
                var reviewHolder = HashMap<String, String>()
                user!!.email?.let { it1 -> reviewHolder.put("User", it1) }
                reviewHolder.put("Review",reviewText)
                // Adding the review to the Firebase for the Bar, as well as associating it to the user
                database.child("Bars").child(barName).child("Reviews").push().setValue(reviewHolder)
                database.child("Reviews").child(user!!.uid).child("Reviews").push().setValue(reviewHolder)

                val reviewTextField = findViewById<EditText>(R.id.ReviewInput)
                Toast.makeText(this, "Your review has been posted!", Toast.LENGTH_LONG).show()
                reviewTextField.setText("")
            }
        }

        // This will set a listener to the Firebase instance to allow for the review listview to display the reviews
        // stored in Firebase
        database.child("Bars").child(barName).orderByKey().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("TAG", "loadTaskList for reviews")
                val tasks = dataSnapshot.children.iterator()
                //Check if current database contains any collection
                if (tasks.hasNext()) {
                    _taskList!!.clear()
                    val listIndex = tasks.next()
                    val itemsIterator = listIndex.children.iterator()
                    //check if the collection has any task or not
                    while (itemsIterator.hasNext()) {
                        //get current review
                        val currentItem = itemsIterator.next()
                        val task = Review.create()
                        //get current data in a map
                        val key = currentItem.value as HashMap<*,*>
                        Log.i("TAG", "printing key")
                        Log.i("TAG", key.toString())
                        //key will contain a given review and the user that created it
                        task.name = key.get("User") as String
                        task.review =key.get("Review") as String
                        _taskList!!.add(task)
                    }
                }
                //alert adapter that has changed
                _adapterBar.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("TAG", "loadReview:onCancelled", databaseError.toException())
                // ...
            }
        })


        // This button handles when a user wants to record a visit, sends them to a different activity to do that
        // User can record their wait times their and report whether a bar is their favorite
        val recordVisitButton = findViewById<Button>(R.id.btnVisit)
        recordVisitButton.setOnClickListener {
            val intent = Intent(this, ReportVisit::class.java)
            intent.putExtra("BarName", barName)
            intent.putExtra("BarRating", barRating)
            startActivity(intent)
        }

        // Getting the average waiting time TextView, this will display by pulling data from firebase
        val avgWait = findViewById<TextView>(R.id.AverageWaitTime)
        // This listener calculates average wait time at a bar by pulling stored user wait times at the bar and finding the average
        database.child("WaitingTime").child(barName).orderByKey().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var total = 0.0
                val tasks = dataSnapshot.children.iterator()
                var count =0.0
                //Check if current database contains any collection
                if (tasks.hasNext()) {
                    val listIndex = tasks.next()
                    val itemsIterator = listIndex.children.iterator()
                    //check if the collection has any task or not
                    while (itemsIterator.hasNext()) {
                        Log.i("TAG", "Inside child iterator")
                        count +=1
                        //get current wait time
                        val currentItem = itemsIterator.next()
                        //get current data in a map
                        val key = currentItem.value as HashMap<*,*>
                        Log.i("Tag","Key for wait time is"+key.toString() )
                        // key contains the Waiting Time reported by any given user
                        total = total + key.get("Waiting Time").toString().toDouble()
                    }
                }
                if(count != (0).toDouble() ) {
                    Log.i("Tag","Count is"+count.toString() )
                    val averagewt = total / count
                    avgWait.text = "Waiting time is: "+ round(averagewt) +" minutes"
                }
                else {
                    val averagewt = "Waiting time not known"
                    avgWait.text = averagewt
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })


        // This part handles image uploading by user for reviews. It allows for users to choose an image fromt their gallery
        // then upload the image to a Google Firebase Storage instance. The URL is stored in the Realtime Database and used to display the
        // images later

        // Buttons for choosing image and uploading it
        val btnUpload = findViewById<Button>(R.id.btnUpload)
        val btnChoose = findViewById<Button>(R.id.btnChoose)

        // image view to display selected picture
        imageView = findViewById<ImageView>(R.id.imgView)

        // Firebase Storage Reference to use for storing images
        storageReference = FirebaseStorage.getInstance().reference

        btnChoose.setOnClickListener {
            Log.i("TAG", "Choose clicked")
            launchGallery()
        }

        btnUpload.setOnClickListener {
            Log.i("TAG", "Upload clicked")
            uploadImage()
        }

        //This listener will be linked to Firebase to listen for all images uploaded there by the user and display the 5 most recent ones
        database.child("Images").child(barName).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i("TAG","Inside ondatachange")
                val tasks = dataSnapshot.children.iterator()
                //Check if current database contains any collection
                if (tasks.hasNext()) {
                    val listIndex = tasks.next()
                    val itemsIterator = listIndex.children.iterator()
                    //check if the collection has any task or not
                    while (itemsIterator.hasNext()) {
                        Log.i("TAG", "Inside image iterator")
                        //get current Image
                        val currentItem = itemsIterator.next()
                        //get current data in a map
                        val key = currentItem.value as HashMap<*, *>
                        Log.i("TAG",key.get("imageUrl").toString()+"-----------------")
                        imagelist.add(key.get("imageUrl").toString())
                        // This handles loading the 5 most recent urls into image views
                        if(imagelist.size == 1) {
                            DownloadImageFromInternet(findViewById(R.id.imageView2)).execute(
                                key.get(
                                    "imageUrl"
                                ).toString()
                            )
                        }
                        else if(imagelist.size == 2){
                            DownloadImageFromInternet(findViewById(R.id.imageView3)).execute(
                                key.get(
                                    "imageUrl"
                                ).toString()
                            )
                        }
                        else if(imagelist.size == 3){
                            DownloadImageFromInternet(findViewById(R.id.imageView4)).execute(
                                key.get(
                                    "imageUrl"
                                ).toString()
                            )
                        }
                        else if(imagelist.size == 4){
                            DownloadImageFromInternet(findViewById(R.id.imageView5)).execute(
                                key.get(
                                    "imageUrl"
                                ).toString()
                            )
                        }
                        else{
                            // Do nothing
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })

        //Menu for navigation at the bottom of screen
        bottomNavigationViewsingle.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> startProfile()
                R.id.bars_list -> startList()
                R.id.bars_map -> startmap()

            }
            true
        }
    }


    // Used for choosing an image, launches the phone's gallery
    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    // Used to upload the image to Firebase Storage
    private fun uploadImage(){
        if(filePath != null){
            // Each image gets a unique path to be stored in
            val ref = storageReference?.child(barName+"/" + UUID.randomUUID().toString())
            // Task to represent an upload, doesn't interfere with Main UI thread
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
                    Log.i("TAG", "Succesful ")
                    val downloadUri = task.result
                    // data will store the url of an uploaded image for later use
                    val data = HashMap<String, Any>()
                    data["imageUrl"] =   (downloadUri.toString())
                    Log.i("TAG", "Entered adding record, data is:" + data)
                    Log.i("TAG", "Barname is"+barName)
                    val user = FirebaseAuth.getInstance().currentUser

                    // Storing this data in Firebase Realtime, so we know what these image urls correspond too
                    database.child("Images").child(barName).child("Images").push().setValue(data)
                    database.child("Images").child(user!!.uid).child("Bar_images").push().setValue(data)

                    Toast.makeText(this, "Upload completed", Toast.LENGTH_SHORT).show()
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

    // This method displays the chosen image to the user before they upload it
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

    // Methods to handle the botton navigation menu
    private fun startList(){
        startActivity(Intent(this, BarView::class.java))
    }

    private fun startmap(){
        startActivity(Intent(this, MapsActivity::class.java))
    }

    private fun startProfile(){
        startActivity(Intent(this, UserProfile::class.java))
    }

    // Async task to handle downloading the images for a bar from the Firebase Storage instance in background
    private inner class DownloadImageFromInternet(var imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            val imageURL = urls[0]
            Log.i("TAG","UrL is +"+imageURL)
            var image: Bitmap? = null
            try {
                val `in` = java.net.URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
            }
            catch (e: Exception) {
                Log.e("Error Message", e.message.toString())
                e.printStackTrace()
            }
            return image
        }
        override fun onPostExecute(result: Bitmap?) {
            imageView.setImageBitmap(result)
        }
    }
}