package com.bg4u.coins4u;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private FirebaseAuth auth;
    private CollectionReference usersCollection;
    private DatabaseReference usersRef; // Reference to the "Users" node in the Firebase Realtime Database
    private FirebaseFirestore database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    
        initializeFirebase();
        
        binding.createNewBtn.setOnClickListener(v -> {
            String email = binding.emailBox.getText().toString().trim();
            String password = binding.passwordBox.getText().toString().trim();
            String name = binding.nameBox.getText().toString().trim();
        //    String referCode = binding.referBox.getText().toString().trim();
            
            if (TextUtils.isEmpty(name) || name.length() < 3 || name.length() > 18) {
                binding.nameBox.setError("Please enter a valid name (3-18 characters).");
                binding.nameBox.requestFocus();
                return;
            }
            
            if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailBox.setError("Please enter a valid email address.");
                binding.emailBox.requestFocus();
                return;
            }
            
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                binding.passwordBox.setError("Please enter a password with at least 6 characters.");
                binding.passwordBox.requestFocus();
                return;
            }
            
        //    final User user = new User(name, email, password, referCode);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Email Verification");
            builder.setMessage("A verification link will be sent to your email. Do you want to continue?");
            
            builder.setPositiveButton("Yes", (dialog, which) -> {
                dialog.dismiss();
            });
            
            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
        
        binding.loginBtn.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
    }
    
    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        usersCollection = firestore.collection("users");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
    }
    
    private void createUserWithEmailAndPassword(String email, String password, User user) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if (firebaseUser != null) {
                    sendEmailVerification(firebaseUser, user);
                } else {
                    handleUserCreationFailure(new Exception("Failed to create user. Please try again."));
                }
            } else {
                handleUserCreationFailure(task.getException());
            }
        });
    }
    
    private void sendEmailVerification(FirebaseUser firebaseUser, User user) {
        firebaseUser.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                handleEmailVerificationSuccess(user);
            } else {
                handleEmailVerificationFailure();
            }
        });
    }
    
    private void handleEmailVerificationSuccess(User user) {
        Toast.makeText(SignupActivity.this, "Verification email sent. Please check your email.", Toast.LENGTH_SHORT).show();
        showEmailVerificationDialog(user);
    }
    
    private void handleEmailVerificationFailure() {
        Toast.makeText(SignupActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show();
    }
    
    private void showEmailVerificationDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Email Verification");
        builder.setMessage("A verification link has been sent to your email. Please click the link to verify your account.");
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
           // saveUserToFirestore(user);
        });
        
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    
    private void createUserDocument() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String name = "Coins 4u " ;
            
            // Save user account information in the Firestore firebase
            User user = new User();
            user.setUid(userId);
            user.setName(name);
            // Set other user properties as needed
            
            usersCollection.document(userId).set(user)
                    .addOnCompleteListener(documentTask -> {
                        if (documentTask.isSuccessful()) {
                            // Store user data in Realtime Database
                            storeUserDataInRealtimeDatabase(userId, name, null);
                            startMainActivity();
                            finish();
                        } else {
                            showErrorDialog(documentTask.getException() != null ? documentTask.getException().getMessage() : getString(R.string.error_unknown));
                        }
                    });
        } else {
            showErrorDialog(getString(R.string.error_retrieve_user_info));
        }
    }
    
    private void storeUserDataInRealtimeDatabase(String userId, String displayName, String email) {
        DatabaseReference userRef = usersRef.child(userId);
        
        // Set the user's display name and email as child nodes under the user's node
        userRef.child("name").setValue(displayName);
        userRef.child("email").setValue(email);
        
        // Set the user's device token as a child node under the user's node
        storeUserDeviceToken(userId);
        
    }
    
    private void storeUserDeviceToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String deviceToken = task.getResult();
                        // Save the user's device token under the user's node in the Firebase Realtime Database
                        usersRef.child(userId).child("device_token").setValue(deviceToken);
                    }
                });
    }
    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(message);
        builder.setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
//    private void saveUserToFirestore(User user) {
//        FirebaseUser firebaseUser = auth.getCurrentUser();
//        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
//            String uid = firebaseUser.getUid(); // Get the user's unique ID (UID)
//
//            // Generate referral code for the user
//            String referralCode = generateReferralCode(user.getName());
//            user.setReferCode(referralCode);
//
//            database.collection("users")
//                    .document(uid) // Use the UID as the document ID
//                    .set(user) // Save the user object to the Firestore document
//                    .addOnSuccessListener(aVoid -> {
//                        // Once the account is created successfully, call referCodeVerification
//                        referCodeVerification(user);
//                    })
//                    .addOnFailureListener(this::handleUserCreationFailure);
//        } else {
//            Toast.makeText(SignupActivity.this, "Please verify your email first.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void referCodeVerification(User user) {
//        String referCode = user.getReferCode();
//        if (TextUtils.isEmpty(referCode)) {
//            // No referral code provided, proceed with account creation
//            saveUserToFirestore(user);
//            return;
//        }
//
//        database.collection("users")
//                .whereEqualTo("referCode", referCode)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        QuerySnapshot querySnapshot = task.getResult();
//                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
//                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
//                            User referrer = document.toObject(User.class);
//                            if (referrer != null) {
//                                referrer.setCoins(referrer.getCoins() + 500);
//                                user.setCoins(user.getCoins() + 500);
//
//                                database.collection("users").document(referrer.getUid()).set(referrer);
//                                database.collection("users").document(user.getUid()).set(user);
//
//                                startMainActivity();
//                            } else {
//                                handleUserCreationFailure(new Exception("Invalid referral code"));
//                            }
//                        } else {
//                            handleUserCreationFailure(new Exception("Invalid referral code"));
//                        }
//                    } else {
//                        handleUserCreationFailure(task.getException());
//                    }
//                });
//    }
//
    private void handleUserCreationFailure(Exception exception) {
        String errorMessage = Objects.requireNonNull(exception).getLocalizedMessage();
        
        if (errorMessage != null && errorMessage.contains("email address is already in use")) {
            // Email address is already registered but not verified
            // deleteUnverifiedUser();
            
            Toast.makeText(SignupActivity.this, "Email address already registered. Please try again.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
    
//    private void deleteUnverifiedUser() {
//        FirebaseUser firebaseUser = auth.getCurrentUser();
//        if (firebaseUser != null) {
//            firebaseUser.delete().addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    Toast.makeText(SignupActivity.this, "Previous unverified account deleted. Please register again.", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(SignupActivity.this, "Failed to delete previous unverified account.", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    }
    
    private void startMainActivity() {
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }
    
    private String generateReferralCode(String name) {
        String base = name.substring(0, Math.min(name.length(), 2)) + "4u";
        String code = base + new Random().nextInt(100);
        return code.substring(0, Math.min(code.length(), 6));
    }
}
