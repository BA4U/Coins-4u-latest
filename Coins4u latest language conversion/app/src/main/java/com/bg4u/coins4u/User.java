package com.bg4u.coins4u;

import java.util.Date;

public class User{
    private String name, email, pass, referCode, profile, phoneNumber, uid, location, age, socialMediaLink, token;
    
    private int easyWin = 0, easyDraw = 0, easyLost = 0,
            mediumWin = 0, mediumDraw = 0, mediumLost = 0,
            hardWin = 0, hardDraw = 0, hardLost = 0,
            onlineWin = 0, onlineDraw = 0, onlineLost = 0;
    
    private int coins = 25; // Default value for coins
    private int dailyCoinsLimit = 1500; // Default value for coins
    private String bio = "Play, Learn and Earn. This is the Coins 4u app - Anuj";
    private String userState;
    private boolean subscription;
    private int correctAnswers;
    private int wrongAnswers;
    private int taskCompleted = 0;
    // Premium plan fields
    private boolean basicPlan;
    private boolean standardPlan;
    private boolean premiumPlan;
    
    private Date basicPlanDeactivationDate;
    private Date standardPlanDeactivationDate;
    private Date premiumPlanDeactivationDate;
    
    private Date premiumActivationDate;
    private Date premiumDeactivationDate;
    private Date lastRewardedDate;
    
    // Add a constant for the default profile image
    public User() {
    }
    public User(String name, String email, String pass, String referCode) {
        this.name = name;
        this.email = email;
        this.pass = pass;
        this.referCode = referCode;
    }
  
    // email binding constructor
    public User(String email, String password) {
        this.email = email;
        this.pass = password;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPass() {
        return pass;
    }
    public void setPass(String pass) {
        this.pass = pass;
    }
    public String getReferCode() {
        return referCode;
    }
    public void setReferCode(String referCode) {
        this.referCode = referCode;
    }
    public int getCoins() {
        return coins;
    }
    public void setCoins(int coins) {
        this.coins = coins;
    }
    public String getProfile() {
        return profile;
    }
    public void setProfile(String profile) {
        this.profile = profile;
    }
    public String getBio() {
        return bio;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }
    public String getSocialMediaLink() {
        return socialMediaLink;
    }
    public void setSocialMediaLink(String socialMediaLink) {
        this.socialMediaLink = socialMediaLink;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public int getCorrectAnswers() {
        return correctAnswers;
    }
    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    public int getWrongAnswers() {
        return wrongAnswers;
    }
    public void setWrongAnswers(int wrongAnswers) {
        this.wrongAnswers = wrongAnswers;
    }
   
    // Getters and setters for premium plans
    public boolean isBasicPlan() { return basicPlan; }
    public void setBasicPlan(boolean basicPlan) {
        this.basicPlan = basicPlan;
    }
    
    public boolean isStandardPlan() {
        return standardPlan;
    }
    public void setStandardPlan(boolean standardPlan) {
        this.standardPlan = standardPlan;
    }
    
    public boolean isPremiumPlan() {
        return premiumPlan;
    }
    public void setPremiumPlan(boolean premiumPlan) {
        this.premiumPlan = premiumPlan;
    }

    public void setPremiumActivationDate(Date premiumActivationDate) {
        this.premiumActivationDate = premiumActivationDate;
    }
    
    public boolean isSubscription() {
        return subscription;
    }
    
    public void setSubscription(boolean subscription) {
        this.subscription = subscription;
    }
    
    public int getEasyWin() {
        return easyWin;
    }
    
    public void setEasyWin(int easyWin) {
        this.easyWin = easyWin;
    }
    
    public int getEasyDraw() {
        return easyDraw;
    }
    
    public void setEasyDraw(int easyDraw) {
        this.easyDraw = easyDraw;
    }
    
    public int getEasyLost() {
        return easyLost;
    }
    
    public void setEasyLost(int easyLost) {
        this.easyLost = easyLost;
    }
    
    public int getMediumDraw() {
        return mediumDraw;
    }
    
    public void setMediumDraw(int mediumDraw) {
        this.mediumDraw = mediumDraw;
    }
    
    public int getMediumWin() {
        return mediumWin;
    }
    
    public void setMediumWin(int mediumWin) {
        this.mediumWin = mediumWin;
    }
    
    public int getMediumLost() {
        return mediumLost;
    }
    
    public void setMediumLost(int mediumLost) {
        this.mediumLost = mediumLost;
    }
    
    public int getHardWin() {
        return hardWin;
    }
    
    public void setHardWin(int hardWin) {
        this.hardWin = hardWin;
    }
    
    public int getHardDraw() {
        return hardDraw;
    }
    
    public void setHardDraw(int hardDraw) {
        this.hardDraw = hardDraw;
    }
    
    public int getHardLost() {
        return hardLost;
    }
    
    public void setHardLost(int hardLost) {
        this.hardLost = hardLost;
    }
    
    public Date getLastRewardedDate() {
        return lastRewardedDate;
    }
    
    public void setLastRewardedDate(Date lastRewardedDate) {
        this.lastRewardedDate = lastRewardedDate;
    }
    
    public Date getBasicPlanDeactivationDate() {
        return basicPlanDeactivationDate;
    }
    
    public void setBasicPlanDeactivationDate(Date basicPlanDeactivationDate) {
        this.basicPlanDeactivationDate = basicPlanDeactivationDate;
    }
    
    public Date getStandardPlanDeactivationDate() {
        return standardPlanDeactivationDate;
    }
    
    public void setStandardPlanDeactivationDate(Date standardPlanDeactivationDate) {
        this.standardPlanDeactivationDate = standardPlanDeactivationDate;
    }
    
    public Date getPremiumPlanDeactivationDate() {
        return premiumPlanDeactivationDate;
    }
    
    public void setPremiumPlanDeactivationDate(Date premiumPlanDeactivationDate) {
        this.premiumPlanDeactivationDate = premiumPlanDeactivationDate;
    }
    
    public int getOnlineWin() {
        return onlineWin;
    }
    
    public void setOnlineWin(int onlineWin) {
        this.onlineWin = onlineWin;
    }
    
    public int getOnlineDraw() {
        return onlineDraw;
    }
    
    public void setOnlineDraw(int onlineDraw) {
        this.onlineDraw = onlineDraw;
    }
    
    public int getOnlineLost() {
        return onlineLost;
    }
    
    public void setOnlineLost(int onlineLost) {
        this.onlineLost = onlineLost;
    }
    
    public int getTaskCompleted() {
        return taskCompleted;
    }
    
    public void setTaskCompleted(int taskCompleted) {
        this.taskCompleted = taskCompleted;
    }
    
    public String getUserState() {
        return userState;
    }
    
    public void setUserState(String userState) {
        this.userState = userState;
    }
    
    public int getDailyCoinsLimit() {
        return dailyCoinsLimit;
    }
    
    public void setDailyCoinsLimit(int dailyCoinsLimit) {
        this.dailyCoinsLimit = dailyCoinsLimit;
    }
}