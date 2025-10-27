package com.example.rootsapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPostActivity extends AppCompatActivity {

    EditText etPostContent;
    Button btnSubmit, btnAutoLocation, btnChooseLocation;
    FirebaseFirestore db;
    FirebaseAuth auth;
    FusedLocationProviderClient fusedLocationClient;

    double selectedLat = 0.0, selectedLon = 0.0;

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int MAP_PICKER_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        etPostContent = findViewById(R.id.etPostContent);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAutoLocation = findViewById(R.id.btnAutoLocation);
        btnChooseLocation = findViewById(R.id.btnChooseLocation);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnAutoLocation.setOnClickListener(v -> getCurrentLocation());
        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_PICKER_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> uploadPost());
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectedLat = location.getLatitude();
                        selectedLon = location.getLongitude();
                        Toast.makeText(this, "Location set ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get location!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPost() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Post cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to post!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLat == 0.0 && selectedLon == 0.0) {
            Toast.makeText(this, "Select or auto-detect a location first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = UUID.randomUUID().toString();
        String userEmail = auth.getCurrentUser().getEmail();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> post = new HashMap<>();
        post.put("id", id);
        post.put("userEmail", userEmail);
        post.put("content", content);
        post.put("timestamp", timestamp);
        post.put("latitude", selectedLat);
        post.put("longitude", selectedLon);

        db.collection("posts").document(id)
                .set(post)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Memory added!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, FeedActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("latitude", 0.0);
            selectedLon = data.getDoubleExtra("longitude", 0.0);
            Toast.makeText(this, "Location chosen ✅", Toast.LENGTH_SHORT).show();
        }
    }
}
