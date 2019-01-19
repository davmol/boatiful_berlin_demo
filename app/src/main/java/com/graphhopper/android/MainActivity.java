package com.graphhopper.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import android.view.View;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;

import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;




public class MainActivity extends Activity {
    private static final int NEW_MENU_ID = Menu.FIRST + 1;
    private MapView mapView;
    private GraphHopper hopper;
    private GeoPoint start;
    private GeoPoint userlocation;
    private GeoPoint end;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    private Context context;

    private ItemizedLayer<MarkerItem> markerLayer;
    private ItemizedLayer<MarkerItem> locationLayer;
    private PathLayer pathLayer;
    private File mapFile;
    private File folder;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean reset;
    private FloatingActionButton fabLocation;
    private FloatingActionButton fabInfo;
    private String travelTime;
    private String travelDistance;
    private boolean userlocationIsStart;
    private boolean longClickToggle;
    private boolean clickToggle;

    protected boolean onLongPress(GeoPoint p) {
        reset = false;

        if (!isReady()) {
            //return false;
            logUser("Still loading");
        }

        if (shortestPathRunning) {
            logUser("Calculation still in progress");
            return false;
        }
        // start end set
        if (start != null && end != null){
            mapView.map().layers().remove(pathLayer);
            markerLayer.removeAllItems();
            mapView.map().updateMap(true);

            start = null;
            end = null;
            reset = true;

        }
        // no end set
        if (start != null && end == null) {
            end = p;
            shortestPathRunning = true;
            markerLayer.addItem(createMarkerItem(p, R.drawable.ic_finsh_flag, "finish"));
            mapView.map().updateMap(true);

            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                    end.getLongitude());

        }
        // no start no end set
        if (start == null && end == null && reset == false) {


            if (userlocation != null && userlocationIsStart == true) {
                start = userlocation;
            }

            else {
                start = p;

            }
            end = null;
            // remove routing layers


            markerLayer.addItem(createMarkerItem(start, R.drawable.ic_start_dot_, "start"));
            mapView.map().updateMap(true);
        }
        return true;
    }

    private void showLocation() {

        if (userlocation != null && userlocationIsStart == true) {

            locationLayer.removeAllItems();
            locationLayer.addItem(createMarkerItem(userlocation, R.drawable.ic_location_dot_start, "userlocation"));


        } else if (userlocation != null) {
            locationLayer.removeAllItems();
            locationLayer.addItem(createMarkerItem(userlocation, R.drawable.ic_location_dot_default, "userlocation"));


        } else {
            logUser("No location found yet");

        }
        mapView.map().updateMap(true);
    }


    private void getLocation() {

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                try {
                    userlocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                    if (userlocationIsStart == true && userlocation != null && end != null) {
                        mapView.map().layers().remove(pathLayer);

                        calcPath(userlocation.getLatitude(), userlocation.getLongitude(), end.getLatitude(), end.getLongitude());
                        markerLayer.removeAllItems();

                        mapView.map().updateMap(true);
                    }

                    showLocation();

                    float bearingF = location.getBearing();
                    float speedF = location.getSpeed() * 3.6f;

                    setTextViewTravelParameter(bearingF, speedF);

                }
                catch (Exception e) {
                    logUser("Couldn't find location:\n" + e);
                    e.printStackTrace();
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

                //logUser("Status Changed");

            }

            @Override
            public void onProviderEnabled(String s) {

                logUser("Location Provider Enabled");

            }

            @Override
            public void onProviderDisabled(String s) {

                logUser("Location Provider Disabled");

            }
        };

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions( new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else {

            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER,5,5,locationListener);
            try {
                Location lastKnownLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);

                userlocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());


                Log.i("on Permission",userlocation.toString());
            }
            catch (Exception e){
                logUser("Couldn't get last known location");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        userlocationIsStart = false;

        setContentView(R.layout.loading);
        getMapFile();
        loadMap();
        getLocation();


        fabLocation = (FloatingActionButton) findViewById(R.id.fab);
        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userlocation != null) {
                    //userlocationIsStart = false;
                    //fabLocation.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.White)));
                    //locationLayer.removeAllItems();
                    //locationLayer.addItem(createMarkerItem(userlocation, R.drawable.ic_location_dot_default, "userlocation"));
                    mapView.map().setMapPosition(userlocation.getLatitude(), userlocation.getLongitude(), mapView.map().viewport().getSyncViewport().getMaxScale());
                    //mapView.map().setMapPosition(userlocation.getLatitude(), userlocation.getLongitude(), 10);
                    mapView.map().updateMap(true);


                }
                else {

                    log("no to show yet");
                }

            }
        });

        fabLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {


                if (userlocation != null && longClickToggle == false) {

                    userlocationIsStart = true;
                    fabLocation.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.LightLightBlue)));
                    fabLocation.setBackgroundResource(R.drawable.ic_my_location_dot_24dp_logged);
                    locationLayer.removeAllItems();
                    locationLayer.addItem(createMarkerItem(userlocation, R.drawable.ic_location_dot_start, "userlocation"));
                    mapView.map().updateMap(true);

                    longClickToggle = true;


                }

                else if (userlocation != null && longClickToggle == true){

                    userlocationIsStart = false;
                    fabLocation.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.White)));
                    fabLocation.setBackgroundResource(R.drawable.ic_my_location_dot_24dp_default);
                    locationLayer.removeAllItems();
                    locationLayer.addItem(createMarkerItem(userlocation, R.drawable.ic_location_dot_default, "userlocation"));
                    mapView.map().updateMap(true);

                    longClickToggle = false;


                }
                else {
                    log("no location found yet for setting location as start point");

                }


                return true;
            }


         });

        fabInfo = (FloatingActionButton) findViewById(R.id.fabinfo);
        fabInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(MainActivity.this, InfoActivity.class));
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hopper != null)
            hopper.close();

        hopper = null;
        // necessary?
        System.gc();

        // Cleanup VTM
        mapView.map().destroy();
    }

    boolean isReady() {
        // only return true if already loaded
        if (hopper != null)
            return true;

        if (prepareInProgress) {
            logUser("Preparation still in progress");
            return false;
        }
        logUser("Preparation finished but graphs not ready. This happens when there was an error while loading the files");
        return false;
    }


