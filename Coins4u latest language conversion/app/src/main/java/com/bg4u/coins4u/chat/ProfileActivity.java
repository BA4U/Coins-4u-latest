package com.bg4u.coins4u.chat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityProfileBinding;
import com.bg4u.coins4u.SubscriptionAdapter;
import com.bg4u.coins4u.SubscriptionModel;
import com.bg4u.coins4u.User;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
    private String receiverId, senderUserId, currentState;
    private Button requestButton, declineButton;
    private ActivityProfileBinding binding;
    private User receiver;
    private boolean isPremiumUser;
    private DatabaseReference chatRequestRef, contactsRef, notificationRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.friend_profile);
        
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        senderUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        
        receiverId = Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id").toString();
        
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        database.collection("users")
                .document(receiverId)
                .get()
                .addOnSuccessListener(this::processReceiverDocument)
                .addOnFailureListener(e -> displayErrorMessage("Failed to fetch user data: " + e.getMessage()));
    
        requestButton = binding.sendMessageRequestButton;
        declineButton = binding.declineMessageRequestButton;
        currentState = "new";
        
        if (senderUserId.equals(receiverId)) {
            requestButton.setVisibility(View.INVISIBLE);
        } else {
            requestButton.setVisibility(View.VISIBLE);
        }
        
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        
        ManageChatRequest();
    }
    
    private void processReceiverDocument(DocumentSnapshot document) {
        if (document.exists()) {
            receiver = document.toObject(User.class);
            if (receiver != null) {
                isPremiumUser = receiver.isSubscription();
                loadReceiverData();
                displayPremiumAnimation();
            }
        } else {
            displayErrorMessage("Receiver data not found");
        }
    }
    
    private void displayPremiumAnimation() {
        if (isPremiumUser && (receiver.isPremiumPlan() || receiver.isStandardPlan() || receiver.isBasicPlan())) {
            binding.premiumIcon.setVisibility(View.VISIBLE);
            binding.premiumIcon.setAnimation(R.raw.premium_gold_icon);
            binding.premiumIcon.playAnimation();
            
            SubscriptionModel subscriptionPlan = null;
            if (receiver.isPremiumPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
            } else if (receiver.isStandardPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
            } else if (receiver.isBasicPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
            }
    
            assert subscriptionPlan != null;
            int avatarLottieResId = subscriptionPlan.getAvatarLottieResId();
            binding.premiumAvatar.setVisibility(View.VISIBLE);
            binding.premiumAvatar.setAnimation(avatarLottieResId);
            binding.premiumAvatar.playAnimation();
        } else {
            binding.premiumIcon.setVisibility(View.GONE);
            binding.premiumAvatar.setVisibility(View.GONE);
        }
    }
    
    private void loadReceiverData() {
        binding.visitUserName.setText(receiver.getName());
        binding.visitStatus.setText(receiver.getBio());
        binding.userLocation.setText(receiver.getLocation());
        binding.currentCoins.setText(String.valueOf(receiver.getCoins()));
        binding.correctAns.setText(String.valueOf(receiver.getCorrectAnswers()));
        binding.wrongAns.setText(String.valueOf(receiver.getWrongAnswers()));
        
        // Binding number of win, draw, and lost for Tic Tac Toe
        binding.easyWin.setText(String.valueOf(receiver.getEasyWin()));
        binding.easyDraw.setText(String.valueOf(receiver.getEasyDraw()));
        binding.easyLost.setText(String.valueOf(receiver.getEasyLost()));
        
        binding.mediumWin.setText(String.valueOf(receiver.getMediumWin()));
        binding.mediumDraw.setText(String.valueOf(receiver.getMediumDraw()));
        binding.mediumLost.setText(String.valueOf(receiver.getMediumLost()));
        
        binding.hardWin.setText(String.valueOf(receiver.getHardWin()));
        binding.hardDraw.setText(String.valueOf(receiver.getHardDraw()));
        binding.hardLost.setText(String.valueOf(receiver.getHardLost()));
    
        binding.onlineWin.setText(String.valueOf(receiver.getOnlineWin()));
        binding.onlineDraw.setText(String.valueOf(receiver.getOnlineDraw()));
        binding.onlineLost.setText(String.valueOf(receiver.getOnlineLost()));
    
        binding.taskCompleted.setText(String.valueOf(receiver.getTaskCompleted()));
        
        Context context = ProfileActivity.this;
        if (receiver.getProfile() != null && !receiver.getProfile().isEmpty()) {
            Glide.with(context)
                    .load(receiver.getProfile())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            binding.visitProfileImage.setImageDrawable(resource);
                        }
                        
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        } else {
            binding.visitProfileImage.setImageResource(R.drawable.user_icon_default);
        }
    }
    
    private void displayErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // ... Previous code ...
    
    private void ManageChatRequest() {
        chatRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(receiverId)) {
                    String requestType = dataSnapshot.child(receiverId).child("request_type").getValue(String.class);
                    if ("sent".equals(requestType)) {
                        currentState = "request_sent";
                        requestButton.setText("Cancel Chat Request");
                    } else if ("received".equals(requestType)) {
                        currentState = "request_received";
                        requestButton.setText("Accept Chat Request");
                        
                        declineButton.setVisibility(View.VISIBLE);
                        declineButton.setEnabled(true);
                        declineButton.setOnClickListener(v -> CancelChatRequest());
                    }
                } else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(receiverId)) {
                                currentState = "friends";
                                requestButton.setText("Remove this contact");
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle onCancelled
                        }
                    });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled
            }
        });
        
        requestButton.setOnClickListener(v -> {
            requestButton.setEnabled(false);
            
            if ("new".equals(currentState)) {
                SendChatRequest();
            } else if ("request_sent".equals(currentState)) {
                CancelChatRequest();
            } else if ("request_received".equals(currentState)) {
                AcceptChatRequest();
            } else if ("friends".equals(currentState)) {
                RemoveSpecificChatRequest();
            }
        });
    }
    
    private void RemoveSpecificChatRequest() {
        contactsRef.child(senderUserId).child(receiverId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactsRef.child(receiverId).child(senderUserId)
                                .removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        requestButton.setEnabled(true);
                                        requestButton.setText("Send Request");
                                        currentState = "new";
                                        
                                        declineButton.setVisibility(View.GONE);
                                        declineButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }
    
    private void AcceptChatRequest() {
        contactsRef.child(senderUserId).child(receiverId)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactsRef.child(receiverId).child(senderUserId)
                                .child("Contacts").setValue("Saved")
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        chatRequestRef.child(senderUserId).child(receiverId)
                                                .removeValue()
                                                .addOnCompleteListener(task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        chatRequestRef.child(receiverId).child(senderUserId)
                                                                .removeValue()
                                                                .addOnCompleteListener(task3 -> {
                                                                    requestButton.setEnabled(true);
                                                                    currentState = "friends";
                                                                    requestButton.setText("Remove this contact");
                                                                    requestButton.setBackgroundResource(R.drawable.button_4);
                                                                    
                                                                    declineButton.setVisibility(View.GONE);
                                                                    declineButton.setEnabled(false);
                                                                });
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }
    
    private void CancelChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestRef.child(receiverId).child(senderUserId)
                                .removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        requestButton.setEnabled(true);
                                        requestButton.setText("Send Request");
                                        currentState = "new";
                                        
                                        declineButton.setVisibility(View.GONE);
                                        declineButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }
    
    private void SendChatRequest() {
        chatRequestRef.child(senderUserId).child(receiverId).child("request_type")
                .setValue("sent")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestRef.child(receiverId).child(senderUserId)
                                .child("request_type").setValue("received")
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        HashMap<String, String> chatNotificationMap = new HashMap<>();
                                        chatNotificationMap.put("from", senderUserId);
                                        chatNotificationMap.put("type", "request");
                                        notificationRef.child(receiverId).push().setValue(chatNotificationMap)
                                                .addOnCompleteListener(task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        requestButton.setEnabled(true);
                                                        currentState = "request_sent";
                                                        declineButton.setVisibility(View.VISIBLE);
                                                        requestButton.setVisibility(View.GONE);
                                                        
                                                    //    requestButton.setText("Cancel Chat Request");
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click here
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
