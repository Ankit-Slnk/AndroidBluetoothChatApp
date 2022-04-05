package com.ankit.bluetoothchatapp.screens;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankit.bluetoothchatapp.R;
import com.ankit.bluetoothchatapp.controller.ChatController;
import com.ankit.bluetoothchatapp.helper.DatabaseHelper;
import com.ankit.bluetoothchatapp.models.Chats;
import com.ankit.bluetoothchatapp.models.Users;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText inputLayout;
    private List<Chats> chatMessages;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    Users user;
    DatabaseHelper db = new DatabaseHelper(this);
    ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        connectingDevice = getIntent().getParcelableExtra("connectingDevice");
        user = (Users) getIntent().getSerializableExtra("user");

        setToolbar();

        recyclerView = findViewById(R.id.recyclerView);
        inputLayout = findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);

        recyclerView.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

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
        adapter = new ChatAdapter(ChatActivity.this, chatMessages);
        recyclerView.setAdapter(adapter);

        registerReceiver(messageReadReceiver, new IntentFilter("MESSAGE_READ"));
        registerReceiver(messageWriteReceiver, new IntentFilter("MESSAGE_WRITE"));
        registerReceiver(messageToastReceiver, new IntentFilter("MESSAGE_TOAST"));
        registerReceiver(messageDeviceObjectReceiver, new IntentFilter("MESSAGE_DEVICE_OBJECT"));

        getChats();
    }

    void getChats() {
        List<Chats> chatsList = db.getUserChats(user.id + "");
        chatMessages.clear();
        for (int i = 0; i < chatsList.size(); i++) {
            chatMessages.add(chatsList.get(i));
            adapter.notifyDataSetChanged();
        }

        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() - 1);
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {

        private List<Chats> listData;
        Activity context;

        public ChatAdapter(Activity context, List<Chats> listData) {
            this.context = context;
            this.listData = listData;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_view, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {

            Chats data = listData.get(position);

            if (data.sender.equals("1")) {
                holder.leftView.setVisibility(View.VISIBLE);
                holder.rightView.setVisibility(View.GONE);
            } else {
                holder.leftView.setVisibility(View.GONE);
                holder.rightView.setVisibility(View.VISIBLE);
            }

            holder.llParent.setBackgroundResource(data.sender.equals("1") ? R.drawable.my_chat_bg : R.drawable.other_chat_bg);

            holder.tvMessage.setText(data.message);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

        }

        @Override
        public int getItemCount() {
            return listData.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;
            LinearLayout llParent;
            View leftView, rightView;

            public MyViewHolder(View view) {
                super(view);
                tvMessage = view.findViewById(R.id.tvMessage);
                llParent = view.findViewById(R.id.llParent);
                leftView = view.findViewById(R.id.leftView);
                rightView = view.findViewById(R.id.rightView);
            }
        }
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
            db.addChat(intent.getStringExtra("message"), user.id + "", "1");
            getChats();
        }
    };

    private final BroadcastReceiver messageReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            db.addChat(intent.getStringExtra("message"), user.id + "", "0");
            getChats();
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