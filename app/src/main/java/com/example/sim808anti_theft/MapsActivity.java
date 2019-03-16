package com.example.sim808anti_theft;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.EOFException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String XDMTK_API_KEY = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        BikeCoordinates bikeCoord = new BikeCoordinates(mMap);
        bikeCoord.start();
    }





    public class BikeCoordinates extends Thread{

        private String COORDINATE_ENDPOINT = "http://api.xdmtk.org/?reqcoords=1";
        public GoogleMap myMap;
        public String coordinateString;


        public BikeCoordinates(GoogleMap m) {
            this.myMap = m;

        }


        private void moveToCurrentLocation(GoogleMap myMapP, LatLng currentLocation)
        {
            myMapP.addMarker(new MarkerOptions().position(currentLocation).title("Current Bike Location"));
            myMapP.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,15));
            // Zoom in, animating the camera.
            myMapP.animateCamera(CameraUpdateFactory.zoomIn());
            // Zoom out to zoom level 10, animating with a duration of 2 seconds.
            myMapP.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);


        }

        public void run() {
            try {
                coordinateString = getCoordinateString();
            }
            catch (Exception e) {
                System.out.println(e);
            }
            finally {
                while (true) {
                    runOnUiThread(new Runnable() {
                        double latitude;
                        double longitude;

                        @Override
                        public void run() {
                            // On good GPS data
                            if (coordinateString != null && coordinateString.length() > 0) {

                                // Split the GPS string and parse
                                String[] coordinates = coordinateString.substring(
                                        coordinateString.indexOf(":")).split(",");

                                // For warmed up GPS coordinates, split by comma should only have 5 values
                                if (coordinates.length <= 5) {
                                    LatLng currentCoordinates = new LatLng(Double.valueOf(coordinates[3]), Double.valueOf(coordinates[4]));
                                    moveToCurrentLocation(myMap, currentCoordinates);
                                }
                            }
                        }
                    });
                    try {
                        Thread.sleep(3000);
                    }
                    catch (Exception e) {
                        System.out.print(e);
                    }
                }
            }
        }


        public String getCoordinateString() throws IOException {

            // Setup HTTP context
            URL url = new URL(COORDINATE_ENDPOINT);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // Connect and get status
            con.connect();
            int status = con.getResponseCode();

            if (status < 299) {

                // Read HTTP response data
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));

                // Build the buffer 'content' with successive reads on
                // the InputStreamReader 'in'
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                // When finished, close reader and return response
                in.close();
                System.out.println("Response content: " + content.toString());
                return content.toString();
            }
            else {
                return "Bad request";
            }
        }
    }

}
