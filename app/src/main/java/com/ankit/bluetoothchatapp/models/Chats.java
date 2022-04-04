package com.ankit.bluetoothchatapp.models;

import java.io.Serializable;

public class Chats implements Serializable {
    public int id;
    public String message;
    public String other_user_id;
    public String sender;

    public Chats(int id, String message, String other_user_id, String sender) {
        this.id = id;
        this.message = message;
        this.other_user_id = other_user_id;
        this.sender = sender;
    }
}
