package com.bg4u.coins4u;

import com.bg4u.coins4u.R;

import java.util.HashMap;
import java.util.Map;

public class SubscriptionAdapter {
    private static final Map<String, SubscriptionModel> subscriptionPlans = new HashMap<>();
    
    static {
        // Premium Plan 1
        SubscriptionModel basicPlan = new SubscriptionModel(
                "Basic",
                1.5, // Coin multiplier
                100, // Daily coins
                5, // Max chat users (set to 0 as chat is not allowed)
                R.raw.basic_avatar_animation, // Avatar Lottie file name
                R.raw.basic_banner_animation, // Banner Lottie file name
                false, // Chat with other users (false means not allowed)
                false, // Ad-free experience (true means ads are disabled)
                2
        );
        
        // Premium Plan 2
        SubscriptionModel standardPlan = new SubscriptionModel(
                "Standard",
                1.5, // Coin multiplier
                200, // Daily coins
                10, // Max chat users (set to 5 to limit the number of users to chat with)
                R.raw.standard_avatar_animation, // Avatar Lottie file name
                R.raw.standard_banner_animation, // Banner Lottie file name
                true, // Chat with other users (true means allowed)
                false, // Ad-free experience (true means ads are disabled)
                5
        );
        
        // Premium Plan 3
        SubscriptionModel premiumPlan = new SubscriptionModel(
                "Premium",
                2.0, // Coin multiplier
                500, // Daily coins
                100, // Max chat users (set to 10 to limit the number of users to chat with)
                R.raw.premium_avatar_animation, // Avatar Lottie file name
                R.raw.premium_banner_animation, // Banner Lottie file name
                true, // Chat with other users (true means allowed)
                true, // Ad-free experience (true means ads are disabled)
                8
        );
        
        // Default Plan
        SubscriptionModel defaultPlan = new SubscriptionModel(
                "default",
                1.0, // Coin multiplier
                25, // Daily coins
                2, // Max chat users (set to 10 to limit the number of users to chat with)
                0, // Avatar Lottie file name
                0, // Banner Lottie file name
                false, // Chat with other users (true means allowed)
                false, // Ad-free experience (true means ads are disabled)
                1
        );
        
        // Add the subscription plans to the map
        subscriptionPlans.put("basic", basicPlan);
        subscriptionPlans.put("standard", standardPlan);
        subscriptionPlans.put("premium", premiumPlan);
        subscriptionPlans.put("default", defaultPlan);
    }
    
    public static SubscriptionModel getSubscriptionPlan(String planName) {
        return subscriptionPlans.get(planName);
    }
}
