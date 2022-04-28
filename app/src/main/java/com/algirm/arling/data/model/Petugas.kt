package com.algirm.arling.data.model

import android.location.Location
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Petugas(
    var name: String? = null,
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var ping: Boolean = false,
    @get:Exclude
    var altitude: Double = 0.0,
    @get:Exclude
    var uid: String? = null,
    @get:Exclude
    var address: String? = null
) {

    @Exclude
    fun getLocation(): Location {
        val loc = Location("Petugas")
        loc.latitude = lat
        loc.longitude = lon
        loc.altitude = altitude
        return loc
    }

}