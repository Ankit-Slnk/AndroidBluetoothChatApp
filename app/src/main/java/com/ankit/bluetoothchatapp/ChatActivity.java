package com.ankit.bluetoothchatapp;

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
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private ListView listView;
    private TextInputLayout inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;

    private ChatController chatController;
    private BluetoothDevice connectingDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        connectingDevice = getIntent().getParcelableExtra("connectingDevice");

        setToolbar();

        listView = findViewById(R.id.list);
        inputLayout = findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputLayout.getEditText().getText().toString().equals("")) {
                    Toast.makeText(ChatActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(inputLayout.getEditText().getText().toString());
                    inputLayout.getEditText().setText("");
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
            chatMessages.add("Me:  " + intent.getStringExtra("message"));
            chatAdapter.notifyDataSetChanged();
        }
    };

    private final BroadcastReceiver messageReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    chatMessages.add(connectingDevice.getName() + ":  " + intent.getStringExtra("message"));
                    chatAdapter.notifyDataSetChanged();
                }
            } else {
                chatMessages.add(connectingDevice.getName() + ":  " + intent.getStringExtra("message"));
                chatAdapter.notifyDataSetChanged();
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