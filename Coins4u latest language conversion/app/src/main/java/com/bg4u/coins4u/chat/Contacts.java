package com.bg4u.coins4u.chat;

public class Contacts {
    private String userId; // New field for the user ID
    public String name, image;
    private String status = "Hey, welcome to this Coins 4u app";
    
    public Contacts() {
    }
    
    public Contacts(String userId, String name, String status, String image) {
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.image = image;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
}
