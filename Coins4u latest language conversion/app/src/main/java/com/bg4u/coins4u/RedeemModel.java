package com.bg4u.coins4u;

public class RedeemModel {
    private int first;
    private int second;
    private int third;
    
    public RedeemModel() {
        // Default constructor
    }
    
    public RedeemModel(int first, int second, int third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
    
    public int getFirst() {
        return first;
    }
    
    public void setFirst(int first) {
        this.first = first;
    }
    
    public int getSecond() {
        return second;
    }
    
    public void setSecond(int second) {
        this.second = second;
    }
    
    public int getThird() {
        return third;
    }
    
    public void setThird(int third) {
        this.third = third;
    }
}
