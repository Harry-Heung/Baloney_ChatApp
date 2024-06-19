package com.example.ea_vtc_chat_project;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private DatabaseReference databaseReference;

    // 单例模式
    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(context.getString(R.string.database_url));
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    // 返回数据库引用
    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }


    // 检查 Chat ID 是否存在
    public void checkIfChatIdExists(String chatId, DatabaseCallback callback) {
        databaseReference.child("chats").orderByKey().equalTo(chatId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onSuccess(chatId);
                        } else {
                            callback.onFailure(new Exception("Chat ID does not exist"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.toException());
                    }
                });
    }

    // 检查 User ID 是否存在
    public void checkIfUniqueNameExists(String uniqueName, DatabaseCallback callback) {
        databaseReference.child("users").orderByChild("unique_name").equalTo(uniqueName)  // 使用 unique_name
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onSuccess(uniqueName);
                        } else {
                            callback.onFailure(new Exception("Unique Name does not exist"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.toException());
                    }
                });
    }

    // 保存用户数据
    public void saveUserData(String uid, Map<String, Object> userData, DatabaseCallback callback) {
        databaseReference.child("users").child(uid).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // 处理数据消息
    public void handleDataMessage(Map<String, String> data) {
        // 这里可以将数据消息处理逻辑添加到数据库中
        String UserID = data.get("UserID");
        String message = data.get("message");
        if (UserID != null && message != null) {
            databaseReference.child("messages").child(UserID).setValue(message);
        }
    }

    // 简单的回调接口
    public interface DatabaseCallback {
        default void onSuccess() {
        }

        default void onSuccess(String... args) {
        }

        default void onSuccess(String arg1, String arg2) {
        }

        default void onFailure(Exception e) {
            Log.e(TAG, "Database operation failed", e);
        }
    }
    private String sanitizeKey(String key) {
        return key.replaceAll("[.#$\\[\\]]", "_");
    }
}