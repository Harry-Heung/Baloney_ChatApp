package com.example.ea_vtc_chat_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ea_vtc_chat_project.chat.Chat;
import com.example.ea_vtc_chat_project.messages.MessagesAdapter;
import com.example.ea_vtc_chat_project.messages.MessagesList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private final List<MessagesList> userMessagesList = new ArrayList<>();
    private MessagesAdapter messagesAdapter;
    private ImageView userProfilePic;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth; // 添加 FirebaseAuth 变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance(); // 初始化 FirebaseAuth
        initViews();
        initFirebase();
        setupRecyclerView();
        setupMenuIcon();
        requestNotificationPermission();
    }

    private void initViews() {
        userProfilePic = findViewById(R.id.userProfilePic);
        userProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });

        // 添加這段代碼來設置分界線的顏色和高度
        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.very_light_gray)); // 設置分界線顏色
        ViewGroup.LayoutParams params = divider.getLayoutParams();
        params.height = 1; // 設置分界線高度
        divider.setLayoutParams(params);
    }

    private void initFirebase() {
        databaseHelper = DatabaseHelper.getInstance(this);
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;  // 使用当前用户的 UID
        if (currentUid != null) {
            Log.d("MainActivity", "Current User UID: " + sanitizeKey(currentUid));
            loadUserProfileImage(currentUid);
            loadUserMessages(currentUid);
        } else {
            Log.e("MainActivity", "User UID is null");
        }
    }

    private void loadUserProfileImage(String uid) {
        databaseHelper.getDatabaseReference().child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(MainActivity.this).load(imageUrl).into(userProfilePic);
                            } else {
                                userProfilePic.setImageResource(R.drawable.usericon_default);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MainActivity", "Failed to load user image: " + error.getMessage());
                    }
                });
    }

    private void setupRecyclerView() {
        RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        messagesRecyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(userMessagesList, MainActivity.this);
        messagesRecyclerView.setAdapter(messagesAdapter);

        // 添加点击事件监听器
        messagesAdapter.setOnItemClickListener((position, v) -> {
            MessagesList selectedMessage = userMessagesList.get(position);
            String currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

            Log.d("MainActivity", "Starting ChatActivity with user_uid: " + currentUserUid + " and other_uid: " + selectedMessage.getUid());

            Intent chatIntent = new Intent(MainActivity.this, Chat.class);
            chatIntent.putExtra("full_name", selectedMessage.getUserFullName());
            chatIntent.putExtra("user_image_url", selectedMessage.getImageUrl());
            chatIntent.putExtra("other_uid", selectedMessage.getUid()); // 传递对方用户的 UID
            chatIntent.putExtra("user_uid", currentUserUid); // 传递当前用户的 UID
            startActivity(chatIntent);
        });
    }

    private void setupMenuIcon() {
        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(this::showPopupMenu);
    }

    private void loadUserMessages(String currentUid) {
        databaseHelper.getDatabaseReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userMessagesList.clear();
                Log.d("MainActivity", "Loading user messages list...");
                for (DataSnapshot userData : snapshot.getChildren()) {
                    final String uid = userData.getKey();
                    if (uid != null) {
                        final String getUserFullName = userData.child("name").getValue(String.class);
                        final String getUserImageUrl = userData.child("imageUrl").getValue(String.class);
                        final String getUniqueName = userData.child("unique_name").getValue(String.class);  // 获取 unique_name
                        final String getEmail = userData.child("email").getValue(String.class);
                        final String getNameID = userData.child("nameID").getValue(String.class);
                        final String getPassword = userData.child("password").getValue(String.class);

                        if (uid.equals(currentUid)) {
                            Log.d("MainActivity", "Skipping current user: " + currentUid);
                            continue;
                        }

                        Log.d("MainActivity", "User UID: " + uid);
                        Log.d("MainActivity", "User Full Name: " + getUserFullName);
                        Log.d("MainActivity", "User Image URL: " + getUserImageUrl);

                        retrieveLastMessage(uid, lastMessage -> {
                            countUnseenMessages(uid, currentUid, unseenMessagesCount -> {
                                String userImageUrl = (getUserImageUrl == null || getUserImageUrl.isEmpty()) ? "default" : getUserImageUrl;

                                MessagesList messagesList = new MessagesList(uid, getEmail, userImageUrl, getUserFullName, getNameID, getPassword, getUniqueName, lastMessage, unseenMessagesCount);  // 使用 getUniqueName
                                Log.d("MainActivity", "Adding user to list: " + getUserFullName + " with UID: " + uid);
                                userMessagesList.add(messagesList);
                                messagesAdapter.updateMessages(userMessagesList);
                            });
                        });
                    } else {
                        Log.d("MainActivity", "Skipping null user UID");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Database Error: " + error.getMessage());
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.action_logout) {
                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                logout();
                return true;
            } else {
                return false;
            }
        });

        setPopupMenuPosition(view, popupMenu);
        popupMenu.show();
    }

    private void setPopupMenuPosition(View view, PopupMenu popupMenu) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1] + view.getHeight();

        try {
            java.lang.reflect.Method method = popupMenu.getClass().getDeclaredMethod("setGravity", int.class);
            method.invoke(popupMenu, Gravity.TOP | Gravity.END);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        MemoryData.saveUniqueName("", this);
        // 使用 Firebase Authentication 注销用户
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void retrieveLastMessage(String uid, OnLastMessageRetrievedListener listener) {
        DatabaseReference chatRef = databaseHelper.getDatabaseReference().child("chat").child(uid);
        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String lastMessage = "";
                for (DataSnapshot message : task.getResult().getChildren()) {
                    String msg = message.child("msg").getValue(String.class);
                    if (msg != null) {
                        lastMessage = msg;
                    }
                }
                listener.onLastMessageRetrieved(lastMessage);
            } else {
                listener.onLastMessageRetrieved("");
            }
        });
    }

    private void countUnseenMessages(String uid, String currentUid, OnUnseenMessagesCountRetrievedListener listener) {
        if (uid != null && currentUid != null) {
            long userLastSeenMessage = Long.parseLong(MemoryData.getLastMsgTS(MainActivity.this, uid));
            DatabaseReference chatRef = databaseHelper.getDatabaseReference().child("chat").child(uid);
            chatRef.get().addOnCompleteListener(task -> {
                int unseenMessagesCount = 0;
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DataSnapshot message : task.getResult().getChildren()) {
                        String msgTimestamp = message.getKey();
                        String sender = message.child("uid").getValue(String.class);
                        if (msgTimestamp != null && sender != null && !sender.equals(currentUid) &&
                                Long.parseLong(msgTimestamp) > userLastSeenMessage) {
                            unseenMessagesCount++;
                        }
                    }
                }
                listener.onUnseenMessagesCountRetrieved(unseenMessagesCount);
            });
        } else {
            listener.onUnseenMessagesCountRetrieved(0);
        }
    }

    private String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replaceAll("[.#$\\[\\]]", "_");
    }

    interface OnLastMessageRetrievedListener {
        void onLastMessageRetrieved(String lastMessage);
    }

    interface OnUnseenMessagesCountRetrievedListener {
        void onUnseenMessagesCountRetrieved(int unseenMessagesCount);
    }
}