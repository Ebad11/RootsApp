package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;

    // Dummy data for login
    String demoEmail = "user@roots.com";
    String demoPassword = "12345";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.equals(demoEmail) && pass.equals(demoPassword)) {
                Intent i = new Intent(MainActivity.this, HomeActivity.class);
                i.putExtra("email", email);
                startActivity(i);
            } else {
                Toast.makeText(this, "Invalid Credentials!", Toast.LENGTH_SHORT).show();
            }
        });

        tvSignup.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(i);
        });
    }
}
