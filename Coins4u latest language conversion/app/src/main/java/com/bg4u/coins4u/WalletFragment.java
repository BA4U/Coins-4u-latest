package com.bg4u.coins4u;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bg4u.coins4u.databinding.FragmentWalletBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;

public class WalletFragment extends Fragment {

    // Declare variables
    FragmentWalletBinding binding;
    FirebaseFirestore database;
    private ListenerRegistration registration;
    private User user;
    private String uid;
    private int userCoins;
    private int taskCompleted;
    private String redeemCode;
    private int minCoinsRedeem = 50000;
    private int minRedeemAmount = 15;
    private int midCoinsRedeem = 150000;
    private int midRedeemAmount = 50;
    private int maxCoinsRedeem = 250000;
    private int maxRedeemAmount = 100;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        // Get the activity and set the action bar title
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("My Wallet");
        }

        // Initialize Firebase components
        database = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        loadRedeemData();

        // Set up click listeners and fetch user data
        setupClickListeners();
        fetchUserData();

        return rootView;
    }

    private void loadRedeemData() {
        DocumentReference coinsDocRef = FirebaseFirestore.getInstance().collection("Redeem").document("coins");
        DocumentReference moneyDocRef = FirebaseFirestore.getInstance().collection("Redeem").document("money");
        coinsDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                RedeemModel RedeemCoinsData = documentSnapshot.toObject(RedeemModel.class);
                if (RedeemCoinsData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.firstCoin.setText(String.valueOf(RedeemCoinsData.getFirst()));
                    binding.secondCoin.setText(String.valueOf(RedeemCoinsData.getSecond()));
                    binding.thirdCoin.setText(String.valueOf(RedeemCoinsData.getThird()));

                    // Update the class-level instance variables with the retrieved values
                    minCoinsRedeem = RedeemCoinsData.getFirst();
                    midCoinsRedeem = RedeemCoinsData.getSecond();
                    maxCoinsRedeem = RedeemCoinsData.getThird();
                }
            }
        });
        moneyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                RedeemModel RedeemMoneyData = documentSnapshot.toObject(RedeemModel.class);
                if (RedeemMoneyData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.firstButton.setText("Win upto  ₹" + String.valueOf(RedeemMoneyData.getFirst()));
                    binding.secondButton.setText("Win upto  ₹" + String.valueOf(RedeemMoneyData.getSecond()));
                    binding.thirdButton.setText("Win upto  ₹" + String.valueOf(RedeemMoneyData.getThird()));

                    // Update the class-level instance variables with the retrieved values
                    minRedeemAmount = RedeemMoneyData.getFirst();
                    midRedeemAmount = RedeemMoneyData.getSecond();
                    maxRedeemAmount = RedeemMoneyData.getThird();
                }
            }
        });
    }

    private void fetchUserData() {
        // Remove any previous listener
        if (registration != null) {
            registration.remove();
        }

        // Add a snapshot listener for the user's data
        registration = database.collection("users")
                .document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            userCoins = user.getCoins();
                            taskCompleted = user.getTaskCompleted();
                            binding.currentCoins.setText(String.valueOf(userCoins));
                            buttonsVisibility();

                            checkRedeemCode();
                        } else {
                            Toast.makeText(getContext(), "You have no coins", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkRedeemCode() {
        database.collection("withdraws").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String redeemCode = documentSnapshot.getString("redeemCode");
                        binding.rewardView.setVisibility(View.VISIBLE);
                        binding.emailBox.setVisibility(View.GONE);
                        if (redeemCode != null) {
                            binding.rewardText.setText(redeemCode);
                            binding.copyButton.setOnClickListener(v -> {

                                // Get the clipboard manager
                                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

                                // Create a ClipData with the redeem code
                                ClipData clip = ClipData.newPlainText("Redeem Code", redeemCode);

                                // Set the ClipData to the clipboard
                                clipboard.setPrimaryClip(clip);

                                // Show a toast indicating successful copy
                                Toast.makeText(getContext(), "Redeem code copied to clipboard", Toast.LENGTH_SHORT).show();

                                // Open the Play Store to the redeem coins page
                                openPlayStoreForRedeemCoins(redeemCode);
                            });

                        }else {
                            binding.rewardText.setText( "Request sent successfully" + " " + "\n" + " " + "You will get your Reward Soon");
                            binding.rewardText.setTextSize(18);
                            binding.copyButton.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void openPlayStoreForRedeemCoins(String redeemCode) {
        // Create an Intent to open the Play Store website with the redeem code
        String redeemUrl = "https://play.google.com/redeem?code=" + redeemCode;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redeemUrl));

        // Start the activity to open the website
        startActivity(intent);
    }

    private void setupClickListeners() {

        binding.thirdButton.setOnClickListener(v -> {
            if (userCoins > maxCoinsRedeem && taskCompleted >= 3) {
                binding.emailBox.setVisibility(View.VISIBLE);
                // binding.rewardView.setVisibility(View.VISIBLE);
                processWithdrawal(maxCoinsRedeem, maxRedeemAmount);
            } else {
                Toast.makeText(getContext(), "You need" + " " + maxCoinsRedeem + " " + "coins to withdraw.", Toast.LENGTH_SHORT).show();
            }
        });
        binding.secondButton.setOnClickListener(v -> {
            if (userCoins > midCoinsRedeem) {
                binding.emailBox.setVisibility(View.VISIBLE);
                // binding.rewardView.setVisibility(View.VISIBLE);
                processWithdrawal(midCoinsRedeem, midRedeemAmount);
            } else {
                Toast.makeText(getContext(), "You need" + " " + midCoinsRedeem + " " + "coins to withdraw.", Toast.LENGTH_SHORT).show();
            }
        });
        binding.firstButton.setOnClickListener(v -> {
            if (userCoins >= minCoinsRedeem) {
                binding.emailBox.setVisibility(View.VISIBLE);
                // binding.rewardView.setVisibility(View.VISIBLE);
                processWithdrawal(minCoinsRedeem, minRedeemAmount);
            } else {
                Toast.makeText(getContext(), "You need" + " " + minCoinsRedeem + " " + "coins to withdraw.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.coinImage1.setOnClickListener(view -> showSnackbar(view, "Complete 2 tasks"));
        binding.coinImage2.setOnClickListener(view -> showSnackbar(view, "Complete 5 tasks"));
        binding.coinImage3.setOnClickListener(view -> showSnackbar(view, "Complete 10 tasks"));

    }

    private void showSnackbar(View view, String message){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireActivity(), android.R.color.holo_orange_light));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.BLACK);
        snackbar.show();
    }

    private void buttonsVisibility() {
        // dim button and stop click on button

        if (userCoins < maxCoinsRedeem) {
            binding.thirdButton.setBackgroundResource(R.drawable.button_4);
            binding.thirdButton.setAlpha(0.5f);
        } else {
            binding.thirdButton.setBackgroundResource(R.drawable.button_2);
            binding.thirdButton.setEnabled(true);
        }

        if (userCoins < midCoinsRedeem) {
            binding.secondButton.setBackgroundResource(R.drawable.button_4);
            binding.secondButton.setAlpha(0.5f);
        } else {
            binding.secondButton.setBackgroundResource(R.drawable.button_2);
            binding.secondButton.setEnabled(true);
        }

        if (userCoins < minCoinsRedeem) {
            binding.firstButton.setBackgroundResource(R.drawable.button_4);
            binding.firstButton.setAlpha(0.5f);
        } else {
            binding.firstButton.setBackgroundResource(R.drawable.button_2);
            binding.firstButton.setEnabled(true);
        }

    }

    private void processWithdrawal(int redeemedCoins, int redeemedAmount) {
        if (userCoins < redeemedCoins) {
            Toast.makeText(getContext(), "You need more Coins to withdraw", Toast.LENGTH_SHORT).show();
        } else {
            String email = binding.emailBox.getText().toString();
            if (email.isEmpty()) {
                binding.emailBox.setError("Please enter your email.");
                binding.emailBox.requestFocus();
                Toast.makeText(getContext(), "Please enter your email.", Toast.LENGTH_SHORT).show();
            } else {
                WithdrawRequest request = new WithdrawRequest(uid, email, user.getName(), userCoins, new Date(), redeemCode, redeemedCoins, redeemedAmount);
                database.collection("withdraws").document(uid).set(request)
                        .addOnSuccessListener(documentReference -> {

                            // Reduce user's coins by the specified amount
                            int updatedCoins = userCoins - redeemedCoins;
                            user.setCoins(updatedCoins);

                            setupClickListeners();
                            checkRedeemCode();

                            // Update the database with the updated coin value
                            database.collection("users").document(uid)
                                    .update("coins", updatedCoins)
                                    .addOnSuccessListener(result -> {
                                        Toast.makeText(getContext(), "Your coins updated.", Toast.LENGTH_SHORT).show();
                                        binding.currentCoins.setText(String.valueOf(updatedCoins));

                                        // Enable buttons again in case user has enough coins for another request
                                        buttonsVisibility();
                                    });

                            Toast.makeText(getContext(), "Request sent successfully. You will get the reward soon.", Toast.LENGTH_SHORT).show();

                        });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove the listener
        if (registration != null) {
            registration.remove();
        }
    }

}
