package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize Firebase (safe to call even if auto-initialized)
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString();

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter valid email");
                etEmail.requestFocus();
                return;
            }
            if (pass.isEmpty() || pass.length() < 6) {
                etPassword.setError("Password must be ≥ 6 chars");
                etPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                // fetch profile name from Firestore (users/{uid})
                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            String name = null;
                                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                                name = documentSnapshot.getString("name");
                                            }
                                            Intent i = new Intent(MainActivity.this, HomeActivity.class);
                                            i.putExtra("name", name);
                                            i.putExtra("email", user.getEmail());
                                            startActivity(i);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            // proceed anyway if profile read fails
                                            Intent i = new Intent(MainActivity.this, HomeActivity.class);
                                            i.putExtra("email", user.getEmail());
                                            startActivity(i);
                                            finish();
                                        });
                            } else {
                                Toast.makeText(MainActivity.this, "Login succeeded but user is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(MainActivity.this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvSignup.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignupActivity.class)));
    }
}
