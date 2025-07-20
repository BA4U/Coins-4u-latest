package com.bg4u.coins4u.chat4u;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for messages in the chat system.
 * Stores message content, type, sender/receiver information and timestamps.
 */
public class Messages {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";

    private String messageID;
    private String from;
    private String to;
    private String senderId;
    private String receiverId;
    private String message;
    private String type;
    private boolean seen;
    @ServerTimestamp
    private Date timestamp;
    private String mediaUrl; // For image messages

    // Empty constructor required for Firestore
    public Messages() {
    }

    public Messages(String from, String to, String message, String type, String messageID, Date timestamp, boolean seen) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.type = type;
        this.messageID = messageID;
        this.timestamp = timestamp;
        this.seen = seen;
    }

    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageId(String messageID) {
        this.messageID = messageID;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}