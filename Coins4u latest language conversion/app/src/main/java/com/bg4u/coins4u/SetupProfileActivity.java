package com.bg4u.coins4u;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.amrdeveloper.lottiedialog.LottieDialog;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivitySetupProfileBinding;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
public class SetupProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 86;
    private ActivitySetupProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private DatabaseReference usersRef; // Reference to the "Users" node in the Firebase Realtime Database
    private Uri selectedImage;
    private LottieDialog dialog;
    private CountryCodePicker countryCodePicker;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String name, bio, location, phoneNumber, age, socialMediaLink;
    
    // List of prohibited slang words
    List<String> prohibitedWords = Arrays.asList("maa", "baap", "beta", "bsdk", "dick", "chut", "mc", "bc", "laude", "lode", "behen", "gandu", "fuck", "sex", "porn", "chutiya", "bitch", "sexy", "chod", "fucker", "asshole", "madharchod");
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("profiles");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users"); // Add this line to assign a value to usersRef
        
        binding.imageView.setOnClickListener(v -> openImagePicker());
        binding.saveProfileBtn.setOnClickListener(v -> saveUserProfile());
    
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.setup_profile);
        
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null) {
                        selectedImage = result;
                        binding.imageView.setImageURI(selectedImage);
                    }
                });
        
        fetchUserData();
    }
    
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }
    
    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            firestore.collection("users")
                    .document(currentUserUid)
                    .get()
                    .addOnSuccessListener(this::populateUserData)
                    .addOnFailureListener(e -> displayErrorMessage("Failed to fetch user data: " + e.getMessage()));
        } else {
            displayErrorMessage("Current user is null");
        }
    }
    
    
    private void populateUserData(DocumentSnapshot document) {
        if (document.exists()) {
            User currentUser = document.toObject(User.class);
            if (currentUser != null) {
                binding.nameBox.setText(currentUser.getName());
                binding.bio.setText(currentUser.getBio());
                binding.locationBox.setText(currentUser.getLocation());
                binding.phoneNumberBox.setText(currentUser.getPhoneNumber());
                binding.ageBox.setText(currentUser.getAge());
                binding.socialMediaLinkBox.setText(currentUser.getSocialMediaLink());
                if (currentUser.getProfile() != null && !currentUser.getProfile().isEmpty()) {
                    Glide.with(this)
                            .load(currentUser.getProfile())
                            .into(binding.imageView);
                }
            } else {
                displayErrorMessage("User data not found");
            }
        }
    }
    
    private void saveUserProfile() {
        name = binding.nameBox.getText().toString().trim();
        bio = binding.bio.getText().toString().trim();
        location = binding.locationBox.getText().toString().trim();
        phoneNumber = binding.phoneNumberBox.getText().toString().trim();
//        phoneNumber = countryCodePicker.getFullNumberWithPlus();
        age = binding.ageBox.getText().toString().trim();
        socialMediaLink = binding.socialMediaLinkBox.getText().toString().trim();
        
        // Check if the name contains prohibited words
        for (String prohibitedWord : prohibitedWords) {
            if (name.toLowerCase().contains(prohibitedWord)) {
                binding.nameBox.setError("Please don't use this word");
                binding.nameBox.requestFocus();
                return;
            }
        }
        // Additional check to ensure no prohibited words in the name
        for (String prohibitedWord : prohibitedWords) {
            if (name.toLowerCase().matches(".*\\b" + Pattern.quote(prohibitedWord) + "\\b.*")) {
                binding.nameBox.setError("Please don't use this word");
                binding.nameBox.requestFocus();
                return;
            }
        }
    
        if (TextUtils.isEmpty(name) || name.length() < 3 || name.length() > 12) {
            binding.nameBox.setError("Username must be between 3 and 12 characters");
            binding.nameBox.requestFocus();
            return;
        }
        
        if (bio.length() > 72) {
            binding.bio.setError("Profile is too long");
            binding.bio.requestFocus();
            return;
        }
    
        // Check if the name contains prohibited words
        for (String prohibitedWord : prohibitedWords) {
            if (bio.toLowerCase().contains(prohibitedWord)) {
                binding.bio.setError("Bio contains prohibited words");
                binding.bio.requestFocus();
                return;
            }
        }
        // Additional check to ensure no prohibited words in the name
        for (String prohibitedWord : prohibitedWords) {
            if (bio.toLowerCase().matches(".*\\b" + Pattern.quote(prohibitedWord) + "\\b.*")) {
                binding.bio.setError("Bio contains prohibited words");
                binding.bio.requestFocus();
                return;
            }
        }
        if (!validatePhoneNumber()) {
            return;
        }
        
        if (!validateAge()) {
            return;
        }
        
        if (!validateSocialMediaLink()) {
            return;
        }
        
        showProgressDialog("Saving...");
        
        if (selectedImage != null) {
            compressAndUploadProfileImage();
        } else {
            updateUserProfile(null, false);
            saveUserDataToRealtimeDatabase(null, false);
        }
    }
    
    private boolean validatePhoneNumber() {
        if (!TextUtils.isEmpty(phoneNumber)) {
            EditText phoneInput = findViewById(R.id.phoneNumberBox);
            countryCodePicker = findViewById(R.id.login_countrycode);
            countryCodePicker.registerCarrierNumberEditText(phoneInput);
            if(!countryCodePicker.isValidFullNumber()){
                binding.phoneNumberBox.setError("Phone number not valid");
                binding.phoneNumberBox.requestFocus();
                return false;
            }
            String cleanedPhoneNumber = phoneNumber.replaceAll("\\D", "");
         
        }
        return true;
    }
    
 
    
    private boolean validateAge() {
        if (!TextUtils.isEmpty(age)) {
            int ageInt = Integer.parseInt(age);
            if (ageInt < 6 || ageInt > 80) {
                binding.ageBox.setError("Age must be between 5 and 85");
                binding.ageBox.requestFocus();
                return false;
            }
        }
        return true;
    }
    
    private boolean validateSocialMediaLink() {
        if (!TextUtils.isEmpty(socialMediaLink) && !android.util.Patterns.WEB_URL.matcher(socialMediaLink).matches()) {
            binding.socialMediaLinkBox.setError("Invalid link");
            binding.socialMediaLinkBox.requestFocus();
            return false;
        }
        return true;
    }
    
    private void compressAndUploadProfileImage() {
        showProgressDialog("Uploading profile...");
        
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            byte[] compressedData = outputStream.toByteArray();
            
            StorageReference profileImageRef = storageRef.child(Objects.requireNonNull(auth.getCurrentUser()).getUid() + "/profile.jpg");
            UploadTask uploadTask = profileImageRef.putBytes(compressedData);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                
                updateUserProfile(imageUrl, true);
                saveUserDataToRealtimeDatabase(imageUrl, true);
                
            }).addOnFailureListener(e -> {
                dismissProgressDialog();
                displayErrorMessage("Failed to get download URL: " + e.getMessage());
            })).addOnFailureListener(e -> {
                dismissProgressDialog();
                displayErrorMessage("Failed to upload image: " + e.getMessage());
            });
        } catch (IOException e) {
            dismissProgressDialog();
            displayErrorMessage("Failed to compress image: " + e.getMessage());
        }
    }
    
    private void updateUserProfile(String imageUrl, boolean updateImage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("bio", bio);
        updates.put("location", location);
        updates.put("phoneNumber", phoneNumber);
        updates.put("age", age);
        updates.put("socialMediaLink", socialMediaLink);
        
        if (updateImage && imageUrl != null) {
            updates.put("profile", imageUrl);
        }
        
        String userId = auth.getUid();
        if (userId != null) {
            firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Firestore data saved successfully
                        dismissProgressDialog();
                        Toast.makeText(SetupProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        displayErrorMessage("Failed to update profile: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            displayErrorMessage("User not found");
        }
    }
 
    // Save data to Firestore Database
    private void saveUserDataToRealtimeDatabase(String imageUrl, boolean updateImage) {
        String userId = auth.getUid();
        assert userId != null;
        DatabaseReference userRef = usersRef.child(userId);
        
        if (!TextUtils.isEmpty(name)) {
            userRef.child("name").setValue(name);
        }
        
        if (!TextUtils.isEmpty(bio)) {
            userRef.child("status").setValue(bio);
        }
        
        // Only set the image URL if it's updated
        if (updateImage && imageUrl != null) {
            userRef.child("image").setValue(imageUrl);
        }
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(SetupProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showProgressDialog(String message) {
        // Show progress dialog or use ProgressBar library instead
        dialog = new LottieDialog(this)
                .setAnimation(R.raw.loading_emoji)
                .setAnimationRepeatCount(LottieDialog.INFINITE)
                .setAutoPlayAnimation(true)
                .setMessage(message)
                .setDialogDimAmount(0.6f)
                .setMessageColor(Color.BLACK)
                .setMessageTextSize(20)
                .setDialogBackground(Color.TRANSPARENT)
                .setCancelable(true);
        dialog.show();
    }
    
    private void dismissProgressDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    private void displayErrorMessage(String message) {
        Toast.makeText(SetupProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click here
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
