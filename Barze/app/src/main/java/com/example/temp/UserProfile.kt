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


// This activity displays details on a single given bar
class UserProfile : AppCompatActivity(){

    private lateinit var database: DatabaseReference  // Firebase DB reference

    // All the #2 variables are for the list view of favorites for the user, while the otheres are for reviews from the users
    lateinit var _adapterBar: TaskAdapterReview
    lateinit var _adapterBar2: TaskAdapterBar


    lateinit var imageView:ImageView
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var storageReference: StorageReference? = null

    var _taskList: MutableList<Review>? = null
    var _taskList2: MutableList<Bar>? = null

    // This variable will store the barname this page is supposed to be displaying.
    // This will usually be passed in by whatever intent is starting this activity.
    lateinit var barName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.userprofile)

        _taskList = mutableListOf()
        _taskList2 = mutableListOf()

        database = FirebaseDatabase.getInstance().reference

        var mListView = findViewById<ListView>(R.id.user_review_list_view);
        var mListView2 = findViewById<ListView>(R.id.favorites_list_view);


        _adapterBar = TaskAdapterReview(this, _taskList!!)
        _adapterBar2 = TaskAdapterBar(this, _taskList2!!)

        database.child("Reviews").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(_taskListener)
        database.child("Favorites").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(_taskListener2)


        mListView.adapter = _adapterBar
        mListView2.adapter = _adapterBar2

        /*var proceedButton = findViewById<Button>(R.id.btnProceed)
        proceedButton.setOnClickListener {
            startActivity(Intent(this, BarView::class.java))
        }*/

        // menu
        bottomNavigationViewprofile.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> startProfile()
                R.id.bars_list -> startList()
                R.id.bars_map -> startmap()

            }
            true
        }
        // Setting the text views to display the above data
        val userEmailView =findViewById<TextView>(R.id.useremailtextview)
        userEmailView.text = FirebaseAuth.getInstance().currentUser!!.email



        // This part handles image uploading by user for profile
        /*val btnUpload = findViewById<Button>(R.id.btnUpload2)
        val btnChoose = findViewById<Button>(R.id.btnChoose2)
        imageView = findViewById<ImageView>(R.id.imgView2)*/

        storageReference = FirebaseStorage.getInstance().reference

       /* btnChoose.setOnClickListener {
            Log.i("Tag", "Choose cliekd")
            launchGallery()
        }

        btnUpload.setOnClickListener {
            uploadImage()
        }
*/
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
                    val user = FirebaseAuth.getInstance().currentUser
                    database.child("Images").child(user!!.uid).child("Profile_Pics").push().setValue(data)

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
        Log.d("TAG", "loadTaskList for user reviews")

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

    private fun loadTaskList2(dataSnapshot: DataSnapshot) {
        Log.d("TAG", "loadTaskList for user favorite bars")

        val tasks = dataSnapshot.children.iterator()


        //Check if current database contains any collection
        if (tasks.hasNext()) {

            _taskList2!!.clear()


            val listIndex = tasks.next()
            val itemsIterator = listIndex.children.iterator()

            //check if the collection has any task or not
            while (itemsIterator.hasNext()) {

                //get current task
                val currentItem = itemsIterator.next()
                val task = Bar.create()

                //get current data in a map
                val key = currentItem.value as HashMap<*,*>
                Log.i("TAG", "printing key")
                Log.i("TAG", key.toString())



                //key will return the Firebase ID
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


    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        menuInflater.inflate(R.menu.action_bar_profileexcluded, menu)
        return super.onCreateOptionsMenu(menu)

        //menuInflater.inflate(R.menu.bottom_nav_menu, menu)

    }*/

    private fun startList(){
        startActivity(Intent(this, BarView::class.java))
    }

    private fun startmap(){
        startActivity(Intent(this, MapsActivity::class.java))
    }

    private fun startProfile(){
        startActivity(Intent(this, UserProfile::class.java))
    }

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.title
        if(id == "Bar List"){
            startActivity(Intent(this, BarView::class.java))
        }else if(id == "Map"){
            startActivity(Intent(this, MapsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }*/


}