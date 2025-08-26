package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileName, tvProfileEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");

        if (name != null) tvProfileName.setText("Name: " + name);
        if (email != null) tvProfileEmail.setText("Email: " + email);
    }
}
