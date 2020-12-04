package com.example.temp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationManager : LocationManager
    private lateinit var placesClient: PlacesClient

    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(38.9897, -76.9378) // default for UMD CP
    private var locationPermissionGranted = false

    private var lat : Double = 38.9897 // defaults for UMD CP
    private var lng : Double = -76.9378 // defaults for UMD CP

    private lateinit var JSONParser : JSONParsersClass //outsource parsing to clean up clutter

    lateinit var barsInfo : HashMap<String, HashMap<String, Double>>
    private lateinit var currentBar : String

    private lateinit var timedFunction : Timer

    private lateinit var sharedPrefs : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        // store and keep track of the most recent bar the user visited this will allow us to more
        // keep a more accurate count of users at a bar when they turn off the app/phone
        sharedPrefs = this.getSharedPreferences("lastBar", Context.MODE_PRIVATE)
        currentBar = sharedPrefs.getString("lastBar", "NONE").toString()
        Log.i(TAG, "Last bar seen $currentBar")

        // set up firebase
        database = FirebaseDatabase.getInstance().reference

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // check if user has allowed us to access their location and ask for it if not
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }

        barsInfo = HashMap<String, HashMap<String, Double>>()

        // this will call a function every 60 seconds that will poll the firebase and update the
        // info snippets on the map markers
        timedFunction = Timer()
        timedFunction.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                JSONParser.updateInfo()
            }
        }, 10 * 1000, 10 * 1000) //put here time 1000 milliseconds=1 second


    }
    // this function is called when the google maps comes back to us ready to go (async)
    // this will tell us the map if ready to be worked on. Sometimes the user won't have given
    // permission yet so we have to wait for that
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap // set our global map value
        if(locationPermissionGranted){
            Log.i(TAG, "Map is ready and permission is granted")
            setupMap()
        }
    }

    // map setup. Create icons on map and onclicks
    @SuppressLint("MissingPermission")
    // we can supress this because we know the only way this function is called is if location
    // permission is granted
    private fun setupMap(){
        // now that map is finally initalized and db initalized in oncreate, we can give
        // a reference to this specific class object to the parser
        JSONParser = JSONParsersClass(this)

        // run the first update for firebase info
        JSONParser.updateInfo()

        map.addMarker( // base
            MarkerOptions()
                .position(defaultLocation)
                .title("Marker in UMD CP")
        ).showInfoWindow()

        // this listener will see if the user clicked on a bars info popup on the map
        // and will start the singlebaractivity for that bar
        map.setOnInfoWindowClickListener { marker ->
            val intent = Intent(this, SingleBarActivity::class.java)
            Log.i("TAG", "putthing this in intent FROM INFOWINDOW ONCLICK IN MAPSACTIVITY "+ marker.title +" "+ barsInfo[marker.title]!!.get("rat"))
            intent.putExtra("BarName", marker.title)
            intent.putExtra("BarRating",barsInfo[marker.title]!!.get("rat")!!.toFloat())
            startActivity(intent)
            false
        }

        // this removes markers for other stores like restraunts and big private companies
        // that are placed onto the map by default
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

        // this will allow a button in the top right to show that will take you back to your
        // location on the map
        map?.isMyLocationEnabled = true
        map?.uiSettings?.isMyLocationButtonEnabled = true

        // create url to make api request for bars in our location at 5000 meter radius
        var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + lat + "," + lng +
                "&radius=5000" + "&type=" + "bar" +
                "&key=" + resources.getString(R.string.google_maps_key);


        // begin to keep track of current location. Using custom location listener defined
        // in locationListener() function above
        // this will give us a gps update everytime the user moves more than 20 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            0, 20f, locationListener())

        // JSONParser will exectue the api request and also deal with placing markers on map where bars
        // are and with the relevant information
        Log.i(TAG, "API request sent")
        JSONParser.MakeAPIRequest(FIND_BARS).execute(url)
    }
    // update the bar population as its listed on the firebase so that we can give users feedback
    // about wait times/popularity
    fun updateBarPop(){
        // due to asynchronous nature of data accumuluation ive had to put these here to make sure
        // this is not run before everything else has had a chance to get things initialized
        if(lastKnownLocation == null || lastKnownLocation!!.latitude == null || lastKnownLocation!!.longitude == null
            || barsInfo == null || barsInfo.isEmpty()){
            return
        }
        Log.i(TAG, "Updating Bar Pop")
         if(currentBar != "NONE"){ // check if user was at a bar on the last check and update it
            // this will decrement a person from the bar they were previously at
             barsInfo.get(currentBar)!!.put("currPop", barsInfo.get(currentBar)!!.get("currPop")!! - 1)
             // update database
            database.child("Bars").child(currentBar).child("currPop").setValue(barsInfo.get(currentBar)!!.getValue("currPop"))
            currentBar = "NONE"
             sharedPrefs.edit().putString("lastBar", "NONE").commit()
        }
        // yes this will create a a possible situation where you could decrement then increment the same bar
        for((name, hashmap) in barsInfo){
            // this will check to see if the user is within a certain range of the bar
            // if its 'close enough' ill count it as at that bar
            if(hashmap["lat"]!! <= lastKnownLocation?.latitude!! + .0001 && hashmap["lat"]!! >= lastKnownLocation?.latitude!! - .0001 &&
                hashmap["lng"]!! <= lastKnownLocation?.longitude!! + .0001 && hashmap["lng"]!! >= lastKnownLocation?.longitude!! - .0001){
                //this will update current bar location and increment the population at that bar
                currentBar = name
                hashmap.put("currPop", hashmap.get("currPop")!! + 1)
                database.child("Bars").child(currentBar).child("currPop").setValue(barsInfo.get(currentBar)!!.getValue("currPop"))
                sharedPrefs.edit().putString("lastBar", currentBar).commit()
            }
        }
        // this will update the snippet on the info window to reflect correct number of users at bar
        for(curr in JSONParser.mapMarkers){
            curr.snippet = barsInfo[curr.title]!!.get("currPop")!!.toInt().toString() +
                                " other users here"
        }

        /*for((name, hashmap) in barsInfo){
            Log.i(TAG, name + " " + hashmap.get("currPop").toString())
        }*/
        //Log.i(TAG, "ENDING OF UPDATEBARPOP")
    }

    // location listener will get information about the device should it change
    // and update the global variable if needed and adjust where the map is looking
    private fun locationListener(): LocationListener {

        return object : LocationListener {
            // Called back when location changes
            override fun onLocationChanged(location: Location) {
                Log.i(TAG, "Updating Location")
                if(lastKnownLocation == null || location.accuracy <= lastKnownLocation!!.accuracy){
                    lastKnownLocation = location
                    map?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            ), DEFAULT_ZOOM.toFloat()
                        )
                    )
                    updateBarPop()
                }
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {  }

            override fun onProviderEnabled(provider: String) { }

            override fun onProviderDisabled(provider: String) {  }
        }
    }

    //permissions callback function. Is called when user accepts/denies our request to use permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        locationPermissionGranted = false // making sure this is set to false so that if we dont
        // get permission we dont break anything
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> { // permission check request code
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }else{ // if not given permission, kill the app because we require location data
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        finishAffinity();
                    } else {
                        finish();
                    }
                }
                // begin to setup map. Was not done before because we had to wait for permission
                setupMap()
            }
        }
    }


    // this function simply create a small box ui with the text of the bars rating in the center
    // it will return a bitmap that can be used to draw onto the map
    fun createMarkers(drawableId: Int, text: String): Bitmap {
        val drawable = this.applicationContext.getDrawable(drawableId)
        val bm = drawable?.toBitmap(width = 100, height = 100, config = null)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 50F
        paint.isFakeBoldText = true
        val canvas = Canvas(bm!!)
        canvas.drawText(text, 50f,70f, paint)
        return bm
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        menuInflater.inflate(R.menu.action_bar_mapexcluded, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.title
        if(id == "Bar List"){
            startActivity(Intent(this, BarView::class.java))
        }else if(id == "Profile"){
            startActivity(Intent(this, UserProfile::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        onStop()
    }

    override fun onStop() {
        super.onStop()
        timedFunction.cancel()
        timedFunction.purge()
        finish()
    }
    companion object {
        private const val TAG = "TAG"
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        //API Request types
        private const val FIND_BARS = 0
    }


}