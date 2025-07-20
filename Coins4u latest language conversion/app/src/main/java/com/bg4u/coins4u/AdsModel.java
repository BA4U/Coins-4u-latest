package com.bg4u.coins4u;

public class AdsModel {
    String id;
    String appopen_g;
    String banner_g;
    String interstitial_g;
    String native_g;
    String reward_g;
    Boolean adsStatus;

    // Default constructor
    public AdsModel() {
    }

    // Parameterized constructor
    public AdsModel(String id, String appopen_g, String banner_g, String interstitial_g, String native_g, String reward_g, Boolean adsStatus) {
        this.id = id;
        this.appopen_g = appopen_g;
        this.banner_g = banner_g;
        this.interstitial_g = interstitial_g;
        this.native_g = native_g;
        this.reward_g = reward_g;
        this.adsStatus = adsStatus;
    }

    // Getter method for id
    public String getId() {
        return id;
    }

    // Setter method for id
    public void setId(String id) {
        this.id = id;
    }

    // Getter and Setter methods for appopen_g
    public String getAppopen_g() {
        return appopen_g;
    }

    public void setAppopen_g(String appopen_g) {
        this.appopen_g = appopen_g;
    }

    // Getter and Setter methods for banner_g
    public String getBanner_g() {
        return banner_g;
    }

    public void setBanner_g(String banner_g) {
        this.banner_g = banner_g;
    }

    // Getter and Setter methods for interstitial_g
    public String getInterstitial_g() {
        return interstitial_g;
    }

    public void setInterstitial_g(String interstitial_g) {
        this.interstitial_g = interstitial_g;
    }

    // Getter and Setter methods for native_g
    public String getNative_g() {
        return native_g;
    }

    public void setNative_g(String native_g) {
        this.native_g = native_g;
    }

    // Getter and Setter methods for reward_g
    public String getReward_g() {
        return reward_g;
    }

    public void setReward_g(String reward_g) {
        this.reward_g = reward_g;
    }

    // Getter and Setter methods for adsStatus
    public Boolean getAdsStatus() {
        return adsStatus;
    }

    public void setAdsStatus(Boolean adsStatus) {
        this.adsStatus = adsStatus;
    }
}
