package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.databinding.ActivitySpinnerBinding;
import com.bg4u.coins4u.SpinWheel.model.LuckyItem;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SpinnerActivity extends AppCompatActivity {
    private OnBackPressedDispatcher onBackPressedDispatcher;
    ActivitySpinnerBinding binding;
    private RewardedAd rewardedAd;
    private boolean isRewardedAdLoaded = false;
    private long coinsWon = 0;
    private boolean isPremiumUser = false;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String SPIN_COUNT_KEY = "spinCount";
    private int SPIN_LIMIT_MAX = 1;  // number of free spin
    private static final long HOUR_IN_MILLIS = 12 * 60 * 60 * 1000;  // free spin after every day
    private int spinCount = 0;
    private long spinResetTime = 0;
    private String status ;
    private String title ;
    private boolean isSpinning = false; // To track if the wheel is spinning
    private Animation zoomAnimation; // Animation for zoom in and out
    private final Handler animationHandler = new Handler();
    private Runnable animationRunnable;
    private MediaPlayer coinSoundEffect;
    private MediaPlayer openActivitySoundEffect;
    private MediaPlayer spinWheelSoundEffect;
    
    // Declare 'data' as a class level variable
    List<LuckyItem> data;
    private User user;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        
        binding = ActivitySpinnerBinding.inflate(getLayoutInflater());
    
        setContentView(binding.getRoot());
    
        ImageButton backBtn = binding.backBtn;
        backBtn.setOnClickListener(view -> {
            //  Move back to the previous activity
            // Get the OnBackPressedDispatcher from the activity
            OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

            // Add a callback to the dispatcher
            dispatcher.addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Handle the back button click here
                    finish();
                }
            });
            // Trigger the callback
            dispatcher.onBackPressed();
        });
    
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Find the AdView and load an ad
        adsData();

        // Initialize 'user' object
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        database.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        user = document.toObject(User.class);
                        if (user != null) {
                            isPremiumUser = user.isSubscription();
                            subscriptionFeature();
                        }
                    }
                });
    
        // Load the zoom in and out animation from the XML file
        zoomAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);
    
        if (!canSpin()) {
            // check spin available or not
            showSpinDialog();
        }
    
        // Initialize the sound effects
        openActivitySoundEffect = MediaPlayer.create(this, R.raw.open_activity_sound_effect);
        spinWheelSoundEffect = MediaPlayer.create(this, R.raw.spin_wheel_sound_effect);
        coinSoundEffect = MediaPlayer.create(this, R.raw.coin_sound_effect);
    
        // Play the open activity sound effect
        openActivitySoundEffect.setLooping(true);
        openActivitySoundEffect.start();
        
        // Load user data from SharedPreferences
        loadUserData();
        
        data = new ArrayList<>();
        
        // Add lucky items
        LuckyItem luckyItem1 = new LuckyItem();
        luckyItem1.topText = "5";
        luckyItem1.secondaryText = "Coins";
        luckyItem1.textColor = Color.parseColor("#212121");
        luckyItem1.color = Color.parseColor("#eceff1");
        data.add(luckyItem1);
        
        LuckyItem luckyItem2 = new LuckyItem();
        luckyItem2.topText = "10";
        luckyItem2.secondaryText = "Coins";
        luckyItem2.color = Color.parseColor("#008bff");
        luckyItem2.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem2);
        
        LuckyItem luckyItem3 = new LuckyItem();
        luckyItem3.topText = "15";
        luckyItem3.secondaryText = "Coins";
        luckyItem3.textColor = Color.parseColor("#212121");
        luckyItem3.color = Color.parseColor("#eceff1");
        data.add(luckyItem3);
        
        LuckyItem luckyItem4 = new LuckyItem();
        luckyItem4.topText = "20";
        luckyItem4.secondaryText = "Coins";
        luckyItem4.color = Color.parseColor("#7f00d9");
        luckyItem4.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem4);
        
        LuckyItem luckyItem5 = new LuckyItem();
        luckyItem5.topText = "-25";
        luckyItem5.secondaryText = "Coins";
        luckyItem5.textColor = Color.parseColor("#212121");
        luckyItem5.color = Color.parseColor("#eceff1");
        data.add(luckyItem5);
        
        LuckyItem luckyItem6 = new LuckyItem();
        luckyItem6.topText = "30";
        luckyItem6.secondaryText = "Coins";
        luckyItem6.color = Color.parseColor("#dc0000");
        luckyItem6.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem6);
        
        LuckyItem luckyItem7 = new LuckyItem();
        luckyItem7.topText = "35";
        luckyItem7.secondaryText = "Coins";
        luckyItem7.textColor = Color.parseColor("#212121");
        luckyItem7.color = Color.parseColor("#eceff1");
        data.add(luckyItem7);
        
        LuckyItem luckyItem8 = new LuckyItem();
        luckyItem8.topText = "50";
        luckyItem8.secondaryText = "Coins";
        luckyItem8.color = Color.parseColor("#00cf00");
        luckyItem8.textColor = Color.parseColor("#ffffff");
        data.add(luckyItem8);
        
        binding.wheelview.setData(data);
        binding.wheelview.setRound(5);
    
        binding.spinBtn.setOnClickListener(v -> spinButtonClicked());
    
        // Start the continuous zoom animation when the activity starts
        startContinuousZoomAnimation();
        
        binding.spinBtn.setEnabled(true);
        binding.wheelview.setLuckyRoundItemSelectedListener(this::updateCash);
        
    }

    private void adsData() {
        DocumentReference docRef = database.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(SpinnerActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Server error: " + error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus()) {
                        Log.d(TAG, "Ads data fetched: " + data);

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

    private void subscriptionFeature() {
        
        if (user.isSubscription() && (user.isPremiumPlan() || user.isStandardPlan() || user.isBasicPlan())){
            
            SubscriptionModel subscriptionPlan;
            if (user.isPremiumPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
                SPIN_LIMIT_MAX = subscriptionPlan.getSpinMaxLimit();
                
            } else if (user.isStandardPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
                SPIN_LIMIT_MAX = subscriptionPlan.getSpinMaxLimit();
                
            } else if (user.isBasicPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
                SPIN_LIMIT_MAX = subscriptionPlan.getSpinMaxLimit();
            } else {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("default");
                SPIN_LIMIT_MAX = subscriptionPlan.getSpinMaxLimit();
            }
        }
    }
    
    private void startContinuousZoomAnimation() {
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSpinning) {
                    // If the button is not clicked, start the zoom animation
                    binding.spinBtn.startAnimation(zoomAnimation);
                    binding.arrow.startAnimation(zoomAnimation);
                } else {
                    // If the button is clicked, stop the zoom animation
                    binding.spinBtn.clearAnimation();
                    binding.arrow.clearAnimation();
                }
                
                // Repeat the animation after a delay
                animationHandler.postDelayed(this, 1000); // Adjust the delay as needed
            }
        };
        
        // Start the initial animation
        animationHandler.post(animationRunnable);
    }
    
    private void spinButtonClicked() {
        // Check if the user can spin
        if (canSpin()) {
            // Play the spin wheel sound effect
            if (!spinWheelSoundEffect.isPlaying()) {
                spinWheelSoundEffect.start();
            }
            // Stop the continuous zoom animation
            isSpinning = true;
            // Start the zoom in and out animation on the wheel
            binding.wheelview.startAnimation(zoomAnimation);
            // Increment spin count
            incrementSpinCount();
            // User can spin
            Random r = new Random();
            int randomNumber = r.nextInt(data.size());
            binding.wheelview.startLuckyWheelWithTargetIndex(randomNumber);
        } else {
            showSpinDialog();
            // Spin limit reached
            // Toast.makeText(this, "Watch ads to get more spin.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        spinCount = prefs.getInt(SPIN_COUNT_KEY, 0);
        spinResetTime = prefs.getLong("spinResetTime", 0);
    }
    
    private void saveUserData() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("isPremiumUser", isPremiumUser);
        editor.putInt(SPIN_COUNT_KEY, spinCount);
        editor.putLong("spinResetTime", spinResetTime);
        editor.apply();
    }
    
    private void incrementSpinCount() {
        if (spinCount < SPIN_LIMIT_MAX) {
            spinCount++;
            saveUserData();
        }
    }
    private void decrementSpinCount() {
        if (spinCount > 0) {
            spinCount--;
            saveUserData();
        }
    }
    
    private boolean canSpin() {
        // Check if spin count is within the limit
        if (spinCount < SPIN_LIMIT_MAX) {
            return true;
        } else {
            // Check if the spin reset time has passed
            long currentTime = System.currentTimeMillis();
            if (currentTime >= spinResetTime) {
                resetSpinCount();
                return true;
            }
        }
        return false;
    }
    
    private void resetSpinCount() {
        binding.spinBtn.setEnabled(true);
        spinCount = 0;
        spinResetTime = System.currentTimeMillis() + HOUR_IN_MILLIS; // Reset after 30 minutes
        saveUserData();
    }
    
    void updateCash(int index) {
           // Update coins based on the selected item
    switch (index) {
        case 0:
            coinsWon = 5;
            break;
        case 1:
            coinsWon = 10;
            break;
        case 2:
            coinsWon = 15;
            break;
        case 3:
            coinsWon = 20;
            break;
        case 4:
            coinsWon = -25;
            break;
        case 5:
            coinsWon = 30;
            break;
        case 6:
            coinsWon = 35;
            break;
        default:
            coinsWon = 50;
            break;
    }
        
        if (coinsWon > 20) {
            // Show rewarded ad if it's loaded
            showRewardedAd();
        }
        if (coinsWon < 0) {
            // win or lost text
            status = getString(R.string.lost);
            title = getString(R.string.better_luck_next_time);
        }
        
        if (coinsWon >= 0) {
            // win or lost text
            status = getString(R.string.won);
            title = getString(R.string.congratulations);
        }
        
        // Show a toast message with the number of coins won
            String message = getString(R.string.you) +" "+ status + " " + Math.abs(coinsWon) + " " + getString(R.string.coins);
        
            // Show congrats dialog
            showCongratsDialog( title, message, R.raw.withdraw_coins_to_wallet);
        
            String uid = FirebaseAuth.getInstance().getUid();
            DocumentReference userRef = database.collection("users").document(Objects.requireNonNull(uid));
            // Update coins and set the flag
            userRef.update("coins", FieldValue.increment(coinsWon));
           
                userRef.update("coins", FieldValue.increment(coinsWon))
                    .addOnSuccessListener(aVoid -> {
                            // Coins added successfully toast message
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    });
        }
    
    private void showCongratsDialog(String title, String body, int animationRes) {
        if (!isFinishing()) {
           String Body = (body + "\n" + getString(R.string.you_have) +" "+ (SPIN_LIMIT_MAX - spinCount) +" "+ getString(R.string.spin_left));
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(SpinnerActivity.this).inflate(R.layout.dialog_box_layout,null);
            
            // Set the title and body text directly on the custom layout
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(Body);
            
            // Set the animation directly on the LottieAnimationView
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(animationRes);
            
            // Create an instance of the custom dialog and pass the custom layout as a parameter
            DialogBox dialogBox = new DialogBox(SpinnerActivity.this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            
            // do not cancel dialog box
            dialogBox.setCanceledOnTouchOutside(false);
            
            // Set the left button action
            dialogBox.setLeftButton(getString(R.string.exit), v -> {
                ExtiMethod();
                // Watch ads to earn additional spins
                // showRewardedAd();
                // Increment spin count
                // decrementSpinCount();
            });
            
            // Set the right button action
            dialogBox.setRightButton(getString(R.string.spin_again), v -> {
    
                // Handle right button click
                dialogBox.dismiss();
                spinButtonClicked();
            });
            
            // Show the dialog
            dialogBox.show();
        }
    }
    
    private void ExtiMethod() {
        // Create an intent to navigate to the ResultActivity
        Intent intent = new Intent(SpinnerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showSpinDialog() {
        String title = getString(R.string.get_premium);
        String body = (getString(R.string.you_have) + (SPIN_LIMIT_MAX - spinCount) + " " + getString(R.string.spin_left));
        
            //  show the dialog
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(SpinnerActivity.this).inflate(R.layout.dialog_box_layout,null);
        
            // Set the title and body text directly on the custom layout
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(body);
        
            // Set the animation directly on the LottieAnimationView
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(R.raw.heart_zoom_in_out);
        
            // Create an instance of the custom dialog and pass the custom layout as a parameter
            DialogBox dialogBox = new DialogBox(SpinnerActivity.this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;

            if(isRewardedAdLoaded){
                dialogBox.setLeftButton(getString(R.string.watch_Ads), v -> {
                    // Handle left button click
                    if (spinWheelSoundEffect != null) {
                        spinWheelSoundEffect.release();
                    }
                    showRewardedAd();
                    dialogBox.dismiss();
                    // Dismiss the dialog
                });
            }else {
                // Set the left button action
                dialogBox.setLeftButton(getString(R.string.quit), v -> {
                    // Handle left button click
                    dialogBox.dismiss();
                    finish();
                    // Dismiss the dialog
                });
            }

            // Set the right button action
            dialogBox.setRightButton(getString(R.string.get_premium), v -> {
                // Handle right button click
                dialogBox.dismiss();
                // Show the rewarded ad before spinning
                if (spinWheelSoundEffect != null) {
                    spinWheelSoundEffect.release();
                }
                startSubscriptionActivity();
            });
        
            // Show the dialog
            dialogBox.show();
    }
    
    private void startSubscriptionActivity() {
        Intent intent = new Intent(this, SubscriptionActivity.class);
        startActivity(intent);
    }
    
    private void startSpinning() {
        // Increment spin count
        incrementSpinCount();
        
        // User can spin
        Random r = new Random();
        int randomNumber = r.nextInt(data.size());
        binding.wheelview.startLuckyWheelWithTargetIndex(randomNumber);
    }

    private void showRewardedAd() {
        if (isRewardedAdLoaded) {
            Activity activityContext = SpinnerActivity.this; // Make sure to use the correct activity context

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
                    // Get the reward amount
                    int rewardAmount = rewardItem.getAmount();
                    decrementSpinCount(); // user can spin more
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
            String REWARDED_AD_UNIT_ID = getString(R.string.SPINNER_REWARDED_AD_UNIT_ID); // Get rewarded ad

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
                                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
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
                            // You can add your error handling logic here
                            // Toast.makeText(SpinnerActivity.this, "Try again...", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer instances
        if (openActivitySoundEffect != null) {
            openActivitySoundEffect.release();
        }
        if (spinWheelSoundEffect != null) {
            spinWheelSoundEffect.release();
        }
        if (coinSoundEffect != null) {
            coinSoundEffect.release();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        saveUserData();
    }

}
