package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileEmail, tvProfileAvatar;
    TextView tvDetailName, tvDetailEmail, tvMemberSince;
    TextView tvMemoriesCount, tvPlacesCount;
    Button btnLogout, btnEditProfile;
    BottomNavigationView bottomNav;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String userName, userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get user data
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");

        // Initialize views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailEmail = findViewById(R.id.tvDetailEmail);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvMemoriesCount = findViewById(R.id.tvMemoriesCount);
        tvPlacesCount = findViewById(R.id.tvPlacesCount);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        bottomNav = findViewById(R.id.bottomNav);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bottom nav setup
        bottomNav.setSelectedItemId(R.id.nav_profile);
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
                Intent i = new Intent(this, MapSimpleActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(ProfileActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        // Edit profile button (placeholder for now)
        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit profile feature coming soon! ✨", Toast.LENGTH_SHORT).show();
        });

        // Load profile data
        loadProfileData();
        loadUserStats();
    }

    private void loadProfileData() {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null) {
            String uid = current.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");

                            // Set avatar letter
                            if (name != null && !name.isEmpty()) {
                                tvProfileAvatar.setText(name.substring(0, 1).toUpperCase());
                                tvProfileName.setText(name);
                                tvDetailName.setText(name);
                            } else if (email != null) {
                                tvProfileAvatar.setText(email.substring(0, 1).toUpperCase());
                                tvProfileName.setText(email.split("@")[0]);
                                tvDetailName.setText("Not set");
                            }

                            if (email != null) {
                                tvProfileEmail.setText(email);
                                tvDetailEmail.setText(email);
                            }

                            // Set member since date (from Firebase user creation)
                            long creationTime = current.getMetadata().getCreationTimestamp();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                            tvMemberSince.setText(sdf.format(new Date(creationTime)));

                        } else {
                            setFromIntent();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        setFromIntent();
                    });
        } else {
            setFromIntent();
        }
    }

    private void loadUserStats() {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null && current.getEmail() != null) {
            String email = current.getEmail();

            // Count memories
            db.collection("posts")
                    .whereEqualTo("userEmail", email)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int memoriesCount = querySnapshot.size();
                        tvMemoriesCount.setText(String.valueOf(memoriesCount));

                        // Count unique places (posts with valid locations)
                        int placesCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Double lat = doc.getDouble("latitude");
                            Double lon = doc.getDouble("longitude");
                            if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                                placesCount++;
                            }
                        }
                        tvPlacesCount.setText(String.valueOf(placesCount));
                    })
                    .addOnFailureListener(e -> {
                        tvMemoriesCount.setText("0");
                        tvPlacesCount.setText("0");
                    });
        }
    }

    private void setFromIntent() {
        if (userName != null && !userName.isEmpty()) {
            tvProfileAvatar.setText(userName.substring(0, 1).toUpperCase());
            tvProfileName.setText(userName);
            tvDetailName.setText(userName);
        } else if (userEmail != null) {
            tvProfileAvatar.setText(userEmail.substring(0, 1).toUpperCase());
            tvProfileName.setText(userEmail.split("@")[0]);
            tvDetailName.setText("Not set");
        }

        if (userEmail != null) {
            tvProfileEmail.setText(userEmail);
            tvDetailEmail.setText(userEmail);
        }

        tvMemberSince.setText("Recently");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }
}