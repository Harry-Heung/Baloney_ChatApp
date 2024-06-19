package com.example.ea_vtc_chat_project;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.ea_vtc_chat_project.api.Country;
import com.example.ea_vtc_chat_project.api.CountryService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "UserProfileActivity";

    private de.hdodenhof.circleimageview.CircleImageView userProfileImage;
    private ImageView uploadProfileImageButton;
    private EditText nameEditText, birthdateEditText, sloganEditText;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton, femaleRadioButton;
    private TextView ageTextView;
    private Spinner regionSpinner;
    private Button saveButton;
    private ProgressBar progressBar;

    private DatabaseHelper databaseHelper;
    private FirebaseStorage firebaseStorage;
    private Uri imageUri;
    private String uploadedImageUrl;
    private ArrayAdapter<String> regionAdapter;

    private String initialName;
    private String userRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initViews();
        initFirebase();
        setupListeners();
        loadUserProfile();
    }

    private void initViews() {
        userProfileImage = findViewById(R.id.profile_image_setting);
        uploadProfileImageButton = findViewById(R.id.upload_image_btn_setting);
        nameEditText = findViewById(R.id.edit_User_name);
        sloganEditText = findViewById(R.id.edit_User_Slogan); // 初始化 Slogan
        birthdateEditText = findViewById(R.id.birthdateEditText);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        maleRadioButton = findViewById(R.id.maleRadioButton);
        femaleRadioButton = findViewById(R.id.femaleRadioButton);
        ageTextView = findViewById(R.id.agePicker);
        regionSpinner = findViewById(R.id.regionSpinner);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar); // 初始化 ProgressBar

        // 初始化地区选择器的适配器
        regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionSpinner.setAdapter(regionAdapter);

        // 初始化返回按钮并设置点击监听
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void initFirebase() {
        databaseHelper = DatabaseHelper.getInstance(this);
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("UserProfileActivity", "User ID: " + uid);
        if (uid == null) {
            Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // 从 Firebase 数据库中加载用户数据
        databaseHelper.getDatabaseReference().child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (snapshot.exists()) {
                            // 存在用户数据，加载到视图中
                            String name = snapshot.child("name").getValue(String.class);
                            initialName = name; // 保存初始名称
                            String slogan = snapshot.child("slogan").getValue(String.class);
                            String gender = snapshot.child("gender").getValue(String.class);
                            String birthdate = snapshot.child("birthdate").getValue(String.class);
                            userRegion = snapshot.child("region").getValue(String.class);
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                            Log.d("UserProfileActivity", "Name: " + name); // 日志

                            if (name != null) {
                                nameEditText.setText(name);
                            }
                            if (slogan != null) {
                                sloganEditText.setText(slogan);
                            }
                            if (birthdate != null) {
                                birthdateEditText.setText(birthdate);
                                int age = calculateAge(birthdate);
                                ageTextView.setText(String.valueOf(age));
                            }
                            if (gender != null) {
                                if ("M".equals(gender)) {
                                    maleRadioButton.setChecked(true);
                                } else if ("F".equals(gender)) {
                                    femaleRadioButton.setChecked(true);
                                }
                            }
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(UserProfileActivity.this).load(imageUrl).into(userProfileImage);
                            } else {
                                userProfileImage.setImageResource(R.drawable.usericon_default);
                            }

                            // 加载国家列表后设置用户的地区
                            fetchCountries(userRegion);
                        } else {
                            Log.e("UserProfileActivity", "Snapshot does not exist");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserProfileActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                        Log.e("UserProfileActivity", "Database error: " + error.getMessage());
                    }
                });
    }

    private void setupListeners() {
        uploadProfileImageButton.setOnClickListener(v -> openFileChooser());

        saveButton.setOnClickListener(v -> saveUserProfile());

        userProfileImage.setOnClickListener(v -> openFileChooser());

        birthdateEditText.setOnClickListener(v -> showDatePickerDialog());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // 使用指定的图片存储路径
        StorageReference fileReference = firebaseStorage.getReferenceFromUrl("gs://ea-vtc-chat.appspot.com/image").child(System.currentTimeMillis() + ".jpg");
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrl = uri.toString();
                    Glide.with(UserProfileActivity.this).load(uploadedImageUrl).into(userProfileImage);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserProfileActivity.this, "Image upload successful", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserProfileActivity.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = nameEditText.getText().toString().trim();
        String slogan = sloganEditText.getText().toString().trim();
        String gender = null;
        if (genderRadioGroup.getCheckedRadioButtonId() != -1) {
            gender = genderRadioGroup.getCheckedRadioButtonId() == R.id.maleRadioButton ? "M" : "F"; // 根据RadioButton的id判断Gender
        }
        String birthdate = birthdateEditText.getText().toString().trim();
        String region = (String) regionSpinner.getSelectedItem();
        progressBar.setVisibility(View.VISIBLE);

        if (!name.equals(initialName)) {
            // 如果名称发生了变化，则生成新的 NameID 和 UniqueName
            String finalGender = gender;
            generateUniqueUserNameRecursive(name, new SecureRandom(), 0, (UniqueName, nameId) -> {
                if (UniqueName == null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UserProfileActivity.this, "Failed to generate unique username", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);
                updates.put("nameID", nameId);
                updates.put("UniqueName", UniqueName);

                if (!slogan.isEmpty()) {
                    updates.put("slogan", slogan);
                }
                if (finalGender != null) {
                    updates.put("gender", finalGender);
                }
                if (!birthdate.isEmpty()) {
                    updates.put("birthdate", birthdate);
                }
                if (region != null && !region.isEmpty()) {
                    updates.put("region", region);
                }
                if (uploadedImageUrl != null) {
                    updates.put("imageUrl", uploadedImageUrl);
                }

                // 更新或创建用户信息
                databaseHelper.getDatabaseReference().child("users").child(uid).updateChildren(updates, (error, ref) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(UserProfileActivity.this, "Failed to update profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UserProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            Map<String, Object> updates = new HashMap<>();
            if (!slogan.isEmpty()) {
                updates.put("slogan", slogan);
            }
            if (gender != null) {
                updates.put("gender", gender);
            }
            if (!birthdate.isEmpty()) {
                updates.put("birthdate", birthdate);
            }
            if (region != null && !region.isEmpty()) {
                updates.put("region", region);
            }
            if (uploadedImageUrl != null) {
                updates.put("imageUrl", uploadedImageUrl);
            }

            if (updates.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新或创建用户信息
            databaseHelper.getDatabaseReference().child("users").child(uid).updateChildren(updates, (error, ref) -> {
                progressBar.setVisibility(View.GONE);
                if (error != null) {
                    Toast.makeText(UserProfileActivity.this, "Failed to update profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void generateUniqueUserNameRecursive(String baseUserName, SecureRandom random, int attempt, OnUniqueUserNameGeneratedListener listener) {
        if (attempt >= 1000) {
            Log.e(TAG, "Failed to generate unique username after 1000 attempts");
            listener.onUniqueUserNameGenerated(null, null);
            return;
        }

        int randomNum = 1000 + random.nextInt(9000); // 生成4位隨機數字
        String nameID = "#" + randomNum;
        String UniqueName = baseUserName + nameID; // 通過組合 Name 和 NameID 創建 UniqueName

        Log.d(TAG, "Attempt " + attempt + ": Checking UniqueName " + UniqueName);

        databaseHelper.checkIfUniqueNameExists(UniqueName, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(String... args) {
                Log.d(TAG, "UniqueName " + UniqueName + " already exists, trying again");
                generateUniqueUserNameRecursive(baseUserName, random, attempt + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "UniqueName " + UniqueName + " is unique");
                listener.onUniqueUserNameGenerated(UniqueName, nameID);
            }
        });
    }

    private void fetchCountries(String userRegion) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://restcountries.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CountryService service = retrofit.create(CountryService.class);

        Call<List<Country>> call = service.getAllCountries();
        call.enqueue(new Callback<List<Country>>() {
            @Override
            public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> countryNames = new ArrayList<>();
                    for (Country country : response.body()) {
                        countryNames.add(country.getName().getCommon());
                    }
                    // 对国家名称进行排序
                    Collections.sort(countryNames);
                    regionAdapter.clear();
                    regionAdapter.addAll(countryNames);
                    regionAdapter.notifyDataSetChanged();

                    // 设置用户的地区
                    if (userRegion != null) {
                        int spinnerPosition = regionAdapter.getPosition(userRegion);
                        if (spinnerPosition >= 0) {
                            regionSpinner.setSelection(spinnerPosition);
                        }
                    }
                    Log.d("UserProfileActivity", "Countries loaded successfully");
                } else {
                    Toast.makeText(UserProfileActivity.this, "Failed to load countries", Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileActivity", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Country>> call, Throwable t) {
                Toast.makeText(UserProfileActivity.this, "Failed to load countries", Toast.LENGTH_SHORT).show();
                Log.e("UserProfileActivity", "Error loading countries", t);
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                UserProfileActivity.this,
                (view, year1, month1, dayOfMonth) -> {
                    String birthdate = String.format("%02d/%02d/%04d", dayOfMonth, month1 + 1, year1);
                    birthdateEditText.setText(birthdate);
                    int age = calculateAge(birthdate);
                    ageTextView.setText(String.valueOf(age));
                },
                year, month, day);
        datePickerDialog.show();
    }

    private int calculateAge(String birthdate) {
        String[] parts = birthdate.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);

        Calendar birth = Calendar.getInstance();
        birth.set(year, month - 1, day);

        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    interface OnUniqueUserNameGeneratedListener {
        void onUniqueUserNameGenerated(String userfullname, String nameId);
    }
}