package com.example.rootsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    FirebaseFirestore db;
    ListView listView;
    ArrayAdapter<String> adapter;
    List<String> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        listView = findViewById(R.id.listViewPosts);
        postList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, postList);
        listView.setAdapter(adapter);

        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    postList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        String content = doc.getString("content");
                        String userEmail = doc.getString("userEmail");
                        postList.add(userEmail + ":\n" + content);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
