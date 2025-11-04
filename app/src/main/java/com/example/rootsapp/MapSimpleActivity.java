package com.example.rootsapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapSimpleActivity extends AppCompatActivity {

    private MapView map;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FrameLayout popupContainer;
    private TextView tvMemoryCount;
    private BottomNavigationView bottomNav;
    private String userName, userEmail;
    private List<Post> sortedPosts = new ArrayList<>();
    private View currentPopupView = null;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map_simple);

        // Get user data
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");

        mAuth = FirebaseAuth.getInstance();

        // If userEmail is null, get from FirebaseAuth
        if (userEmail == null && mAuth.getCurrentUser() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
        }

        map = findViewById(R.id.map);
        db = FirebaseFirestore.getInstance();
        popupContainer = findViewById(R.id.popupContainer);
        tvMemoryCount = findViewById(R.id.tvMemoryCount);
        bottomNav = findViewById(R.id.bottomNav);

        geocoder = new Geocoder(this, Locale.getDefault());

        // Configure map
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);
        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(18.0);

        // Start at India center
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629);
        map.getController().setZoom(5.5);
        map.getController().setCenter(startPoint);

        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Bottom nav setup
        bottomNav.setSelectedItemId(R.id.nav_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

//            if (id == R.id.nav_feed) {
//                Intent i = new Intent(this, FeedActivity.class);
//                i.putExtra("name", userName);
//                i.putExtra("email", userEmail);
//                startActivity(i);
//                overridePendingTransition(0, 0);
//                return true;
//            } else
                if (id == R.id.nav_add_post) {
                Intent i = new Intent(this, AddPostActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_maps) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        popupContainer.setOnClickListener(v -> closePopup());

        // Load only logged-in user's memories
        loadMemoryMap();
    }

    private void loadMemoryMap() {
        if (userEmail == null) {
            Toast.makeText(this, "Unable to load memories. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query only posts by logged-in user
        db.collection("posts")
                .whereEqualTo("userEmail", userEmail)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "You haven't created any memories yet 🌸", Toast.LENGTH_SHORT).show();
                        tvMemoryCount.setText("0 Memories");
                        map.invalidate();
                        return;
                    }

                    sortedPosts.clear();
                    List<GeoPoint> points = new ArrayList<>();

                    int count = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post == null) continue;

                        Double lat = post.getLatitude();
                        Double lon = post.getLongitude();

                        if (lat == null || lon == null || lat == 0.0 || lon == 0.0) continue;

                        sortedPosts.add(post);
                        GeoPoint point = new GeoPoint(lat, lon);
                        points.add(point);

                        // Create memory marker
                        Marker marker = createMemoryMarker(point, post, count + 1);
                        map.getOverlays().add(marker);
                        count++;
                    }

                    tvMemoryCount.setText(count + (count == 1 ? " Memory" : " Memories"));

                    // Draw connecting lines
                    if (points.size() > 1) {
                        drawMemoryConnections(points);
                    }

                    // Zoom to show all memories
                    if (!points.isEmpty()) {
                        zoomToShowAllPoints(points);
                    }

                    map.invalidate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load memories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Marker createMemoryMarker(GeoPoint point, Post post, int memoryNumber) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        Drawable redPin = ContextCompat.getDrawable(this, R.drawable.red_memory_pin);
        marker.setIcon(redPin);

        marker.setRelatedObject(post);
        marker.setTitle("Memory #" + memoryNumber);

        marker.setOnMarkerClickListener((m, mapView) -> {
            showMemoryPopup(post, memoryNumber);
            return true;
        });

        return marker;
    }

    private void drawMemoryConnections(List<GeoPoint> points) {
        Polyline line = new Polyline();
        line.setPoints(points);

        line.setColor(0xFFFFD700);
        line.setWidth(6f);
        line.getPaint().setStyle(Paint.Style.STROKE);
        line.getPaint().setStrokeCap(Paint.Cap.ROUND);
        line.getPaint().setStrokeJoin(Paint.Join.ROUND);

        map.getOverlays().add(0, line);
    }

    private void zoomToShowAllPoints(List<GeoPoint> points) {
        if (points.isEmpty()) return;

        double minLat = points.get(0).getLatitude();
        double maxLat = points.get(0).getLatitude();
        double minLon = points.get(0).getLongitude();
        double maxLon = points.get(0).getLongitude();

        for (GeoPoint point : points) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }

        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;
        GeoPoint center = new GeoPoint(centerLat, centerLon);

        double latSpan = maxLat - minLat;
        double lonSpan = maxLon - minLon;
        double maxSpan = Math.max(latSpan, lonSpan);

        double zoom = 12.0;
        if (maxSpan > 10) zoom = 5.0;
        else if (maxSpan > 5) zoom = 6.5;
        else if (maxSpan > 2) zoom = 8.0;
        else if (maxSpan > 1) zoom = 9.5;
        else if (maxSpan > 0.5) zoom = 11.0;

        map.getController().setCenter(center);
        map.getController().setZoom(zoom);
    }

    private String getLocationName(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder location = new StringBuilder();

                if (address.getLocality() != null) {
                    location.append(address.getLocality());
                }

                if (address.getAdminArea() != null) {
                    if (location.length() > 0) location.append(", ");
                    location.append(address.getAdminArea());
                }

                if (address.getCountryName() != null && location.length() == 0) {
                    location.append(address.getCountryName());
                }

                if (location.length() > 0) {
                    return location.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.format("%.4f, %.4f", latitude, longitude);
    }

    private void showMemoryPopup(Post post, int memoryNumber) {
        if (currentPopupView != null) {
            closePopup();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.map_memory_popup, popupContainer, false);

        TextView tvTitle = popupView.findViewById(R.id.tvPopupTitle);
        TextView tvLocation = popupView.findViewById(R.id.tvPopupLocation);
        TextView tvContent = popupView.findViewById(R.id.tvPopupContent);
        TextView tvUser = popupView.findViewById(R.id.tvPopupUser);
        TextView tvDate = popupView.findViewById(R.id.tvPopupDate);
        ImageView ivImage = popupView.findViewById(R.id.ivPopupImage);
        CardView cvImageContainer = popupView.findViewById(R.id.cvImageContainer);
        ImageView ivClose = popupView.findViewById(R.id.ivClosePopup);

        tvTitle.setText("📍 Memory #" + memoryNumber);

        String locationName = getLocationName(post.getLatitude(), post.getLongitude());
        tvLocation.setText(locationName);

        tvContent.setText(post.getContent() != null ? post.getContent() : "No description");
        tvUser.setText("👤 " + (post.getUserEmail() != null ? post.getUserEmail() : "Unknown"));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(post.getTimestamp()));
        tvDate.setText("📅 " + dateStr);

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            cvImageContainer.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl()).into(ivImage);
        } else {
            cvImageContainer.setVisibility(View.GONE);
        }

        ivClose.setOnClickListener(v -> closePopup());

        popupContainer.addView(popupView);
        popupContainer.setVisibility(View.VISIBLE);
        currentPopupView = popupView;

        popupView.setAlpha(0f);
        popupView.setScaleX(0.8f);
        popupView.setScaleY(0.8f);
        popupView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closePopup() {
        if (currentPopupView != null) {
            currentPopupView.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        popupContainer.removeAllViews();
                        popupContainer.setVisibility(View.GONE);
                        currentPopupView = null;
                    })
                    .start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_maps);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onBackPressed() {
        if (currentPopupView != null) {
            closePopup();
        } else {
            super.onBackPressed();
        }
    }
}