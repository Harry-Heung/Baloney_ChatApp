package com.example.ea_vtc_chat_project;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView profileImage;
    private MyProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private DatabaseHelper databaseHelper;

    private TextView passwordStrength;
    private ImageView confirmPasswordCheck;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int USER_ID_LENGTH = 12;
    private static final String TAG = "Register";

    // Activity Result Launcher for picking image
    private final ActivityResultLauncher<Intent> pickImageResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();
                        profileImage.setImageURI(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance(this);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new MyProgressDialog(this);
        progressDialog.setCancelable(false);

        setListeners();
    }

    private void initViews() {
        profileImage = findViewById(R.id.r_profile_image);
        passwordStrength = findViewById(R.id.password_strength);
        confirmPasswordCheck = findViewById(R.id.confirm_password_check);
    }

    private void setListeners() {
        findViewById(R.id.r_upload_image_btn).setOnClickListener(v -> openFileChooser());
        profileImage.setOnClickListener(v -> openFileChooser());

        EditText password = findViewById(R.id.r_password);
        EditText confirmPassword = findViewById(R.id.r_confirm_password);

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordStrength(s.toString());
                checkConfirmPassword(password.getText().toString(), confirmPassword.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkConfirmPassword(password.getText().toString(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        findViewById(R.id.r_registerBtn).setOnClickListener(v -> registerUser());
        findViewById(R.id.r_loginNowBtn).setOnClickListener(v -> {
            startActivity(new Intent(Register.this, Login.class));
            finish();
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageResultLauncher.launch(intent);
    }

    private void registerUser() {
        EditText email = findViewById(R.id.r_email);
        EditText password = findViewById(R.id.r_password);
        EditText confirmPassword = findViewById(R.id.r_confirm_password);
        EditText userName = findViewById(R.id.r_user_name);

        String emailTxt = email.getText().toString();
        String passwordTxt = password.getText().toString();
        String confirmPasswordTxt = confirmPassword.getText().toString();
        String userNameTxt = userName.getText().toString();

        if (emailTxt.isEmpty() || passwordTxt.isEmpty() || confirmPasswordTxt.isEmpty() || userNameTxt.isEmpty()) {
            Toast.makeText(Register.this, "Please fill in all information", Toast.LENGTH_SHORT).show();
        } else if (!isEmailValid(emailTxt)) {
            Toast.makeText(Register.this, "Invalid Email Format", Toast.LENGTH_SHORT).show();
        } else if (!passwordTxt.equals(confirmPasswordTxt)) {
            Toast.makeText(Register.this, "The password is different, please re-enter it.", Toast.LENGTH_SHORT).show();
        } else if (passwordStrength.getVisibility() == View.VISIBLE && passwordStrength.getText().toString().equals("Weak")) {
            Toast.makeText(Register.this, "Password is too weak", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.show();
            mAuth.createUserWithEmailAndPassword(emailTxt, passwordTxt)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            generateUniqueChatId(chatId -> {
                                if (chatId != null) {
                                    generateUniqueUserName(userNameTxt, uniqueUserName -> {
                                        if (uniqueUserName != null) {
                                            registerUserWithUid(uid, chatId, userNameTxt, uniqueUserName, emailTxt, passwordTxt);
                                        } else {
                                            progressDialog.dismiss();
                                            Toast.makeText(Register.this, "Failed to generate unique username", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(Register.this, "Failed to generate unique chat ID", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(Register.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void registerUserWithUid(String uid, String chatId, String name, String uniqueName, String email, String password) {
        if (imageUri != null) {
            StorageReference fileReference = FirebaseStorage.getInstance().getReference("profile_images")
                    .child(uid + "_" + System.currentTimeMillis() + "." + getFileExtension(imageUri));
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveUserData(uid, chatId, name, uniqueName, email, password, imageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(Register.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveUserData(uid, chatId, name, uniqueName, email, password, "");
        }
    }

    private void saveUserData(String uid, String chatId, String name, String uniqueName, String email, String password, String imageUrl) {
        String nameID = uniqueName.substring(name.length()); // 提取 NameID

        Map<String, Object> userData = new HashMap<>();
        userData.put("chatId", chatId);
        userData.put("email", email);
        userData.put("name", name);
        userData.put("nameID", nameID);
        userData.put("unique_name", uniqueName); // 保存 unique_name
        userData.put("password", password);
        userData.put("imageUrl", imageUrl);
        userData.put("uid", uid);

        databaseHelper.saveUserData(uid, userData, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                Toast.makeText(Register.this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show();
                mAuth.signOut(); // 注销用户
                openLoginActivity();
            }

            @Override
            public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(Register.this, "Failed to register. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openLoginActivity() {
        startActivity(new Intent(Register.this, Login.class));
        finish();
    }

    private void generateUniqueChatId(OnUniqueChatIdGeneratedListener listener) {
        generateUniqueChatIdRecursive(new SecureRandom(), listener);
    }

    private void generateUniqueChatIdRecursive(SecureRandom random, OnUniqueChatIdGeneratedListener listener) {
        String chatId = generateRandomString(USER_ID_LENGTH, random);

        databaseHelper.checkIfChatIdExists(chatId, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(String... args) {
                listener.onUniqueChatIdGenerated(null); // Chat ID 已存在
            }

            @Override
            public void onFailure(Exception e) {
                listener.onUniqueChatIdGenerated(chatId); // Chat ID 是唯一的
            }
        });
    }

    private void generateUniqueUserName(String baseUserName, OnUniqueUserNameGeneratedListener listener) {
        generateUniqueUserNameRecursive(baseUserName, new SecureRandom(), 0, listener);
    }

    private void generateUniqueUserNameRecursive(String baseUserName, SecureRandom random, int attempt, OnUniqueUserNameGeneratedListener listener) {
        if (attempt >= 1000) {
            Log.e(TAG, "Failed to generate unique username after 1000 attempts");
            listener.onUniqueUserNameGenerated(null);
            return;
        }

        int randomNum = 1000 + random.nextInt(9000); // 生成4位随机数字
        String nameID = "#" + randomNum;
        String uniqueName = baseUserName + nameID; // 通过组合 Name 和 NameID 创建 uniqueName

        Log.d(TAG, "Attempt " + attempt + ": Checking uniqueName " + uniqueName);

        databaseHelper.checkIfUniqueNameExists(uniqueName, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(String... args) {
                Log.d(TAG, "uniqueName " + uniqueName + " already exists, trying again");
                generateUniqueUserNameRecursive(baseUserName, random, attempt + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "uniqueName " + uniqueName + " is unique");
                listener.onUniqueUserNameGenerated(uniqueName);
            }
        });
    }

    private String generateRandomString(int length, SecureRandom random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private boolean isEmailValid(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private void checkPasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrength.setVisibility(View.GONE);
            return;
        }

        passwordStrength.setVisibility(View.VISIBLE);

        if (password.length() < 8) {
            passwordStrength.setText("Weak");
            passwordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            return;
        }

        int strength = 0;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[!@#$%^&*+=?-].*")) strength++;

        switch (strength) {
            case 2:
                passwordStrength.setText("Weak");
                passwordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                break;
            case 3:
                passwordStrength.setText("Medium");
                passwordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                break;
            case 4:
                passwordStrength.setText("Strong");
                passwordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                break;
            default:
                passwordStrength.setText("Weak");
                passwordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                break;
        }
    }

    private void checkConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            confirmPasswordCheck.setVisibility(View.GONE);
            return;
        }

        confirmPasswordCheck.setVisibility(View.VISIBLE);

        if (password.equals(confirmPassword)) {
            confirmPasswordCheck.setImageResource(R.drawable.baseline_check_24);
        } else {
            confirmPasswordCheck.setImageResource(R.drawable.baseline_close_red24);
        }
    }

    interface OnUniqueChatIdGeneratedListener {
        void onUniqueChatIdGenerated(String chatId);
    }

    interface OnUniqueUserNameGeneratedListener {
        void onUniqueUserNameGenerated(String uniqueUserName);
    }
}