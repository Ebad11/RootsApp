package com.example.rootsapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPostActivity extends AppCompatActivity {

    EditText etPostContent;
    Button btnSubmit, btnAutoLocation, btnChooseLocation, btnPickImage, btnTakePhoto;
    ImageView ivPreview;
    FirebaseFirestore db;
    FirebaseAuth auth;
    FusedLocationProviderClient fusedLocationClient;
    Cloudinary cloudinary;

    double selectedLat = 0.0, selectedLon = 0.0;
    Uri imageUri = null;
    File photoFile = null;

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int MAP_PICKER_REQUEST = 102;
    private static final int PICK_IMAGE_REQUEST = 103;
    private static final int CAMERA_REQUEST = 104;
    private File getFileFromUri(Uri uri) throws IOException {
        File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
             java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        etPostContent = findViewById(R.id.etPostContent);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAutoLocation = findViewById(R.id.btnAutoLocation);
        btnChooseLocation = findViewById(R.id.btnChooseLocation);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        ivPreview = findViewById(R.id.ivPreview);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Cloudinary
        Map config = new HashMap();
        config.put("cloud_name", "dqkgride1");
        config.put("api_key", "595934896864127");
        config.put("api_secret", "Im0dm6IQVkZetCbBVS-kTJzwHTs");
        cloudinary = new Cloudinary(config);

        btnPickImage.setOnClickListener(v -> openGallery());
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnAutoLocation.setOnClickListener(v -> getCurrentLocation());
        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_PICKER_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> uploadPost());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            photoFile = File.createTempFile("photo_", ".jpg", getExternalFilesDir("Pictures"));
            imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectedLat = location.getLatitude();
                        selectedLon = location.getLongitude();
                        Toast.makeText(this, "Location set ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get location!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPost() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Post cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to post!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            String imageUrl = null;
            try {
                if (imageUri != null) {
                    File fileToUpload = getFileFromUri(imageUri);
                    Map uploadResult = cloudinary.uploader().upload(fileToUpload, ObjectUtils.emptyMap());
                    imageUrl = (String) uploadResult.get("secure_url");
                }

                String id = UUID.randomUUID().toString();
                String userEmail = auth.getCurrentUser().getEmail();
                long timestamp = System.currentTimeMillis();

                Map<String, Object> post = new HashMap<>();
                post.put("id", id);
                post.put("userEmail", userEmail);
                post.put("content", content);
                post.put("timestamp", timestamp);
                post.put("latitude", selectedLat);
                post.put("longitude", selectedLon);
                post.put("imageUrl", imageUrl);

                db.collection("posts").document(id)
                        .set(post)
                        .addOnSuccessListener(a -> {
                            pd.dismiss();
                            Toast.makeText(this, "Memory added!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, FeedActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            pd.dismiss();
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } catch (Exception e) {
                pd.dismiss();
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivPreview.setImageURI(imageUri);
            ivPreview.setVisibility(ImageView.VISIBLE);
        }

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            ivPreview.setImageBitmap(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
            ivPreview.setVisibility(ImageView.VISIBLE);
        }

        if (requestCode == MAP_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("latitude", 0.0);
            selectedLon = data.getDoubleExtra("longitude", 0.0);
            Toast.makeText(this, "Location chosen ✅", Toast.LENGTH_SHORT).show();
        }
    }
}
