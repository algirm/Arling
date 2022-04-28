package com.algirm.arling.util

object Constants {
    const val REQUEST_LOCATION_PERMISSIONS_CODE = 10
    const val REQUEST_CAMERA_PERMISSIONS_CODE = 11

    // Tracking Options
    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_UPDATE_INTERVAL = 5000L

    // Map Options
    const val MAP_ZOOM = 15f

    const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 0 // 10 meters

    const val MIN_TIME_BW_UPDATES: Long = 0 //1000 * 60 * 1; // 1 minute

    // Canvas
    const val CIRCLE_MAX_RADIUS = 30f
    const val CIRCLE_MIN_RADIUS = 10f

    const val TEXT_MAX_SIZE = 60f
    const val TEXT_MIN_SIZE = 10f

    const val TOUCH_RADIUS = 50

}