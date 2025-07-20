package com.bg4u.coins4u;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class SubscriptionManager {
    
    private static SubscriptionManager instance;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private User user;
    private Calendar currentDate;
    
    private SubscriptionManager() {
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        currentDate = Calendar.getInstance();
        
        String currentUserUid = firebaseAuth.getCurrentUser().getUid();
        database.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        user = document.toObject(User.class);
                    }
                });
    }
    
    public static synchronized SubscriptionManager getInstance() {
        if (instance == null) {
            instance = new SubscriptionManager();
        }
        return instance;
    }
    
    public boolean isPremiumUser() {
        return user != null && user.isSubscription();
    }
    
    public boolean isPremiumPlan() {
        return user != null && user.isPremiumPlan() &&  (user.getPremiumPlanDeactivationDate() == null || user.getPremiumPlanDeactivationDate().after(currentDate.getTime()));
    }
    
    public boolean isStandardPlan() {
        return user != null && user.isStandardPlan() && (user.getStandardPlanDeactivationDate() == null || user.getStandardPlanDeactivationDate().after(currentDate.getTime()));
    }
    
    public boolean isBasicPlan() {
        return user != null && user.isBasicPlan() &&  (user.getBasicPlanDeactivationDate() == null || user.getBasicPlanDeactivationDate().after(currentDate.getTime()));
    }
    
    public double getCoinMultiplier() {
        SubscriptionModel subscriptionPlan;
        if (isPremiumPlan()) {
            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
        } else if (isStandardPlan()) {
            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
        } else if (isBasicPlan()) {
            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
        } else {
            subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("default");
        }
        return subscriptionPlan.getCoinMultiplier();
    }
    
    public double getAdProbabilityFactor() {
        if (isPremiumPlan()) {
            return 0.60;
        } else if (isStandardPlan()) {
            return 0.40;
        } else if (isBasicPlan()) {
            return 0.10;
        } else {
            return 0.80; // 80% chance of getting ads for non-subscribers
        }
    }
    
    // Add methods for calculating coin rewards based on subscription
    // ...
    
    // Add other relevant utility methods
    // ...
}
