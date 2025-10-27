package com.example.rootsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class MapSimpleActivity extends AppCompatActivity {

    private MapView map;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map_simple);

        map = findViewById(R.id.map);
        db = FirebaseFirestore.getInstance();

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // India center
        map.getController().setZoom(5.5);
        map.getController().setCenter(startPoint);

        // Request permission if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Load posts from Firestore and show markers
        loadMemoriesOnMap();
    }

    private void loadMemoriesOnMap() {
        db.collection("posts").get().addOnSuccessListener(query -> {
            if (query.isEmpty()) {
                Toast.makeText(this, "No memories yet 🌱", Toast.LENGTH_SHORT).show();
                return;
            }

            for (DocumentSnapshot doc : query.getDocuments()) {
                Double lat = doc.getDouble("latitude");
                Double lon = doc.getDouble("longitude");
                String content = doc.getString("content");
                String userEmail = doc.getString("userEmail");

                if (lat == null || lon == null) continue;

                GeoPoint point = new GeoPoint(lat, lon);
                Marker marker = new Marker(map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("👤 " + userEmail);
                marker.setSubDescription(content);
                marker.setSnippet("Long press to expand memory 💭");

                // Custom InfoWindow when clicked
                marker.setOnMarkerClickListener((m, v) -> {
                    if (m.isInfoWindowOpen()) {
                        m.closeInfoWindow();
                    } else {
                        m.showInfoWindow();
                    }
                    return true;
                });

                // Long press to show memory popup
                marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker m, MapView mapView) {
                        // Example: show post preview or toast
                        Toast.makeText(MapSimpleActivity.this, "Memory: " + m.getTitle(), Toast.LENGTH_SHORT).show();

                        // Later we'll show a floating post card here
                        return true; // true means event consumed
                    }
                });


                map.getOverlays().add(marker);
            }

            map.invalidate();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() { super.onResume(); map.onResume(); }
    @Override
    protected void onPause() { super.onPause(); map.onPause(); }
}
