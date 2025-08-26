package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SignupActivity extends AppCompatActivity {

    EditText etName, etEmailSignup, etPasswordSignup, etConfirmPassword;
    Button btnSignup;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        db = new DBHelper(this);

        etName = findViewById(R.id.etName);
        etEmailSignup = findViewById(R.id.etEmailSignup);
        etPasswordSignup = findViewById(R.id.etPasswordSignup);
        etConfirmPassword = findViewById(R.id.confirmPassword);
        btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmailSignup.getText().toString().trim();
            String pass = etPasswordSignup.getText().toString();
            String confirm = etConfirmPassword.getText().toString();

            if (name.isEmpty()) { etName.setError("Enter name"); etName.requestFocus(); return; }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmailSignup.setError("Valid email"); etEmailSignup.requestFocus(); return; }
            if (pass.length() < 6) { etPasswordSignup.setError("Password >= 6 chars"); etPasswordSignup.requestFocus(); return; }
            if (!pass.equals(confirm)) { etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return; }

            if (db.isEmailExists(email)) {
                Toast.makeText(SignupActivity.this, "Email already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashed = DBHelper.sha256(pass);
            boolean ok = db.addUser(name, email, hashed);
            if (ok) {
                Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(SignupActivity.this, HomeActivity.class);
                i.putExtra("name", name);
                i.putExtra("email", email);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(SignupActivity.this, "Signup failed (db)", Toast.LENGTH_LONG).show();
            }
        });
    }
}
