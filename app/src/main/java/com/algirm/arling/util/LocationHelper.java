package com.algirm.arling.util;

import android.location.Location;

/**
 * Created by ntdat on 1/13/17.
 */

public class LocationHelper {
    private final static double WGS84_A = 6378137.0;                  // WGS 84 semi-major axis constant in meters
    private final static double WGS84_E2 = 0.00669437999014;          // square of WGS 84 eccentricity

    public static float[] WGS84toENU(Location curLoc, Location poiLoc) {
        double radLat = Math.toRadians(curLoc.getLatitude());
        double radLon = Math.toRadians(curLoc.getLongitude());
        double radLat1 = Math.toRadians(poiLoc.getLatitude());
        double radLon1 = Math.toRadians(poiLoc.getLongitude());

        float clat = (float) Math.cos(radLat);
        float slat = (float) Math.sin(radLat);

        float X = (float) (Math.sqrt(1.0 - WGS84_E2 * slat * slat));
        float N = (float) (WGS84_A * (1.0 - WGS84_E2) / (X * X * X));

        float dlat = (float) (radLat1 - radLat);
        float dlon = (float) (radLon1 - radLon);
        float dh = (float) (poiLoc.getAltitude() - curLoc.getAltitude());
        float alt = (float) curLoc.getAltitude();

        float east = (float) ((WGS84_A / X + alt) * clat * dlon - (N + alt) * slat * dlat * dlon + clat * dlon * dh);
        float north = (float) ((N + alt) * dlat + 1.5 * WGS84_A * clat * slat * WGS84_E2 * dlat * dlat + dh * dlat + 0.5 * slat * clat * ((WGS84_A / X) + alt) * dlon * dlon);
        float up = (float) (dh - 0.5 * (WGS84_A - 1.5 * WGS84_A * WGS84_E2 * clat * clat + 0.5 * WGS84_A * WGS84_E2 + alt) * dlat * dlat - 0.5 * clat * clat * (WGS84_A / X - alt) * dlon * dlon);

        return new float[]{east, north, up, 1};
    }

}
