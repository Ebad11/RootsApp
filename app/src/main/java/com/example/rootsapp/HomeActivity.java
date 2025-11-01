package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the user data
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");

        // Immediately redirect to FeedActivity
        Intent i = new Intent(HomeActivity.this, FeedActivity.class);
        i.putExtra("name", name);
        i.putExtra("email", email);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}