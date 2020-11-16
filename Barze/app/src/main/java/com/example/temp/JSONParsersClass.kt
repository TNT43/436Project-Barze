package com.example.temp

import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class JSONParsersClass(mapActivityRef : MapsActivity) {

    private var mMapActivityRef = mapActivityRef
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
            Log.i(TAG, "JSONOBJECT: " + jsonObject.toString(jsonObject.length()))
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
    inner class NearbyBarsJSONParser : AsyncTask<String, Int, List<HashMap<String, String>>>(){
        // this class will take the json array, and parse each bar into a list of hashmaps
        // each element of the list will be a different bar, and the key/value pairs of the hashmap
        // will be variable names and values from the json object associated with that bar
        // it will then give this list of hashmaps to post to allow us to interact with data
        override fun doInBackground(vararg params: String?): List<HashMap<String, String>> {
            var mapList: ArrayList<HashMap<String, String>> // set up location for parsing
            var obj = JSONObject(params[0]) //json is passed in as string and converted to Json object
            mapList = parseBars(obj) as ArrayList<HashMap<String, String>> // parse the json into map
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

                    mMapActivityRef.map.addMarker(
                        MarkerOptions()
                            .position(latlng)
                            .title(name)
                            .snippet("WHY HELLO THERE")
                            .icon(BitmapDescriptorFactory.fromBitmap(mMapActivityRef.writeTextOnDrawable(R.drawable.rectangle, rat!!)))
                    )
                }
            }
        }

        //parses to | [name of key] | [value inside] actual below
        //          |     name      | name of bar
        //          |      lat      | latitude of bar
        //          |      lng      | longitude of bar
        //          |      rat      | Google maps rating of bar
        // there are other things in the Json response we could take out if we wanted to
        private fun parseBars(obj : JSONObject) : List<HashMap<String, String>>{
            // grab array we want out of massive original json object
            var jsonArray = obj.getJSONArray("results")
            var dataList = ArrayList<HashMap<String, String>>()
            // each element in json array is another json object for a different bar
            for(i in 0 until jsonArray.length()){
                var currObj = jsonArray.get(i) as JSONObject
                var dataMap = HashMap<String, String>()
                var name = currObj.getString("name")
                var lat = currObj.getJSONObject("geometry").getJSONObject("location").getString("lat")
                var lng = currObj.getJSONObject("geometry").getJSONObject("location").getString("lng")
                var rat = currObj.getString("rating")
                dataMap.put("name", name)
                dataMap.put("lat", lat)
                dataMap.put("lng", lng)
                dataMap.put("rat", rat)
                //Log.i(TAG,"Adding to db")
                //mMapActivityRef.database.child("Bars").child(name).setValue(dataList)
                dataList.add(dataMap)
            }
            return dataList
        }

    }



    companion object {
        private const val TAG = "TAG"

        //API Request types
        private const val FIND_BARS = 0
    }
}