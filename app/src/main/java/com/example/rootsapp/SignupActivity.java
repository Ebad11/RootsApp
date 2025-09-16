package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText etName, etEmailSignup, etPasswordSignup, etConfirmPassword;
    Button btnSignup;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmailSignup = findViewById(R.id.etEmailSignup);
        etPasswordSignup = findViewById(R.id.etPasswordSignup);
        etConfirmPassword = findViewById(R.id.confirmPassword); // make sure ID matches your XML
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

            btnSignup.setEnabled(false);
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        btnSignup.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                Map<String, Object> map = new HashMap<>();
                                map.put("name", name);
                                map.put("email", email);
                                // write profile into Firestore
                                db.collection("users").document(uid).set(map)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                                            Intent i = new Intent(SignupActivity.this, HomeActivity.class);
                                            i.putExtra("name", name);
                                            i.putExtra("email", email);
                                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(i);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            // If profile save fails, still allow login but notify user
                                            Toast.makeText(SignupActivity.this, "Signup succeeded but profile save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                Toast.makeText(SignupActivity.this, "Signup succeeded but user is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Signup failed";
                            Toast.makeText(SignupActivity.this, "Signup failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
