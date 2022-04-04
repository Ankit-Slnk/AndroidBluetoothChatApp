package com.ankit.bluetoothchatapp.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ankit.bluetoothchatapp.models.Chats;
import com.ankit.bluetoothchatapp.models.Users;

import java.util.ArrayList;
import java.util.List;

// https://www.javatpoint.com/android-sqlite-tutorial

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "bluetoothChat";

    private static final String USERS_TABLE = "users";
    private static final String KEY_ID = "id";
    private static final String KEY_BLUETOOTH_NAME = "name";
    private static final String KEY_BLUETOOTH_ADDRESS = "address";

    private static final String CHATS_TABLE = "chats";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_OTHER_USER = "other_user_id";
    private static final String KEY_SENDER = "sender"; // 1 - me,0 - other

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + USERS_TABLE + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_BLUETOOTH_NAME + " TEXT,"
                + KEY_BLUETOOTH_ADDRESS + " TEXT" + ")");

        db.execSQL("CREATE TABLE " + CHATS_TABLE + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_MESSAGE + " TEXT,"
                + KEY_OTHER_USER + " TEXT,"
                + KEY_SENDER + " TEXT" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CHATS_TABLE);

        onCreate(db);
    }

    public List<Chats> getUserChats(String userId) {
        List<Chats> chatsList = new ArrayList<Chats>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + CHATS_TABLE + " WHERE " + KEY_OTHER_USER + "='" + userId + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                chatsList.add(new Chats(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
            } while (cursor.moveToNext());
        }
        return chatsList;
    }

    public void addChat(String message, String other_user_id, String sender) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE, message);
        values.put(KEY_OTHER_USER, other_user_id);
        values.put(KEY_SENDER, sender);

        db.insert(CHATS_TABLE, null, values);
        db.close();
    }

    public long addUser(String name, String address) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BLUETOOTH_NAME, name);
        values.put(KEY_BLUETOOTH_ADDRESS, address);

        long lastInsertId = db.insert(USERS_TABLE, null, values);

        db.close();

        return lastInsertId;
    }

    public List<Users> getAllUsers() {
        List<Users> usersList = new ArrayList<Users>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + USERS_TABLE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                usersList.add(new Users(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2)));
            } while (cursor.moveToNext());
        }

        return usersList;
    }

    public Users getUser(String address) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(USERS_TABLE, new String[]{KEY_ID, KEY_BLUETOOTH_NAME, KEY_BLUETOOTH_ADDRESS}, KEY_BLUETOOTH_ADDRESS + "=?",
                new String[]{address}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        try {
            Users user = new Users(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2));
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    public Users getUser(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(USERS_TABLE, new String[]{KEY_ID, KEY_BLUETOOTH_NAME, KEY_BLUETOOTH_ADDRESS}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        try {
            Users user = new Users(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2));
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
