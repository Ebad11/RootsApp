package com.example.rootsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FaceLoginActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ProgressBar progressBar;
    private Executor cameraExecutor;

    private List<Bitmap> capturedFaces = new ArrayList<>();
    private int captureCount = 0;
    private final int MAX_CAPTURES = 3;
    private final int INTERVAL_MS = 2000;

    private Bitmap registeredFaceBitmap;
    float SIMILARITY_THRESHOLD = 0.8f; // instead of 0.9


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_login);

        previewView = findViewById(R.id.cameraContainerLogin);
        progressBar = findViewById(R.id.progressBarLogin);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        loadRegisteredFace();
    }

    private void loadRegisteredFace() {
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            File faceFile = new File(getCacheDir(), "face_" + uid + ".jpg");
            if (!faceFile.exists()) {
                Toast.makeText(this, "No registered face found. Login manually.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            FileInputStream fis = new FileInputStream(faceFile);
            registeredFaceBitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            Log.d("FaceLogin", "Registered face bitmap loaded: " + faceFile.getAbsolutePath());

            startCamera();

        } catch (Exception e) {
            Log.e("FaceLogin", "Error loading registered face", e);
            Toast.makeText(this, "Error loading registered face", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceLogin", "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        FaceDetector faceDetector = FaceDetection.getClient(options);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        cameraExecutor = ContextCompat.getMainExecutor(this);

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> analyzeFrame(faceDetector, imageProxy));

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeFrame(FaceDetector detector, ImageProxy imageProxy) {
        if (captureCount >= MAX_CAPTURES) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeExperimentalUsageError")
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        Bitmap bitmap = previewView.getBitmap();
                        if (bitmap != null) {
                            capturedFaces.add(bitmap);
                            captureCount++;
                            progressBar.setProgress((captureCount * 100) / MAX_CAPTURES);
                            Log.d("FaceLogin", "Captured face " + captureCount);

                            if (captureCount == MAX_CAPTURES) {
                                verifyFaces();
                            }
                        }
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("FaceLogin", "Face detection failed", e);
                    imageProxy.close();
                });
    }

    private void verifyFaces() {
        Log.d("FaceLogin", "Verifying captured faces...");
        new Thread(() -> {
            boolean matched = false;
            for (Bitmap faceBitmap : capturedFaces) {
                float similarity = getBitmapSimilarity(faceBitmap, registeredFaceBitmap);
                Log.d("FaceLogin", "Face similarity: " + similarity);
                if (similarity > SIMILARITY_THRESHOLD) {
                    matched = true;
                    break;
                }
            }

            boolean finalMatch = matched;
            runOnUiThread(() -> {
                if (finalMatch) {
                    Log.d("FaceLogin", "Face recognized! Navigating to HomeActivity.");
                    Toast.makeText(this, "Face recognized! Logging in...", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(FaceLoginActivity.this, HomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("name", getIntent().getStringExtra("name"));
                    i.putExtra("email", getIntent().getStringExtra("email"));
                    startActivity(i);
                } else {
                    Log.d("FaceLogin", "Face not recognized. Redirecting to manual login.");
                    Toast.makeText(this, "Face not recognized. Please login manually.", Toast.LENGTH_LONG).show();

                    // Redirect to MainActivity (manual login)
                    Intent i = new Intent(FaceLoginActivity.this, MainActivity.class);
                    i.putExtra("faceLoginFailed", true);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }
            });
        }).start();
    }

    // Simple similarity: pixel-wise comparison normalized to [0,1]
    private float getBitmapSimilarity(Bitmap bmp1, Bitmap bmp2) {
        Bitmap b1 = Bitmap.createScaledBitmap(bmp1, 100, 100, false);
        Bitmap b2 = Bitmap.createScaledBitmap(bmp2, 100, 100, false);

        int width = b1.getWidth();
        int height = b1.getHeight();
        long diff = 0;

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int pixel1 = b1.getPixel(x, y);
                int pixel2 = b2.getPixel(x, y);
                int r = Math.abs(((pixel1 >> 16) & 0xff) - ((pixel2 >> 16) & 0xff));
                int g = Math.abs(((pixel1 >> 8) & 0xff) - ((pixel2 >> 8) & 0xff));
                int b = Math.abs((pixel1 & 0xff) - (pixel2 & 0xff));
                diff += r + g + b;
            }

        double maxDiff = 3L * 255 * width * height;
        return 1.0f - ((float) diff / (float) maxDiff);
    }
}
