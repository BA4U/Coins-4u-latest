package com.bg4u.coins4u;

import android.app.Application;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class MyApplication extends Application {

    // NOTE: Replace the below with your own ONESIGNAL_APP_ID
    private static final String ONESIGNAL_APP_ID = "aaa1c7c6-ff49-4c47-901e-222cafdd7681";

    private static User currentUser; // To hold the user data

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Verbose Logging set to help debug issues, remove before releasing your app.
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {
            if (r.isSuccess()) {
                if (r.getData()) {
                    // `requestPermission` completed successfully and the user has accepted permission
                }
                else {
                    // `requestPermission` completed successfully but the user has rejected permission
                }
            }
            else {
                // `requestPermission` completed unsuccessfully, check `r.getThrowable()` for more info on the failure reason
            }
        }));


        // Initialize the click sound helper
        ClickSoundHelper.initialize(this);

        // Load user profile and subscription status
        loadUserProfileAndSubscription();
    }

    private void loadUserProfileAndSubscription() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String currentUserID = firebaseUser.getUid();
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(currentUserID);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        // Access user data
                        String userName = currentUser.getName(); // Get user name
                        String profilePicUrl = currentUser.getProfile(); // Get user profile picture URL
                        boolean subscriptionStatus = currentUser.isSubscription(); // Get subscription status

                        // Check user's current plan
                        if (currentUser.isPremiumPlan()) {
                            // Handle premium plan
                        } else if (currentUser.isStandardPlan()) {
                            // Handle standard plan
                        } else if (currentUser.isBasicPlan()) {
                            // Handle basic plan
                        }
                    } else {
                        // Handle the case when currentUser is null
                    }
                }
            }).addOnFailureListener(e -> {
                // Handle the error
            });
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getProfilePicUrl() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return currentUser.getProfile(); // Get user profile picture URL
        }
        return null; // Return null if currentUser is not available
    }

    public static boolean isUserSubscribed() {
        return currentUser != null && currentUser.isSubscription();
    }

    public static String getUserSubscriptionPlan() {
        if (currentUser != null) {
            if (currentUser.isPremiumPlan()) {
                return "Premium Plan";
            } else if (currentUser.isStandardPlan()) {
                return "Standard Plan";
            } else if (currentUser.isBasicPlan()) {
                return "Basic Plan";
            }
        }
        return "null";
    }

}
