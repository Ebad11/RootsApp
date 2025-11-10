package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_POST_NOTIF = 222;

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;
    LinearLayout loginContainer;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate called");

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        permissionHelper = new PermissionHelper(this);

        // Request all permissions explicitly
        permissionHelper.requestAllPermissions();

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

        // Face login check
        boolean faceLoginFailed = getIntent().getBooleanExtra("faceLoginFailed", false);
        if (!faceLoginFailed) {
            checkFaceLogin();
        } else {
            showLoginUI();
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
            }
        }

        // Create channel
        NotificationHelper.createChannelIfNeeded(this);

        // If already logged in, schedule notifications immediately
        if (mAuth.getCurrentUser() != null) {
            scheduleSuggestionWorkers();
        }
    }

    private void scheduleSuggestionWorkers() {
        Log.d(TAG, "Scheduling suggestion workers (15-min + immediate)");

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(SuggestionWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "roots_suggestion_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
        );

        OneTimeWorkRequest now = new OneTimeWorkRequest.Builder(SuggestionWorker.class).build();
        WorkManager.getInstance(this).enqueue(now);
    }

    private void checkFaceLogin() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            Log.d(TAG, "User already signed in: " + uid);

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String faceUrl = documentSnapshot.getString("faceUrl");

                        Log.d(TAG, "Fetched user profile: " + name + ", faceUrl: " + faceUrl);

                        if (faceUrl != null && !faceUrl.isEmpty()) {
                            Intent i = new Intent(MainActivity.this, FaceLoginActivity.class);
                            i.putExtra("name", name);
                            i.putExtra("email", user.getEmail());
                            i.putExtra("uid", uid);
                            startActivity(i);
                            finish();
                        } else {
                            launchHomeActivity(name, user.getEmail());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user profile", e);
                        showLoginUI();
                    });
        } else {
            showLoginUI();
        }
    }

    private void showLoginUI() {
        loginContainer.setVisibility(View.VISIBLE);
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

                            // Schedule notifications after login
                            scheduleSuggestionWorkers();
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
                    String name = documentSnapshot.getString("name");
                    String faceUrl = documentSnapshot.getString("faceUrl");

                    Log.d(TAG, "Fetched profile post-login: " + name + ", faceUrl: " + faceUrl);

                    if (faceUrl != null && !faceUrl.isEmpty()) {
                        Intent i = new Intent(MainActivity.this, FaceLoginActivity.class);
                        i.putExtra("name", name);
                        i.putExtra("email", user.getEmail());
                        i.putExtra("uid", uid);
                        startActivity(i);
                        finish();
                    } else {
                        launchHomeActivity(name, user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile post-login", e);
                    launchHomeActivity(null, user.getEmail());
                });
    }

    private void launchHomeActivity(String name, String email) {
        Intent i = new Intent(MainActivity.this, HomeActivity.class);
        i.putExtra("name", name);
        i.putExtra("email", email);
        startActivity(i);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.handlePermissionResult(requestCode, grantResults);
    }
}
