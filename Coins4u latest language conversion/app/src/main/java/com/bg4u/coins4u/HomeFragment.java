package com.bg4u.coins4u;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.databinding.FragmentHomeBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;
    private FirebaseFirestore database;
    private ArrayList<CategoryModel> categories;
    private ArrayList<PromotionModel> promotions;
    private CategoryAdapter categoryAdapter;
    private PromotionAdapter promotionAdapter;
    private ListenerRegistration categoriesListener;
    private ListenerRegistration promotionsListener;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private Handler autoScrollCategoryHandler; // Handler for category list auto-scroll
    private Runnable autoScrollCategoryRunnable; // Runnable for category list auto-scroll
    private int currentScrollPosition;
    private int currentCategoryScrollPosition; // Track the current scroll position for category list
    private boolean isPremiumUser;
    
    private User currentUser;
    // Declare the nextActivityIntent variable to hold the Intent for the next activity
    private Intent nextActivityIntent;
    public HomeFragment() {
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Coins 4u");
        }
        
        database = FirebaseFirestore.getInstance();
        categories = new ArrayList<>();
        promotions = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(getContext(), categories);
        promotionAdapter = new PromotionAdapter(getContext(), promotions);

        binding.addFloatBtn.setOnClickListener(view1 -> {
            if(isPremiumUser){
                openChatActivity();
            }
            else {
                showCongratsDialog(getString(R.string.buy_subscription), getString(R.string.watch_an_ad_to_continue), R.raw.premium_gold_icon);
            }
        });
        
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        database.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            isPremiumUser = currentUser.isSubscription();
                            displayUserData(currentUser);
                            displayPremiumAnimation(isPremiumUser);
                        }
                    } else {
                        displayErrorMessage("login again");
                        logout();
                    }
                })
                .addOnFailureListener(e -> displayErrorMessage("Failed to fetch user data: " + e.getMessage()));
        
        return view;
    }
    
    private void logout() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("USER_PREF", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
    
    private void openChatActivity() {
        // Open the MainActivity
        Intent intent = new Intent(getActivity(), com.bg4u.coins4u.chat4u.MainActivity.class);
        startActivity(intent);
    }
    
    private void displayPremiumAnimation(boolean isPremiumUser) {
        // Check if the binding object and premiumIconHome view are not null
        if (binding != null ) {
            // Display the Lottie animation if the user is premium
            LottieAnimationView premiumIcon = binding.premiumIconHome;
            if (isPremiumUser) {
                binding.premiumAnimationBtn.setVisibility(View.GONE);
                premiumIcon.setVisibility(View.VISIBLE);
                premiumIcon.setAnimation(R.raw.premium_gold_icon);
                premiumIcon.playAnimation();
            } else {
                // Hide the Lottie animation if the user is not premium
                premiumIcon.setVisibility(View.GONE);
            }
        }
    }
    
    private void displayErrorMessage(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayUserData(User user) {
        // Check if the binding object is null before trying to access its fields
        if (binding != null){
            // Display the user's name, profile, bio, and coin balance in the UI
            binding.username.setText(user.getName());
            Context context = getContext();
            if (context != null) {
                if (user.getProfile() != null && !user.getProfile().isEmpty()) {
                    Glide.with(context)
                            .load(user.getProfile())
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @com.google.firebase.database.annotations.Nullable Transition<? super Drawable> transition) {
                                    if (binding != null) {
                                        binding.userImage.setImageDrawable(resource);
                                    }
                                }
                                @Override
                                public void onLoadCleared(@com.google.firebase.database.annotations.Nullable Drawable placeholder) {
                                }
                            });
                } else if (binding != null) {
                    binding.userImage.setImageResource(R.drawable.user_icon_default);
                }
            }
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        
        // Set the toolbar title with logo
        if (getActivity() != null && ((MainActivity) getActivity()).getSupportActionBar() != null) {
            ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
            // Set the logo
            actionBar.setTitle("Coins 4u"); // Replace "coins4u_home" with your actual logo resource
            //   actionBar.setIcon(R.drawable.coins4ulogo);
            // Not set the title
        }
        
        // Set up RecyclerViews and adapters
        binding.categoryList.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.categoryList.setAdapter(categoryAdapter);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.promotionList.setLayoutManager(layoutManager);
        binding.promotionList.setAdapter(promotionAdapter);
        
        // Add native ads to the category list RecyclerView
        
        // Add native ads to the promotion list RecyclerView
        
        // Retrieve categories and promotions from Firestore
        retrieveCategories();
        retrievePromotions();

        binding.freefire.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            startActivity(new Intent(getContext(), FreefireActivity.class));
        });

