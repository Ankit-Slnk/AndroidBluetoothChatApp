package com.ankit.bluetoothchatapp.screens;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.ankit.bluetoothchatapp.R;
import com.ankit.bluetoothchatapp.controller.ChatController;
import com.ankit.bluetoothchatapp.helper.DatabaseHelper;
import com.ankit.bluetoothchatapp.models.Users;

import java.util.List;

public class ChatUsersActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    ListView chatsList;
    LinearLayout llProgressBar;
    DatabaseHelper db = new DatabaseHelper(this);
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    Button btnScanForDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_users);

        setToolbar();

        chatsList = findViewById(R.id.chatsList);
        llProgressBar = findViewById(R.id.llProgressBar);
        btnScanForDevices = findViewById(R.id.btnScanForDevices);

        btnScanForDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ChatUsersActivity.this, DevicesActivity.class));
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        chatsList.setAdapter(discoveredDevicesAdapter);

        getUsers();
    }

    void getUsers() {
        List<Users> users = db.getAllUsers();
        discoveredDevicesAdapter.clear();
        discoveredDevicesAdapter.notifyDataSetChanged();

        if (users.size() > 0) {
            for (int i = 0; i < users.size(); i++) {
                discoveredDevicesAdapter.add(users.get(i).name + "\n" + users.get(i).address);
            }
            chatsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    llProgressBar.setVisibility(View.VISIBLE);
                    connectToDevice(discoveredDevicesAdapter.getItem(i).split("\n")[1]);
                }
            });
        } else {
            discoveredDevicesAdapter.add("No Chat Users");
        }
    }

    private void connectToDevice(String deviceAddress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = ChatController.getInstance();
                    chatController.init(this);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatController = ChatController.getInstance();
            chatController.init(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(messageDeviceObjectReceiver, new IntentFilter("MESSAGE_DEVICE_OBJECT"));
        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
        getUsers();
    }

    private final BroadcastReceiver messageDeviceObjectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(ChatUsersActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    connectingDevice = intent.getParcelableExtra("DEVICE_OBJECT");
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
                    unregisterReceiver(messageDeviceObjectReceiver);
                    llProgressBar.setVisibility(View.GONE);

                    Users user = db.getUser(connectingDevice.getAddress());

                    if (user == null) {
                        user = db.getUser(db.addUser(connectingDevice.getName(), connectingDevice.getAddress()));
                    }

                    Intent i = new Intent(ChatUsersActivity.this, ChatActivity.class);
                    i.putExtra("connectingDevice", connectingDevice);
                    i.putExtra("user", user);
                    startActivity(i);
                }
            } else {
                connectingDevice = intent.getParcelableExtra("DEVICE_OBJECT");
                Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
                unregisterReceiver(messageDeviceObjectReceiver);
                llProgressBar.setVisibility(View.GONE);

                Users user = db.getUser(connectingDevice.getAddress());

                if (user == null) {
                    user = db.getUser(db.addUser(connectingDevice.getName(), connectingDevice.getAddress()));
                }

                Intent i = new Intent(ChatUsersActivity.this, ChatActivity.class);
                i.putExtra("connectingDevice", connectingDevice);
                i.putExtra("user", user);
                startActivity(i);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Chats");
//        toolbar.setNavigationIcon(R.drawable.ic_back);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
    }
}