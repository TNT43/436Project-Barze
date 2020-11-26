package com.example.temp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import java.util.*
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
    private  var currentBar : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // set up fireb
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
    }
    // this function is called when the google maps comes back to us ready to go (async)
    // this will tell us the map if ready to be worked on. Sometimes the user won't have given
    // permission yet so we have to wait for that
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap // set our global map value
        if(locationPermissionGranted){
            setupMap()
        }
    }

    // map setup. Create icons on map and onclicks
    @SuppressLint("MissingPermission")
    // we can supress this because we know the only way this function is called is if location
    // permission is granted
    private fun setupMap(){
        // begin to keep track of current location. Using custom location listener defined
        // in locationListener() function above
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            1000 * 10.toLong(), 10f, locationListener())

        map.addMarker( // tester
            MarkerOptions()
                .position(defaultLocation)
                .title("Marker in UMD CP")
        ).showInfoWindow()

        // this is where we'll set up the listener so anytime someone clicks on a marker
        // we can do something with that information
        map.setOnMarkerClickListener { marker ->
            val position: LatLng = marker.position
            Toast.makeText(
                this,
                "Lat " + position.latitude + " "
                        + "Long " + position.longitude,
                Toast.LENGTH_LONG
            ).show()
            false
        }
        // this removes markers for other stores like restraunts and big private companies
        //that are placed onto the map by default
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

        // now that map is finally initalized and db initalized in oncreate, we can give
        // a reference to this specific class object to the parser
        JSONParser = JSONParsersClass(this)
        // JSONParser will exectue the api request and also deal with placing markers on map where bars
        // are and with the relevant information
        JSONParser.MakeAPIRequest(FIND_BARS).execute(url)
    }
    // update the bar population as its listed on the firebase so that we can give users feedback
    // about wait times/popularity
    // TODO: Make it actually update firebase
    private fun updateBarPop(){
        Log.i(TAG, "START OF UPDATEBARPOP")
        for((name, hashmap) in barsInfo){
            // this will check to see if the user is within a certain range of the bar
            // if its 'close enough' ill count it as at that bar
            if(hashmap["lat"]!! <= lastKnownLocation?.latitude!! + .0001 && hashmap["lat"]!! >= lastKnownLocation?.latitude!! - .0001 &&
                hashmap["lng"]!! <= lastKnownLocation?.longitude!! + .0001 && hashmap["lng"]!! >= lastKnownLocation?.longitude!! - .0001){
                if(currentBar != ""){ // check if user was at a bar on the last check and update it
                    // this will decrement a person from the bar they were previously at
                    barsInfo.get(currentBar)!!.put("currentPopulation", barsInfo.get(currentBar)!!.get("currentPopulation")!! - 1)
                }

                //this will update current bar location and increment the population at that bar
                currentBar = name
                hashmap.put("currentPopulation", hashmap.get("currentPopulation")!! + 1)
            }
        }
        for((name, hashmap) in barsInfo){
            Log.i(TAG, name + " " + hashmap.get("currentPopulation").toString())
        }
        Log.i(TAG, "ENDING OF UPDATEBARPOP")
    }

    // location listener will get information about the device should it change
    // and update the global variable if needed and adjust where the map is looking
    private fun locationListener(): LocationListener {

        return object : LocationListener {
            // Called back when location changes
            override fun onLocationChanged(location: Location) {
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
    fun writeTextOnDrawable(drawableId: Int, text: String): Bitmap? {
        val drawable = this.applicationContext.getDrawable(drawableId)
        val bm = drawable?.toBitmap(width = 100, height = 100, config = null)
        val tf: Typeface = Typeface.create("Helvetica", Typeface.BOLD)
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.typeface = tf
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 50F
        val textRect = Rect()
        paint.getTextBounds(text, 0, text.length, textRect)
        val canvas = bm?.let { Canvas(it) }


        //Calculate the positions
        val xPos: Int = canvas!!.width / 2 - 2 //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2).toInt()
        canvas.drawText(text, xPos.toFloat(), yPos.toFloat(), paint)
        return bm
    }

    companion object {
        private const val TAG = "TAG"
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5

        //API Request types
        private const val FIND_BARS = 0
    }


}