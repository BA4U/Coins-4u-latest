package com.bg4u.coins4u;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.amrdeveloper.lottiedialog.LottieDialog;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {
    private OnBackPressedCallback onBackPressedCallback;
    boolean isChecked = false;
    private static final int RC_SIGN_IN = 9001;
    
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private CollectionReference usersCollection;
    private DatabaseReference usersRef; // Reference to the "Users" node in the Firebase Realtime Database
    private LottieDialog dialog;
    private LottieAnimationView checkBoxAnimation;
    private ActivityResultLauncher<Intent> signInLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    
        initializeFirebase();
        checkIfUserLoggedIn();


        // Get the OnBackPressedDispatcher from the activity
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // Add a callback to the dispatcher
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        };
        dispatcher.addCallback(onBackPressedCallback);
        
        binding.createNewBtn.setOnClickListener(v -> {
            // Show the emailBox and passBox
            binding.emailBox.setVisibility(View.VISIBLE);
            binding.passwordBox.setVisibility(View.VISIBLE);
            binding.forgotPassword.setVisibility(View.VISIBLE);
            binding.loginTextView.setVisibility(View.VISIBLE);
        
            // Show the submitBtn
            binding.submitBtn.setVisibility(View.VISIBLE);
            
        });

        binding.termsAndConditions.setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.termsandcondition, null);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(LoginActivity.this);
            dialogBuilder.setView(dialogView);
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setOnShowListener(dialogInterface -> {
                // Change the dialog box background color
                Objects.requireNonNull(alertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_box_with_edit_text);
            });
            alertDialog.show();
        });
        binding.privacyPolicy.setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.termsandcondition, null);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(LoginActivity.this);
            dialogBuilder.setView(dialogView);
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setOnShowListener(dialogInterface -> {
                // Change the dialog box background color
                Objects.requireNonNull(alertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_box_with_edit_text);
            });
            alertDialog.show();
        });

        // Set the OnClickListener for checkBoxAnimation
        binding.anima.setOnClickListener(view -> handleCheckBoxAnimation());
        binding.submitBtn.setOnClickListener(v -> handleLogin());
    //    binding.createNewBtn.setOnClickListener(v -> startSignupActivity());
        binding.forgotPassword.setOnClickListener(v -> handleForgotPassword());
        binding.googleBtn.setOnClickListener(v -> signInWithGoogle());
        binding.privacyPolicy.setOnClickListener(v -> termsAndConditions());
        
        binding.guestBtn.setOnClickListener(v -> signInAnonymously());
        
        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        handleGoogleSignInResult(data);
                    } else {
                        Toast.makeText(this, getString(R.string.error_google_signin_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void termsAndConditions(){
    }

    // Define a function to handle checkbox animation
    private void handleCheckBoxAnimation() {
        isChecked = !isChecked;
        binding.anima.setSpeed(isChecked ? 2 : -2);
        binding.anima.playAnimation();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        usersCollection = firestore.collection("users");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
    }
    
    private void checkIfUserLoggedIn() {
        if (auth.getCurrentUser() != null) {
            startMainActivity();
            finish();
        }
    }
    
    private void handleLogin() {
        if (!isChecked) {
            Toast.makeText(this, "Please agree to the Privacy Policy", Toast.LENGTH_SHORT).show();
            binding.anima.requestFocus(); // Set focus on the checkbox animation
            return;
        }

        String email = binding.emailBox.getText().toString().trim();
        String password = binding.passwordBox.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailBox.setError(getString(R.string.error_invalid_email));
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.passwordBox.setError(getString(R.string.error_password_required));
            return;
        }
        
        loginWithEmailAndPassword(email, password);
    }
    
    private void loginWithEmailAndPassword(String email, String password) {
        if (!isChecked) {
            Toast.makeText(this, "Please agree to the Privacy Policy", Toast.LENGTH_SHORT).show();
            binding.anima.requestFocus(); // Set focus on the checkbox animation
            return;
        }
        showProgressDialog(getString(R.string.logging_in));
        
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    dismissProgressDialog();
                    if (task.isSuccessful()) {
                        startMainActivity();
                        finish();
                    } else {
                        handleLoginError(task.getException());
                    }
                });
    }
    
    private void handleLoginError(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            binding.emailBox.setError(getString(R.string.error_invalid_email));
            binding.emailBox.requestFocus();
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            binding.passwordBox.setError(getString(R.string.error_invalid_password));
            binding.passwordBox.requestFocus();
        } else {
            showErrorDialog(exception != null ? exception.getMessage() : getString(R.string.error_unknown));
        }
    }
    
    private void startSignupActivity() {
        startActivity(new Intent(LoginActivity.this, SignupActivity.class));
    }
    
    private void handleForgotPassword() {
        String email = binding.emailBox.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_enter_email), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }
        
        sendPasswordResetEmail(email);
    }
    
    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.reset_link_sent), Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    OLD deprecated method
    private void signInWithGoogle() {
        if (!isChecked) {
            Toast.makeText(this, "Please agree to the Privacy Policy", Toast.LENGTH_SHORT).show();
            binding.anima.requestFocus(); // Set focus on the checkbox animation
            return;
        }
        // Check if the user is already authenticated with Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            // User is not authenticated with Google, initiate Google Sign-In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

            Intent signInIntent = googleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
    }

    private void signInAnonymously() {
        if (!isChecked) {
            Toast.makeText(this, "Please agree to the Privacy Policy", Toast.LENGTH_SHORT).show();
            binding.anima.requestFocus(); // Set focus on the checkbox animation
            return;
        }
        showProgressDialog(getString(R.string.logging_in));
        
        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    dismissProgressDialog();
                    if (task.isSuccessful()) {
                        createUserDocumentForAnonymousUser();
                    } else {
                        showErrorDialog(task.getException() != null ? task.getException().getMessage() : getString(R.string.error_unknown));
                    }
                });
    }
    
    private void createUserDocumentForAnonymousUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String name = "Coins 4u " + generateRandomNumber();
            
            // Save user account information in the Firestore firebase
            User user = new User();
            user.setUid(userId);
            user.setName(name);

            // Set other user properties as needed
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String deviceToken = task.getResult();
                            // Save the user's device token under the user's node in the Firebase Realtime Database
                            user.setToken(deviceToken);
                        }
                    });
            
            usersCollection.document(userId).set(user)
                    .addOnCompleteListener(documentTask -> {
                        if (documentTask.isSuccessful()) {
                            // Store user data in Realtime Database
                            storeUserDataInRealtimeDatabase(userId, name, null);
                            startProfileSetupActivity();
                            finish();
                        } else {
                            showErrorDialog(documentTask.getException() != null ? documentTask.getException().getMessage() : getString(R.string.error_unknown));
                        }
                    });
        } else {
            showErrorDialog(getString(R.string.error_retrieve_user_info));
        }
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
    
    private void startProfileSetupActivity() {
        startActivity(new Intent(LoginActivity.this, SetupProfileActivity.class));
    }
    
    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
    }
    
    private void showProgressDialog(String message) {
        // Show progress dialog or use ProgressBar library instead
        dialog = new LottieDialog(this)
                .setAnimation(R.raw.loading_emoji)
                .setAnimationRepeatCount(LottieDialog.INFINITE)
                .setAutoPlayAnimation(true)
                .setMessage(message)
                .setMessageColor(Color.BLACK)
                .setMessageTextSize(20)
                .setDialogDimAmount(0.6f)
                .setDialogBackground(Color.TRANSPARENT)
                .setCancelable(true)
                .setOnShowListener(dialogInterface -> {
                    // Perform actions when dialog is shown
                })
                .setOnDismissListener(dialogInterface -> {
                    // Perform actions when dialog is dismissed
                })
                .setOnCancelListener(dialogInterface -> {
                    // Perform actions when dialog is canceled
                });
        
        dialog.show();
    }
    
    private void dismissProgressDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle(getString(R.string.error_title));
        builder.setMessage(message);
        builder.setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            handleGoogleSignInResult(data);
        }
    }
    
    private void handleGoogleSignInResult(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(account -> {
                    String idToken = account.getIdToken();
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                   
                    auth.signInWithCredential(credential)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = auth.getCurrentUser();
    
                                    if (firebaseUser != null) {
                                        // Check if the user document already exists in Firestore
                                        String userId = firebaseUser.getUid();
                                        usersCollection.document(userId).get()
                                                .addOnCompleteListener(documentTask -> {
                                                    if (documentTask.isSuccessful()) {
                                                        if (documentTask.getResult().exists()) {
                                                            // User document already exists, no need to create a new one
                                                            checkIfUserLoggedIn();
                                                        } else {
                                                            // User document doesn't exist, create a new one
                                                            String displayName = firebaseUser.getDisplayName();
                                        String email = firebaseUser.getEmail();
                                        Uri photoUrl = firebaseUser.getPhotoUrl();
                                        
                                        // Use the extracted profile information as needed
                                        if (displayName != null) {
                                            // Display the user's display name
                                            Toast.makeText(this, "Your Name: " + displayName, Toast.LENGTH_SHORT).show();
                                        }
                                        
                                        if (email != null) {
                                            // Display the user's email address
                                            Toast.makeText(this, "Your email: " + email, Toast.LENGTH_SHORT).show();
                                        }
                                        
                                        createUserDocument(firebaseUser.getUid(), displayName, email, photoUrl);
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(this, getString(R.string.error_google_signin_failed), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_google_signin_failed), Toast.LENGTH_SHORT).show());
    }
    
    private void createUserDocument(String userId, String displayName, String email, Uri photoUrl) {
        showProgressDialog(getString(R.string.creating_user));
        
        // Save user account information in the Firestore firebase
        User user = new User();
        user.setUid(userId);
        user.setName(displayName);
        user.setEmail(email);
        user.setProfile(photoUrl != null ? photoUrl.toString() : null);
        // Set other user properties as needed
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String deviceToken = task.getResult();
                        // Save the user's device token under the user's node in the Firebase Realtime Database
                        user.setToken(deviceToken);
                    }
                });
        
        usersCollection.document(userId).set(user)
                .addOnCompleteListener(documentTask -> {
                    dismissProgressDialog();
                    if (documentTask.isSuccessful()) {
                        storeUserDataInRealtimeDatabase(userId, displayName, email);
                        startMainActivity();
                        finish();
                    } else {
                        showErrorDialog(documentTask.getException() != null ? documentTask.getException().getMessage() : getString(R.string.error_unknown));
                    }
                });
    }
    
    private void storeUserDataInRealtimeDatabase(String userId, String displayName, String email) {
        DatabaseReference userRef = usersRef.child(userId);
        
        // Set the user's display name and email as child nodes under the user's node
        userRef.child("name").setValue(displayName);
        userRef.child("email").setValue(email);
        
        // Set the user's device token as a child node under the user's node
        storeUserDeviceToken(userId);
        
    }
    
    private int generateRandomNumber() {
        return new Random().nextInt(1000) + 1;
    }

}
