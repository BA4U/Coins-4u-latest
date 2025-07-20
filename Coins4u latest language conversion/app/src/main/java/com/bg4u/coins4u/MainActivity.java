package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.airbnb.lottie.LottieAnimationView;
import com.amrdeveloper.lottiedialog.LottieDialog;
import com.bg4u.coins4u.databinding.ActivityMainBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.EventListener;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private RewardedAd rewardedAd;
    private boolean isRewardedAdLoaded = false;
    String currentLanguage = "en", currentLang;
    private int dailyCoins = 25;
    private LottieDialog dialog;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private DatabaseReference databaseReference;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean isOnline = false;
    private Menu menu; // Store the inflated menu as a member variable
    private ViewPager2 viewPager;
    private MyPagerAdapter pagerAdapter;
    // Declare dialogBox as a class-level variable
    private DialogBox dialogBox;
    // Define a constant for the payment request code
    private static final String TAG = "MainActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        
        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

        // Show the status bar
        // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(binding.getRoot());

        // Handle the back button press here
        // Check if the back button has been pressed twice within 2 seconds
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button press here
                // Check if the back button has been pressed twice within 2 seconds
                if (doubleBackToExitPressedOnce) {
                    // Exit the app
                    finishAffinity();
                } else {
                    // Show a toast message and set the flag to true
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    doubleBackToExitPressedOnce = true;

                    // Reset the flag after 2 seconds
                    new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 500);
                }
            }
        };

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback);
        
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
    
        // Check for app updates
        checkAppUpdates();
        adsData();
        
        // Check user is paid or not
        VerifyUserExistence();
        
        currentLanguage = getIntent().getStringExtra(currentLang);
        
        // Initialize mobile ads
        initializeAds();
        
        // Check internet connectivity on activity start
        checkInternetConnectivity();
        
        // Check if already subscribed to "notification" topic
        initializeFirebaseMessaging();
        
        setSupportActionBar(binding.toolbar);
        
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
    
        // Set up a listener to update the toolbar title when the page is changed
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                String pageTitle = pagerAdapter.getPageTitle(position);
                updateToolbarTitle(pageTitle);
            }
        });
        
        // Set up bottom navigation
        binding.bottomBar.setOnItemSelectedListener(i -> {
            viewPager.setCurrentItem(i, true);
            return false;
        });
    
        // Set up a listener to update the selected item in the bottom bar when the page is changed
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.bottomBar.setItemActiveIndex(position);
            }
        });
    }

    private void initializeAds() {
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {}
        });
    }
    
    private void initializeFirebaseMessaging() {
        // Check if already subscribed to "notification" topic
        FirebaseMessaging.getInstance().subscribeToTopic("notification")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Successfully subscribed
                    } else {
                        // Failed to subscribe
                    }
                });
    }
    
    private void VerifyUserExistence() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String currentUserID = null; // Default value
        if (currentUser != null) {
           currentUserID = currentUser.getUid();
            // Rest of the code using currentUserID
        }
    }

    private void adsData() {
        firestore.collection("app_updates").document("ads")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (value != null && value.exists()) {
                            AdsModel data = value.toObject(AdsModel.class);
                            if (data != null && data.adsStatus) {
                                // Create an ad request using the ad unit fetched from Firestore
                                AdRequest adRequest = new AdRequest.Builder().build();
                                // adView.setAdUnitId(data.banner_g);  // Assuming banner_g is the ad unit ID
                                // adView.loadAd(adRequest);
                            }
                        }
                    }
                });
    }

    private void updateUserStatus(String state) {
        DateFormat currentDate = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat currentTime = DateFormat.getTimeInstance(DateFormat.SHORT);
        String saveCurrentDate = currentDate.format(new Date());
        String saveCurrentTime = currentTime.format(new Date());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("time", saveCurrentTime);
        hashMap.put("date", saveCurrentDate);
        hashMap.put("state", state);

        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        new Thread(() -> {
            firestore.collection("users").document(currentUserId).update("userState", state);
            databaseReference.child("Users").child(currentUserId).child("userState").updateChildren(hashMap);
        }).start();
    }
    
    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            sendUserToLoginActivity();
        } else {
            new Thread(() -> updateUserStatus("online")).start();
            awardCoinsForPremiumPlan(); // Assuming this method is for premium plan logic

            // Save the updated user object (assuming you have a method to save user data)
            saveUserToDatabase();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            new Thread(() -> updateUserStatus("online")).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            new Thread(() -> updateUserStatus("offline")).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            new Thread(() -> updateUserStatus("offline")).start();
        }
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.menu == null) { // Check if the menu is already inflated
            getMenuInflater().inflate(R.menu.home_menu, menu);
            this.menu = menu; // Store the inflated menu
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, AboutMeActivity.class));
            Toast.makeText(this, "This app is made by Anuj", Toast.LENGTH_SHORT).show();
        } else if (item.getItemId() == R.id.subscription) {
            startActivity(new Intent(this, SubscriptionActivity.class));
            Toast.makeText(this, getString(R.string.buy_subscription), Toast.LENGTH_SHORT).show();
        }
        else if (item.getItemId() == R.id.chat) {
            // showInterstitialAd();
            startActivity(new Intent(this, com.bg4u.coins4u.chat.MainActivity.class));
        }
