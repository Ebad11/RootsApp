package com.example.rootsapp;

public class Post {
    private String userEmail;
    private String content;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private long timestamp;

    public Post() {}

    public Post(String userEmail, String content, String imageUrl, double latitude, double longitude, long timestamp) {
        this.userEmail = userEmail;
        this.content = content;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getUserEmail() { return userEmail; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
}
