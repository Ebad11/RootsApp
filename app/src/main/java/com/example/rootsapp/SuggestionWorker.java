package com.example.rootsapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SuggestionWorker extends Worker {
    private static final String TAG = "SuggestionWorker";
    private static final String GEMINI_API_KEY = "AIzaSyBEGPtOHTce71eQxNmFpUf2SnHA3rM0v8Q";

    public SuggestionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: SuggestionWorker started");

        Context ctx = getApplicationContext();

        // Run only if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Skipping suggestion worker.");
            return Result.success();
        }

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No location permission - aborting suggestion fetch");
            return Result.success();
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);

        final CountDownLatch latch = new CountDownLatch(1);
        final double[] latlon = new double[2];
        final boolean[] gotLocation = {false};

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latlon[0] = location.getLatitude();
                        latlon[1] = location.getLongitude();
                        gotLocation[0] = true;
                        Log.d(TAG, "Location obtained: " + latlon[0] + "," + latlon[1]);
                    } else {
                        Log.w(TAG, "Last location null");
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    latch.countDown();
                });

        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for location", e);
        }

        if (!gotLocation[0]) {
            Log.w(TAG, "No location available - aborting suggestion");
            return Result.success();
        }

        String prompt = buildPrompt(latlon[0], latlon[1]);
        String aiText = callGemini(prompt);

        if (aiText == null || aiText.isEmpty()) {
            Log.w(TAG, "Gemini returned empty text");
            return Result.success();
        }

        NotificationHelper.notifySimple(ctx, "Nearby suggestions", aiText);

        return Result.success();
    }

    private String buildPrompt(double lat, double lon) {
        return "You are a helpful local guide. Given the coordinates: " + lat + "," + lon +
                ". Suggest up to 4 interesting places nearby (restaurant, tourist site, cafe, or park). " +
                "For each suggestion give: Name — Type — approximate distance — one short reason why it's worth visiting. Also please don't use * asterisk in your response";
    }

    private String callGemini(String prompt) {
        try {
            JSONObject textPart = new JSONObject().put("text", prompt);
            JSONArray partsArray = new JSONArray().put(textPart);
            JSONObject contentsObject = new JSONObject().put("parts", partsArray);
            JSONArray contentsArray = new JSONArray().put(contentsObject);
            JSONObject requestBodyJson = new JSONObject().put("contents", contentsArray);

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(requestBodyJson.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;
            Request request = new Request.Builder().url(url).post(body).build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Gemini request failed: " + response.code() + " - " + response.message());
                return null;
            }
            String respBody = response.body() != null ? response.body().string() : "";
            if (respBody.isEmpty()) return null;

            JSONObject json = new JSONObject(respBody);
            if (json.has("candidates")) {
                JSONArray candidates = json.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                JSONObject p0 = parts.getJSONObject(0);
                                if (p0.has("text")) {
                                    return p0.getString("text").trim();
                                }
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "No candidates/text found in Gemini response");
        } catch (Exception e) {
            Log.e(TAG, "callGemini error", e);
        }
        return null;
    }
}
