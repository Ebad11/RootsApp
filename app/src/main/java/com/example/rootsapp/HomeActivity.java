package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnProfile, btnLogout;

    Button btnAddPost, btnViewFeed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);


        btnAddPost = findViewById(R.id.btnAddPost);
        btnViewFeed = findViewById(R.id.btnViewFeed);

        btnAddPost.setOnClickListener(v -> startActivity(new Intent(this, AddPostActivity.class)));
        btnViewFeed.setOnClickListener(v -> startActivity(new Intent(this, FeedActivity.class)));


        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");

        if (name != null && !name.isEmpty()) tvWelcome.setText("Welcome " + name + " to Roots!");
        else if (email != null) tvWelcome.setText("Welcome " + email + " to Roots!");

        btnProfile.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
            i.putExtra("name", name);
            i.putExtra("email", email);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(HomeActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
