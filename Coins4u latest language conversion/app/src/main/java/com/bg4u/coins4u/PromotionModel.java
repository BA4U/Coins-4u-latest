package com.bg4u.coins4u;

public class PromotionModel {
    private String promotionId;
    private String promotionImage;
    private String promotionButtonUrl;
    private String promotionButtonText;
    private Boolean promotionStatus;

    public PromotionModel() {
        // Empty constructor required for Firestore
    }

    public PromotionModel(String promotionId, String promotionImage, String promotionButtonUrl, String promotionButtonText, Boolean promotionStatus) {
        this.promotionId = promotionId;
        this.promotionImage = promotionImage;
        this.promotionButtonUrl = promotionButtonUrl;
        this.promotionButtonText = promotionButtonText;
        this.promotionStatus = promotionStatus;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getPromotionImage() {
        return promotionImage;
    }

    public void setPromotionImage(String promotionImage) {
        this.promotionImage = promotionImage;
    }

    public String getPromotionButtonUrl() {
        return promotionButtonUrl;
    }

    public void setPromotionButtonUrl(String promotionButtonUrl) {
        this.promotionButtonUrl = promotionButtonUrl;
    }

    public String getPromotionButtonText() {
        return promotionButtonText;
    }

    public void setPromotionButtonText(String promotionButtonText) {
        this.promotionButtonText = promotionButtonText;
    }

    public Boolean getPromotionStatus() {
        return promotionStatus;
    }

    public void setPromotionStatus(Boolean promotionStatus) {
        this.promotionStatus = promotionStatus;
    }
}
