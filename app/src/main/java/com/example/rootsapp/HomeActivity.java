package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnProfile = findViewById(R.id.btnProfile);

        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");

        if(name != null) {
            tvWelcome.setText("Welcome " + name + " to Roots!");
        }

        btnProfile.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
            i.putExtra("name", name);
            i.putExtra("email", email);
            startActivity(i);
        });
    }
}
