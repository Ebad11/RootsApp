package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileEmail;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        db = FirebaseFirestore.getInstance();

        // Prefer reading from Firestore if user is logged in
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null) {
            String uid = current.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            tvProfileName.setText("Name: " + (name != null ? name : ""));
                            tvProfileEmail.setText("Email: " + (email != null ? email : ""));
                        } else {
                            // fallback to intent extras
                            setFromIntent();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setFromIntent();
                    });
        } else {
            // not logged in - use passed intent values (if any)
            setFromIntent();
        }
    }

    private void setFromIntent() {
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        if (name != null) tvProfileName.setText("Name: " + name);
        if (email != null) tvProfileEmail.setText("Email: " + email);
    }
}