//TODO array list & for loop to generate files
    void getMapFile(){

        ArrayList <String> fileNames = new ArrayList<String>();
        fileNames.add("berlin.map");
        fileNames.add("edges");
        fileNames.add("geometry");
        fileNames.add("location_index");
        fileNames.add("names");
        fileNames.add("nodes");
        fileNames.add("nodes_ch_fastest_boat");
        fileNames.add("properties");
        fileNames.add("shortcuts_fastest_boat");


        folder = new File(getCacheDir().getPath());


        for (String fileName: fileNames) {

            File file = new File(folder,fileName);

            if (!file.exists()) {
                try {

                    InputStream in = getAssets().open(fileName);
                    int size = in.available();
                    byte[] buffer = new byte[size];
                    in.read(buffer);
                    in.close();
                    File mf = new File(folder,fileName);
                    FileOutputStream fos = new FileOutputStream(mf);
                    fos.write(buffer);
                    fos.close();
                }

                catch (Exception e) {
                    throw new RuntimeException(e);

                }
            }
            else {
                log("filenames already exist");

            }

            File[] files = folder.listFiles();
            Log.d("Files", "Size: "+ files.length);
            for (int i = 0; i < files.length; i++)
            {
                Log.d("Boatiful: Files", "FileName:" + files[i].getName());
            }

        }

    }


    void loadMap() {

        //logUser("loading map");
        mapView = new MapView(this);

        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.Map);
        // Map events receiver
        mapView.map().layers().add(new MapEventsReceiver(mapView.map()));

        // Map file source
        MapFileTileSource tileSource = new MapFileTileSource();

        mapFile = new File(folder, "/berlin.map");
        //mapFile.setReadable(true, false);


        if (!mapFile.exists())
             log("boatiful: map File does not exist");

        if (mapFile.canRead()){
            log("boatiful: can read map File");
        }
        else {
            log("boatiful: can not read map File");
        if (!mapFile.exists()) {
            log("boatiful: map File does not exist");
        }

       }

        tileSource.setMapFile(mapFile.getAbsolutePath());

        //TODO implement folder to .jar (cruiser -> vtm)
        VectorTileLayer l = mapView.map().setBaseMap(tileSource);
        mapView.map().setTheme(VtmThemes.DEFAULT);
        mapView.map().layers().add(new BuildingLayer(mapView.map(), l));
        mapView.map().layers().add(new LabelLayer(mapView.map(), l));

        // Markers layer

        markerLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
        locationLayer = new ItemizedLayer<>(mapView.map(),(MarkerSymbol) null);
        mapView.map().layers().add(markerLayer);
        mapView.map().layers().add(locationLayer);

        // Map position
        GeoPoint center = tileSource.getMapInfo().boundingBox.getCenterPoint();
        mapView.map().setMapPosition(center.getLatitude(), center.getLongitude(), 1 << 15);

        loadGraphStorage();
    }

    void loadGraphStorage() {
        //logUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v)  {
                GraphHopper tmph = new GraphHopper().forMobile();
                //tmph.load((mapsFolder.getAbsolutePath()));
                tmph.load((folder.getAbsolutePath()));
                log("found graph " + tmph.getGraphHopperStorage().toString() + ", nodes:" + tmph.getGraphHopperStorage().getNodes());
                hopper = tmph;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    //logUser("Finished loading graph. Long press to define where to start and end the route.");
                }

                finishPrepare();
            }
        }.execute();
    }

    private void finishPrepare() {
        prepareInProgress = false;
    }

    private PathLayer createPathLayer(PathWrapper response) {
        Style style = Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(0x99e31a1c)
                .strokeWidth(6 * getResources().getDisplayMetrics().density)
                .fillAlpha(10)
                .build();
        PathLayer pathLayer = new PathLayer(mapView.map(), style);
        List<GeoPoint> geoPoints = new ArrayList<>();
        PointList pointList = response.getPoints();
        for (int i = 0; i < pointList.getSize(); i++)
            geoPoints.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
        pathLayer.setPoints(geoPoints);
        return pathLayer;
    }

    @SuppressWarnings("deprecation")
    private MarkerItem createMarkerItem(GeoPoint p, int resource, String title) {
        Drawable drawable = getResources().getDrawable(resource);
        Bitmap bitmap = AndroidGraphics.drawableToBitmap(drawable);
        MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, 0.5f, 1);
        MarkerItem markerItem = new MarkerItem(title, "", p);
        markerItem.setMarker(markerSymbol);
        return markerItem;
    }
    private void setTextViewTravelParameter(float bearingF, float speedF){


        String bearingText;
        int bearing = Math.round(bearingF);

        if (bearing >= 337.5f && bearing< 22.5f) {
            bearingText = String.valueOf(bearing) + "° N";
        }
        else if (bearing >= 22.5f && bearing< 67.5f) {
            bearingText = String.valueOf(bearing) + "° NE";
        }
        else if (bearing >= 67.5 && bearing< 112.5f) {
            bearingText = String.valueOf(bearing) + "° E";
        }
        else if (bearing >= 112.5f && bearing< 157.5f) {
            bearingText = String.valueOf(bearing) + "° SE";
        }
        else if (bearing >= 157.5f && bearing< 202.5f) {
            bearingText = String.valueOf(bearing) + "° S";
        }
        else if (bearing >= 202.5f && bearing< 247.5f) {
            bearingText = String.valueOf(bearing) + "° SW";
        }
        else if (bearing >= 247.5f && bearing< 292.5f) {
            bearingText = String.valueOf(bearing) + "° W";
        }
        else if (bearing >= 292.5f && bearing< 337.5f) {
            bearingText = String.valueOf(bearing) + "° NW";
        }
        else {
            bearingText = "no bearing";
        }

        final TextView bearingTextView = (TextView) findViewById(R.id.text1);
        bearingTextView.setText(bearingText);


        int speed = Math.round(speedF);
        final TextView speedTextView = (TextView) findViewById(R.id.text2);
        speedTextView.setText(String.valueOf(speed) + " km/h");

    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon) {

        try {
            log("calculating path ...");
            new AsyncTask<Void, Void, PathWrapper>() {
                float time;

                protected PathWrapper doInBackground(Void... v) {
                    StopWatch sw = new StopWatch().start();
                    GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                            setAlgorithm(Algorithms.DIJKSTRA_BI);
                    req.getHints().
                            put(Routing.INSTRUCTIONS, "false");
                    GHResponse resp = hopper.route(req);
                    time = sw.stop().getSeconds();
                    return resp.getBest();
                }

                protected void onPostExecute(PathWrapper resp) {
                    if (!resp.hasErrors()) {
                        log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                                + toLon + " found path with distance:" + resp.getDistance()
                                / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                                + time + " " + resp.getDebugInfo());




                        travelDistance = String.valueOf(Math.floor(resp.getDistance() / 100) / 10f+ " km");
                        travelTime = String.valueOf(Math.round(resp.getTime() / 60000f ) + " min");


                        //ogUser("Distanz = " + (int) (resp.getDistance() / 100) / 10f
                        //        + " km\nbenötigte Zeit: " + (int) Math.floor(resp.getTime() / 60000f) + " min\ndebug Zeit: " + time);

                        pathLayer = createPathLayer(resp);
                        mapView.map().layers().add(pathLayer);
                        mapView.map().updateMap(true);
                    } else {
                        logUser("Error:" + resp.getErrors());
                    }
                    shortestPathRunning = false;

                    final TextView travelDistanceTextView = (TextView) findViewById(R.id.text4);
                    travelDistanceTextView.setText(travelDistance);



                    final TextView travelTimeTextView = (TextView) findViewById(R.id.text3);
                    travelTimeTextView.setText(travelTime);

                }
            }.execute();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void log(String str) {
        Log.i("GH", str);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, NEW_MENU_ID, 0, "Google");
        // menu.add(0, NEW_MENU_ID + 1, 0, "Other");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case NEW_MENU_ID:
                if (start == null || end == null) {
                    logUser("long tap screen to set start and end of route");
                    break;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                // get rid of the dialog
                intent.setClassName("com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity");
                intent.setData(Uri.parse("http://maps.google.com/maps?saddr="
                        + start.getLatitude() + "," + start.getLongitude() + "&daddr="
                        + end.getLatitude() + "," + end.getLongitude()));
                startActivity(intent);
                break;
        }
        return true;
    }

    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(org.oscim.map.Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.LongPress) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                return onLongPress(p);
            }
            return false;
        }
    }
}
