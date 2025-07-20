package com.bg4u.coins4u;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.RowLeaderboardsBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class LeaderboardsAdapter extends RecyclerView.Adapter<LeaderboardsAdapter.LeaderboardViewHolder> {
    
    private ListenerRegistration registration;
    Context context;
    ArrayList<User> users;
    private AdapterView.OnItemClickListener listener;
    private static final int MAX_USERS = 3; // Maximum number of users to display
    private Calendar currentDate = Calendar.getInstance();
    
    public LeaderboardsAdapter(Context context, ArrayList<User> users) {
        this.context = context;
        this.users = users;
        // make the code to show Max user (eg 100)
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_leaderboards, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        User user = users.get(position);
     
        holder.binding.name.setText(user.getName());
        holder.binding.coins.setText(String.valueOf(user.getCoins()));
        holder.binding.bio.setText(user.getBio());
    
        if (user.getUserState() != null && user.getUserState().equals("online")) {
            // Set the border color to green for online users
            int borderColor = ContextCompat.getColor(context, R.color.green);
            holder.binding.profilePicture.setBorderColor(borderColor);
        } else {
            // Set the border color to gray for offline users or users without a userState field
            int borderColor = ContextCompat.getColor(context, R.color.light_grey);
            holder.binding.profilePicture.setBorderColor(borderColor);
        }
    
    
        // Use String.format(Locale, ...) instead of String.format(...)
        if (position >= 100) {
            holder.binding.index.setText("99+");
        } else {
            holder.binding.index.setText(String.format(Locale.getDefault(), "%d", position + 1));
        }
        
        if (user.getProfile() != null && !user.getProfile().isEmpty()) {
            // User has uploaded a profile image
            Glide.with(context)
                    .load(user.getProfile())
                    .into(holder.binding.profilePicture);
        } else {
            // User has not uploaded a profile image, set default image
            Glide.with(context)
                    .load(R.drawable.user_icon_default)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.binding.profilePicture);
        }
    

        if (user.isSubscription() && (user.isPremiumPlan() || user.isStandardPlan() || user.isBasicPlan())){
    
            SubscriptionModel subscriptionPlan;
            if (user.isPremiumPlan() && (user.getPremiumPlanDeactivationDate() == null || user.getPremiumPlanDeactivationDate().after(currentDate.getTime()))) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                holder.binding.premiumIcon.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setVisibility(View.VISIBLE);
                holder.binding.animationViewBanner.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setAnimation(avatarLottieResId);
                holder.binding.animationViewBanner.setAnimation(bannerLottieResId);
                holder.binding.animationViewBanner.playAnimation();
                holder.binding.animationViewRank.playAnimation();
            } else if (user.isStandardPlan() && (user.getPremiumPlanDeactivationDate() == null || user.getPremiumPlanDeactivationDate().after(currentDate.getTime()))) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                holder.binding.premiumIcon.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setVisibility(View.VISIBLE);
                holder.binding.animationViewBanner.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setAnimation(avatarLottieResId);
                holder.binding.animationViewBanner.setAnimation(bannerLottieResId);
                holder.binding.animationViewBanner.playAnimation();
                holder.binding.animationViewRank.playAnimation();
            } else if (user.isBasicPlan() && (user.getBasicPlanDeactivationDate() == null || user.getBasicPlanDeactivationDate().after(currentDate.getTime()))) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
                int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
                int bannerLottieResId = subscriptionPlan.getBannerLottieResId();
                holder.binding.premiumIcon.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setVisibility(View.VISIBLE);
                holder.binding.animationViewBanner.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setAnimation(avatarLottieResId);
                holder.binding.animationViewBanner.setAnimation(bannerLottieResId);
                holder.binding.animationViewBanner.playAnimation();
                holder.binding.animationViewRank.playAnimation();
            }else {
                // Hide the animation view for other players
                holder.binding.index.setVisibility(View.VISIBLE);
                holder.binding.animationViewRank.setVisibility(View.GONE);
                holder.binding.animationViewBanner.setVisibility(View.GONE);
                holder.binding.premiumIcon.setVisibility(View.GONE);
                    // Set the default background for other users
                // holder.binding.animationViewBanner.setAnimation(R.raw.background_blue_rotation);
    
                // Get the Drawable object from the resource ID
                Drawable backgroundDrawable = ContextCompat.getDrawable(context, R.color.colorPurple);
    
                // Set the background drawable on the view
                holder.binding.bannerImage.setBackground(backgroundDrawable);
            }
        }
    
        // Check if it's the first player
        else if (position == 0) {
            // Display the Lottie animation for the first player
            holder.binding.animationViewRank.setVisibility(View.VISIBLE);
            holder.binding.index.setVisibility(View.GONE);
            // Set the Lottie animation for the animation views
            holder.binding.animationViewRank.setAnimation(R.raw.winner_top_one);
            holder.binding.animationViewRank.playAnimation();
            holder.binding.animationViewBanner.setVisibility(View.VISIBLE);
            holder.binding.animationViewBanner.setAnimation(R.raw.winner_banner_animation);
            holder.binding.animationViewBanner.playAnimation();
        } else {
            // Hide the animation view for other players
            holder.binding.index.setVisibility(View.VISIBLE);
            holder.binding.animationViewRank.setVisibility(View.GONE);
            holder.binding.premiumIcon.setVisibility(View.GONE);
            // Set the default background for other users
            // holder.binding.animationViewBanner.setAnimation(R.raw.background_blue_rotation);
            
            // Get the Drawable object from the resource ID
            Drawable backgroundDrawable = ContextCompat.getDrawable(context, R.color.colorPurple);

            // Set the background drawable on the view
            holder.binding.bannerImage.setBackground(backgroundDrawable);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {

        RowLeaderboardsBinding binding;
        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowLeaderboardsBinding.bind(itemView);
        }
    }
    
    @Override
    public void onViewRecycled(@NonNull LeaderboardViewHolder holder) {
        super.onViewRecycled(holder);
        if (registration != null) {
            registration.remove(); // Remove the listener when the view is recycled
        }
    }
    
}