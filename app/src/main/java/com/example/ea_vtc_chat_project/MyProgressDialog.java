package com.example.ea_vtc_chat_project;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

public final class MyProgressDialog extends Dialog {
    private TextView messageTextView;

    public MyProgressDialog(@NonNull Context context) {
        super(context, R.style.TransparentProgressDialog); // 使用自定義樣式
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_progress_dialog_layout); // 使用正確的佈局資源
    }

    public void updateMessage(String message) {
        if (messageTextView != null) {
            messageTextView.setText(message);
        }
    }
}

