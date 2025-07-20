package com.bg4u.coins4u.chat;

import java.util.Date;

public class Messages {
    private String from, message, type, to, messageID, time, date, name, token;
    private Date timestamp; // Timestamp of the message
    private boolean seen; // Whether the message has been seen by the receiver

    public Messages()    {   }

    // Constructor for creating new message objects
    public Messages(String from, String to, String message, String type, String messageID, Date timestamp, boolean seen) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.type = type;
        this.messageID = messageID;
        this.timestamp = timestamp;
        this.seen = seen;
        this.name = null;
        this.token = null;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
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

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
