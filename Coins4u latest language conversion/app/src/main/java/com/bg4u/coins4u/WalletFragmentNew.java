package com.bg4u.coins4u;

import android.annotation.SuppressLint;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.databinding.FragmentWalletNewBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;

public class WalletFragmentNew extends Fragment implements RewardAdapter.OnRewardClickListener {

    // Declare variables
    FragmentWalletNewBinding binding;
    FirebaseFirestore database;
    private ListenerRegistration registration;
    private User user;
    private String uid;
    private int userCoins;
    private int taskCompleted;
    private String redeemCode;
    private final int minCoinsRedeem = 50000;
    private final int minRedeemAmount = 15;
    private final int midCoinsRedeem = 150000;
    private final int midRedeemAmount = 50;
    private final int maxCoinsRedeem = 250000;
    private final int maxRedeemAmount = 100;
    private RewardAdapter rewardAdapter;
    private ArrayList<RewardModel> rewards;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentWalletNewBinding.inflate(inflater, container, false);
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

        // Set up click listeners and fetch user data
        fetchUserData();

        // Set up RecyclerView for rewards
        RecyclerView recyclerView = binding.rewardList;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rewards = new ArrayList<>();
        rewardAdapter = new RewardAdapter(getContext(), rewards, this, userCoins, taskCompleted);
        recyclerView.setAdapter(rewardAdapter);

        return rootView;
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
                            //    buttonsVisibility();

                            // Notify the adapter of the updated userCoins and taskCompleted values
                            rewardAdapter = new RewardAdapter(getContext(), rewards, this, userCoins, taskCompleted);
                            binding.rewardList.setAdapter(rewardAdapter);

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

    @Override
    public void onRewardButtonClick(RewardModel rewardModel) {
        if (userCoins >= rewardModel.getRewardLostCoin()) {
            // User has enough coins
            // Change button drawable and make it clickable
            binding.emailBox.setVisibility(View.VISIBLE);
            // binding.rewardView.setVisibility(View.VISIBLE);
            processWithdrawal(rewardModel.getRewardLostCoin(), rewardModel.getRewardAmount());
            Toast.makeText(getContext(), "Reward claimed!", Toast.LENGTH_SHORT).show();
            processReward(rewardModel);
        } else {
            // User does not have enough coins
            Toast.makeText(getContext(), "Not enough coins!", Toast.LENGTH_SHORT).show();
        }
    }

    private void processReward(RewardModel rewardModel) {
        // Handle the reward processing logic here
        // For example, deduct coins, update UI, etc.
        userCoins -= rewardModel.getRewardLostCoin();
        binding.currentCoins.setText(String.valueOf(userCoins));
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

                            checkRedeemCode();

                            // Update the database with the updated coin value
                            database.collection("users").document(uid)
                                    .update("coins", updatedCoins)
                                    .addOnSuccessListener(result -> {
                                        Toast.makeText(getContext(), "Your coins updated.", Toast.LENGTH_SHORT).show();
                                        binding.currentCoins.setText(String.valueOf(updatedCoins));

                                        // Enable buttons again in case user has enough coins for another request
                                        // buttonsVisibility();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        // Set up RecyclerViews and adapters
        binding.rewardList.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rewards = new ArrayList<>(); // Initialize rewards list here
        rewardAdapter = new RewardAdapter(getContext(), rewards, this, userCoins, taskCompleted); // Initialize adapter here
        binding.rewardList.setAdapter(rewardAdapter);

        // Retrieve categories and promotions from Firestore
        retrieveRewards();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void retrieveRewards() {
        database.collection("rewards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rewards.clear();

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        RewardModel model = snapshot.toObject(RewardModel.class);
                        if (model != null) {
                            model.setRewardId(snapshot.getId());
                            rewards.add(model);
                        }
                    }

                    rewardAdapter.notifyDataSetChanged();

                });
    }


    private void showSnackbar(View view, String message){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireActivity(), android.R.color.holo_orange_light));
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(Color.BLACK);
        snackbar.show();
    }
}
