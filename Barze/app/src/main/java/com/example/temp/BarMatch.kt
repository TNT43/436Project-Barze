package com.example.temp

import android.widget.Toast

public class BarMatch {

    // Utility to handle displaying of stock photos for bars in College Park
    public fun MatchImageWithName(name:String): Int {
        when(name) {
            "Buffalo Wild Wings" -> return R.drawable.bww
            "RJ Bentley's" -> return R.drawable.bents
            "Cornerstone Grill & Loft" -> return R.drawable.cstone
            "Looney's Pub" ->return R.drawable.loons
            "Old Dominion BrewHouse" -> return R.drawable.old
            "TGI Fridays" ->return R.drawable.tgif
            "Terrapin's Turf" ->return R.drawable.turf
            "The Common" ->return R.drawable.com
            else ->return R.drawable.placeholder

        }
    }
}