// Set click listeners for spinwheel and tictacgame buttons
        binding.spinwheel.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            startActivity(new Intent(getContext(), SpinnerActivity.class));
        });
    
        binding.tictacgame.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            startActivity(new Intent(getContext(), TicTacToeActivity.class));
        });

// Set click listeners for go premium animation buttons
        binding.premiumAnimationBtn.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            startSubscriptionActivity();
        });
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private void retrieveCategories() {
        database.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categories.clear();
                    
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        CategoryModel model = snapshot.toObject(CategoryModel.class);
                        if (model != null && (model.getCategoryStatus() == null || Boolean.TRUE.equals(model.getCategoryStatus()))) {
                            model.setCategoryId(snapshot.getId());
                            categories.add(model);
                        }
                    }
                    
                    // Shuffle the categories list
                    Collections.shuffle(categories, new Random());
                    
                    categoryAdapter.notifyDataSetChanged();
                    
                    // Start auto-scroll for category list
                    if (!categories.isEmpty()) {
                        startAutoScrollCategory();
                    }
                });
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private void retrievePromotions() {
        database.collection("promotions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    promotions.clear();
                    
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        PromotionModel model = snapshot.toObject(PromotionModel.class);
                        if (model != null && (model.getPromotionStatus() == null || Boolean.TRUE.equals(model.getPromotionStatus()))) {
                            promotions.add(model);
                        }
                    }
                    
                    promotionAdapter.notifyDataSetChanged();
                    
                    if (!promotions.isEmpty()) {
                        startAutoScroll();
                    }
                });
    }
    
    // Add native ads to the category list RecyclerView
    
    private void startSubscriptionActivity() {
        Intent intent = new Intent(getContext(), SubscriptionActivity.class);
        startActivity(intent);
    }
    
    private void startAutoScroll() {
        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                currentScrollPosition++;
                
                if (currentScrollPosition >= promotions.size()) {
                    currentScrollPosition = 0;
                }
                
                if (binding != null) {
                    binding.promotionList.smoothScrollToPosition(currentScrollPosition);
                }
                
                autoScrollHandler.postDelayed(this, 4000);
            }
        };
        
        autoScrollHandler.postDelayed(autoScrollRunnable, 4000);
    }
    
    // Start auto-scroll for category list
    private void startAutoScrollCategory() {
        autoScrollCategoryHandler = new Handler(Looper.getMainLooper());
        autoScrollCategoryRunnable = new Runnable() {
            @Override
            public void run() {
                currentCategoryScrollPosition++;
                
                if (currentCategoryScrollPosition >= categories.size()) {
                    currentCategoryScrollPosition = 0;
                }
                
                if (binding != null) {
                    binding.categoryList.scrollToPosition(currentCategoryScrollPosition);
                }
                
                autoScrollCategoryHandler.postDelayed(this, 6000);
            }
        };
        
        autoScrollCategoryHandler.postDelayed(autoScrollCategoryRunnable, 3000);
    }
    
    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        
        // Stop auto-scroll for category list
        if (autoScrollCategoryHandler != null && autoScrollCategoryRunnable != null) {
            autoScrollCategoryHandler.removeCallbacks(autoScrollCategoryRunnable);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        stopAutoScroll();
        
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        
        if (promotionsListener != null) {
            promotionsListener.remove();
        }
    }
    private void showCongratsDialog(String title, String body, int animationRes) {
        // Inflate your custom layout for the dialog content
        View customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_box_layout, null);
        
        // Set the title and body text directly on the custom layout
        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
        TextView dialogBody = customView.findViewById(R.id.dialog_body);
        dialogTitle.setText(title);
        dialogBody.setText(body);
        
        // Set the animation directly on the LottieAnimationView
        LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
        lottieAnimation.setAnimation(animationRes);
        
        // Create an instance of the custom dialog and pass the custom layout as a parameter
        DialogBox dialogBox = new DialogBox(requireContext(), customView);
        Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        
        dialogBox.setCancelable(false);
        
        // Set the left button action
        dialogBox.setLeftButton(getString(R.string.get_premium), v -> {
            ClickSoundHelper.playClickSound();
            // open activity for user to join premium
            startSubscriptionActivity();
        });
        
        // Set the right button action
        dialogBox.setRightButton(getString(R.string.watch_Ads), v -> {
            ClickSoundHelper.playClickSound();
            // Handle right button click
            dialogBox.dismiss();
            // Show rewarded ad if it's loaded
            // showRewardedAd();
            // Open chat activity
            openChatActivity();
        });
        
        // Show the dialog
        dialogBox.show();
    }

}
