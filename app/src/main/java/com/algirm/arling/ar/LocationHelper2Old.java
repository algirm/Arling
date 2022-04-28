package com.algirm.arling.ar;

import android.location.Location;

/**
 * Created by ntdat on 1/13/17.
 */

public class LocationHelper2Old {
    private final static double WGS84_A = 6378137.0;                  // WGS 84 semi-major axis constant in meters
    private final static double WGS84_E2 = 0.00669437999014;          // square of WGS 84 eccentricity

    public static float[] WSG84toECEF(Location loc) {
        double radLat = Math.toRadians(loc.getLatitude());
        double radLon = Math.toRadians(loc.getLongitude());

        float clat = (float) Math.cos(radLat);
        float slat = (float) Math.sin(radLat);
        float clon = (float) Math.cos(radLon);
        float slon = (float) Math.sin(radLon);

        float N = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));

        float x = (float) ((N + loc.getAltitude()) * clat * clon);
        float y = (float) ((N + loc.getAltitude()) * clat * slon);
        float z = (float) ((N * (1.0 - WGS84_E2) + loc.getAltitude()) * slat);

        return new float[] {x , y, z};
    }

    public static float[] ECEFtoENU(Location curLoc, Location poiLoc) {
        double radLat = Math.toRadians(curLoc.getLatitude());
        double radLon = Math.toRadians(curLoc.getLongitude());
        double radLat1 = Math.toRadians(poiLoc.getLatitude());
        double radLon1 = Math.toRadians(poiLoc.getLongitude());

        float clat = (float)Math.cos(radLat);
        float slat = (float)Math.sin(radLat);
//        float clon = (float)Math.cos(radLon);
//        float slon = (float)Math.sin(radLon);
//
//        float clat1 = (float)Math.cos(radLat1);
//        float slat1 = (float)Math.sin(radLat1);
//        float clon1 = (float)Math.cos(radLon1);
//        float slon1 = (float)Math.sin(radLon1);

//        float N = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));
        float X = (float) (Math.sqrt(1.0 - WGS84_E2 * slat * slat));

//        float dx = (float) ((((-WGS84_A * clon * slat * (1.0 - WGS84_E2)) / X) - alt * clon * slat)
//                - (WGS84_A * slon * clat / X + alt * slon * clat) + clat * clon + WGS84_A / 4
//                * clat * clon * (-2 - 7 * WGS84_E2 + 9 * WGS84_E2 * clat * clat) - alt / 2 * clon
//                * clat);

//        float dx = ecefCurrentLocation[0] - ecefPOI[0];
//        float dy = ecefCurrentLocation[1] - ecefPOI[1];
//        float dz = ecefCurrentLocation[2] - ecefPOI[2];

        float dlat = (float) (radLat1 - radLat);
        float dlon = (float) (radLon1 - radLon);
        float dh = (float) (poiLoc.getAltitude()-curLoc.getAltitude());
        float alt = (float) curLoc.getAltitude();

        float east = (float) ((WGS84_A/X+alt)*clat*dlon - (WGS84_A*(1.0-WGS84_E2)/(X*X*X)+alt)*slat*dlat*dlon+clat*dlon*dh);
//        float north = (float) ((WGS84_A*(1.0-WGS84_E2)/X*X*X+alt)*dlat+1.5*clat*slat*WGS84_A*WGS84_E2*dlat*dlat+slat*slat*dh*dlat
//                +0.5*slat*clat*(WGS84_A/X+alt)*dlon*dlon);
        float north = (float) ((((WGS84_A*(1.0-WGS84_E2)/(X*X*X)) + alt)*dlat) + (1.5*clat*slat*WGS84_A*WGS84_E2*(dlat*dlat)) + ((slat*slat)*dh*dlat) +
                (0.5*slat*clat*((WGS84_A/X)+alt)*(dlon*dlon)));
        float up = (float) (dh-0.5*(WGS84_A-1.5*WGS84_A*WGS84_E2*clat*clat+0.5*WGS84_A*WGS84_E2+alt)*dlat*dlat-0.5*clat*clat*(WGS84_A/X-alt)*dlon*dlon);
//
//        float east = -slon*dx + clon*dy;
//
//        float north = -slat*clon*dx - slat*slon*dy + clat*dz;
//
//        float up = clat*clon*dx + clat*slon*dy + slat*dz;

        return new float[] {east , north, up, 1};
    }

}
