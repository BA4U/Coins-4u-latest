package com.bg4u.coins4u.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bg4u.coins4u.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_IMAGE_PICK = 1;
    
    private EditText username, userStatus;
    private CircleImageView userProfileImage;
    
    private DatabaseReference databaseReference;
    private String currentUserId;
    private String photoUri;
    
    private StorageReference userProfileStorageReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userProfileStorageReference = FirebaseStorage.getInstance().getReference().child("Profile Images");
    
        Button updateAccountSettings = findViewById(R.id.update_settings_button);
        username = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
        
        updateAccountSettings.setOnClickListener(v -> updateSettings());
        
        RetrieveUserInfo();
        
        userProfileImage.setOnClickListener(v -> openImagePicker());
    }
    
    private void openImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUEST_CODE_IMAGE_PICK);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            handleImageUpload(imageUri);
        }
    }
    
    private void handleImageUpload(Uri imageUri) {
        StorageReference filePath = userProfileStorageReference.child(currentUserId + ".jpg");
        UploadTask uploadTask = filePath.putFile(imageUri);
        uploadTask.addOnSuccessListener(this, this::handleSuccess);
    }
    
    private void handleSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
        firebaseUri.addOnSuccessListener(this, uri -> {
            if (uri != null) {
                saveImageUri(uri);
            } else {
                Toast.makeText(SettingsActivity.this, "Image URI is null", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    
    private void saveImageUri(Uri uri) {
        final String downloadUrl = uri.toString();
        databaseReference.child("Users").child(currentUserId).child("image")
                .setValue(downloadUrl)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Image saved in database successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = Objects.requireNonNull(task.getException()).toString();
                        Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateSettings() {
        String setUsername = username.getText().toString();
        String setUserStatus = userStatus.getText().toString();
        
        if (TextUtils.isEmpty(setUsername)) {
            Toast.makeText(this, "Please write your username first...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(setUserStatus)) {
            Toast.makeText(this, "Please write your status first...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if photoUri is null before using it
        if (photoUri == null) {
            Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserId);
        profileMap.put("name", setUsername);
        profileMap.put("status", setUserStatus);
        profileMap.put("image", photoUri);
        
        databaseReference.child("Users").child(currentUserId).updateChildren(profileMap)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Your profile has been updated...", Toast.LENGTH_SHORT).show();
                        sendUserToMainActivity();
                    } else {
                        String errorMessage = Objects.requireNonNull(task.getException()).toString();
                        Toast.makeText(SettingsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
    
    private void RetrieveUserInfo() {
        DatabaseReference userRef = databaseReference.child("Users").child(currentUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String retrieveUsername = dataSnapshot.child("name").getValue(String.class);
                    String retrieveUserStatus = dataSnapshot.child("status").getValue(String.class);
                    String retrieveUserImage = dataSnapshot.child("image").getValue(String.class);
                    
                    if (retrieveUsername != null && retrieveUserStatus != null) {
                        username.setText(retrieveUsername);
                        userStatus.setText(retrieveUserStatus);
                    } else {
                        Toast.makeText(SettingsActivity.this, "Please set and update your profile information...", Toast.LENGTH_SHORT).show();
                    }
                    
                    if (retrieveUserImage != null) {
                        photoUri = retrieveUserImage;
                        // Load user profile image using Picasso
                        Picasso.get().load(retrieveUserImage).into(userProfileImage);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SettingsActivity", "Database Error: " + databaseError.getMessage());
            }
        });
    }
    
}
