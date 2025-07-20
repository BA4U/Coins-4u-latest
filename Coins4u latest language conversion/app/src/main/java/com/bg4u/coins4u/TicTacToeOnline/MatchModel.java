package com.bg4u.coins4u.TicTacToeOnline;

public class MatchModel {
    private String userName = "Coins 4u";
    private String userProfilePic ;
    private int entryFee = 25 ;
    private String code;
    private String friendUid;
    private String currentUserUid;
    private long startTime;
    private boolean isPrivate = false;
    public MatchModel() {
        // Default constructor required for Firebase
    }
    
    public MatchModel(String userName, int coins, String code, String friendUid, String currentUserUid) {
        this.userName = userName;
        this.entryFee = coins;
        this.code = code;
        this.friendUid = friendUid;
        this.currentUserUid = currentUserUid;
    }
    
    // Getters and setters for the fields
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public int getCoins() {
        return entryFee;
    }
    
    public void setCoins(int coins) {
        this.entryFee = coins;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getFriendUid() {
        return friendUid;
    }
    
    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }
    
    public String getCurrentUserUid() {
        return currentUserUid;
    }
    
    public void setCurrentUserUid(String currentUserUid) {
        this.currentUserUid = currentUserUid;
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public String getUserProfilePic() {
        return userProfilePic;
    }
    
    public void setUserProfilePic(String userProfilePic) {
        this.userProfilePic = userProfilePic;
    }
}
