package com.example.temp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.AsyncTask
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    lateinit var database: DatabaseReference  // Firebase DB reference

    lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(38.9897, -76.9378) // default for UMD CP
    private var locationPermissionGranted = false

    private var lat : Double = 38.9897 // defaults for UMD CP
    private var lng : Double = -76.9378 // defaults for UMD CP

    private lateinit var JSONParser : JSONParsersClass //outsource parsing to clean up clutter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        database = FirebaseDatabase.getInstance().reference

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
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
    }
    // this function is called when the google maps comes back to us ready to go (async)
    // Defaults for map should be made here instead of onCreate because map is NULL until
    // this function is called. So things like on click listeners or map properties will be
    // put in here
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap // set our global map value
        map.addMarker( // tester
            MarkerOptions()
                .position(defaultLocation)
                .title("Marker in UMD CP")
                .snippet("WHY HELLO THERE")
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
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        // change some map defaults, or this will kill app if location is not turned on
        // since location is required for this app to work (for now, this is something i made up
        // arbitrarily, we can make it different if we want)
        updateLocationUI()
        // figure out where we are right now
        // this might be removed if we decide to just roll with CP area
        getDeviceLocation()
        // create url to make api request for bars in our location at 5000 meter radius
        var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + lat + "," + lng +
                "&radius=5000" + "&type=" + "bar" +
                "&key=" + resources.getString(R.string.google_maps_key);

        // now that map is finally initalized and db initalized in oncreate, we can give
        // a reference to this specific class object to the parser
        JSONParser = JSONParsersClass(this)
        // this will exectue the api request and also deal with placing markers on map where bars
        // are and with the relevant information
        JSONParser.MakeAPIRequest(FIND_BARS).execute(url)
    }


    // function will set up defaults/settings for maps, like a UI init function
    // for now will kill the app if location permission not granted
    @SuppressLint("MissingPermission")
    // suppress missing permission for line   map?.isMyLocationEnabled = true because we know if
    // we made it into the if statement that we have permission, so it will never error out
    private fun updateLocationUI() {

        if (locationPermissionGranted) { // checks if we have permission
            // if we do, these two settings will enable the little icon where we are and
            // a button in the top right corner that when clicked will center us back on our
            // location
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
        } else { // otherwise we'll kill the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity();
            } else {
                finish();
            }
        }
    }

    // this function uses built in service fusedLocationProviderClient to locate most recent
    // latitude and longitude of deivce. It will then assign that to our lat lng variables
    // and move the camera to be over the point
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        Log.i(TAG, "Getting Device Location.")
        try {
            if (locationPermissionGranted) { // check if we are allowed location
                // use built in function to get location
                val locationResult = fusedLocationProviderClient.lastLocation
                Log.i(TAG, "Waiting for task.")
                // when async function comes back operate on it
                locationResult.addOnCompleteListener(this) { task ->
                    // we found a location
                    if (!task.isSuccessful) {
                        Log.i(TAG, "Task Successful.")
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            lat = lastKnownLocation!!.latitude
                            lng = lastKnownLocation!!.longitude
                            Log.d(TAG, "Location non-null")
                            // Set the map's camera position to the current location of the device.
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else { // no location was found for some reason so we use CP defaults
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        //map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.i(TAG, "Location permission turned OFF")
            Log.e("Exception: %s", e.message, e)
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
                }
                // if we get permission we flip our flag to true
                // otherwise we leave it as be and we act accordingly elsewhere depending on flag
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