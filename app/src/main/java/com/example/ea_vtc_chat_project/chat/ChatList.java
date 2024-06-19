package com.example.ea_vtc_chat_project.chat;


public class ChatList {
    private String chatId;
    private String email;
    private String imageUrl;
    private String name;
    private String uniqueName;
    private String message;
    private String date;
    private String time;
    private String audioUrl;
    private String uid;

    // 无参数构造函数
    public ChatList() {
        // 必须有一个无参数构造函数
    }

    // 带参数的构造函数
    public ChatList(String chatId, String email, String imageUrl, String name, String uniqueName, String message, String date, String time, String audioUrl, String uid) {
        this.chatId = chatId;
        this.email = email;
        this.imageUrl = imageUrl;
        this.name = name;
        this.uniqueName = uniqueName;
        this.message = message;
        this.date = date;
        this.time = time;
        this.audioUrl = audioUrl;
        this.uid = uid;
    }

    // Getter 和 Setter 方法
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}