package com.rf.thomas.openstreetmaptest;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.Utils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Route;
import io.ticofab.androidgpxparser.parser.domain.RoutePoint;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String MAP_FILE = "Ostrava.map";

    GPXParser mParser = new GPXParser();

    TextView mText;

    List<Route> routes = new ArrayList<>();
    List<RoutePoint> points = new ArrayList<>();

    Marker marker;

    Location mLocation;
    GoogleApiClient mGoogleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 5000;  /* 10 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 secs */

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();

    private final static int ALL_PERMISSIONS_RESULT = 101;

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        marker = createTappableMarker(this, R.drawable.marker_red, null, "Marker" );


        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);
        permissions.add(WRITE_EXTERNAL_STORAGE);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Gpx parsedGpx = null;
        try {
            InputStream in = getAssets().open("cesta.gpx");
            parsedGpx = mParser.parse(in);
        } catch (IOException | XmlPullParserException e) {
            // do something with this exception
            e.printStackTrace();
        }
        if (parsedGpx != null) {
            // error parsing track
            routes = parsedGpx.getRoutes();
        } else {

        }

        AndroidGraphicFactory.createInstance(this.getApplication());

        this.mapView = new MapView(this);

        mText = (TextView) findViewById(R.id.mText);
        mapView = (MapView) findViewById(R.id.mapView);

        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.setZoomLevelMin((byte) 10);
        this.mapView.setZoomLevelMax((byte) 20);

        // create a tile cache of suitable size
        TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // tile renderer layer using internal render theme

        MapDataStore mapDataStore = new MapFile(new File(Environment.getExternalStorageDirectory(), MAP_FILE));

        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);

        this.mapView.setCenter(new LatLong(50.239958, 17.187825));
        this.mapView.setZoomLevel((byte) 12);

        Style style = Style.STROKE;

        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5.0f);
        paint.setStyle(style);

        // instantiating the polyline object
        Polyline polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);

        LatLong gp;

        // set lat lng for the polyline
        List<LatLong> coordinateList = polyline.getLatLongs();

        for(Route route: routes) {
            points = route.getRoutePoints();

            for (RoutePoint point: points) {
                gp = new LatLong(point.getLatitude(), point.getLongitude());
                coordinateList.add(gp);
            }
        }

        // adding the layer to the mapview
        mapView.getLayerManager().getLayers().add(polyline);
        mapView.getLayerManager().getLayers().add(marker);

        /*Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    if(i==0) {
                        marker.setLatLong(new LatLong(50.239958, 17.187825));
                    } else if (i==1) {
                        marker.setLatLong(new LatLong(50.246435, 17.176938));
                    }  else if (i==2) {
                        marker.setLatLong(new LatLong(50.257834, 17.171777));
                    }  else if (i==3) {
                        marker.setLatLong(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==4) {
                        marker.setLatLong(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==5) {
                        marker.setLatLong(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==6) {
                        marker.setLatLong(new LatLong(50.259941, 17.142041));
                    }
                    else if (i==7) {
                        marker.setLatLong(new LatLong(50.258941, 17.142041));
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        });
        thread.start();*/
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    protected void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkPlayServices()) {
            mText.setText("Please install Google Play services.");

        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else
                finish();

            return false;
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

        if(location!=null) {
            mText.setText("Latitude : "+ location.getLatitude()+" , Longitude : "+ location.getLongitude());
            LatLong latLong = new LatLong(location.getLatitude(), location.getLongitude());
            marker.setLatLong(latLong);
            mapView.invalidate();
            mapView.setCenter(latLong);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        if(mLocation!=null)
        {
            mText.setText("Latitude : "+mLocation.getLatitude()+" , Longitude : "+mLocation.getLongitude());
            LatLong latLong = new LatLong(mLocation.getLatitude(), mLocation.getLongitude());
            marker.setLatLong(latLong);
            mapView.invalidate();
            mapView.setCenter(latLong);
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Enable Permissions", Toast.LENGTH_LONG).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void stopLocationUpdates()
    {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static Marker createTappableMarker(final Context c, int resourceIdentifier,
                                       LatLong latLong,final String name) {
        Drawable drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? c.getDrawable(resourceIdentifier) : c.getResources().getDrawable(resourceIdentifier);
        Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
        bitmap.incrementRefCount();
        bitmap.scaleTo(37, 60);
        return new Marker(latLong, bitmap, 0, -bitmap.getHeight() / 2) {
            @Override
            public boolean onTap(LatLong geoPoint, Point viewPosition,
                                 Point tapPoint) {
                if (contains(viewPosition, tapPoint)) {
                    Toast.makeText(c,
                            "The Marker was tapped " + name + " location: " + geoPoint.toString(),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        };
    }
}