//        else if (item.getItemId() == R.id.shop) {
//            startActivity(new Intent(this, com.bg4u.coins4u.ShopActivity.class));
//        }
        else {
            return super.onOptionsItemSelected(item);
        }
     //   startActivity(intent);
        return true;
    }
    
    private void checkInternetConnectivity() {
        new Thread(() -> {
            isOnline = isNetworkAvailable();
            if (!isOnline) {
                runOnUiThread(this::showNoInternetDialog);
            }
        }).start();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    private void showNoInternetDialog() {
        dialog = new LottieDialog(this)
                .setAnimation(R.raw.offline_animation)
                .setAnimationRepeatCount(LottieDialog.INFINITE)
                .setAutoPlayAnimation(true)
                .setMessage(getString(R.string.no_internet_connection_available))
                .setMessageColor(Color.BLACK)
                .setMessageTextSize(14)
                .setDialogDimAmount(0.6f)
                .setDialogBackground(Color.TRANSPARENT)
                .setCancelable(false)
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
    
    
    // Method to update the toolbar title dynamically
    public void updateToolbarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }
    private class MyPagerAdapter extends FragmentStateAdapter {
        private static final int NUM_PAGES = 4; // Number of pages in the ViewPager
        
        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, getLifecycle());
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return the fragment for each page based on the position
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new LeaderboardsFragment();
                case 2:
                    return new WalletFragmentNew();
                case 3:
                    return new ProfileFragment();
                default:
                    return null;
            }
        }
    
        public String getPageTitle(int position) {
            // Return the title for each page based on the position
            switch (position) {
                case 0:
                    return getString(R.string.home);
                case 1:
                    return getString(R.string.Leaderboard);
                case 2:
                    return getString(R.string.wallet);
                case 3:
                    return getString(R.string.my_profile);
                default:
                    return "";
            }
        }
        
        @Override
        public int getItemCount() {
            // Return the total number of pages
            return NUM_PAGES;
        }
    }
    
    private void checkAppUpdates() {
        firestore.collection("app_updates").document("update_info").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int latestVersionCode = Objects.requireNonNull(documentSnapshot.getLong("version_code")).intValue();
                        String updateTitle = documentSnapshot.getString("updateTitle");
                        String updateMessage = documentSnapshot.getString("updateMessage");
                        String updateUrl = documentSnapshot.getString("update_url");
                        Boolean cancelable = documentSnapshot.getBoolean("cancelable");
                        boolean isCancelable = cancelable != null ? cancelable : true;

                        dailyCoins = Objects.requireNonNull(documentSnapshot.getLong("dailyCoins")).intValue();

                        int currentVersionCode = getVersionCode();
                        if (currentVersionCode < latestVersionCode) {
                            showUpdateDialog(updateTitle, updateMessage, updateUrl, isCancelable);
                        }

                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking app updates", e));
    }

    private int getVersionCode() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1; // Return a default value in case of an error
    }
    private void showUpdateDialog(String updateTitle, String updateMessage, String updateUrl, boolean isCancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(updateTitle)
                .setIcon(R.drawable.updated)
                .setMessage(updateMessage)
                .setPositiveButton(getString(R.string.update_now), (dialog, which) -> openUpdateUrl(updateUrl));
        
        if (isCancelable) {
            builder.setNegativeButton((getString(R.string.cancel)), (dialog, which) -> {
                // Handle cancel action if needed
            });
        } else {
            builder.setCancelable(false);
        }
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void openUpdateUrl(String updateUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
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
            saveUserToDatabase();
             // Show a success message to the user
            showToastMessage(getString(R.string.payment_successful_you_are_now_a_premium_user));
             // Dismiss the dialog after successful payment
            dialogBox.dismiss();
        } else {
            // Payment failed or canceled, keep user as free user
            showToastMessage("Payment canceled or failed. You remain a free user.");
        }
    } else if (resultCode == RESULT_CANCELED) {
        // Payment was canceled
        showToastMessage("Payment canceled by the user. You remain a free user.");
    } else {
        // Payment failed or unknown error
        showToastMessage("Payment failed. Please try again. You remain a free user.");
    }
}

 // Method to display toast messages
