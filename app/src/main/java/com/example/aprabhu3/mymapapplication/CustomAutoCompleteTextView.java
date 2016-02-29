package com.example.aprabhu3.mymapapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aprabhu3 on 2/25/16.
 */


public class CustomAutoCompleteTextView extends AppCompatActivity implements OnItemClickListener, OnMapReadyCallback {

    private static final String LOG_TAG = "Places Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String TYPE_DETAILS = "/details";
    private static LatLng sourceLatLng = null;
    private static LatLng destinationLatLng = null;

    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyBqTaEQ22_PZPrTqYylk2KRuhcaRtqJQ4U";
    private static List<HashMap<String, String>> list = null;
    private static ArrayList placeIDArrayList;
    private GoogleMap mMap;
    private static Map markerList = new HashMap();
    private Utils utils = new Utils();
    List<List<HashMap<String, String>>> result = new ArrayList<>();
    private Double distance;
    private Context con;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        con=this;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AutoCompleteTextView sourceAutoCompView = (AutoCompleteTextView) findViewById(R.id.sourceAutoCompleteTextView);

        sourceAutoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        sourceAutoCompView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String placeID = placeIDArrayList.get(position).toString();
                hideKeyBoard();
                AsyncTaskRunner asyncTaskRunner = new AsyncTaskRunner();
                asyncTaskRunner.execute(placeID, "0");
                //int pos = Arrays.asList(regions).indexOf(selected);
            }
        });

        AutoCompleteTextView destinationAutoCompView = (AutoCompleteTextView) findViewById(R.id.destinationAutoCompleteTextView);

        destinationAutoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item_dest));
        destinationAutoCompView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String placeID = placeIDArrayList.get(position).toString();
                hideKeyBoard();
                AsyncTaskRunner asyncTaskRunner = new AsyncTaskRunner();
                asyncTaskRunner.execute(placeID, "1");
                //int pos = Arrays.asList(regions).indexOf(selected);
            }
        });
    }
    public void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }
    public double calculateAutoFare() {
        if (sourceLatLng != null && destinationLatLng != null) {
            DrawRouteAsync drawRouteAsync = new DrawRouteAsync();
            utils.clearMap();
            drawRouteAsync.execute(utils.makeURL(sourceLatLng.latitude, sourceLatLng.longitude,
                                        destinationLatLng.latitude, destinationLatLng.longitude));
        }
        return 0f;
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String json = null;

        @Override
        protected String doInBackground(String... params) {
            String jsonObject = findLatLng(params[0], params[1]);
            return jsonObject;
        }

        @Override
        protected void onPostExecute(String latlngStr) {
            String type = latlngStr.substring(latlngStr.indexOf("=") + 1);
            latlngStr = latlngStr.substring(latlngStr.indexOf("(") + 1, latlngStr.indexOf(")"));

            String[] latlong = latlngStr.split(",");
            double latitude = Double.parseDouble(latlong[0]);
            double longitude = Double.parseDouble(latlong[1]);
            LatLng latlng = new LatLng(latitude, longitude);
            Marker marker;
            if (type.equals("0")) {
                marker = mMap.addMarker(new MarkerOptions().position(latlng).title("Source"));
                if (markerList.get("source") != null) {

                    ((Marker) markerList.get("source")).remove();
                    markerList.remove("source");
                }
                markerList.put("source", marker);
            } else {
                marker = mMap.addMarker(new MarkerOptions().position(latlng).title("Destination"));
                if (markerList.get("destination") != null) {
                    ((Marker) markerList.get("destination")).remove();
                    markerList.remove("destination");
                }
                markerList.put("destination", marker);
            }


            mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
            calculateAutoFare();

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }

    public String findLatLng(String placeId, String type) { //type is 0 for source and 1 for destination

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAILS + OUT_JSON);
            sb.append("?key=" + API_KEY);

            sb.append("&placeid=" + placeId);

            URL url = new URL(sb.toString());
            Log.i(LOG_TAG, sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return null;
        } finally {
            if (conn != null) {
                //conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONObject jsonObject2 = ((JSONObject) ((JSONObject) ((JSONObject) jsonObj.get("result")).get("geometry")).get("location"));
            double lat = Double.parseDouble(jsonObject2.get("lat").toString());
            double lng = Double.parseDouble(jsonObject2.get("lng").toString());
            LatLng latlng = new LatLng(lat, lng);
            if (Integer.parseInt(type) == 0)
                sourceLatLng = new LatLng(lat, lng);

            else if (Integer.parseInt(type) == 1)
                destinationLatLng = new LatLng(lat, lng);


            return latlng.toString() + "type=" + type;


        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }


        return null;
    }

    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public static ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:IN");
            sb.append("&address=bengaluru");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            Log.i(LOG_TAG, sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            //Use the places from place detection API

            //  list = placeDetailsJSONParser.parse(jsonObj);
            // Extract the Place descriptions from the results
            placeIDArrayList = new ArrayList(predsJsonArray.length());

            resultList = new ArrayList(predsJsonArray.length());

            JSONObject obj;
            for (int i = 0; i < predsJsonArray.length(); i++) {
                obj = predsJsonArray.getJSONObject(i);
                placeIDArrayList.add(obj.get("place_id"));
                resultList.add(obj.get("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {

            Object a = resultList.get(index);
            return a.toString();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(CustomAutoCompleteTextView.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomAutoCompleteTextView.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
        mMap.setMyLocationEnabled(true);
    }

    private class DrawRouteAsync extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... url) {
            JSONParser jParser = new JSONParser();
            String jsonString = null;
            JSONArray jRoutes = null;
            try {
                jsonString = jParser.jsonFromURL(url[0]);
                JSONObject jsonObject = new JSONObject(jsonString);

                jRoutes = jsonObject.getJSONArray("routes");

                distance = utils.getDistance(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            result = jParser.parse(jRoutes);

            return null;
        }

        @Override
        protected void onPostExecute(String result2) {
            utils.drawPath(result, mMap);
            TextView textView = (TextView)findViewById(R.id.autoFare);
            double fare = utils.fareCalculation(distance);
            textView.setText(distance+" km -> Rs."+fare);
            new AlertDialog.Builder(con).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
                    .setTitle("AUTO FARE ESTIMATE")
                    .setMessage(String.format("%.2f km -> Rs. %.2f",distance, fare))
                    .show();
        }

    }


}






