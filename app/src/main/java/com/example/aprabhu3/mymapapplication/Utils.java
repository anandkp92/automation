package com.example.aprabhu3.mymapapplication;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aprakash2 on 2/25/16.
 */
public class Utils {
    private static Polyline polyline;
    public LatLng getLatLongFromPlace(Place place){
        return place.getLatLng();
    }

    public double getDistance(JSONObject jsonObject) throws JSONException {
        JSONObject jsonObject1 = jsonObject.getJSONArray("routes").getJSONObject(0);
        jsonObject1 = jsonObject1.getJSONArray("legs").getJSONObject(0);
        jsonObject1 = (JSONObject) jsonObject1.get("distance");

        String distance = jsonObject1.get("text").toString();
        distance = distance.replace(",","");
        return Double.parseDouble(distance.substring(0, distance.indexOf(' ')));
    }

    public void drawPath(List<List<HashMap<String, String>>> result, GoogleMap mMap) {

        //Tranform the string into a json object
        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;
        MarkerOptions markerOptions = new MarkerOptions();
        for(int i=0;i<result.size();i++){
            points = new ArrayList<LatLng>();
            lineOptions = new PolylineOptions();

            // Fetching i-th route
            List<HashMap<String, String>> path = result.get(i);

            // Fetching all the points in i-th route
            for(int j=0;j<path.size();j++){
                HashMap<String,String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            lineOptions.width(20);
            lineOptions.color(Color.BLUE);
        }

        // Drawing polyline in the Google Map for the i-th route
        polyline = mMap.addPolyline(lineOptions);
    }

    public Double fareCalculation(double distance){
        Double fare = 0.0d;

        if(distance<=1.9){
            fare = 25d;
        }
        else {
            fare = 25d + (distance-1.9)*13;
        }

        return fare;
    }

    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyCroODapTLqWYuOz74lPMsvGfWhdXtLmq0");
        return urlString.toString();
    }

    public void clearMap(){
        if(polyline!=null) {
            polyline.remove();
        }
    }
}
