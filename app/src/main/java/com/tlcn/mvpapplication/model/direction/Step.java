package com.tlcn.mvpapplication.model.direction;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tlcn.mvpapplication.model.CustomLatLng;
import com.tlcn.mvpapplication.model.Locations;
import com.tlcn.mvpapplication.utils.DecodePolyLine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tlcn.mvpapplication.utils.KeyUtils.checkLevel;

/**
 * Created by ducthinh on 17/09/2017.
 */

public class Step implements Serializable {

    @SerializedName("distance")
    @Expose
    private Distance distance;
    @SerializedName("duration")
    @Expose
    private Duration duration;
    @SerializedName("end_location")
    @Expose
    private Location endLocation;
    @SerializedName("start_location")
    @Expose
    private Location startLocation;
    @SerializedName("polyline")
    @Expose
    private Polyline polyline;
    @SerializedName("travel_mode")
    @Expose
    private String travelMode;
    @SerializedName("html_instructions")
    @Expose
    private String description;
    @SerializedName("maneuver")
    @Expose
    private String maneuver;

    private List<CustomLatLng> customLatLng;

    private List<Locations> locations = new ArrayList<>();

    public List<Locations> getLocations() {
        return locations;
    }

    public void setLocations(List<Locations> locations) {
        this.locations = locations;
    }

    public void addLocation(Locations locations) {
        if (locations != null)
            this.locations.add(locations);
    }

    /**
     * @param newLocation is a location want to check
     * @return 0: list location not change
     * 1: add a new location to list
     * 2: set location to list with current level is increase
     * 3: set location to list with current level is reduction
     */
    public int checkAddLocation(Locations newLocation) {
        if (locations != null) {
            if (locations.size() == 0) {
                locations.add(newLocation);
                return 1;
            } else {
                for (int i = 0; i < locations.size(); i++) {
                    Locations current = locations.get(i);
                    if (Objects.equals(current.getId(), newLocation.getId())) {
                        if (checkLevel(newLocation.getCurrent_level()) > checkLevel(current.getCurrent_level())) {
                            locations.set(i, newLocation);
                            return 2;
                        } else if (checkLevel(newLocation.getCurrent_level()) < checkLevel(current.getCurrent_level())) {
                            locations.set(i, newLocation);
                            return 3;
                        } else return 0;
                    }
                }
                locations.add(newLocation);
                return 1;
            }
        } else return 0;
    }

    public Distance getDistance() {
        return distance;
    }

    public String getDescription() {
        return description;
    }


    public List<LatLng> getPoints() {
        if (this.polyline == null)
            return new ArrayList<>();
        return DecodePolyLine.decodePolyLine(this.polyline.getPoints());
    }

    public int getCurrentLevel() {
        int result = 0;
        if (locations.size() != 0) {
            for (Locations location : locations) {
                result += location.getCurrent_level();
            }
        }
        return result;
    }

    public void createMarkPlace() {
        customLatLng = new ArrayList<>();
        for (LatLng latLng : DecodePolyLine.decodePolyLine(polyline.getPoints())) {
            customLatLng.add(new CustomLatLng(latLng));
        }
    }

    public List<CustomLatLng> getCustomLatLng() {
        return customLatLng;
    }

    public int getCountLocationPassed() {
        int count = 0;
        for (CustomLatLng customLatLng : customLatLng) {
            if (customLatLng.getState() == 1)
                count++;
        }
        return count;
    }

    public List<LatLng> getLatLngNonePass() {
        List<LatLng> temp = new ArrayList<>();
        for (CustomLatLng customLatLng : customLatLng) {
            if (customLatLng.getState() == 0)
                temp.add(customLatLng.getLatLng());
        }
        return temp;
    }

    public List<CustomLatLng> getLocationNonePass() {
        List<CustomLatLng> temp = new ArrayList<>();
        for (CustomLatLng customLatLng : customLatLng) {
            if (customLatLng.getState() == 0) {
                temp.add(customLatLng);
            }
        }
        return temp;
    }

    public CustomLatLng getStartLocation() {
        return getLocationNonePass().size() > 0 ? getLocationNonePass().get(0) : null;
    }

    public CustomLatLng getEndLocation() {
        return getLocationNonePass().size() > 1 ? getLocationNonePass().get(1) : null;
    }
}
