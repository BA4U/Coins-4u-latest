package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bg4u.coins4u.databinding.FragmentProfileBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Locale;
import java.util.Objects;

public class ProfileFragment extends Fragment {
    private OnBackPressedCallback onBackPressedCallback;
    private RewardedAd rewardedAd;
    private AdView mAdView;
    private FirebaseFirestore database;
    Spinner spinner;
    Locale myLocale;
    
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private User currentUser;
    private boolean isPremiumUser;
    
    private static final int RC_SIGN_IN = 123;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    public ProfileFragment() {
        // Required empty public constructor
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        
        spinner = (Spinner) binding.spinner;
        
        // initializeLanguageSpinner();
        
        String appVersion = getAppVersion();
        binding.appVersionTextView.setText("App Version: " + appVersion);
        
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.my_profile));
        }
    
        if (isAnonymousUser()) {
            // User is anonymous, show UI for linking Google account
            binding.emailCardView.setVisibility(View.GONE);
        } else {
            // User is not anonymous, handle accordingly
            binding.googleBtn.setVisibility(View.GONE);
        }
    
        // Initialize the Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        handleGoogleSignInResult(data);
                    } else {
                        // Handle Google Sign-In failure
                        // For example, display a toast or perform other error handling
                        Toast.makeText(requireContext(), "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        firestore = FirebaseFirestore.getInstance();
        
        firestore.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(this::processUserDocument)
                .addOnFailureListener(e -> displayErrorMessage("Failed to fetch user data: " + e.getMessage()));
        
        binding.logoutBtn.setOnClickListener(v -> showLogoutDialog());
        binding.editProfile.setOnClickListener(v -> openSetupProfileActivity());
    
        binding.googleBtn.setOnClickListener(v -> {
            // Check if Google Play Services are available
            if (isGooglePlayServicesAvailable()) {
                // Start the Google Sign-In process
                linkAnonymousToGoogle();
            } else {
                // Handle Google Play Services not available
                // For example, display a dialog or a toast
                Toast.makeText(requireContext(), "Google Play Services not available.", Toast.LENGTH_SHORT).show();
            }
        });
       
        return binding.getRoot();
    }
    
    
    private void openSetupProfileActivity() {
        Intent Intent = new Intent(requireContext(), SetupProfileActivity.class);
        startActivity(Intent);
    }
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext());
        return resultCode == ConnectionResult.SUCCESS;
    }
    private boolean isAnonymousUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        
        return user != null && user.isAnonymous();
    }
    private void linkAnonymousToGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
    
        // Launch the Google Sign-In intent to choose an account
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    private void handleGoogleSignInResult(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(account -> {
                    // Obtain the ID token from the signed-in Google account
                    String idToken = account.getIdToken();

                    // Create an AuthCredential using the Google ID token
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

                    // Link the anonymous account with Google
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseUser user = auth.getCurrentUser();

                    if (user != null) {
                        user.linkWithCredential(credential)
                                .addOnCompleteListener(linkTask -> {
                                    if (linkTask.isSuccessful()) {
                                        // Account linking successful
                                        // You can perform further actions here
                                        Toast.makeText(requireContext(), "Account linked with Google.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Account linking failed
                                        // For example, display a toast or perform other error handling
                                        Toast.makeText(requireContext(), "Failed to link account with Google.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle Google Sign-In failure
                    // For example, display a toast or perform other error handling
                    Toast.makeText(requireContext(), "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                });
    }
    
//    private void handleGoogleSignInResult(Intent data) {
//        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//        try {
//            // Get the signed-in Google account
//            GoogleSignInAccount account = task.getResult(ApiException.class);
//            if (account != null) {
//                // Obtain the ID token from the signed-in Google account
//                String idToken = account.getIdToken();
//
//                // Create an AuthCredential using the Google ID token
//                AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//
//                // Link the anonymous account with Google
//                FirebaseAuth auth = FirebaseAuth.getInstance();
//                FirebaseUser user = auth.getCurrentUser();
//
//                if (user != null) {
//                    user.linkWithCredential(credential)
//                            .addOnCompleteListener(linkTask -> {
//                                if (linkTask.isSuccessful()) {
//                                    // Account linking successful
//                                    // You can perform further actions here
//                                    Toast.makeText(requireContext(), "Account linked with Google.", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    // Account linking failed
//                                    Toast.makeText(requireContext(), "Failed to link account with Google.", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                }
//            }
//        } catch (ApiException e) {
//            // Handle Google Sign-In failure
//            Toast.makeText(requireContext(), "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
    private void processUserDocument(DocumentSnapshot document) {
        if (document.exists()) {
            currentUser = document.toObject(User.class);
            if (currentUser != null) {
                isPremiumUser = currentUser.isSubscription();
                displayUserData(currentUser);
                displayPremiumAnimation();
            }
        } else {
            displayErrorMessage("User data not found");
        }
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
    
    private void displayUserData(User user) {
        if (binding != null) {
            binding.name.setText(user.getName());
            binding.emailBox.setText(user.getEmail());
            binding.socialBox.setText(user.getSocialMediaLink());
            binding.currentCoins.setText(String.valueOf(user.getCoins()));
            binding.correctAns.setText(String.valueOf(user.getCorrectAnswers()));
            binding.wrongAns.setText(String.valueOf(user.getWrongAnswers()));
            binding.userLocation.setText(user.getLocation());
            binding.phoneNumberBox.setText(user.getPhoneNumber());
            binding.ageBox.setText(user.getAge());
            binding.bio.setText(user.getBio());
    
            // Binding number of win, draw, and lost for Tic Tac Toe
            binding.easyWin.setText(String.valueOf(user.getEasyWin()));
            binding.easyDraw.setText(String.valueOf(user.getEasyDraw()));
            binding.easyLost.setText(String.valueOf(user.getEasyLost()));
    
            binding.mediumWin.setText(String.valueOf(user.getMediumWin()));
            binding.mediumDraw.setText(String.valueOf(user.getMediumDraw()));
            binding.mediumLost.setText(String.valueOf(user.getMediumLost()));
    
            binding.hardWin.setText(String.valueOf(user.getHardWin()));
            binding.hardDraw.setText(String.valueOf(user.getHardDraw()));
            binding.hardLost.setText(String.valueOf(user.getHardLost()));
    
            binding.onlineWin.setText(String.valueOf(user.getOnlineWin()));
            binding.onlineDraw.setText(String.valueOf(user.getOnlineDraw()));
            binding.onlineLost.setText(String.valueOf(user.getOnlineLost()));
            
            binding.taskCompleted.setText(String.valueOf(user.getTaskCompleted()));
    
            Context context = getContext();
            if (context != null) {
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
                } else if (binding != null) {
                    binding.profile.setImageResource(R.drawable.user_icon_default);
                }
            }
        }
    }
    
    private void showLogoutDialog() {
        //  show Ads

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setIcon(R.drawable.logout)
                .setMessage(getString(R.string.are_you_sure_you_want_to_logout))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> logout())
                .setCancelable(true)
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    
    private void logout() {
        // Clear user preferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("USER_PREF", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    
        // Set status as offline
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            firestore.collection("users").document(currentUserId)
                    .update("status", "offline")
                    .addOnSuccessListener(aVoid -> {
                        // Successfully updated status
                        // Sign out from FirebaseAuth
                        FirebaseAuth.getInstance().signOut();
                        
                        // Disconnect Google Sign-In client
                        googleSignOut();
                        
                        // Redirect to LoginActivity
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .addOnFailureListener(e -> {
                        // Handle the failure to update status
                        // You might want to show an error message or log the error
                        // Consider not signing out or redirecting on failure
                    });
        } else {
            // Handle the case when the user is not authenticated
            // You might want to show an error message or log the error
        }
    }

    private void googleSignOut(){
        // Disconnect Google Sign-In client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        googleSignInClient.signOut();
    
    }
    // This method logout user and revoke google sign in access
//    private void logout() {
//        FirebaseAuth.getInstance().signOut(); // Sign out of Firebase Authentication
//
//        // Revoke Google Sign-In access
//        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
//        googleSignInClient.revokeAccess().addOnCompleteListener(requireActivity(), new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                // Clear SharedPreferences
//                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("USER_PREF", Context.MODE_PRIVATE);
//                sharedPreferences.edit().clear().apply();
//
//                // Start LoginActivity
//                Intent intent = new Intent(getActivity(), LoginActivity.class);
//                startActivity(intent);
//                requireActivity().finish();
//            }
//        });
//    }

    private String getAppVersion() {
        try {
            Context context = requireContext();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private void displayErrorMessage(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Release the binding
    }
    
}
