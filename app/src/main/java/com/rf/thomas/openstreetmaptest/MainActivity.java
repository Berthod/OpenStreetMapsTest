package com.rf.thomas.openstreetmaptest;

import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.osmdroid.util.GeoPoint;
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

public class MainActivity extends AppCompatActivity {

    private static final String MAP_FILE = "czech_republic.map";

    GPXParser mParser = new GPXParser();

    List<Route> routes = new ArrayList<>();
    List<RoutePoint> points = new ArrayList<>();
    List<WayPoint> wayPoints = new ArrayList<>();

    private MapView mapView;

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gpx parsedGpx = null;
        try {
            InputStream in = getAssets().open("cesta.gpx");
            parsedGpx = mParser.parse(in);
        } catch (IOException | XmlPullParserException e) {
            // do something with this exception
            e.printStackTrace();
        }
        if (parsedGpx == null) {
            // error parsing track
        } else {
            routes = parsedGpx.getRoutes();
        }



        AndroidGraphicFactory.createInstance(this.getApplication());

        this.mapView = new MapView(this);
        setContentView(this.mapView);

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

       /* Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    if(i==0) {
                        mapView.setCenter(new LatLong(50.239958, 17.187825));
                    } else if (i==1) {
                        mapView.setCenter(new LatLong(50.246435, 17.176938));
                    }  else if (i==2) {
                        mapView.setCenter(new LatLong(50.257834, 17.171777));
                    }  else if (i==3) {
                        mapView.setCenter(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==4) {
                        mapView.setCenter(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==5) {
                        mapView.setCenter(new LatLong(50.258941, 17.142041));
                    }
                    else if (i==6) {
                        mapView.setCenter(new LatLong(50.259941, 17.142041));
                    }
                    else if (i==7) {
                        mapView.setCenter(new LatLong(50.258941, 17.142041));
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

    @Override
    protected void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }
}
