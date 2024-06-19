package com.example.ea_vtc_chat_project.messages;

public class MessagesList {
    private String uid;
    private String email;
    private String imageUrl;
    private String userFullName;
    private String nameID;
    private String password;
    private String uniqueName;
    private String lastMessage;
    private int unseenMessagesCount;

    public MessagesList(String uid, String email, String imageUrl, String userFullName, String nameID, String password, String uniqueName, String lastMessage, int unseenMessagesCount) {
        this.uid = uid;
        this.email = email;
        this.imageUrl = imageUrl;
        this.userFullName = userFullName;
        this.nameID = nameID;
        this.password = password;
        this.uniqueName = uniqueName;
        this.lastMessage = lastMessage;
        this.unseenMessagesCount = unseenMessagesCount;
    }

    // Getters and setters for all fields

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getNameID() {
        return nameID;
    }

    public void setNameID(String nameID) {
        this.nameID = nameID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnseenMessagesCount() {
        return unseenMessagesCount;
    }

    public void setUnseenMessagesCount(int unseenMessagesCount) {
        this.unseenMessagesCount = unseenMessagesCount;
    }
}