private void showToastMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
}
    
    // Method to save the updated user data (assuming you have a method to save user data)
    private void saveUserToDatabase() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid(); // Early exit if UID is not available
        if (uid == null) {
            Toast.makeText(this, "UID is null, cannot proceed", Toast.LENGTH_SHORT).show();
            return; // Or handle the error appropriately
        }

        DocumentReference userRef = database.collection("users").document(uid);

        // Retrieve the user document from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    updateSubscriptionStatus(userRef, user);
                } else {
                    Toast.makeText(this, "User document is null", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            // Handle the failure to retrieve the user document
            Toast.makeText(this, "Failed to retrieve user document", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSubscriptionStatus(DocumentReference userRef, User user) {
        Date currentDate = new Date();
        Map<String, Object> updates = new HashMap<>();

        // Check and update the plan statuses based on deactivation dates
        boolean basicPlanExpired = user.isBasicPlan() && user.getBasicPlanDeactivationDate() != null
                && user.getBasicPlanDeactivationDate().compareTo(currentDate) < 0;
        boolean standardPlanExpired = user.isStandardPlan() && user.getStandardPlanDeactivationDate() != null
                && user.getStandardPlanDeactivationDate().compareTo(currentDate) < 0;
        boolean premiumPlanExpired = user.isPremiumPlan() && user.getPremiumPlanDeactivationDate() != null
                && user.getPremiumPlanDeactivationDate().compareTo(currentDate) < 0;

        if (basicPlanExpired) {
            updates.put("basicPlan", false);
            // Show a dialog box with the Buy subscription message
            showDialog("Basic Plan Expired", "Renew your plan and earn more reward.", R.raw.basic_avatar_animation, "Buy later", "Buy Now", SpinnerActivity.class, SubscriptionActivity.class);
        }

        if (standardPlanExpired) {
            updates.put("standardPlan", false);
            // Show a dialog box with the Buy subscription message
            showDialog("Standard Plan Expired", "Renew your plan and earn more reward.", R.raw.standard_avatar_animation, "Buy later", "Buy Now", SpinnerActivity.class, SubscriptionActivity.class);
        }

        if (premiumPlanExpired) {
            updates.put("premiumPlan", false);
            // Show a dialog box with the Buy subscription message
            showDialog("Premium Plan Expired", "Renew your plan and earn more reward.", R.raw.premium_avatar_animation, "Buy later", "Buy Now", SpinnerActivity.class, SubscriptionActivity.class);
        }

        // Apply updates to Firestore if there are any
        if (!updates.isEmpty()) {
            userRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User document updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update user document", Toast.LENGTH_SHORT).show();
                    });
        } else {
        }

        // Update subscription status if all plans are inactive
        if (!user.isBasicPlan() && !user.isStandardPlan() && !user.isPremiumPlan()) {
            updates.put("subscription", false);
        } else {
            Toast.makeText(this, "You have active subscription plan", Toast.LENGTH_SHORT).show();
        }

    }
    // Method to award daily coins free and based on the user's active premium plan
    private void awardCoinsForPremiumPlan() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return; // User not logged in, exit the method
        }

        String currentUserID = currentUser.getUid();
        DocumentReference userRef = firestore.collection("users").document(currentUserID);

        // Retrieve the user document from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.isSubscription()) {
                    Date currentDate = new Date();

                    // Check if the user has already received the rewards for the current day
                    Date lastRewardedDate = user.getLastRewardedDate();
                    if (isSameDay(currentDate, lastRewardedDate)) {
                        return; // The user has already received the rewards for today, exit the method
                    }

                    SubscriptionModel subscriptionPlan = null;

                    // Determine the subscription plan and set dailyCoins accordingly
                    if (user.isSubscription()) {
                        if (user.isPremiumPlan()) {
                            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
                        } else if (user.isStandardPlan()) {
                            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
                        } else if (user.isBasicPlan()) {
                            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
                        }
                    }

                    if (subscriptionPlan != null) {
                        dailyCoins = subscriptionPlan.getDailyCoins();
                    }

                    // Award coins to the user
                    Integer currentCoins = user.getCoins();
                    if (currentCoins != null) {
                        user.setCoins(currentCoins + dailyCoins);
                    } else {
                        user.setCoins(dailyCoins); // Set initial coins if user coins are null
                    }

                    // Update the last rewarded date for daily coins to the current date
                    user.setLastRewardedDate(currentDate);

                    // Save the updated user object back to Firestore
                    userRef.set(user)
                            .addOnSuccessListener(aVoid -> {
                                // Success, user data updated in the database
                                // Show a dialog box with the coin award message
                                showDialog(getString(R.string.login_reward), getString(R.string.login_daily_and_claim_your) + " " + dailyCoins + " " + getString(R.string.coins), R.raw.reward_box, getString(R.string.get_more), getString(R.string.claim), SpinnerActivity.class, null);
                            })
                            .addOnFailureListener(e -> {
                                // Failed to save user data to the database
                            });
                } else {
                    Date currentDate = new Date();
                    // Check if the user has already received the rewards for the current day
                    Date lastRewardedDate = user != null ? user.getLastRewardedDate() : null;
                    if (isSameDay(currentDate, lastRewardedDate)) {
                        return; // The user has already received the rewards for today, exit the method
                    }

                    // Award coins to the user if the user object and coins are not null
                    Integer currentCoins = (user != null) ? user.getCoins() : null;
                    if (currentCoins != null) {
                        user.setCoins(currentCoins + dailyCoins);
                    } else {
                        user.setCoins(dailyCoins); // Set initial coins if user coins are null
                    }
                    // Update the last rewarded date to the current date
                    user.setLastRewardedDate(currentDate);

                    // Save the updated user object back to Firestore
                    userRef.set(user)
                            .addOnSuccessListener(aVoid -> {
                                // Success, user data updated in the database
                                // Show a dialog box with the coin award message
                                showDialog(getString(R.string.login_reward), getString(R.string.login_daily_and_claim_your) + " " + dailyCoins + " " + getString(R.string.coins), R.raw.reward_box, getString(R.string.get_more), getString(R.string.claim), SpinnerActivity.class, null);
                            })
                            .addOnFailureListener(e -> {
                                // Failed to save user data to the database
                            });
                }
            }
        }).addOnFailureListener(e -> {
            // Failed to retrieve user document from Firestore
        });
    }

    // Helper method to check if two dates belong to the same day
    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        int year1 = cal1.get(Calendar.YEAR);
        int month1 = cal1.get(Calendar.MONTH);
        int day1 = cal1.get(Calendar.DAY_OF_MONTH);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int year2 = cal2.get(Calendar.YEAR);
        int month2 = cal2.get(Calendar.MONTH);
        int day2 = cal2.get(Calendar.DAY_OF_MONTH);
        
        return (year1 == year2) && (month1 == month2) && (day1 == day2);
    }
    
    private void showDialog(String title, String body, int animationResId, String leftButtonText, String rightButtonText, Class<?> activityLeftClass, Class<?> activityRightClass ) {
        // Inflate your custom layout for the dialog content
        View customView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_box_layout, null);
        // Set the title and body text directly on the custom layout
        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
        TextView dialogBody = customView.findViewById(R.id.dialog_body);
        dialogTitle.setText(title);
        dialogBody.setText(body);
        // Set the animation directly on the LottieAnimationView
        LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
        lottieAnimation.setAnimation(animationResId);
        // Create an instance of the custom dialog and pass the custom layout as a parameter
        DialogBox dialogBox = new DialogBox(MainActivity.this, customView);
        Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
    
        // do not cancel dialog box
        dialogBox.setCanceledOnTouchOutside(false);
        
        // Set the left button action
        dialogBox.setLeftButton(leftButtonText, v -> {
            dialogBox.dismiss();
            // Handle left button click
            if (activityLeftClass == null) {
                // Show interstitial ad if it's loaded
                // Add 7 days trial peroid method
            }else {
                startActivity(new Intent(this, SpinnerActivity.class));
            }
        });
        
        // Set the right button action
        dialogBox.setRightButton(rightButtonText, v -> {
            // Handle right button click
            if (activityRightClass != null) {
                // Launch PaymentActivity with a custom amount
                double customAmount = 49.00; // Change this to your desired amount
                Intent paymentIntent = new Intent(MainActivity.this, activityRightClass);
                paymentIntent.putExtra("AMOUNT", customAmount);
                paymentLauncher.launch(paymentIntent);
            } else {
                dialogBox.dismiss();
            }
        });
     
        // Show the dialog
        dialogBox.show();
    }
    
    @Override
    protected void onUserLeaveHint()
    {
        Log.d("onUserLeaveHint","Home button pressed");
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            updateUserStatus("offline");
        }
        super.onUserLeaveHint();
    }

}
