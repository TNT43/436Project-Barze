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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var database: DatabaseReference  // Firebase DB reference

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(38.9897, -76.9378) // default for UMD CP
    private var locationPermissionGranted = false

    private var lat : Double = 38.9897 // defaults for UMD CP
    private var lng : Double = -76.9378 // defaults for UMD CP

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

        // this will exectue the api request and also deal with placing markers on map where bars
        // are and with the relevant information
        FindNearbyBarsAsync().execute(url)
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

    // next 3 inner classes are for finding the nearby bars and parsing the returned json array
    // everything is done Async to allow user to use app while calls to API are being made
    inner class FindNearbyBarsAsync : AsyncTask<String, Int, String> () {

        // make a call to google API using a custom library i found called OkHttpClient
        // api call will return a json array
        // we'll then turn the json array into a string and send to the post
        override fun doInBackground(vararg  args: String) : String? {
            var url = args[0]
            val request = Request.Builder().url(url).build()
            val response = OkHttpClient().newCall(request).execute().body?.string()
            val jsonObject = JSONObject(response)
            Log.i(TAG, "JSONOBJECT: " + jsonObject.toString(jsonObject.length()))
            return jsonObject.toString()
        }

        // post will take json array and send it to json parser
        override fun onPostExecute(result: String?) {
            JSONParserAsync().execute(result)
        }
    }
    inner class JSONParserAsync : AsyncTask<String, Int, List<HashMap<String, String>>> (){
        // this class will take the json array, and parse each bar into a list of hashmaps
        // each element of the list will be a different bar, and the key/value pairs of the hashmap
        // will be variable names and values from the json object associated with that bar
        // it will then give this list of hashmaps to post to allow us to interact with data
        override fun doInBackground(vararg params: String?): List<HashMap<String, String>> {
            var jsonParser = JsonParser() // create object of type custom JsonParser defined below
            var mapList: ArrayList<HashMap<String, String>> // set up location for parsing
            var obj = JSONObject(params[0]) //json is passed in as string and converted to Json object
            mapList = jsonParser.parseResult(obj) as ArrayList<HashMap<String, String>> // parse the json into map
            //parses to | [name of key] | [value inside] actual below
            //          |     name      | name of bar
            //          |      lat      | latitude of bar
            //          |      lng      | longitude of bar
            //          |      rat      | Google maps rating of bar
            // there are other things in the Json response we could take out if we wanted to
            // putting a whole response for 1 location is a lot for comments so i recommend a google
            return mapList
        }
        // post will go through each element of list, which each is just a different bar
        // and will create a marker on the map at appropriate lat/lng as  well as placing
        // the name above the marker WHEN CLICKED as well as custom snippet
        // function also makes each marker a custom marker defined in R.drawable.rectangle
        // which is just a blue square with the rating of the bar written in the center as white text
        override fun onPostExecute(result: List<HashMap<String, String>>?) {
            //map.clear()
            if (result != null) {
                for(i in result.indices){
                    var hashMapList = result.get(i)
                    var lat = hashMapList.get("lat")!!.toDouble()
                    var lng = hashMapList.get("lng")!!.toDouble()
                    var name = hashMapList.get("name")
                    var rat = hashMapList.get("rat")
                    var latlng = LatLng(lat, lng)

                    map.addMarker(
                        MarkerOptions()
                            .position(latlng)
                            .title(name)
                            .snippet("WHY HELLO THERE")
                            .icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable(R.drawable.rectangle, rat!!)))
                    )
                }
            }
        }

    }
    inner class JsonParser{
        // this function simply extracts the results array from the json object
        fun parseResult(obj : JSONObject) : List<HashMap<String, String>> {
            var jsonArray = obj.getJSONArray("results")
            return parseJsonArray(jsonArray)
        }

        // this function parses and single json object. each object has certain data that we want
        // and can be extracted this way. you can look at logcat with using tag TAG to see an example
        // of how some of the json objects look and what other data is stored in them
        private fun parseJsonObject(obj : JSONObject) : HashMap<String, String> {
            var dataList = HashMap<String, String>()
            var name = obj.getString("name")



            var lat = obj.getJSONObject("geometry").getJSONObject("location").getString("lat")
            var lng = obj.getJSONObject("geometry").getJSONObject("location").getString("lng")
            var rat = obj.getString("rating")
            dataList.put("name", name)
            dataList.put("lat", lat)
            dataList.put("lng", lng)
            dataList.put("rat", rat)

            /// This is commented out as the nodes already exist in the DB, don't want any duplicates.
            //Log.i(TAG,"Adding to db")
           // database.child("Bars").child(name).setValue(dataList)

            return dataList
        }
        // this function goes through each object in the json array and parses it. each object
        // is associated with a different bar returned by google.
        private fun parseJsonArray(obj : JSONArray) : List<HashMap<String, String>> {
            var dataList = ArrayList<HashMap<String, String>>()
            for(i in 0 until obj.length()){
                var data = parseJsonObject(obj.get(i) as JSONObject)
                dataList.add(data)
            }
            return dataList
        }
    }

    // this function simply create a small box ui with the text of the bars rating in the center
    // it will return a bitmap that can be used to draw onto the map
    private fun writeTextOnDrawable(drawableId: Int, text: String): Bitmap? {
        val drawable = this.applicationContext.getDrawable(R.drawable.rectangle)
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
    }
}