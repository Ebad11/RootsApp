package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rootsapp.HomeActivity;

public class SignupActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword;
    Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmailSignup);
        etPassword = findViewById(R.id.etPasswordSignup);
        btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if(name.isEmpty() || email.isEmpty()){
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show();
            } else {
                Intent i = new Intent(SignupActivity.this, HomeActivity.class);
                i.putExtra("name", name);
                i.putExtra("email", email);
                startActivity(i);
            }
        });
    }
}
