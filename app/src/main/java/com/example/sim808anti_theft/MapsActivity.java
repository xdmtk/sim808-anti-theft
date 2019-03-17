package com.example.sim808anti_theft;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

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
    public BikeCoordinates bikeCoord;

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

        bikeCoord = new BikeCoordinates(mMap);
        bikeCoord.start();

    }




    public class BikeCoordinates extends Thread implements GoogleMap.OnCameraMoveStartedListener{

        private String COORDINATE_ENDPOINT = "http://api.xdmtk.org/?reqcoords=1";
        private String REQUEST_ENDPOINT = "http://api.xdmtk.org/?requests=1";
        private GoogleMap myMap;
        private String coordinateString;
        private String requestString;
        private String lastRequestString = "";
        private TextView lastUpdatedText = (TextView)findViewById(R.id.text_view);
        private int UPDATE_INTERVAL = 25;
        public boolean cameraMoveLock = false;
        private double lat;
        private double lon;



        public BikeCoordinates(GoogleMap m) {
            this.myMap = m;
            m.setOnCameraMoveStartedListener(this);

            final Button recenter = findViewById(R.id.recenter);
            recenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moveToCurrentLocation(myMap, new LatLng(lat,lon));
                }
            });

        }

        @Override
        public void onCameraMoveStarted(int reason) {
            this.cameraMoveLock = true;
        }

        private void moveToCurrentLocation(GoogleMap myMapP, LatLng currentLocation)
        {
            myMapP.addMarker(new MarkerOptions().position(currentLocation).title("Current Bike Location"));
            myMapP.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,15));
        }

        public void run() {
            while (true) {
                try {
                    coordinateString = getAccessCoordinates("Coordinates");
                    requestString = getAccessCoordinates("Requests");
                } catch (Exception e) {
                    System.out.println(e);
                } finally {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            if (lastRequestString.equals("") || !requestString.equals(lastRequestString)) {

                                // On good GPS data
                                if (coordinateString != null && coordinateString.length() > 0) {

                                    // Split the GPS string and parse
                                    String[] coordinates = coordinateString.substring(
                                            coordinateString.indexOf(":")).split(",");

                                    // For warmed up GPS coordinates, split by comma should only have 5 values
                                    if (coordinates.length <= 5) {
                                        lat = Double.valueOf(coordinates[3]);
                                        lon = Double.valueOf(coordinates[4]);

                                        LatLng currentCoordinates = new LatLng(lat,lon);
                                        if (!cameraMoveLock) {
                                            moveToCurrentLocation(myMap, currentCoordinates);
                                        }
                                    }
                                }
                                lastRequestString = requestString;
                                String updatedTextSlice = requestString.split("-")[0].substring(8);
                                lastUpdatedText.setText("Last Updated: " + updatedTextSlice);
                            }

                        }
                    });
                    try {
                        new Thread() {
                            public void run() {
                                // Start update countdown
                                TextView updateTimerText = (TextView) findViewById(R.id.update_timer);
                                int seconds = UPDATE_INTERVAL;

                                while (seconds > 0) {
                                    String updateTextSet = "Next Update: " + seconds + " seconds";
                                    updateTimerText.setText(updateTextSet);
                                    seconds--;
                                    try {
                                        Thread.sleep(1000);
                                    }
                                    catch (Exception e){
                                        System.out.print(e);
                                    }
                                };
                            }
                        }.start();
                        Thread.sleep(UPDATE_INTERVAL*1000);
                    } catch (Exception e) {
                        System.out.print(e);
                    }
                }
            }
        }


        private String getAccessCoordinates(String mode) throws IOException {

            URL url;
            // Setup HTTP context
            if (mode == "Coordinates") {
                url = new URL(COORDINATE_ENDPOINT);
            }
            else if (mode == "Requests") {
                url = new URL(REQUEST_ENDPOINT);
            }
            else {
                return "No mode";
            }

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
