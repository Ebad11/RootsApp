package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;
    LinearLayout loginContainer; // To show/hide login UI

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link UI
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        loginContainer = findViewById(R.id.loginContainer);

        tvSignup.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to SignupActivity");
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
        });

        btnLogin.setOnClickListener(v -> loginUser());

        // ===== FACE LOGIN CHECK =====
        boolean faceLoginFailed = getIntent().getBooleanExtra("faceLoginFailed", false);

        if (!faceLoginFailed) {
            // Only check face login if it hasn’t failed before
            checkFaceLogin();
        } else {
            Log.d(TAG, "Face login previously failed. Showing manual login UI.");
            showLoginUI();
        }

    }

    private void checkFaceLogin() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            Log.d(TAG, "User already signed in: " + uid);

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = null;
                        String faceUrl = null;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            name = documentSnapshot.getString("name");
                            faceUrl = documentSnapshot.getString("faceUrl");
                        }

                        Log.d(TAG, "Fetched user profile. Name: " + name + ", faceUrl: " + faceUrl+ "uid bhi dekh: "+uid+" docs bhi: "+documentSnapshot);

                        if (faceUrl != null && !faceUrl.isEmpty()) {
                            // Face is registered → launch FaceLoginActivity
                            Log.d(TAG, "Launching FaceLoginActivity for UID: " + uid);
                            Intent i = new Intent(MainActivity.this, FaceLoginActivity.class);
                            i.putExtra("name", name);
                            i.putExtra("email", user.getEmail());
                            i.putExtra("uid", uid); // pass UID to load registered face
                            startActivity(i);
                            finish();
                        } else {
                            // No face registered → normal login
                            Log.d(TAG, "No face registered. Showing manual login UI.");
                            showLoginUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user profile", e);
                        // fallback to manual login
                        showLoginUI();
                    });
        } else {
            Log.d(TAG, "No signed-in user. Showing manual login UI.");
            showLoginUI();
        }
    }

    private void showLoginUI() {
        loginContainer.setVisibility(View.VISIBLE); // Make login fields visible
    }

    private void loginUser() {
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
        Log.d(TAG, "Attempting manual login for: " + email);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Manual login successful. UID: " + user.getUid());
                            fetchUserProfile(user);
                        } else {
                            Log.w(TAG, "Manual login succeeded but user is null");
                            Toast.makeText(this, "Login succeeded but user is null", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Log.e(TAG, "Manual login failed: " + msg);
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserProfile(FirebaseUser user) {
        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = null;
                    String faceUrl = null;

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        name = documentSnapshot.getString("name");
                        faceUrl = documentSnapshot.getString("faceUrl");
                    }

                    Log.d(TAG, "Fetched user profile post-login. Name: " + name + ", faceUrl: " + faceUrl+" docs bhi: "+documentSnapshot+ " uid bhi dekh: "+uid);

                    if (faceUrl != null && !faceUrl.isEmpty()) {
                        // Face is registered → launch FaceLoginActivity
                        Log.d(TAG, "Launching FaceLoginActivity post-login for UID: " + uid);
                        Intent i = new Intent(MainActivity.this, FaceLoginActivity.class);
                        i.putExtra("name", name);
                        i.putExtra("email", user.getEmail());
                        i.putExtra("uid", uid);
                        startActivity(i);
                        finish();
                    } else {
                        // No face registered → normal login
                        Log.d(TAG, "No face registered post-login. Launching HomeActivity.");
                        Intent i = new Intent(MainActivity.this, HomeActivity.class);
                        i.putExtra("name", name);
                        i.putExtra("email", user.getEmail());
                        startActivity(i);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile post-login", e);
                    Intent i = new Intent(MainActivity.this, HomeActivity.class);
                    i.putExtra("email", user.getEmail());
                    startActivity(i);
                    finish();
                });
    }
}
