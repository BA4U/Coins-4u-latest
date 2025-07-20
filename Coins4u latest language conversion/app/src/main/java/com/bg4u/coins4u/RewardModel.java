package com.bg4u.coins4u;

public class RewardModel {
    private String rewardId;
    private String rewardImage;
    private int rewardAmount;
    private int rewardLostCoin = 1000; // Default value for reward lost coin
    private int totalTask = 1; // Default task
    private String rewardText;

    public RewardModel(String rewardId, int rewardAmount, String rewardImage, int rewardLostCoin, int totalTask, String rewardText) {
        this.rewardId = rewardId;
        this.rewardAmount = rewardAmount;
        this.rewardImage = rewardImage;
        this.rewardLostCoin = rewardLostCoin;
        this.totalTask = totalTask;
        this.rewardText = rewardText;
    }

    public RewardModel() {
    }

    public String getRewardId() {
        return rewardId;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(int rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getRewardImage() {
        return rewardImage;
    }

    public void setRewardImage(String rewardImage) {
        this.rewardImage = rewardImage;
    }

    public int getRewardLostCoin() {
        return rewardLostCoin;
    }

    public void setRewardLostCoin(int rewardLostCoin) {
        this.rewardLostCoin = rewardLostCoin;
    }

    public int getTotalTask() {
        return totalTask;
    }

    public void setTotalTask(int totalTask) {
        this.totalTask = totalTask;
    }

    public String getRewardText() {
        return rewardText;
    }

    public void setRewardText(String rewardText) {
        this.rewardText = rewardText;
    }
}
