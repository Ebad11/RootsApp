package com.example.rootsapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPostActivity extends AppCompatActivity {

    private static final String TAG = "AddPostActivity";

    EditText etPostContent;
    ImageButton btnSubmit, btnPickImage, btnTakePhoto, btnChooseLocation, btnAutoLocation, btnVoiceInput;
    ImageView ivPreview, ivRemoveImage;
    ProgressBar progressBar;
    BottomNavigationView bottomNav;
    Button btnUpload;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FusedLocationProviderClient fusedLocationClient;
    Cloudinary cloudinary;

    double selectedLat = 0.0, selectedLon = 0.0;
    Uri imageUri = null;
    File photoFile = null;
    String userName, userEmail;

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int MAP_PICKER_REQUEST = 102;
    private static final int PICK_IMAGE_REQUEST = 103;
    private static final int CAMERA_REQUEST = 104;
    private static final int VOICE_REQUEST_CODE = 200;

    private static final String GEMINI_API_KEY = "AIzaSyBEGPtOHTce71eQxNmFpUf2SnHA3rM0v8Q";

    // Will hold the Gemini generated caption
    private String generatedCaption = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        // User info
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");

        // Views
        etPostContent = findViewById(R.id.etPostContent);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseLocation = findViewById(R.id.btnChooseLocation);
        btnAutoLocation = findViewById(R.id.btnAutoLocation);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        ivPreview = findViewById(R.id.ivPreview);
        ivRemoveImage = findViewById(R.id.ivRemoveImage);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.bottomNav);

        // Upload button already in XML
        btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setVisibility(View.GONE);

        // Firebase & Location
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Cloudinary setup
        Map config = new HashMap();
        config.put("cloud_name", "dqkgride1");
        config.put("api_key", "595934896864127");
        config.put("api_secret", "Im0dm6IQVkZetCbBVS-kTJzwHTs");
        cloudinary = new Cloudinary(config);

        // Bottom nav
        bottomNav.setSelectedItemId(R.id.nav_add_post);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add_post) return true;
            else if (id == R.id.nav_maps) {
                startActivity(new Intent(this, MapSimpleActivity.class)
                        .putExtra("name", userName).putExtra("email", userEmail));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class)
                        .putExtra("name", userName).putExtra("email", userEmail));
                overridePendingTransition(0,0);
                return true;
            }
            return false;
        });

        // Listeners
        btnPickImage.setOnClickListener(v -> openGallery());
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnAutoLocation.setOnClickListener(v -> getCurrentLocation());
        btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_PICKER_REQUEST);
        });
        btnVoiceInput.setOnClickListener(v -> startVoiceInput());
        ivRemoveImage.setOnClickListener(v -> {
            imageUri = null;
            photoFile = null;
            ivPreview.setImageURI(null);
            Toast.makeText(this,"Image removed",Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> generateCaption());
        btnUpload.setOnClickListener(v -> uploadPost(generatedCaption));
    }

    /** ================= Gemini Caption ================== **/
    private void generateCaption() {
        String userText = etPostContent.getText().toString().trim();
        if(userText.isEmpty()) {
            Toast.makeText(this,"Please type or speak something",Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject imagePart = null;
            if(imageUri != null) {
                String base64 = uriToBase64(imageUri);
                imagePart = new JSONObject().put("inlineData",
                        new JSONObject().put("mimeType","image/jpeg").put("data",base64));
            }

            String fullPrompt = "Create a catchy 2-3 lines post for social media using the following info:\n" +
                    "Text: "+userText+"\n"+
                    "Location: "+selectedLat+", "+selectedLon+"\n"+
                    "Make it engaging, short, and fun. Reference the image.";

            JSONObject textPart = new JSONObject().put("text", fullPrompt);
            org.json.JSONArray partsArray = new org.json.JSONArray();
            if(imagePart!=null) partsArray.put(imagePart);
            partsArray.put(textPart);

            JSONObject contentsObject = new JSONObject().put("parts",partsArray);
            org.json.JSONArray contentsArray = new org.json.JSONArray().put(contentsObject);
            JSONObject requestBodyJson = new JSONObject().put("contents",contentsArray);

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(requestBodyJson.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;
            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(AddPostActivity.this,"AI failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body()!=null ? response.body().string() : "";
                    if(response.isSuccessful()){
                        try{
                            JSONObject json = new JSONObject(respBody);
                            String caption = "";
                            if(json.has("candidates")){
                                org.json.JSONArray candidates = json.getJSONArray("candidates");
                                if(candidates.length()>0){
                                    JSONObject candidate = candidates.getJSONObject(0);
                                    JSONObject content = candidate.getJSONObject("content");
                                    org.json.JSONArray parts = content.getJSONArray("parts");
                                    if(parts.length()>0 && parts.getJSONObject(0).has("text"))
                                        caption = parts.getJSONObject(0).getString("text");
                                }
                            }
                            generatedCaption = caption;
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnSubmit.setEnabled(true);
                                if(!generatedCaption.isEmpty()){
                                    etPostContent.setText(generatedCaption);
                                    btnUpload.setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(AddPostActivity.this,"AI returned empty text",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(AddPostActivity.this,"AI request failed",Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private String uriToBase64(Uri uri) throws IOException {
        Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);

        int maxDim = 1024;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        float scale = Math.min((float)maxDim/width, (float)maxDim/height);
        if(scale<1){
            bmp = Bitmap.createScaledBitmap(bmp, Math.round(width*scale), Math.round(height*scale), true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG,50,baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    /** ================= Firestore Upload ================== **/
//    private void uploadPost(String content) {
//        if(content==null || content.isEmpty()){
//            Toast.makeText(this,"Nothing to upload",Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        btnUpload.setEnabled(false);
//        progressBar.setVisibility(View.VISIBLE);
//
//        new Thread(() -> {
//            try{
//                String imageUrl = null;
//                if(imageUri!=null){
//                    File f = new File(imageUri.getPath());
//                    Map uploadResult = cloudinary.uploader().upload(f, ObjectUtils.emptyMap());
//                    imageUrl = (String) uploadResult.get("secure_url");
//                }
//
//                String id = UUID.randomUUID().toString();
//                Map<String,Object> post = new HashMap<>();
//                post.put("id",id);
//                post.put("userEmail",auth.getCurrentUser().getEmail());
//                post.put("content",content);
//                post.put("timestamp",System.currentTimeMillis());
//                post.put("latitude",selectedLat);
//                post.put("longitude",selectedLon);
//                post.put("imageUrl",imageUrl);
//
//                db.collection("posts").document(id).set(post)
//                        .addOnSuccessListener(a -> runOnUiThread(() -> {
//                            Toast.makeText(AddPostActivity.this,"Memory posted!",Toast.LENGTH_SHORT).show();
//                            progressBar.setVisibility(View.GONE);
//                            btnUpload.setEnabled(true);
//                            finish();
//                        }))
//                        .addOnFailureListener(e -> runOnUiThread(() -> {
//                            Toast.makeText(AddPostActivity.this,"Upload failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();
//                            progressBar.setVisibility(View.GONE);
//                            btnUpload.setEnabled(true);
//                        }));
//
//            } catch (Exception e){
//                e.printStackTrace();
//                runOnUiThread(() -> {
//                    Toast.makeText(AddPostActivity.this,"Upload failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
//                    progressBar.setVisibility(View.GONE);
//                    btnUpload.setEnabled(true);
//                });
//            }
//        }).start();
//    }
    private void uploadPost(String content) {
        if(content==null || content.isEmpty()){
            Toast.makeText(this,"Nothing to upload",Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String imageUrl = null;
                if(imageUri != null){
                    // Convert Uri to bytes
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] data = baos.toByteArray();

                    Map uploadResult = cloudinary.uploader().upload(data, ObjectUtils.emptyMap());
                    imageUrl = (String) uploadResult.get("secure_url");
                }

                String id = UUID.randomUUID().toString();
                Map<String,Object> post = new HashMap<>();
                post.put("id",id);
                post.put("userEmail",auth.getCurrentUser().getEmail());
                post.put("content",content);
                post.put("timestamp",System.currentTimeMillis());
                post.put("latitude",selectedLat);
                post.put("longitude",selectedLon);
                post.put("imageUrl",imageUrl);

                db.collection("posts").document(id).set(post)
                        .addOnSuccessListener(a -> runOnUiThread(() -> {
                            Toast.makeText(AddPostActivity.this,"Memory posted!",Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            Toast.makeText(AddPostActivity.this,"Upload failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                        }));

            } catch (Exception e){
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(AddPostActivity.this,"Upload failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                });
            }
        }).start();
    }


    /** ================= Gallery / Camera / Location / Voice ================== **/
    private void openGallery(){
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_IMAGE_REQUEST);
    }

    private void openCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try{
            photoFile = File.createTempFile("photo_",".jpg",getExternalFilesDir("Pictures"));
            imageUri = FileProvider.getUriForFile(this,getPackageName()+".provider",photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
            startActivityForResult(intent,CAMERA_REQUEST);
        }catch(IOException e){
            e.printStackTrace();
            Toast.makeText(this,"Camera error",Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if(location!=null){
                selectedLat = location.getLatitude();
                selectedLon = location.getLongitude();
                Toast.makeText(this,"Location set",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Unable to get location",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startVoiceInput(){
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            Toast.makeText(this,"Speech recognition not available",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak your memory...");
        startActivityForResult(intent, VOICE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==PICK_IMAGE_REQUEST && resultCode==RESULT_OK && data!=null){
            imageUri = data.getData();
            ivPreview.setImageURI(imageUri);
        }
        if(requestCode==CAMERA_REQUEST && resultCode==RESULT_OK){
            ivPreview.setImageBitmap(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
        }
        if(requestCode==MAP_PICKER_REQUEST && resultCode==RESULT_OK && data!=null){
            selectedLat = data.getDoubleExtra("latitude",0.0);
            selectedLon = data.getDoubleExtra("longitude",0.0);
        }
        if(requestCode==VOICE_REQUEST_CODE && resultCode==RESULT_OK && data!=null){
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(results!=null && results.size()>0) etPostContent.setText(results.get(0));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bottomNav!=null) bottomNav.setSelectedItemId(R.id.nav_add_post);
    }
}
