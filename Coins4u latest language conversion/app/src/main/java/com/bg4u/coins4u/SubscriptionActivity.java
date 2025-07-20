package com.bg4u.coins4u;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class SubscriptionActivity extends AppCompatActivity {
    
    // Constants for plan prices
    private static int BASIC_PRICE_WEEK = 29;
    private static int STANDARD_PRICE_MONTH = 99;
    private static int PREMIUM_PRICE_MONTH = 159;
    
    // Constants for plan types
    private static final int PLAN_BASIC = 0;
    private static final int PLAN_STANDARD = 1;
    private static final int PLAN_PREMIUM = 2;

    // Declare the subscription plan buttons
    private Button btnBasic;
    private Button btnStandard;
    private Button btnPremium;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        // Find the premium plan buttons in the layout
        
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.buy_subscription);

        setupBackPressHandler();

        // Initialize and load the banner ad
    
        // Find the premium plan buttons in the layout
        btnBasic = findViewById(R.id.btnBasic);
        btnStandard = findViewById(R.id.btnStandard);
        btnPremium = findViewById(R.id.btnPremium);
        
        Button btnRedeem50 = findViewById(R.id.btnRedeem);

        LoadSubcriptionAmount();

// Set click listeners for the premium plan buttons
        btnBasic.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            initiatePayment(BASIC_PRICE_WEEK, PLAN_BASIC);
        });
    
        btnStandard.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            initiatePayment(STANDARD_PRICE_MONTH, PLAN_STANDARD);
        });
    
        btnPremium.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            initiatePayment(PREMIUM_PRICE_MONTH, PLAN_PREMIUM);
        });

        // Set click listeners for the premium plan buttons
        // btnRedeem50.setOnClickListener(v -> billingConnector.purchase(this, PRODUCT_ID_ONE_TIME_PURCHASE));

    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void LoadSubcriptionAmount() {
        Task<DocumentSnapshot> docRef = FirebaseFirestore.getInstance().collection("app_updates").document("subscription").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BASIC_PRICE_WEEK = Objects.requireNonNull(documentSnapshot.getLong("basic")).intValue();
                        STANDARD_PRICE_MONTH = Objects.requireNonNull(documentSnapshot.getLong("standard")).intValue();
                        PREMIUM_PRICE_MONTH = Objects.requireNonNull(documentSnapshot.getLong("premium")).intValue();

                        // Update button text with the prices
                        btnBasic.setText(BASIC_PRICE_WEEK + "/week");
                        btnStandard.setText(STANDARD_PRICE_MONTH + "/month");
                        btnPremium.setText(PREMIUM_PRICE_MONTH + "/month");
                    }
                })
                .addOnFailureListener(e -> showToastMessage("Failed to load subscription amounts. Please try again later."));
    }

    private void initiatePayment(double amount, int planType) {
        // Launch PaymentActivity with the selected amount and plan type
        Intent paymentIntent = new Intent(SubscriptionActivity.this, PaymentActivity.class);
        paymentIntent.putExtra("AMOUNT", amount);
        paymentIntent.putExtra("PLAN_TYPE", planType);
        paymentLauncher.launch(paymentIntent);
    }
    
    // Create an ActivityResultLauncher for starting PaymentActivity
    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handlePaymentResult(result.getResultCode(), result.getData())
    );
    
    private void handlePaymentResult(int resultCode, Intent data) {
        // Check if the payment request code matches
        if (resultCode == RESULT_OK && data != null) {
            // Get the payment status from the result intent
            boolean paymentStatus = data.getBooleanExtra("PAYMENT_STATUS", false);
            
            // Update the user's paid status accordingly
            if (paymentStatus) {
                // Save the updated user object (assuming you have a method to save user data)
                int planType = data.getIntExtra("PLAN_TYPE", PLAN_BASIC);
                saveUserToDatabase(planType);
    
                // Show a popup with a Lottie animation
                showCongratsDialog(getString(R.string.payment_successful), getString(R.string.you_purchased) + " " + planType , R.raw.premium_gold_icon);
    
            } else {
                // Payment failed or canceled, keep user as a free user
                // Show a popup with a Lottie animation
                showCongratsDialog(getString(R.string.purchase_failed), "Please try again ...", R.raw.payment_online_animation);
    
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Payment was canceled
            // Show a popup with a Lottie animation
            showCongratsDialog(getString(R.string.purchase_failed), "Please try again ...", R.raw.payment_online_animation);
    
        } else {
            // Payment failed or unknown error
            // Show a popup with a Lottie animation
            showCongratsDialog(getString(R.string.purchase_failed), "Please try again ...", R.raw.payment_online_animation);
    
        }
    }
    
    private void saveUserToDatabase(int planType) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();
    
        if (uid == null) {
            showToastMessage("User not authenticated.");
            return;
        }
        
        DocumentReference userRef = database.collection("users").document(Objects.requireNonNull(uid));
        
        // Declare and initialize the calendar variable
        Calendar calendar = Calendar.getInstance();
        
        // Retrieve the user document from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    // Set the user as premium and activate the corresponding plan
                    user.setSubscription(true);
                    user.setPremiumActivationDate(new Date()); // Set current day as activation date
                    
                    // Set the plan values according to the purchased plan
                    if (planType == 0) { // Basic Plan
                        user.setBasicPlan(true);
                        // Basic plan is for 7 days (1 week)
                        calendar.add(Calendar.DAY_OF_MONTH, 7);
                        // Set the premium deactivation date based on the selected plan type
                        user.setBasicPlanDeactivationDate(calendar.getTime());
                    } else if (planType == 1) { // Standard Plan
                        user.setStandardPlan(true);
                        // Standard plan is for 30 days (1 month)
                        calendar.add(Calendar.DAY_OF_MONTH, 30);
                        // Set the premium deactivation date based on the selected plan type
                        user.setStandardPlanDeactivationDate(calendar.getTime());
                    } else if (planType == 2) { // Premium Plan
                        user.setPremiumPlan(true);
                        // Premium plan is for 30 days (1 month)
                        calendar.add(Calendar.DAY_OF_MONTH, 30);
                        // Set the premium deactivation date based on the selected plan type
                        user.setPremiumPlanDeactivationDate(calendar.getTime());
                    }
                    
                    // Save the updated user object back to Firestore
                    userRef.set(user)
                            .addOnSuccessListener(aVoid -> {
                                // Success, user data saved to the database
                                // Show a popup with a Lottie animation
                                showCongratsDialog(getString(R.string.congratulations), planType + " " + getString(R.string.activated), R.raw.premium_gold_icon);
    
                            })
                            .addOnFailureListener(e -> {
                                // Failed to save user data to the database
                                showToastMessage("Failed to save user data. Please contact support.");
                            });
                }
            }
        }).addOnFailureListener(e -> {
            // Failed to retrieve user document from Firestore
            showToastMessage("Failed to retrieve user data. Please try again later.");
        });
    }
    // Method to deactivate the premium plan and setSubscription to false
    private void deactivatePremiumPlan() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();
        DocumentReference userRef = database.collection("users").document(Objects.requireNonNull(uid));
        
        // Retrieve the user document from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    // Set the user's premium fields to false and subscription to false
                    // Set the user's premium fields to false and subscription to false
                    user.setSubscription(false);
                    user.setBasicPlan(false);
                    user.setStandardPlan(false);
                    user.setPremiumPlan(false);
                    
                    // Save the updated user object back to Firestore
                    userRef.set(user)
                            .addOnSuccessListener(aVoid -> {
                                // Success, user data updated in the database
                                showToastMessage("Your premium plan has been deactivated.");
                            })
                            .addOnFailureListener(e -> {
                                // Failed to update user data in the database
                            });
                }
            }
        }).addOnFailureListener(e -> {
            // Failed to retrieve user document from Firestore
        });
    }
    
    // Method to display toast messages
    private void showToastMessage(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    
    private void showCongratsDialog(String title, String body, int animationRes) {
        if (!isFinishing()) {
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(SubscriptionActivity.this).inflate(R.layout.dialog_box_layout, null);
            
            // Set the title and body text directly on the custom layout
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(body);
            
            // Set the animation directly on the LottieAnimationView
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(animationRes);
            
            // Create an instance of the custom dialog and pass the custom layout as a parameter
            DialogBox dialogBox = new DialogBox(SubscriptionActivity.this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            
            // Set the left button action
            dialogBox.setLeftButton("Home", v -> {
                // Handle left button click
                dialogBox.dismiss();
                startActivity(new Intent(SubscriptionActivity.this, MainActivity.class));
            });
            
            // Set the right button action
            dialogBox.setRightButton("Okay", v -> {
                // Handle right button click
                dialogBox.dismiss();
            });
            
            // Show the dialog
            dialogBox.show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
