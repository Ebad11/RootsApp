package com.example.rootsapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.Marker;

public class MapPickerActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map_picker);

        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // India center
        map.getController().setZoom(5.0);
        map.getController().setCenter(startPoint);

        // Add MapEventsOverlay for handling clicks
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false; // ignore single taps
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                map.getOverlays().clear();

                // Add marker where user long-presses
                Marker marker = new Marker(map);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("Selected Location");
                map.getOverlays().add(marker);
                map.invalidate();

                Toast.makeText(MapPickerActivity.this,
                        "Location: " + p.getLatitude() + ", " + p.getLongitude(),
                        Toast.LENGTH_SHORT).show();

                // send data back to AddPostActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", p.getLatitude());
                resultIntent.putExtra("longitude", p.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
                return true;
            }
        };

        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(OverlayEvents);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
