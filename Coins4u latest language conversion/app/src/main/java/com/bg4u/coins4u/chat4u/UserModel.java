package com.bg4u.coins4u.chat4u;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for Users in the chat system.
 * Stores user profile information and online status.
 */
public class UserModel {
    private String uid;
    private String name;
    private String profile;
    private String status;
    private String phone;
    private String email;
    private String token; // FCM token for notifications
    private boolean online;
    @ServerTimestamp
    private Date lastSeen;

    // Empty constructor required for Firestore
    public UserModel() {
    }

    public UserModel(String uid, String name, String profile, String status) {
        this.uid = uid;
        this.name = name;
        this.profile = profile;
        this.status = status;
        this.online = false;
    }

    @Exclude
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}