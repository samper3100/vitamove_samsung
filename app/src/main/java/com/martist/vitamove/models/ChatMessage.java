package com.martist.vitamove.models;

public class ChatMessage {
    private String text;
    private boolean isFromUser;
    private long timestamp;

    public ChatMessage(String text, boolean isFromUser) {
        this.text = text;
        this.isFromUser = isFromUser;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFromUser() {
        return isFromUser;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 