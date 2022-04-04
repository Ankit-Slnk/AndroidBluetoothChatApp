package com.ankit.bluetoothchatapp.screens;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.ankit.bluetoothchatapp.R;
import com.ankit.bluetoothchatapp.controller.ChatController;
import com.ankit.bluetoothchatapp.helper.DatabaseHelper;
import com.ankit.bluetoothchatapp.models.Chats;
import com.ankit.bluetoothchatapp.models.Users;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ListView listView;
    private EditText inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    Users user;
    DatabaseHelper db = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        connectingDevice = getIntent().getParcelableExtra("connectingDevice");
        user = (Users) getIntent().getSerializableExtra("user");

        setToolbar();

        listView = findViewById(R.id.list);
        inputLayout = findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputLayout.getText().toString().equals("")) {
                    Toast.makeText(ChatActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(inputLayout.getText().toString());
                    inputLayout.setText("");
                }
            }
        });

        //set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        listView.setAdapter(chatAdapter);

        registerReceiver(messageReadReceiver, new IntentFilter("MESSAGE_READ"));
        registerReceiver(messageWriteReceiver, new IntentFilter("MESSAGE_WRITE"));
        registerReceiver(messageToastReceiver, new IntentFilter("MESSAGE_TOAST"));
        registerReceiver(messageDeviceObjectReceiver, new IntentFilter("MESSAGE_DEVICE_OBJECT"));

        getChats();
    }

    void getChats() {
        List<Chats> chatsList = db.getUserChats(user.id + "");
        chatAdapter.clear();
        chatAdapter.notifyDataSetChanged();
        for (int i = 0; i < chatsList.size(); i++) {
            chatMessages.add(chatsList.get(i).message);
            chatAdapter.notifyDataSetChanged();
        }

        listView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listView.setSelection(chatAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            chatController = ChatController.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    private final BroadcastReceiver messageToastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), intent.getStringExtra("toast"), Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver messageWriteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            db.addChat("Me:  " + intent.getStringExtra("message"), user.id + "", "1");
            getChats();
        }
    };

    private final BroadcastReceiver messageReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    db.addChat(connectingDevice.getName() + ":  " + intent.getStringExtra("message"), user.id + "", "0");
                    getChats();
                }
            } else {
                db.addChat(connectingDevice.getName() + ":  " + intent.getStringExtra("message"), user.id + "", "0");
                getChats();
            }
        }
    };

    private final BroadcastReceiver messageDeviceObjectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    connectingDevice = intent.getParcelableExtra("DEVICE_OBJECT");
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
                }
            } else {
                connectingDevice = intent.getParcelableExtra("DEVICE_OBJECT");
                Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void sendMessage(String message) {
        if (chatController.getState() != ChatController.STATE_CONNECTED) {
            Toast.makeText(this, "Connection was lost!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatController.write(send);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(messageReadReceiver);
        unregisterReceiver(messageWriteReceiver);
        unregisterReceiver(messageToastReceiver);
        unregisterReceiver(messageDeviceObjectReceiver);
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                toolbar.setTitle(connectingDevice.getName());
            }
        } else {
            toolbar.setTitle(connectingDevice.getName());
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}