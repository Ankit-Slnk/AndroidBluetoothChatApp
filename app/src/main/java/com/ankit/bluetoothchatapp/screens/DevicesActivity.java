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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.ankit.bluetoothchatapp.R;
import com.ankit.bluetoothchatapp.controller.ChatController;
import com.ankit.bluetoothchatapp.helper.DatabaseHelper;
import com.ankit.bluetoothchatapp.models.Users;

import java.util.List;
import java.util.Set;

public class DevicesActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    Button btnScanForDevices;
    ListView pairedDeviceList, chatsList;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    LinearLayout llProgressBar;
    DatabaseHelper db = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        setToolbar();

        btnScanForDevices = findViewById(R.id.btnScanForDevices);
        pairedDeviceList = findViewById(R.id.pairedDeviceList);
        llProgressBar = findViewById(R.id.llProgressBar);
        chatsList = findViewById(R.id.chatsList);

        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }

        scanForDevices();

        btnScanForDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanForDevices();
            }
        });

        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        chatsList.setAdapter(discoveredDevicesAdapter);

        getUsers();
    }

    void getUsers(){
        List<Users> users = db.getAllUsers();
        discoveredDevicesAdapter.clear();
        discoveredDevicesAdapter.notifyDataSetChanged();

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
    }

    void scanForDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        pairedDeviceList.setAdapter(pairedDevicesAdapter);

        // Register for broadcasts when a device is discovered
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
//        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add("No devices have been paired");
        }

        //Handling listview item click event
        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(DevicesActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                llProgressBar.setVisibility(View.VISIBLE);
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
            }

        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "onDestroy");
        if (chatController != null)
            chatController.stop();
    }

//    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            discoveredDevicesAdapter.clear();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(DevicesActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
//                }
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                if (discoveredDevicesAdapter.getCount() == 0) {
//                    discoveredDevicesAdapter.add("No devices found");
//                }
//            }
//        }
//    };

    private final BroadcastReceiver messageDeviceObjectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(DevicesActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    connectingDevice = intent.getParcelableExtra("DEVICE_OBJECT");
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
                    unregisterReceiver(messageDeviceObjectReceiver);
                    llProgressBar.setVisibility(View.GONE);

                    Users user = db.getUser(connectingDevice.getAddress());

                    if (user == null) {
                        user = db.getUser(db.addUser(connectingDevice.getName(), connectingDevice.getAddress()));
                    }

                    Intent i = new Intent(DevicesActivity.this, ChatActivity.class);
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

                Intent i = new Intent(DevicesActivity.this, ChatActivity.class);
                i.putExtra("connectingDevice", connectingDevice);
                i.putExtra("user", user);
                startActivity(i);
            }
        }
    };

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Bluetooth Chat");
//        toolbar.setNavigationIcon(R.drawable.ic_back);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
    }
}