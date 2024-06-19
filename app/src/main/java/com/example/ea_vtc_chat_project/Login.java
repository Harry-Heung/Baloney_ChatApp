package com.example.ea_vtc_chat_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {
    private FirebaseAuth mAuth; // Reference to FirebaseAuth
    private MyProgressDialog progressDialog; // Progress bar for login process

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Set the layout for the login activity

        // 確保 Firebase 已初始化
        FirebaseApp.initializeApp(this);

        // Initializing FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initializing input fields and buttons from the layout
        final EditText emailET = findViewById(R.id.l_email);
        final EditText passwordET = findViewById(R.id.l_password);
        final AppCompatButton loginNowBtn = findViewById(R.id.l_LoginBtn);
        final TextView registerNowTV = findViewById(R.id.l_registerNowBtn);

        // Check if user is already logged in. If yes, open MainActivity; otherwise, user needs to register
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            openMainActivity();
        }

        // Create and configure a progress dialog for the login process
        progressDialog = new MyProgressDialog(this);
        progressDialog.setCancelable(false);

        // Set click listeners
        loginNowBtn.setOnClickListener(v -> handleLoginClick(emailET, passwordET));
        registerNowTV.setOnClickListener(v -> handleRegisterClick());
    }

    private void handleLoginClick(EditText emailET, EditText passwordET) {
        // Retrieve user's entered data from EditText
        final String emailTxt = emailET.getText().toString();
        final String passwordTxt = passwordET.getText().toString();

        // Check if user's entered email and password are empty or not
        if (emailTxt.isEmpty() || passwordTxt.isEmpty()) {
            Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
        } else {
            // Login the user
            loginUser(emailTxt, passwordTxt);
        }
    }

    private void handleRegisterClick() {
        // Open the Register activity and finish the Login activity
        startActivity(new Intent(Login.this, Register.class));
        finish();
    }

    private void loginUser(String email, String password) {
        // Show progress bar
        progressDialog.show();

        // 使用 Firebase Authentication 進行用戶登錄
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 登录成功
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 获取用户的 UID 和 Email
                            String userId = user.getUid();
                            String userName = user.getEmail(); // 使用 Email 作为用户名

                            // Save user's ID and email for future logins
                            MemoryData.saveUniqueName(userId, Login.this);
                            MemoryData.saveName(userName, Login.this);

                            // Open the MainActivity and finish the Login activity
                            progressDialog.dismiss();
                            Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();
                            openMainActivity();
                        }
                    } else {
                        // 登录失败
                        progressDialog.dismiss();
                        Toast.makeText(Login.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openMainActivity() {
        // Open the MainActivity and finish the Login activity
        startActivity(new Intent(Login.this, MainActivity.class));
        finish();
    }
}