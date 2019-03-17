package com.example.sim808anti_theft;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
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

        bikeCoord = new BikeCoordinates(mMap, this);
        bikeCoord.start();

    }

    private void sendUrgentNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "bike_channel")
                    .setSmallIcon(R.drawable.ic_baseline_report_problem_24px)
                .setContentTitle("hello")
                .setContentText("testing")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(001, builder.build());

    }




    public class BikeCoordinates extends Thread implements GoogleMap.OnCameraMoveStartedListener{

        // API endpoints
        private String COORDINATE_ENDPOINT = "http://api.xdmtk.org/?reqcoords=1";
        private String REQUEST_ENDPOINT = "http://api.xdmtk.org/?requests=1";

        // Google map object
        private GoogleMap myMap;

        // Coordinate strings
        private String coordinateString;
        private String requestString;
        private String lastRequestString = "";
        private TextView lastUpdatedText = (TextView)findViewById(R.id.text_view);
        private int UPDATE_INTERVAL = 25;

        // In meters
        private int LOCK_RADIUS = 15;

        public boolean firstRun = true;
        public boolean cameraMoveLock = false;
        public boolean theftLock = false;
        private double lat;
        private double lon;
        private double lat_lock;
        private double lon_lock;




        public BikeCoordinates(GoogleMap m, MapsActivity mainobject) {

            // Setup map event listeners
            this.myMap = m;
            m.setOnCameraMoveStartedListener(this);


            // Setup button event listeners
            final Button recenter = findViewById(R.id.recenter);
            recenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moveToCurrentLocation(myMap, new LatLng(lat,lon));
                    cameraMoveLock = false;
                }
            });

            final Button lock = findViewById(R.id.lock);
            lock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!theftLock) {
                        theftLock = true;
                        lock.setText("Unlock");
                        setGPSLock(lat,lon);

                    }
                    else {
                        theftLock = false;
                        lock.setText("Lock");
                    }
                }
            });


            // Give title to app toolbar
            final Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle("SIM808 Anti-Theft");

        }


        // Disable automatic move to bike coordinates if map is manually moved
        @Override
        public void onCameraMoveStarted(int reason) {
            if (firstRun) {
                firstRun = false;
                return;
            }
            this.cameraMoveLock = true;
        }

        // Function to animate map and add marker
        private void moveToCurrentLocation(GoogleMap myMapP, LatLng currentLocation)
        {
            myMapP.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,15));
        }

        // Called on Thread start
        public void run() {

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Bike Channel";
                String description = "Notifications to signal bike coordinate updates";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("bike_channel", name, importance);
                channel.setDescription(description);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            sendUrgentNotification();


            // Repeats update process indefinitely
            while (true) {
                try {

                    // Fetch the coordinate strings and request history from API
                    coordinateString = getAccessCoordinates("Coordinates");
                    requestString = getAccessCoordinates("Requests");
                } catch (Exception e) {
                    System.out.println(e);
                } finally {

                    // When data is fetched, update UI with coordinate data and request data
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            // Only update if fetched coordinates are a new update from SIM808
                            if (lastRequestString.equals("") || !requestString.equals(lastRequestString)) {

                                // On good GPS data continue with parsing
                                if (coordinateString != null && coordinateString.length() > 0) {

                                    // Split the GPS string and parse
                                    String[] coordinates = coordinateString.substring(
                                            coordinateString.indexOf(":")).split(",");

                                    // For warmed up GPS coordinates, split by comma should only have 5 values
                                    if (coordinates.length <= 5) {
                                        lat = Double.valueOf(coordinates[3]);
                                        lon = Double.valueOf(coordinates[4]);

                                        // Main anti-theft action -> Determine whether GPS coordinates
                                        // have moved outside of defined radius
                                        if (theftLock) {

                                            // If checkLockRadius returns true, send push notification
                                            // to alert of unauthorized movement
                                            if (checkLockRadius(lat, lon)) {



                                            }
                                        }

                                        LatLng currentCoordinates = new LatLng(lat,lon);
                                        myMap.clear();
                                        myMap.addMarker(new MarkerOptions().position(currentCoordinates).title("Current Bike Location"));

                                        // Move camera if no manual movement
                                        if (!cameraMoveLock) {
                                            moveToCurrentLocation(myMap, currentCoordinates);
                                        }
                                    }
                                }

                                // Reset new request string
                                lastRequestString = requestString;

                                // Show update string
                                String updatedTextSlice = requestString.split("-")[0].substring(8);
                                lastUpdatedText.setText("Last Updated: " + updatedTextSlice);
                            }

                        }
                    });
                    try {


                        // Setup new thread to update UI with update counter
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

                        // Pause main (sub) thread execution for interval timer specification
                        Thread.sleep(UPDATE_INTERVAL*1000);
                    } catch (Exception e) {
                        System.out.print(e);
                    }
                }
            }
        }



        // On lock button press, save last GPS coordinates to lock on
        private void setGPSLock(double latitude, double longitude) {
            lat_lock = latitude;
            lon_lock = longitude;
        }



        // If theftLock is set, for each new update, call this function to determine whether
        // GPS coordinates are moving outside defined lock radius
        private boolean checkLockRadius(double latitude, double longtidue) {
            double latMid, m_per_deg_lat, m_per_deg_lon, deltaLat, deltaLon,dist_m;

            latMid = (lat+lat_lock)/2.0;  // or just use Lat1 for slightly less accurate estimate


            m_per_deg_lat = 111132.954 - 559.822 * Math.cos( 2.0 * latMid ) + 1.175 * Math.cos( 4.0 * latMid);
            m_per_deg_lon = (3.14159265359/180 ) * 6367449 * Math.cos ( latMid );

            deltaLat = Math.abs(lat-lat_lock);
            deltaLon = Math.abs(lon - lon_lock);

            dist_m = Math.sqrt (  Math.pow( deltaLat * m_per_deg_lat,2) + Math.pow( deltaLon * m_per_deg_lon , 2) );
            if (dist_m > LOCK_RADIUS) {
                return true;
            }
            return false;
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
