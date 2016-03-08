package finalproject.ee461l.journey;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JourneyHome extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MapFragment mapFragment;
    private HelpFragment helpFragment;
    private static boolean firstUpdate;
    private static LatLng currentLocation;
    private static boolean useTTS;
    private static TextToSpeech speaker;
    private static final int START_TRIP = 0;
    private static final int JOIN_TRIP = 1;
    private static final int VOICE_START = 2;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey_home);

        //Initialize fragments
        mapFragment = MapFragment.newInstance();
        helpFragment = HelpFragment.newInstance();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().add(R.id.home_view, mapFragment).commit();
        mapFragment.getMapAsync(this);
        JourneyHome.firstUpdate = true;
        JourneyHome.useTTS = true;

        //Navigation Drawer
        ArrayList<String> navItems = new ArrayList<String>();
        navItems.add("Log In");
        navItems.add("Settings");
        navItems.add("Help");
        navItems.add("About");

        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, navItems));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new NavClickListener());

        //Handling navigation drawer open/close
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                System.out.println("Just Opened the drawer");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                System.out.println("Just Closed the drawer");
            }
        });

        //Let's also set up the TTS engine
        JourneyHome.speaker = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    //Failed to set up TTS engine
                    JourneyHome.speaker.setLanguage(Locale.US);
                }
                else {
                    JourneyHome.useTTS = false;
                }
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(JourneyHome.currentLocation));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == JourneyHome.START_TRIP) {
            // From Start Handler
            if (resultCode == RESULT_OK) {
                //It worked
                //We will start by changing the buttons on screen
                adjustView();

                //Now we will deal with the route directions
                JSONObject directions = null;
                try {
                    directions = new JSONObject(data.getStringExtra("JSONDirections"));
                    System.out.println(directions);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String valid = null;
                try {
                    valid = directions.getString("status");
                    if (!valid.equals("OK")) return;
                    //For this particular function, we do not need to worry about waypoints
                    JSONArray steps = getRouteSteps(directions, true);

                    //Need to convert polyline points into legitimate points
                    //Reverse engineering this: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
                    List<LatLng> leg = convertPolyline(steps);

                    //Next we need to create a PolylineOptions object and give it all of the points in the step
                    PolylineOptions options = new PolylineOptions();
                    for (LatLng coord : leg) {
                        options.add(coord);
                    }

                    //Finally, we add the polyline to the map
                    mMap.addPolyline(options);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                //Null directions
            }
        }
        else if (requestCode == JourneyHome.VOICE_START) {
            //Voice recognition
            if (resultCode == RESULT_OK) {
                //Currently just going to repeat what was said here
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                System.out.println("Successful voice recog!");
                System.out.println("Voice results: " + results);
                System.out.println(results.get(0));

                if (!JourneyHome.useTTS) {
                    System.out.println("Failed to set up TTS");
                    return;
                }

                //TTS is set up
                //Currently just repeating what was just said
                if (Build.VERSION.SDK_INT >= 21) {
                    speaker.speak(results.get(0), TextToSpeech.QUEUE_FLUSH, null, "test");
                }
                else {
                    speaker.speak(results.get(0), TextToSpeech.QUEUE_FLUSH, null);
                }

            }
        }
    }

    public void adjustView() {
        ViewGroup layout = (RelativeLayout) JourneyHome.this.findViewById(R.id.journey_layout);
        Button startButton = (Button) JourneyHome.this.findViewById(R.id.start_trip);
        View joinButton = (Button) JourneyHome.this.findViewById(R.id.join_trip);
        layout.removeView(joinButton);

        //Will simply change the text and activity of the start button to "Add Stop"
        startButton.setText("Add Stop to Route");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopHandler(v);
            }
        });

        //Need to also add a speech button
        ImageButton speech = new ImageButton(JourneyHome.this);
        //Image edited from: http://3.bp.blogspot.com/-WOpREKAmsfY/Ua2uLrKiGuI/AAAAAAABJu0/yJt8I49pO5o/s640/chrome-iphone-voice-search-2.png
        speech.setImageResource(R.drawable.google_microphone_logo);
        speech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceComm(v);
            }
        });

        //Now we need to deal with Layout parameters
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        speech.setLayoutParams(params);
        speech.getBackground().setAlpha(0);
        speech.setId(R.id.speech_button);
        layout.addView(speech);
    }

    public JSONArray getRouteSteps(JSONObject directions, boolean isFirstTime) {
        JSONArray steps = null;
        if (isFirstTime) {
            //No waypoints
            try {
                JSONArray routes = directions.getJSONArray("routes");

                //Need to get the legs[]
                JSONObject firstRoute = routes.optJSONObject(0); //If we look for more than 1 route, we'll need a loop
                JSONArray legs = firstRoute.getJSONArray("legs");

                //Need to get the steps[] now
                JSONObject firstLeg = legs.optJSONObject(0); //Once we add waypoints there will be more legs
                steps = firstLeg.getJSONArray("steps");
            }
            catch (JSONException e) {
                //JSON Error
            }
        }
        else {
            //At least 1 waypoint to worry about
        }
        return steps;
    }

    public ArrayList<LatLng> convertPolyline(JSONArray steps) {
        ArrayList<LatLng> leg = new ArrayList<LatLng>();
        for (int i = 0; i < steps.length(); i++) {
            String points = "";
            try {
                points = steps.getJSONObject(i).getJSONObject("polyline").getString("points");
            }
            catch (JSONException e) {
                //JSON Error
            }
            double latitude = 0;
            double longitude = 0; //Out here b/c path uses relative lat/lng changes
            int index = 0;
            int length = points.length();
            while (index < length) {
                //Need to decode each character
                //Start with latitude
                int[] lat = getCoord(points, index);
                latitude += lat[0] / 1e5; //step 2
                index = lat[1];

                //Repeat with longitude
                int[] lng = getCoord(points, index);
                longitude += lng[0] / 1e5;
                index = lng[1];

                LatLng current = new LatLng(latitude, longitude);
                leg.add(current);
            }
        }

        return leg;
    }

    public int[] getCoord(String points, int currIndex) {
        int result = 1;
        int shift = 0;
        int character = 0x20;
        int index = currIndex;
        while (character >= 0x1f) {
            //>=x1f is because every chunk gets ORd with 0x20 except the last one per code
            if (index >= points.length()) break;
            character = points.charAt(index);
            character -= 64; //step 10
            result += (character << shift); //step 7
            shift += 5; //5-bit chunks
            index++;
        }

        //Need to determine if original value was negative or not (step 5)
        //Since there is a left shift of 1 before any inversion, a positive # will always have
        //a 0 in the LSB
        if ((result & 1) == 1) result = ~(result >> 1); //RSHF on inside so MSB = 1
        else result = result >> 1; //step 4

        return new int[] {result, index};
    }

    public void startVoiceComm(View view) {
        //Start voice recognition activity
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, JourneyHome.VOICE_START);
    }

    /**
     * This function gets called when the "Start Road Trip" button is pressed
     * It will begin a new Activity
     *
     * @param view
     */
    public void startHandler(View view) {
        Intent intent = new Intent(this, StartTrip.class);
        //If we decide to pass values from this screen, we do that here
        startActivityForResult(intent, JourneyHome.START_TRIP);
    }

    /**
     * This function gets called when the "Join Road Trip" button is pressed
     * It will begin a new Activity
     *
     * @param view
     */
    public void joinHandler(View view) {
        Intent intent = new Intent(this, JoinTrip.class);
        //If we decide to pass values from this screen, we do that here
        startActivity(intent);
    }

    public void stopHandler(View view) {
        //Do nothing right now
        System.out.println("Called for a stop");
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

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                updateLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        try {
            mMap.setMyLocationEnabled(true);
            mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            //TODO: Add something here?
        }
    }

    public void updateLocation(Location location) {
        LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(currentLoc).title("Current Location"));
        if (JourneyHome.firstUpdate) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));
            JourneyHome.firstUpdate = false;
        }
        JourneyHome.currentLocation = currentLoc;
    }

    /**
     * This class serves as the onClickListener for our navigation drawer
     */
    private class NavClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            DrawerLayout drawer = (DrawerLayout) JourneyHome.this.findViewById(R.id.drawer_layout);
            drawer.closeDrawer(Gravity.LEFT);
            switch (position) {
                case 0:
                    //Log In
                    AlertDialog.Builder builder = new AlertDialog.Builder(JourneyHome.this);
                    LayoutInflater inflater = JourneyHome.this.getLayoutInflater();
                    builder.setView(inflater.inflate(R.layout.login, null))
                            .setPositiveButton("Log In", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Log in here
                                    EditText emailView = (EditText) JourneyHome.this.findViewById(R.id.email);
                                    EditText passView = (EditText) JourneyHome.this.findViewById(R.id.password);
                                    Editable emailEdit = emailView.getText();
                                    Editable passEdit = passView.getText();
                                    String email = emailEdit.toString();
                                    String pass = passEdit.toString();
                                    System.out.println("Email: " + email + ", Password: " + pass);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setTitle("Log Into Journey");
                    builder.show();
                    break;
                case 1:
                    //Settings
                    System.out.println("Settings selected");
                    break;
                case 2:
                    //Help
                    //First we need to remove whatever buttons are on the screen
                    if (JourneyHome.this.findViewById(R.id.start_trip) != null) {
                        //Trip not started yet
                        Button button = (Button) JourneyHome.this.findViewById(R.id.start_trip);
                        button.setVisibility(View.GONE);
                        button = (Button) JourneyHome.this.findViewById(R.id.join_trip);
                        button.setVisibility(View.GONE);
                    }
                    else {
                        //Trip started
                        Button button = (Button) JourneyHome.this.findViewById(R.id.start_trip);
                        button.setVisibility(View.GONE);
                        ImageButton ibutton = (ImageButton) JourneyHome.this.findViewById(R.id.speech_button);
                        ibutton.setVisibility(View.GONE);
                    }

                    FragmentManager manager = getFragmentManager();
                    manager.beginTransaction().replace(R.id.home_view, helpFragment).commit();
                    System.out.println("Help selected");
                    break;
                case 3:
                    //About
                    System.out.println("About selected");
                    break;
            }
        }
    }
}
