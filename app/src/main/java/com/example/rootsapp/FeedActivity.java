package com.example.rootsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    FirebaseFirestore db;
    RecyclerView recyclerView;
    PostAdapter adapter;
    List<Post> postList;
    BottomNavigationView bottomNav;
    String userName, userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        // Get user data
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewPosts);
        bottomNav = findViewById(R.id.bottomNav);

        postList = new ArrayList<>();
        adapter = new PostAdapter(this, postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set Feed as selected
//        bottomNav.setSelectedItemId(R.id.nav_feed);

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

//            if (id == R.id.nav_feed) {
//                // Already on Feed, do nothing
//                return true;
            if (id == R.id.nav_add_post) {
                Intent i = new Intent(this, AddPostActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0); // No animation for smooth feel
                return true;
            } else if (id == R.id.nav_maps) {
                Intent i = new Intent(this, MapSimpleActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.putExtra("name", userName);
                i.putExtra("email", userEmail);
                startActivity(i);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Load posts
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    postList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Post post = doc.toObject(Post.class);
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep Feed selected when returning to this activity
//        bottomNav.setSelectedItemId(R.id.nav_feed);
    }
}