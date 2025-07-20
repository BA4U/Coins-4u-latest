package com.bg4u.coins4u;

public class ShopModel {
    private String productId;
    private String productImage;
    private int productAmount;
    private int productLostCoin = 1000; // Default value for product lost coin
    private int totalTask = 1; // Default task
    private String productText;

    public ShopModel(String productId, int productAmount, String productImage, int productLostCoin, int totalTask, String productText) {
        this.productId = productId;
        this.productAmount = productAmount;
        this.productImage = productImage;
        this.productLostCoin = productLostCoin;
        this.totalTask = totalTask;
        this.productText = productText;
    }

    public ShopModel() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getProductAmount() {
        return productAmount;
    }

    public void setProductAmount(int productAmount) {
        this.productAmount = productAmount;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public int getProductLostCoin() {
        return productLostCoin;
    }

    public void setProductLostCoin(int productLostCoin) {
        this.productLostCoin = productLostCoin;
    }

    public int getTotalTask() {
        return totalTask;
    }

    public void setTotalTask(int totalTask) {
        this.totalTask = totalTask;
    }

    public String getProductText() {
        return productText;
    }

    public void setProductText(String productText) {
        this.productText = productText;
    }
}
