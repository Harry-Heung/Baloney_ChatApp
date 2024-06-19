package com.example.ea_vtc_chat_project;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化 Firebase 实例
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // 初始化 Emoji 库
        EmojiManager.install(new GoogleEmojiProvider());
    }
}





