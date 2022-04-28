package com.algirm.arling;

/**
 * Created by ntdat on 1/13/17.
 */

public class LocationHelperTest2 {
    private final static double WGS84_A = 6378137.0;                  // WGS 84 semi-major axis constant in meters
    private final static double WGS84_E2 = 0.00669437999014;          // square of WGS 84 eccentricity

    public static float[] WSG84toECEF(Double lat, Double lon, Double alt) {
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);

        float clat = (float) Math.cos(radLat);
        float slat = (float) Math.sin(radLat);
        float clon = (float) Math.cos(radLon);
        float slon = (float) Math.sin(radLon);

        float N = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));

        float x = (float) ((N + alt) * clat * clon);
        float y = (float) ((N + alt) * clat * slon);
        float z = (float) ((N * (1.0 - WGS84_E2) + alt) * slat);

        return new float[] {x , y, z};
    }

    public static float[] ECEFtoENU(Double lat, Double lon, Double alt, Double lat1, Double lon1, Double alt1) {
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double radLat1 = Math.toRadians(lat1);
        double radLon1 = Math.toRadians(lon1);

        float clat = (float)Math.cos(radLat);
        float slat = (float)Math.sin(radLat);

        float X = (float) (Math.sqrt(1.0 - WGS84_E2 * slat * slat));
//        float X = (float) (WGS84_A / Math.sqrt(1.0 - WGS84_E2 * slat * slat));

//        float dx = (float) ((((-WGS84_A * clon * slat * (1.0 - WGS84_E2)) / X) - alt * clon * slat)
//                - (WGS84_A * slon * clat / X + alt * slon * clat) + clat * clon + WGS84_A / 4
//                * clat * clon * (-2 - 7 * WGS84_E2 + 9 * WGS84_E2 * clat * clat) - alt / 2 * clon
//                * clat);

        float dlat = (float) (radLat1 - radLat);
        float dlon = (float) (radLon1 - radLon);
        float dh = (float) (alt1-alt);

        float east = (float) ((WGS84_A/X+alt)*clat*dlon - ((WGS84_A*(1.0-WGS84_E2)/(X*X*X))+alt)*slat*dlat*dlon+clat*dlon*dh);
//        float north = (float) ((WGS84_A*(1.0-WGS84_E2)/X*X*X+alt)*dlat+1.5*clat*slat*WGS84_A*WGS84_E2*dlat*dlat+slat*slat*dh*dlat
//                +0.5*slat*clat*(WGS84_A/X+alt)*dlon*dlon);
//        float north1 = (float) ((((WGS84_A*(1.0-WGS84_E2)/(X*X*X)) + alt)*dlat) + (1.5*clat*slat*WGS84_A*WGS84_E2*(dlat*dlat)) + ((slat*slat)*dh*dlat) +
//                (0.5*slat*clat*((WGS84_A/X)+alt)*(dlon*dlon)));

//        float north = (float) (((((WGS84_A-(WGS84_A*(-WGS84_E2)))/(X*X*X))+alt)*dlat)+(1.5*clat*slat*WGS84_A*WGS84_E2*(dlat*dlat))+((slat*slat)*dh*dlat) +
//                (0.5*slat*clat*((WGS84_A/X)+alt)*(dlon*dlon)));
        float north = (float) (((WGS84_A*(1.0-WGS84_E2))/(X*X*X)+alt)*dlat+1.5*WGS84_A*clat*slat*WGS84_E2*dlat*dlat+dh*dlat +
                0.5*slat*clat*((WGS84_A/X)+alt)*dlon*dlon);

        float up = (float) (dh-0.5*(WGS84_A-1.5*WGS84_A*WGS84_E2*clat*clat+0.5*WGS84_A*WGS84_E2+alt)*dlat*dlat-0.5*clat*clat*(WGS84_A/X-alt)*dlon*dlon);

        return new float[] {east , north, up, 1};
    }

}
