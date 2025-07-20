package com.bg4u.coins4u;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
 public class WithdrawRequest {
    private String userId;
    private String emailAddress;
    private String requestedBy;
    private String redeemCode;
    private int currentCoins;
    private int redeemedCoins;
    private int redeemAmount;
    
     public WithdrawRequest(){
         // Default constructor
     }
     public WithdrawRequest(String uid, String emailAddress, String name, int currentCoins, Date requestedDate, String redeemCode, int redeemedCoins, int redeemAmount) {
        this.userId = uid;
        this.emailAddress = emailAddress;
        this.requestedBy = name;
        this.currentCoins = currentCoins;
        this.redeemCode = redeemCode; // Initialize the redeemCode as null
        this.redeemedCoins = redeemedCoins;
        this.redeemAmount = redeemAmount;
    }
     
     public String getUserId() {
        return userId;
    }
     public void setUserId(String userId) {
        this.userId = userId;
    }
     public String getEmailAddress() {
        return emailAddress;
    }
     public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
     public String getRequestedBy() {
        return requestedBy;
    }
     public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
     public int getCoins() {
        return currentCoins;
    }
     public void setCoins(int coins) {
        this.currentCoins = coins;
    }
     @ServerTimestamp
     private Date createdAt;
     public Date getCreatedAt() {
        return createdAt;
    }
     public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
     // Getter and setter for redeem code
     public String getRedeemCode() {
         return redeemCode;
     }
    
     public void setRedeemCode(String redeemCode) {
         this.redeemCode = redeemCode;
     }
    
     // Getter and setter for redeemedCoins
     public int getRedeemedCoins() {
         return redeemedCoins;
     }
    
     public void setRedeemedCoins(int redeemedCoins) {
         this.redeemedCoins = redeemedCoins;
     }
    
     // Getter and setter for redeemAmount
     public int getRedeemAmount() {
         return redeemAmount;
     }
    
     public void setRedeemAmount(int redeemAmount) {
         this.redeemAmount = redeemAmount;
     }
 }