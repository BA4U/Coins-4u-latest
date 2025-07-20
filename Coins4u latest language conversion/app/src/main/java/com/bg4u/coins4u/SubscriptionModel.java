package com.bg4u.coins4u;

public class SubscriptionModel {
    private String SubscriptionPlanName;
    private double coinMultiplier;
    private int dailyCoins;
    private int maxChatUsers;
    private int avatarLottieResId; // Lottie resource ID for avatar
    private int bannerLottieResId; // Lottie resource ID for banner
    private boolean chatWithUsersLimited;
    private boolean showLessAds;
    private int spinMaxLimit;
    
    public SubscriptionModel(String planName, double coinMultiplier, int dailyCoins, int maxChatUsers, int avatarLottieResId, int bannerLottieResId, boolean chatWithUsersLimited, boolean showLessAds, int spinMaxLimit) {
        this.SubscriptionPlanName = planName;
        this.coinMultiplier = coinMultiplier;
        this.dailyCoins = dailyCoins;
        this.maxChatUsers = maxChatUsers;
        this.avatarLottieResId = avatarLottieResId;
        this.bannerLottieResId = bannerLottieResId;
        this.chatWithUsersLimited = chatWithUsersLimited;
        this.showLessAds = showLessAds;
        this.spinMaxLimit = spinMaxLimit;
    }
    
    public SubscriptionModel() {
    }
    
    // Getters for all the properties
    public String getSubscriptionPlanName() {
        return SubscriptionPlanName;
    }
    
    public double getCoinMultiplier() {
        return coinMultiplier;
    }
    
    public int getDailyCoins() {
        return dailyCoins;
    }
    
    public int getMaxChatUsers() {
        return maxChatUsers;
    }
    
    public int getAvatarLottieResId() {
        return avatarLottieResId;
    }
    
    public int getBannerLottieResId() {
        return bannerLottieResId;
    }
    
    public boolean isChatWithUsersLimited() {
        return chatWithUsersLimited;
    }
    
    public boolean isShowLessAds() {
        return showLessAds;
    }

    public int getSpinMaxLimit() {
        return spinMaxLimit;
    }
}
