package com.example.rootsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class FaceCaptureActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ProgressBar progressBar;

    private Cloudinary cloudinary;
    private Executor cameraExecutor;
    private boolean faceCaptured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);

        previewView = findViewById(R.id.cameraContainer);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        // Cloudinary config
        Map config = ObjectUtils.asMap(
                "cloud_name", "dqkgride1",
                "api_key", "595934896864127",
                "api_secret", "Im0dm6IQVkZetCbBVS-kTJzwHTs"
        );
        cloudinary = new Cloudinary(config);

        startCamera();
    }

    private void startCamera() {
        Log.d("FaceCapture", "Starting CameraX...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceCapture", "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Log.d("FaceCapture", "Binding camera preview...");
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Face detection options
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();

        FaceDetector faceDetector = FaceDetection.getClient(options);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        cameraExecutor = ContextCompat.getMainExecutor(this);

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            processImageProxy(faceDetector, imageProxy);
        });

        Camera camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis
        );

        Log.d("FaceCapture", "Camera bound successfully");
    }

    private void processImageProxy(FaceDetector detector, ImageProxy imageProxy) {
        if (faceCaptured) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeExperimentalUsageError")
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();

        if (planes.length < 1 || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d("FaceCapture", "Faces detected: " + faces.size());
                    if (faces.size() > 0 && !faceCaptured) {
                        faceCaptured = true;
                        // Capture first detected face after 3 sec
                        progressBar.setProgress(50);
                        new Handler().postDelayed(() -> {
                            captureFace(imageProxy, faces.get(0));
                        }, 3000);
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("FaceCapture", "Face detection failed", e);
                    imageProxy.close();
                });
    }

    private void captureFace(ImageProxy imageProxy, Face face) {
        Log.d("FaceCapture", "Capturing face bitmap...");

        @SuppressWarnings("UnsafeExperimentalUsageError")
        Bitmap bitmap = previewView.getBitmap();

        if (bitmap == null) {
            Log.e("FaceCapture", "Bitmap capture failed");
            Toast.makeText(this, "Face capture failed, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(getCacheDir(), "face.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            Log.d("FaceCapture", "Face bitmap saved: " + file.getAbsolutePath());

            progressBar.setProgress(70);

            // Upload to Cloudinary in background
            new Thread(() -> {
                try {
                    Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
                    String imageUrl = (String) uploadResult.get("secure_url");
                    Log.d("FaceCapture", "Face uploaded successfully: " + imageUrl);
                    // After successful Cloudinary upload
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("faceUrl", imageUrl);

                    db.collection("users").document(uid)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> Log.d("FaceCapture", "Face URL saved in Firestore"))
                            .addOnFailureListener(e -> Log.e("FaceCapture", "Failed to save face URL", e));
                    // After successful Cloudinary upload
                    File faceFile = new File(getCacheDir(), "face_" + uid + ".jpg");
                    FileOutputStream fos1 = new FileOutputStream(faceFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos1);
                    fos1.flush();
                    fos1.close();
                    Log.d("FaceCapture", "Face bitmap saved locally for recognition: " + faceFile.getAbsolutePath());



                    runOnUiThread(() -> {
                        progressBar.setProgress(100);
                        Toast.makeText(this, "Face registered successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to HomeActivity
                        Intent i = new Intent(FaceCaptureActivity.this, HomeActivity.class);
                        i.putExtra("name", getIntent().getStringExtra("name")); // pass name
                        i.putExtra("email", getIntent().getStringExtra("email")); // pass email
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    });


                } catch (Exception e) {
                    Log.e("FaceCapture", "Cloudinary upload failed", e);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Face upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            Log.e("FaceCapture", "Error saving bitmap", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("FaceCapture", "Activity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("FaceCapture", "Activity destroyed");
    }
}
