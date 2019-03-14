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
        BikeCoordinates bikeCoord = new BikeCoordinates();
        bikeCoord.start();

        // Add a marker in Sydney and move the cameraV
        LatLng bikeLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(bikeLocation).title("Current Bike Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(bikeLocation));
    }




    public static class BikeCoordinates extends Thread{

        private String COORDINATE_ENDPOINT = "http://api.xdmtk.org/?reqcoords=1";
        private String coordinateString;

        double latitude;
        double longitude;

        public void run() {
            try {

                // Make HTTP request to get latest GPS string
                this.coordinateString = getCoordinateString();
            }
            catch (Exception e) {
                System.out.println(e.toString());
            }
            finally {

                // On good GPS data
                if (this.coordinateString.length() > 0) {

                    // Split the GPS string and parse
                    String[] coordinates = coordinateString.substring(
                            coordinateString.indexOf(":")).split(",");

                    // For warmed up GPS coords, split by comma should only have 5 values
                    if (coordinates.length <= 5) {
                        this.latitude = Double.valueOf(coordinates[3]);
                        this.longitude = Double.valueOf(coordinates[4]);
                    }
                    // For more than 5 comma values, bad GPS ( non warmed up ) data
                    else {
                        // Bad GPS string handle

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
