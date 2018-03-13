package com.tlcn.mvpapplication.api.request;

/**
 * Created by apple on 3/6/18.
 */

public class LocationByDistanceRequest {
    private double latitude;
    private double longitude;
    private double distance;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
