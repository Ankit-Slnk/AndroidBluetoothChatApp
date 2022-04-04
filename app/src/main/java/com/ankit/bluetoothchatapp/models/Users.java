package com.ankit.bluetoothchatapp.models;

import java.io.Serializable;

public class Users implements Serializable {
    public int id;
    public String name;
    public String address;

    public Users(int id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }
}
