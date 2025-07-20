package com.bg4u.coins4u;

public class CategoryModel {
    private String categoryId;
    private String categoryName;
    private String categoryImage;
    private int categoryCoin = 1; // New property for category coins
    private int categoryLostCoin = 2; // New property for category coins
    private Boolean categoryStatus; // Boolean status field
    private String categoryPromotionLink; // New property for category promotion link

    public CategoryModel(String categoryId, String categoryName, String categoryImage, int categoryCoin, int categoryLostCoin, Boolean categoryStatus, String categoryPromotionLink) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.categoryCoin = categoryCoin;
        this.categoryLostCoin = categoryLostCoin;
        this.categoryStatus = categoryStatus;
        this.categoryPromotionLink = categoryPromotionLink;
    }

    public CategoryModel() {
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryImage() {
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }

    public int getCategoryCoin() {
        return categoryCoin;
    }

    public void setCategoryCoin(int categoryCoin) {
        this.categoryCoin = categoryCoin;
    }

    public int getCategoryLostCoin() {
        return categoryLostCoin;
    }

    public void setCategoryLostCoin(int categoryLostCoin) {
        this.categoryLostCoin = categoryLostCoin;
    }

    public Boolean getCategoryStatus() {
        return categoryStatus;
    }

    public void setCategoryStatus(Boolean categoryStatus) {
        this.categoryStatus = categoryStatus;
    }

    public String getCategoryPromotionLink() {
        return categoryPromotionLink;
    }

    public void setCategoryPromotionLink(String categoryPromotionLink) {
        this.categoryPromotionLink = categoryPromotionLink;
    }
}
