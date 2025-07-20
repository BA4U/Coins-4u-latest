package com.bg4u.coins4u;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class AboutMeActivity extends AppCompatActivity {
    private final String YOUR_DYNAMIC_LINK_DOMAIN = "https://coins4u.page.link/Coins4u";
    private LottieAnimationView donateBtn;
    private String aboutMe;
    private TextView aboutMeTitle, aboutMeBody, ratingText;
    private FirebaseFirestore firestore;
    
    private boolean appRated = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_me);
    
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("About me");
    
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(); // This is current player uid
        firestore = FirebaseFirestore.getInstance();
    
        // Initialize UI elements
        ImageView profileImage = findViewById(R.id.profileImage);
        aboutMeTitle = findViewById(R.id.aboutMeTitle);
        aboutMeBody= findViewById(R.id.aboutMeBody);
        ratingText = findViewById(R.id.rateText);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        Button shareAppBtn = findViewById(R.id.appShareBtn);
        donateBtn = findViewById(R.id.donateBtn);
        
        loadAboutMe();
        donationPayment();
        
        // Set profile image, name, and about me text
        profileImage.setImageResource(R.drawable.coinlogowithtext_4u); // Replace with your image resource
        aboutMeTitle.setText("Coins 4u : Play and Learn"); // Replace with your name
        aboutMeBody.setText(String.valueOf(R.id.aboutMeBody)); // Replace with your description
    
        if (appRated) {
            // User has already rated and reviewed the app, hide the button or show a thank you message
            ratingBar.setVisibility(View.GONE);
            ratingText.setVisibility(View.GONE);
        } else {
            ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
                if (fromUser) {
                    // Open the Play Store rating and review dialog
                    Uri uri = Uri.parse("market://details?id=" + getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        // If the Play Store app is not available, open the Play Store website
                        Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
                        Intent goToWeb = new Intent(Intent.ACTION_VIEW, webUri);
                        startActivity(goToWeb);
                    }
                }
            });
        }
    
        shareAppBtn.setOnClickListener(v -> {
            // Generate a referral link using Firebase Dynamic Links
            // Replace "YOUR_DYNAMIC_LINK_DOMAIN" with your actual dynamic link domain
            String dynamicLinkDomain = YOUR_DYNAMIC_LINK_DOMAIN;
            String appPackageName = getPackageName();
        
            Uri dynamicLink = Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName);
            String referrerUid = currentUserUid; // Replace with the actual user ID
        
            String link = "https://" + dynamicLinkDomain + "/?link=" + dynamicLink.toString() + "&referrerUid=" + referrerUid;
        
            // Share the referral link through an Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Download this app and earn coins");
            shareIntent.putExtra(Intent.EXTRA_TEXT, link);
            startActivity(Intent.createChooser(shareIntent, "Share App"));
        });
        
        
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Handle back button press
            }
        };
        
        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
        
        
    }
    
    private void loadAboutMe() {
        firestore.collection("app_updates").document("update_info").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        aboutMe = documentSnapshot.getString("about_me");
                        aboutMeBody.setText(aboutMe);
                    }
                });
    }
    
    private void donationPayment() {
        donateBtn.setOnClickListener(view -> {
            // Launch PaymentActivity with a custom amount
            
            // Open the link in a web browser
            String url = "https://rzp.io/l/c7mvoGnBN0";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }

    
//  private void donationPayment() {
//        donateBtn.setOnClickListener(view -> {
//            // Launch PaymentActivity with a custom amount
//            double customAmount = 01.00; // Change this to your desired amount
//            Intent paymentIntent = new Intent(AboutMeActivity.this, PaymentActivity.class);
//            paymentIntent.putExtra("AMOUNT", customAmount);
//            paymentLauncher.launch(paymentIntent);
//        });
//    }
    
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
                // saveUserToDatabase();
                // Show a success message to the user
                showToastMessage(getString(R.string.payment_successful_you_are_now_a_premium_user));
               
            } else {
                // Payment failed or canceled, keep user as free user
                showToastMessage("Payment canceled or failed. Try again...");
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Payment was canceled
            showToastMessage("Payment canceled by the user. Try again...");
        } else {
            // Payment failed or unknown error
            showToastMessage("Payment failed. Please try again...");
        }
    }
    
    // Method to save the updated user data (assuming you have a method to save user data)
    
    // Method to display toast messages
    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click here
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
