package com.example.ea_vtc_chat_project.chat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ea_vtc_chat_project.AppLifecycleObserver;
import com.example.ea_vtc_chat_project.DatabaseHelper;
import com.example.ea_vtc_chat_project.MemoryData;
import com.example.ea_vtc_chat_project.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.google.GoogleEmojiProvider;
import com.vanniktech.emoji.EmojiEditText;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class Chat extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String CHANNEL_ID = "chat_notifications";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private final List<ChatList> chatLists = new ArrayList<>();
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private final Handler handler = new Handler();

    private DatabaseHelper databaseHelper;
    private FirebaseStorage firebaseStorage;
    private MediaRecorder recorder;
    private ChatAdapter chatAdapter;

    private String userName;
    private String chatKey;
    private Uri imageUri;
    private String uploadedImageUrl;
    private String fileName;
    private String recipientName;

    private String userChatId;
    private String otherChatId;

    private RecyclerView chattingRecyclerView;
    private LinearLayout previewLayout;
    private ImageView previewImage;
    private TextView recordingTimer;
    private EmojiEditText messageInput; // 使用 EmojiEditText
    private ImageButton sendButton;
    private ImageButton emojiButton; // 添加表情按钮
    private EmojiPopup emojiPopup; // 添加 EmojiPopup

    private boolean loadingFirstTime = true;
    private boolean permissionToRecordAccepted = false;
    private boolean isRecordingCancelled = false;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        retrieveIntentData(); // 确保在初始化视图和 Firebase 之前获取 Intent 数据
        initViews();
        initFirebase();
        initializeRecyclerView();
        setupListeners();
        requestPermissions();
        createNotificationChannel();
        loadChatMessages(); // 加载聊天消息
    }

    private void getUserInfo(String uid, OnUserInfoRetrievedListener listener) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ChatList user = snapshot.getValue(ChatList.class);
                    listener.onUserInfoRetrieved(user);
                } else {
                    listener.onUserInfoRetrieved(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onUserInfoRetrieved(null);
            }
        });
    }

    private void enterChatRoom(String userChatId, String otherChatId) {
        // 你的进入聊天室的逻辑
    }

    interface OnUserInfoRetrievedListener {
        void onUserInfoRetrieved(ChatList user);
    }

    interface DatabaseCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    private void initViews() {
        previewLayout = findViewById(R.id.previewLayout);
        previewImage = findViewById(R.id.previewImage);
        chattingRecyclerView = findViewById(R.id.chattingRecyclerView);
        messageInput = findViewById(R.id.message_input); // 使用 EmojiEditText
        sendButton = findViewById(R.id.send_and_recording_button);
        recordingTimer = findViewById(R.id.recording_time); // 初始化 recordingTimer
        emojiButton = findViewById(R.id.emoji_icon); // 初始化表情按钮

        // 初始化 EmojiPopup
        emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(R.id.root_view)).build(messageInput);

        emojiButton.setOnClickListener(v -> emojiPopup.toggle());

        updateSendButtonIcon();
    }

    private void initFirebase() {
        databaseHelper = DatabaseHelper.getInstance(this);
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void initializeRecyclerView() {
        chattingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatLists, this);
        chattingRecyclerView.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.send_and_recording_button).setOnTouchListener(this::handleSendBtnTouch);

        // 监听输入框内容变化
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonIcon();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不需要处理
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }
    }

    private void createNewChatData(String chatKey, String userChatId, String otherChatId) {
        if (chatKey == null || userChatId == null || otherChatId == null) {
            Log.e(TAG, "Chat key or user chat IDs are null");
            Toast.makeText(this, "Failed to create new chat data", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference chatRef = databaseHelper.getDatabaseReference().child("chats").child(chatKey);

        // 保存参与双方的 UID 到 participants 节点
        chatRef.child("participants").child("user1_uid").setValue(userChatId);
        chatRef.child("participants").child("user2_uid").setValue(otherChatId);
    }

    private void loadChatMessages() {
        if (chatKey == null || chatKey.isEmpty()) return;
        databaseHelper.getDatabaseReference().child("chats").child(chatKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatLists.clear();
                        final long lastSavedTimestamps = Long.parseLong(MemoryData.getLastMsgTS(Chat.this, chatKey));

                        for (DataSnapshot chatMessages : snapshot.getChildren()) {
                            if (chatMessages.hasChild("msg") && chatMessages.hasChild("user_chat_id")) {
                                final long getMessageTimeStamps = Long.parseLong(Objects.requireNonNull(chatMessages.getKey()));
                                final String getUserChatId = chatMessages.child("user_chat_id").getValue(String.class);
                                final String getMsg = chatMessages.child("msg").getValue(String.class);
                                final String imageUrl = chatMessages.hasChild("imageUrl") ? chatMessages.child("imageUrl").getValue(String.class) : null;
                                final String audioUrl = chatMessages.hasChild("audioUrl") ? chatMessages.child("audioUrl").getValue(String.class) : null;

                                final String messageDate = generateDateFromTimestamps(getMessageTimeStamps, "dd-MM-yyyy");
                                final String messageTime = generateDateFromTimestamps(getMessageTimeStamps, "hh:mm aa");

                                // 使用正确的构造函数创建 ChatList 对象
                                final ChatList chatList = new ChatList(
                                        chatKey,
                                        null, // email
                                        imageUrl,
                                        null, // name
                                        null, // uniqueName
                                        getMsg,
                                        messageDate,
                                        messageTime,
                                        audioUrl,
                                        getUserChatId // uid
                                );

                                chatLists.add(chatList);
                                chatAdapter.updateChatList(chatLists);
                                chattingRecyclerView.scrollToPosition(chatLists.size() - 1);

                                MemoryData.saveLastMsgTS(String.valueOf(getMessageTimeStamps), chatKey, Chat.this);

                                if (!getUserChatId.equals(userChatId)) {
                                    showNotification(recipientName, getMsg);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Chat.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean handleSendBtnTouch(View v, MotionEvent event) {
        ImageButton sendBtn = (ImageButton) v;
        LinearLayout recordingLayout = findViewById(R.id.recording_layout);
        LinearLayout chatInputLayout = findViewById(R.id.chat_input_layout);
        TextView recordingTime = findViewById(R.id.recording_time);
        TextView slideCancel = findViewById(R.id.slide_cancel);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (sendBtn.getDrawable().getConstantState().equals(ContextCompat.getDrawable(this, R.drawable.baseline_keyboard_voice_24).getConstantState())) {
                    startRecording();
                    animateButtonScale(sendBtn, 1.5f); // 放大按鈕

                    recordingLayout.setVisibility(View.VISIBLE);
                    chatInputLayout.setVisibility(View.GONE);
                    recordingTime.setText("0:00");
                    slideCancel.setText("滑動以取消");

                    // 設置錄音按鈕的z順序
                    sendBtn.bringToFront();
                    sendBtn.setElevation(20); // 提升按鈕的Z軸位置
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getRawX();
                sendBtn.setX(x - sendBtn.getWidth() / 2); // 跟隨手指移動

                if (x < sendBtn.getLeft()) {
                    slideCancel.setText("取消錄音");
                    slideCancel.setTextColor(Color.RED);
                    // 淡化處理
                    float distance = Math.abs(x - recordingTime.getX());
                    float alpha = 1.0f - Math.min(distance / 300f, 1.0f);
                    slideCancel.setAlpha(alpha);
                } else {
                    slideCancel.setText("滑動以取消");
                    slideCancel.setTextColor(Color.GRAY);
                    slideCancel.setAlpha(1.0f);
                }
                break;
            case MotionEvent.ACTION_UP:
                // 恢復按钮状态
                sendBtn.setPressed(false);
                sendBtn.invalidate();
                animateButtonScale(sendBtn, 1.0f); // 恢復按鈕大小

                if (slideCancel.getText().equals("取消錄音")) {
                    cancelRecording();
                    animateCancelRecording(sendBtn, recordingLayout, chatInputLayout);
                } else {
                    stopRecording();
                    sendRecording();
                    recordingLayout.setVisibility(View.GONE);
                    chatInputLayout.setVisibility(View.VISIBLE);
                }
                // 向右滑動回到原位
                float originalX = sendBtn.getX();
                ObjectAnimator moveX = ObjectAnimator.ofFloat(sendBtn, "translationX", sendBtn.getLeft() - originalX, 0);
                moveX.setDuration(500);
                moveX.start();
                return true;
        }
        return false;
    }

    private void animateButtonScale(View view, float scale) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", scale);
        AnimatorSet scaleAnim = new AnimatorSet();
        scaleAnim.playTogether(scaleX, scaleY);
        scaleAnim.setDuration(300);
        scaleAnim.start();
    }

    private void animateCancelRecording(View sendBtn, LinearLayout recordingLayout, LinearLayout chatInputLayout) {
        float originalX = sendBtn.getX();
        ObjectAnimator moveX = ObjectAnimator.ofFloat(sendBtn, "translationX", sendBtn.getLeft() - originalX, 0);
        moveX.setDuration(500);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(recordingLayout, "alpha", 1f, 0f);
        fadeOut.setDuration(500);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(chatInputLayout, "alpha", 0f, 1f);
        fadeIn.setDuration(500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveX, fadeOut, fadeIn);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recordingLayout.setVisibility(View.GONE);
                chatInputLayout.setVisibility(View.VISIBLE);
                recordingLayout.setAlpha(1f); // 重置透明度
            }
        });
        animatorSet.start();
    }

    private void startRecording() {
        // 初始化錄音設置
        if (recorder != null) {
            recorder.release();
        }

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("Chat", "prepare() failed: " + e.getMessage());
        }

        try {
            recorder.start();
        } catch (IllegalStateException e) {
            Log.e("Chat", "start() failed: " + e.getMessage());
        }

        startTime = System.currentTimeMillis();
        recordingTimer.setVisibility(View.VISIBLE);
        handler.post(updateTimer);
        isRecordingCancelled = false;
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e("Chat", "stop() failed: " + e.getMessage());
            }
            recorder.release();
            recorder = null;
        }
        handler.removeCallbacks(updateTimer);
        recordingTimer.setVisibility(View.GONE);
    }

    private void cancelRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e("Chat", "stop() failed: " + e.getMessage());
            }
            recorder.release();
            recorder = null;
        }
        handler.removeCallbacks(updateTimer);
        recordingTimer.setVisibility(View.GONE);
        isRecordingCancelled = true;
    }

    private void sendRecording() {
        File cacheDir = getExternalCacheDir();
        if (cacheDir == null) {
            Toast.makeText(this, "Unable to access cache directory", Toast.LENGTH_SHORT).show();
            return;
        }

        fileName = cacheDir.getAbsolutePath() + "/audiorecordtest.3gp";

        Uri fileUri = Uri.fromFile(new File(fileName));
        if (fileUri.getLastPathSegment() == null) {
            Toast.makeText(this, "Invalid file path", Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用指定的音频存储路径
        StorageReference storageRef = firebaseStorage.getReferenceFromUrl("gs://ea-vtc-chat.appspot.com/audio").child(fileUri.getLastPathSegment());
        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String audioUrl = uri.toString();
                    sendMessageWithAudioUrl(audioUrl); // 发送音频 URL
                }).addOnFailureListener(e -> Log.e("Chat", "Failed to get download URL", e)))
                .addOnFailureListener(e -> Log.e("Chat", "Upload failed", e));
    }

    private void sendMessageWithAudioUrl(String audioUrl) {
        if (chatKey == null || chatKey.isEmpty()) {
            // 如果 chatKey 为 null 或空，创建新的 chatKey 并初始化参与者数据
            if (userChatId != null && otherChatId != null) {
                chatKey = sanitizeKey(userChatId + "_" + otherChatId); // 生成 chatKey
                createNewChatData(chatKey, userChatId, otherChatId);
            } else {
                Log.e(TAG, "User chat IDs are null");
                Toast.makeText(this, "Failed to send audio message", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 发送音频消息
        sendMessage(chatKey, "", userChatId, audioUrl, new DatabaseCallback() {
            @Override
            public void onSuccess() {
                // Audio message sent successfully
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(Chat.this, "Failed to send audio message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAudioMessageToDatabase(String chatKey, String audioUrl) {
        if (chatKey == null || audioUrl == null) {
            Log.e(TAG, "Chat key or audio URL is null");
            Toast.makeText(this, "Failed to send audio message", Toast.LENGTH_SHORT).show();
            return;
        }

        sendMessage(chatKey, "", userChatId, audioUrl, new DatabaseCallback() {
            @Override
            public void onSuccess() {
                // Audio message sent successfully
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(Chat.this, "Failed to send audio message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSendButtonIcon() {
        if (messageInput.getText().toString().trim().isEmpty() && imageUri == null) {
            sendButton.setImageResource(R.drawable.baseline_keyboard_voice_24); // 显示录音图标
        } else {
            sendButton.setImageResource(R.drawable.round_send_24); // 显示发送图标
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            previewLayout.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUri).into(previewImage);
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) return;

        // 使用指定的图片存储路径
        StorageReference fileReference = firebaseStorage.getReferenceFromUrl("gs://ea-vtc-chat.appspot.com/image").child(System.currentTimeMillis() + ".jpg");
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrl = uri.toString();
                    Toast.makeText(Chat.this, "Image upload successful", Toast.LENGTH_SHORT).show();
                    updateSendButtonIcon(); // 更新按钮图标
                }).addOnFailureListener(e -> {
                    Toast.makeText(Chat.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    previewLayout.setVisibility(View.GONE);
                    imageUri = null;
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(Chat.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    previewLayout.setVisibility(View.GONE);
                    imageUri = null;
                });
    }

    private void sendMessageToDatabase(String message) {
        if (chatKey == null || chatKey.isEmpty()) {
            // 如果 chatKey 为 null 或空，创建新的 chatKey 并初始化参与者数据
            if (userChatId != null && otherChatId != null) {
                chatKey = sanitizeKey(userChatId + "_" + otherChatId); // 生成 chatKey
                createNewChatData(chatKey, userChatId, otherChatId);
            } else {
                Log.e(TAG, "User chat IDs are null");
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 发送消息
        sendMessage(chatKey, message, userChatId, uploadedImageUrl, new DatabaseCallback() {
            @Override
            public void onSuccess() {
                if (uploadedImageUrl != null) {
                    uploadedImageUrl = null;
                    imageUri = null;
                    previewLayout.setVisibility(View.GONE);
                }
                updateSendButtonIcon(); // 更新按钮图标
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(Chat.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String chatKey, String message, String userChatId, String mediaUrl, DatabaseCallback callback) {
        long currentTimestamp = System.currentTimeMillis();

        // 格式化日期和时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        String formattedDate = dateFormat.format(new Date(currentTimestamp));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault());
        String formattedTime = timeFormat.format(new Date(currentTimestamp));

        // Sanitize the chatKey
        chatKey = sanitizeKey(chatKey);

        DatabaseReference newMessageRef = databaseHelper.getDatabaseReference().child("chats").child(chatKey).child(String.valueOf(currentTimestamp));
        newMessageRef.child("msg").setValue(message);
        newMessageRef.child("date").setValue(formattedDate); // 存储格式化后的日期
        newMessageRef.child("time").setValue(formattedTime); // 存储格式化后的时间
        newMessageRef.child("user_chat_id").setValue(userChatId);
        if (mediaUrl != null) {
            if (mediaUrl.endsWith(".jpg") || mediaUrl.endsWith(".png")) {
                newMessageRef.child("imageUrl").setValue(mediaUrl);
            } else if (mediaUrl.endsWith(".3gp")) {
                newMessageRef.child("audioUrl").setValue(mediaUrl);
            }
        }
        callback.onSuccess();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chat Notifications";
            String description = "Notifications for new chat messages";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String sender, String message) {
        if (AppLifecycleObserver.isAppInForeground()) {
            Log.d("Chat", "App is in foreground, not sending notification.");
            return;
        }

        Intent intent = new Intent(this, Chat.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_chat_24)
                .setContentTitle(sender)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        notificationManager.notify(0, builder.build());
    }

    private String generateDateFromTimestamps(long timestamps, String format) {
        Timestamp ts = new Timestamp(timestamps);
        Date date = new Date(ts.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
        return sdf.format(date);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                finish();
            }
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final Runnable updateTimer = new Runnable() {
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            recordingTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            handler.postDelayed(this, 500);
        }
    };

    private static final String TAG = "ChatActivity";

    private void retrieveIntentData() {
        Intent intent = getIntent();
        String userUid = intent.getStringExtra("user_uid");
        String otherUid = intent.getStringExtra("other_uid");

        Log.d(TAG, "user_uid from intent: " + userUid);
        Log.d(TAG, "other_uid from intent: " + otherUid);

        if (userUid != null && otherUid != null) {
            retrieveChatIds(userUid, otherUid);
        } else {
            Log.e(TAG, "User UIDs are null");
            Toast.makeText(this, "Failed to retrieve user information", Toast.LENGTH_SHORT).show();
            finish(); // 结束活动以避免进一步的错误
        }
    }

    private void retrieveChatIds(String userUid, String otherUid) {
        getUserInfo(userUid, user1 -> {
            if (user1 != null) {
                getUserInfo(otherUid, user2 -> {
                    if (user2 != null) {
                        userChatId = user1.getChatId();
                        otherChatId = user2.getChatId();

                        if (userChatId != null && otherChatId != null) {
                            // 打印 chatId 到日志
                            Log.d(TAG, "User Chat ID: " + userChatId);
                            Log.d(TAG, "Other Chat ID: " + otherChatId);

                            // 设置 UI 信息
                            setUIInformation(user1, user2);

                            // 不在此处创建新的聊天数据，将其移到发送消息时处理
                        } else {
                            Log.e(TAG, "Chat IDs are null");
                            Toast.makeText(this, "Failed to retrieve chat IDs", Toast.LENGTH_SHORT).show();
                            finish(); // 结束活动以避免进一步的错误
                        }
                    } else {
                        Log.e(TAG, "Other user not found");
                        Toast.makeText(this, "Other user not found", Toast.LENGTH_SHORT).show();
                        finish(); // 结束活动以避免进一步的错误
                    }
                });
            } else {
                Log.e(TAG, "User not found");
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                finish(); // 结束活动以避免进一步的错误
            }
        });
    }

    private void setUIInformation(ChatList user1, ChatList user2) {
        recipientName = user2.getName();
        userName = MemoryData.getName(this);
        String userImageUrl = user2.getImageUrl();

        if (recipientName != null) {
            ((TextView) findViewById(R.id.name)).setText(recipientName);
        } else {
            recipientName = "Unknown User"; // 或者提供一个默认值
            ((TextView) findViewById(R.id.name)).setText(recipientName);
        }

        if (userImageUrl != null && !userImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(userImageUrl)
                    .placeholder(R.drawable.usericon_default) // 加载中的占位符
                    .error(R.drawable.usericon_default)      // 加载失败时显示的图像
                    .into((ImageView) findViewById(R.id.profilePic));
        } else {
            // 加载默认的用户图片
            Glide.with(this)
                    .load(R.drawable.usericon_default)
                    .into((ImageView) findViewById(R.id.profilePic));
        }
    }

    private String sanitizeKey(String key) {
        return key.replaceAll("[.#$\\[\\]]", "_");
    }
}