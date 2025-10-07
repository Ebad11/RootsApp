package com.example.rootsapp;

public class Post {
    private String id;
    private String userEmail;
    private String content;
    private long timestamp;

    public Post() {} // Needed for Firestore

    public Post(String id, String userEmail, String content, long timestamp) {
        this.id = id;
        this.userEmail = userEmail;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
