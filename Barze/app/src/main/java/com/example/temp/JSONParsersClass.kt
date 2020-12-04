package com.example.temp

import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

 class JSONParsersClass(mapActivityRef : MapsActivity) {

     private var mMapActivityRef = mapActivityRef
     var mapMarkers = java.util.ArrayList<Marker>()
    // so pretty much this classes whole job is to take the URL given to it, make the API request
    // and give the resulting json to the appropriate parsing class
    // I decided to refractor it this way to make it easier to try and parse an API request of a
    // different type, like getting directions (you might not see it but i tried and it was very
    // hard to get working with the original layout)
    // API Type requests are constant int values defined at bottom of both mapsactivity and this class
    inner class MakeAPIRequest(requestType : Int) : AsyncTask<String, Int, String>() {
        private val request = requestType
        override fun doInBackground(vararg  args: String) : String? {
            var url = args[0]
            val request = Request.Builder().url(url).build()
            val response = OkHttpClient().newCall(request).execute().body?.string()
            val jsonObject = JSONObject(response)
            //Log.i(TAG, "JSONOBJECT: " + jsonObject.toString(jsonObject.length()))
            return jsonObject.toString()
        }

        // post will take json array and send it to json parser
        override fun onPostExecute(result: String?) {
            when(request){
                FIND_BARS -> NearbyBarsJSONParser().execute(result)
            }
        }
    }
    // parser class for json objects associated with finding nearby bars
    inner class NearbyBarsJSONParser : AsyncTask<String, Int, JSONObject>(){
        // this class will take the json array, and parse each bar into a list of hashmaps
        // each element of the list will be a different bar, and the key/value pairs of the hashmap
        // will be variable names and values from the json object associated with that bar
        // it will then give this list of hashmaps to post to allow us to interact with data
        override fun doInBackground(vararg params: String?): JSONObject {
            var obj = JSONObject(params[0]) //json is passed in as string and converted to Json object
            return obj
        }
        override fun onPostExecute(result: JSONObject) {
            //parses to | [name of key] | [value inside] actual below
            //          |     name      | name of bar
            //          |      lat      | latitude of bar
            //          |      lng      | longitude of bar
            //          |      rat      | Google maps rating of bar
            // there are other things in the Json response we could take out if we wanted to
            // grab array we want out of massive original json object
            var jsonArray = result.getJSONArray("results")
            var fullMap = HashMap<String, HashMap<String, Double>>()
            // each element in json array is another json object for a different bar
            for(i in 0 until jsonArray.length()){
                var currObj = jsonArray.get(i) as JSONObject
                var dataMap = HashMap<String, Double>()
                var name = currObj.getString("name")
                // this will go through the list of bars we've handpicked to keep on the DB
                // barList is initalized previously with keys taken from live DB
                // it will parse the json object for the allowed bar and then put an icon
                // on the map with information about the bar. It will keep a list of references
                // to each marker so they can be updated later
                if(barList.containsKey(name)){
                    var lat = currObj.getJSONObject("geometry").getJSONObject("location").getString("lat")
                    var lng = currObj.getJSONObject("geometry").getJSONObject("location").getString("lng")
                    var rat = currObj.getString("rating")
                    dataMap.put("lat", lat.toDouble())
                    dataMap.put("lng", lng.toDouble())
                    dataMap.put("rat", rat.toDouble())
                    dataMap.put("currPop", barList[name]!!)
                    //Log.i(TAG,"Adding to db")
                    //mMapActivityRef.database.child("Bars").child(name).setValue(dataMap)
                    fullMap.put(name, dataMap)

                    var latlng = LatLng(lat.toDouble(), lng.toDouble())
                    // create a marker to place on the map with relevant information, and store the marker
                    var marker = mMapActivityRef.map.addMarker(
                        MarkerOptions()
                            .position(latlng)
                            .title(name)
                            .snippet(barList[name]!!.toInt().toString() + " other users here")
                            .icon(BitmapDescriptorFactory.fromBitmap(mMapActivityRef.createMarkers(R.drawable.rectangle, rat.toString())))
                    )
                    // add to list of map markers
                    // this is done so we can go back and change data on map markers later
                    mapMarkers.add(marker)
                }
            }
            mMapActivityRef.barsInfo = fullMap
            mMapActivityRef.updateBarPop()
        }// end of postExecute
    }// end of inner class


     private var barList = HashMap<String, Double>()

     // update info will poll the firebase for most up to date info and provide the callback object
     // that will go through each map marker and update it with the most current count of user pop
     fun updateInfo(){
         var database = FirebaseDatabase.getInstance().reference
         var createBarList: ValueEventListener = object : ValueEventListener {
             override fun onDataChange(dataSnapshot: DataSnapshot) {
                 barList = HashMap<String, Double>()
                 Log.i(TAG, "Updating Data to match firebase")
                 // go through current data snapshot and pull out current information on each bar
                 if(dataSnapshot.children.iterator().hasNext()){
                     val listIndex = dataSnapshot.children.iterator().next()
                     val itemsIterator = listIndex.children.iterator()
                     while (itemsIterator.hasNext()){
                         val cur = itemsIterator.next()
                         val map = cur.getValue() as HashMap<*, *>
                         barList.put(map.get("name").toString(), map.get("currPop").toString().toDouble())
                         mMapActivityRef.barsInfo[map.get("name").toString()]?.put("currPop", map.get("currPop").toString().toDouble())
                     }
                 }
                 // go through each map marker and update its snippet info
                 for(curr in mapMarkers){
                     curr.snippet = barList.get(curr.title)!!.toInt().toString() +
                             " other users here"
                 }
             }
             override fun onCancelled(databaseError: DatabaseError) {
                 // Getting Item failed, log a message
                 Log.w("TAG", "loadItem:onCancelled", databaseError.toException())
             }
         }
         database.addListenerForSingleValueEvent(createBarList)

     }


    companion object {
        private const val TAG = "TAG"

        //API Request types
        private const val FIND_BARS = 0

    }
}