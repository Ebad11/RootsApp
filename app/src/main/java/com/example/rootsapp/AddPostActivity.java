package com.example.rootsapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPostActivity extends AppCompatActivity {

    EditText etPostContent;
    Button btnSubmit;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        etPostContent = findViewById(R.id.etPostContent);
        btnSubmit = findViewById(R.id.btnSubmit);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnSubmit.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Post cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "You must be logged in to post!", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            String userEmail = auth.getCurrentUser().getEmail();
            String id = UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();

            Map<String, Object> post = new HashMap<>();
            post.put("id", id);
            post.put("userId", userId);
            post.put("userEmail", userEmail);
            post.put("content", content);
            post.put("timestamp", timestamp);

            db.collection("posts").document(id)
                    .set(post)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Post added!", Toast.LENGTH_SHORT).show();
                        // Navigate to HomeActivity after posting
                        Intent intent = new Intent(AddPostActivity.this, FeedActivity.class);
                        startActivity(intent);
                        finish(); // Finish AddPostActivity
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
