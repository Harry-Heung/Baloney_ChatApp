package com.example.ea_vtc_chat_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    private ImageView userProfileImage;
    private TextView userfullnameTextView;
    private TextView userSloganTextView;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化视图
        userProfileImage = findViewById(R.id.userProfileImage);
        userfullnameTextView = findViewById(R.id.userfullname);
        userSloganTextView = findViewById(R.id.userslogan);

        // 初始化 Firebase
        databaseHelper = DatabaseHelper.getInstance(this);

        // 加载用户数据
        loadUserData();

        // 设置返回按钮的点击事件
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // 返回主页
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 结束当前活动
        });

        // 设置搜索按钮的点击事件
        ImageView searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            // 暂时显示一个提示
            Toast.makeText(SettingsActivity.this, "Search clicked", Toast.LENGTH_SHORT).show();
        });

        // 设置用户头像的点击事件
        View userProfileSection = findViewById(R.id.userProfile); // 这里的ID要与你的布局文件一致
        userProfileSection.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseHelper.getDatabaseReference().child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String userfullname = snapshot.child("userfullname").getValue(String.class);
                            String slogan = snapshot.child("slogan").getValue(String.class);
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                            if (userfullname != null) {
                                userfullnameTextView.setText(userfullname);
                            }
                            if (slogan != null) {
                                userSloganTextView.setText(slogan);
                            }
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(SettingsActivity.this).load(imageUrl).into(userProfileImage);
                            } else {
                                userProfileImage.setImageResource(R.drawable.usericon_default); // 默认头像
                            }
                        } else {
                            Toast.makeText(SettingsActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SettingsActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}