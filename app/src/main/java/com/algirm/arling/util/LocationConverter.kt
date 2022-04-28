package com.algirm.arling.util

import android.location.Location
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationConverter {
    private val WGS84_A = 6378137.0 // WGS 84 semi-major axis constant in meters

    private val WGS84_E2 = 0.00669437999014 // square of WGS 84 eccentricity


    fun WGS84toENU(curLoc: Location, poiLoc: Location): FloatArray {
        val radLat = Math.toRadians(curLoc.latitude)
        val radLon = Math.toRadians(curLoc.longitude)
        val radLat1 = Math.toRadians(poiLoc.latitude)
        val radLon1 = Math.toRadians(poiLoc.longitude)

        val clat = cos(radLat).toFloat()
        val slat = sin(radLat).toFloat()

        val X = sqrt(1.0 - WGS84_E2 * slat * slat).toFloat()
        val N = (WGS84_A * (1.0 - WGS84_E2) / (X * X * X)).toFloat()

        val dlat = (radLat1 - radLat).toFloat()
        val dlon = (radLon1 - radLon).toFloat()
        val dh = (poiLoc.altitude - curLoc.altitude).toFloat()
        val alt = curLoc.altitude.toFloat()

        val east =
            ((WGS84_A / X + alt) * clat * dlon - (N + alt) * slat * dlat * dlon + clat * dlon * dh).toFloat()
        val north =
            ((N + alt) * dlat + 1.5 * WGS84_A * clat * slat * WGS84_E2 * dlat * dlat + dh * dlat + 0.5 * slat * clat * (WGS84_A / X + alt) * dlon * dlon).toFloat()
        val up =
            (dh - 0.5 * (WGS84_A - 1.5 * WGS84_A * WGS84_E2 * clat * clat + 0.5 * WGS84_A * WGS84_E2 + alt) * dlat * dlat - 0.5 * clat * clat * (WGS84_A / X - alt) * dlon * dlon).toFloat()
        return floatArrayOf(east, north, up, 1f)
    }
}