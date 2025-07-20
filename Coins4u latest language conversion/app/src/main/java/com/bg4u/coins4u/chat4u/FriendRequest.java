package com.bg4u.coins4u.chat4u;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for friend requests in the chat system.
 * Keeps track of sender, receiver, status and timestamp.
 */
public class FriendRequest {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_DECLINED = "declined";

    private String requestId;
    private String senderId;
    private String receiverId;
    private String status;
    @ServerTimestamp
    private Date timestamp;

    // Empty constructor required for Firestore
    public FriendRequest() {
    }

    public FriendRequest(String senderId, String receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = STATUS_PENDING;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}