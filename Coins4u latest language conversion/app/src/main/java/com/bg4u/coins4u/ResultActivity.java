package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityResultBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
public class ResultActivity extends AppCompatActivity {
    
    private static final String REWARDED_AD_UNIT_ID = "/6499/example/rewarded-video";
    private RewardedAd rewardedAd;
    private final boolean adLoaded = false;
    private boolean isRewardedAdLoaded = false;
    private User currentUser;
    private long points;
    private ActivityResultBinding binding;
    // Initialize the InterstitialAd instance
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityResultBinding.inflate(getLayoutInflater());
    
        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        
        setContentView(binding.getRoot());

        // Find the AdView and load an ad
        adsData();

        if(isRewardedAdLoaded){
            showRewardedAd();
        }

        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        database.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(this::processUserDocument)
                .addOnFailureListener(e -> displayErrorMessage("Failed to fetch user data: " + e.getMessage()));
        
        // display result
        displayResult();
    }

    private void adsData() {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@androidx.annotation.Nullable DocumentSnapshot value, @androidx.annotation.Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(ResultActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore error: " + error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus()) {
                        //  Load rewarded ad
                        loadRewardedAd();
                    } else {
                        Log.e(TAG, "Ads status is false or AdsModel data is null.");
                    }
                } else {
                    Log.e(TAG, "Document does not exist.");
                }
            }
        });
    }

    private void displayResult() {
        int correctAnswers = getIntent().getIntExtra("correct", 0);
        int wrongAnswers = getIntent().getIntExtra("wrong", 0);
        int totalQuestions = getIntent().getIntExtra("total", 0);
        int POINTS = getIntent().getIntExtra("catCoin", 0);
        int NEGATIVE_POINTS = getIntent().getIntExtra("catLostCoin", 0);
       
        
        String quizName = getIntent().getStringExtra("catName");
        // Get the cat image data from the intent
        String catImage = getIntent().getStringExtra("catImage");
        // Set the cat image using Glide (or any other image loading library you're using)
        Glide.with(this)
                .load(catImage)
                .into(binding.catImage);
        
        int TotalNegativePoints = wrongAnswers * NEGATIVE_POINTS;
        int TotalCorrectPoints = correctAnswers * POINTS;
        
        points = TotalCorrectPoints - TotalNegativePoints;
        
        String quizText =  quizName + " " + getString(R.string.quiz) ; // Add the desired text before the quiz name
        binding.quizName.setText(quizText);
        
        binding.correctAns.setText(String.format(Locale.getDefault(), "%d", correctAnswers));
        binding.wrongAns.setText(String.format(Locale.getDefault(), "%d", wrongAnswers));
        binding.earnedCoins.setText(String.valueOf(points));
        binding.correctAnswerPoints.setText(String.valueOf(POINTS));
        binding.wrongAnsPoints.setText(String.valueOf(NEGATIVE_POINTS));
//      binding.quizName.setText(String.valueOf(quizName));
        
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        DocumentReference userRef = database.collection("users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        
        // update coin after showing ads;
        updateCoinsAndPerformActions(points);
    
        // Update the user's coins and set the flag if needed
        userRef.update("coins", FieldValue.increment(points));
        
        // Update correct and wrong answers
        userRef.update("correctAnswers", FieldValue.increment(correctAnswers));
        userRef.update("wrongAnswers", FieldValue.increment(wrongAnswers));
        
        binding.restartBtn.setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, MainActivity.class));
            finish();
        });
    
        binding.shareBtn.setOnClickListener(v -> {
            // Capture the screenshot of the current app screen
            Bitmap screenshot = getScreenshot();
            // Save the screenshot to a temporary file and get the content URI
            Uri screenshotUri = saveScreenshotToFile(screenshot);
            if (screenshotUri != null) {
                // Create a share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                // Grant read permission to the receiving app
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Start the share activity
                startActivity(Intent.createChooser(shareIntent, "Share Screenshot"));
            }
        });
    
    }
    
    private void processUserDocument(DocumentSnapshot document) {
        if (document.exists()) {
            currentUser = document.toObject(User.class);
            if (currentUser != null) {
                displayUserData(currentUser);
                displayPremiumAnimation();
            }
        } else {
            displayErrorMessage("User data not found");
        }
    }
    
    private void displayUserData(User user) {
        if (binding != null && !isDestroyed()) {
            binding.name.setText(user.getName());
            
            Context context = ResultActivity.this;
            if (context != null && !isDestroyed()) {
                if (user.getProfile() != null && !user.getProfile().isEmpty()) {
                    Glide.with(context)
                            .load(user.getProfile())
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    if (binding != null) {
                                        binding.profile.setImageDrawable(resource);
                                    }
                                }
                                
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                } else if (binding != null && !isDestroyed()) {
                    binding.profile.setImageResource(R.drawable.user_icon_default);
                }
            }
        }
    }
    
    private Bitmap getScreenshot() {
        // Get the root view of the current app screen
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        // Capture the screenshot of the root view
        Bitmap screenshot = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);
        return screenshot;
    }
    
    private Uri saveScreenshotToFile(Bitmap screenshot) {
        try {
            // Get the external storage directory
            File externalDir = getExternalFilesDir(null);
            if (externalDir != null) {
                // Create a temporary file in the external storage directory
                File screenshotFile = new File(externalDir, "screenshot.png");
                
                // Compress and save the screenshot to the file
                FileOutputStream outputStream = new FileOutputStream(screenshotFile);
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                
                // Get the content URI using FileProvider
                Context context = getApplicationContext();
                String authority = context.getPackageName() + ".fileprovider";
                return FileProvider.getUriForFile(context, authority, screenshotFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // display Premium Animation based on subscription Feature
    private void displayPremiumAnimation() {
        
        if (currentUser.isSubscription() && (currentUser.isPremiumPlan() || currentUser.isStandardPlan() || currentUser.isBasicPlan())){
            
            binding.premiumIcon.setVisibility(View.VISIBLE);
            binding.premiumIcon.setAnimation(R.raw.premium_gold_icon);
            binding.premiumIcon.playAnimation();
            
            SubscriptionModel subscriptionPlan;
            if (currentUser.isPremiumPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                
                binding.premiumAvatar.setVisibility(View.VISIBLE);
                binding.premiumAvatar.setAnimation(avatarLottieResId);
                binding.premiumAvatar.playAnimation();
                
            } else if (currentUser.isStandardPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                
                binding.premiumAvatar.setVisibility(View.VISIBLE);
                binding.premiumAvatar.setAnimation(avatarLottieResId);
                binding.premiumAvatar.playAnimation();
                
            } else if (currentUser.isBasicPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                
                binding.premiumAvatar.setVisibility(View.VISIBLE);
                binding.premiumAvatar.setAnimation(avatarLottieResId);
                binding.premiumAvatar.playAnimation();
                
            }
        } else {
            binding.premiumIcon.setVisibility(View.GONE);
            binding.premiumAvatar.setVisibility(View.GONE);
        }
    }
    
    // Method to update user's coins and perform actions after earning rewards
    private void updateCoinsAndPerformActions(long points) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        // Assuming the user's UID is obtained correctly
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    
        DocumentReference userRef = database.collection("users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        
        // Update the user's coins and set the flag if needed
        userRef.update("coins", FieldValue.increment(points));
    }
    
    private void displayErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showRewardedAd() {
        if (isRewardedAdLoaded) {
            Activity activityContext = ResultActivity.this; // Make sure to use the correct activity context

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "onAdShowedFullScreenContent");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                    loadRewardedAd(); // Load a new ad for future use
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "onAdDismissedFullScreenContent");
                    // Set the flag to false after ad is dismissed
                    isRewardedAdLoaded = false;
                    loadRewardedAd(); // Load a new ad for future use
                }
            });

            rewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    // Update the user's coin count and perform other actions here
                    // updateCoinsAndPerformActions(points);
                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet or failed to load.");
            // Load a new rewarded ad
            loadRewardedAd();
        }
    }

    private void loadRewardedAd() {
        if (!isRewardedAdLoaded) {
            AdRequest adRequest = new AdRequest.Builder().build();
            String REWARDED_AD_UNIT_ID = getString(R.string.QUIZ_REWARDED_AD_UNIT_ID); // Get rewarded ad

            RewardedAd.load(
                    this,
                    REWARDED_AD_UNIT_ID,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                            isRewardedAdLoaded = true; // Set the flag to true when ad is loaded
                            Log.d(TAG, "onAdLoaded");

                            // Set up the FullScreenContentCallback here
                            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    Log.d(TAG, "Ad dismissed fullscreen content.");
                                    rewardedAd = null;
                                    // Don't load a new ad here; it will be loaded when necessary
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    // Called when ad fails to show.
                                    Log.e(TAG, "Ad failed to show fullscreen content: " + adError.getMessage());
                                    // Don't set rewardedAd to null here
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    // Called when ad is shown.
                                    Log.d(TAG, "Ad showed fullscreen content.");
                                }
                            });

                            // Proceed with showing the ad if needed
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            Log.d(TAG, "Ad failed to load: " + loadAdError.getMessage());
                            // Set the flag to false if ad failed to load
                            isRewardedAdLoaded = false;
                        }
                    });
        }
    }
    
    @Override
    public void onBackPressed() {
        //  show ad
        super.onBackPressed();
    
        Intent intent = new Intent(ResultActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
}
