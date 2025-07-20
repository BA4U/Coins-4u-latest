package com.bg4u.coins4u;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FreefireActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TournamentAdapter tournamentAdapter;
    private FirebaseFirestore db;
    private List<TournamentModel> tournamentList;
    private boolean isPaymentEnabled;
    private String userUid;
    private String userName;
    private String gameName;
    private String tournamentId;
    private String gameUID;
    private int userCoins;
    private int coinsToDeduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freefire);

        initializeViews();
        setupToolbar();
        setupBackPressHandler();
        initializeFirestore();
        loadUserData();
        fetchTournamentsFromFirestore();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tournamentList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Tournaments");
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void initializeFirestore() {
        db = FirebaseFirestore.getInstance();
        userUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    private void loadUserData() {
        db.collection("users").document(userUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userCoins = documentSnapshot.getLong("coins").intValue();
                        userName = documentSnapshot.getString("name");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading user data", e));
    }

    private void fetchTournamentsFromFirestore() {
        db.collection("tournaments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tournamentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        TournamentModel tournament = document.toObject(TournamentModel.class);
                        tournament.setTournamentId(document.getId());
                        tournamentList.add(tournament);
                    }
                    setupRecyclerView();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching tournaments", e);
                    Toast.makeText(this, "Error fetching tournaments", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRecyclerView() {
        tournamentAdapter = new TournamentAdapter(this, tournamentList);
        recyclerView.setAdapter(tournamentAdapter);

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                TextView entryFeeBtn = view.findViewById(R.id.entryFeeBtn);
                if (entryFeeBtn != null) {
                    entryFeeBtn.setOnClickListener(v -> {
                        int position = recyclerView.getChildAdapterPosition(view);
                        if (position != RecyclerView.NO_POSITION) {
                            handleEntryFeeClick(tournamentList.get(position));
                        }
                    });
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // Not needed
            }
        });
    }

    private void handleEntryFeeClick(TournamentModel tournament) {
        if (tournament.getFilledSlot() >= tournament.getTotalSlot()) {
            Toast.makeText(this, "Tournament is full!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userCoins < tournament.getEntryFee()) {
            Toast.makeText(this, "Insufficient coins to join the tournament.", Toast.LENGTH_SHORT).show();
            return;
        }
        loadPaymentSatus(tournament);
        showJoinDialog(tournament);
    }

    private void loadPaymentSatus(TournamentModel tournament) {
        db.collection("tournaments").document(tournament.getTournamentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isPaymentEnabled = documentSnapshot.exists() &&
                            Boolean.TRUE.equals(documentSnapshot.getBoolean("paid"));
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading payment settings", e);
                    isPaymentEnabled = false; // Default to free tournaments on error
                });
    }


    private void showJoinDialog(TournamentModel tournament) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_tournament, null);
        EditText nameInput = dialogView.findViewById(R.id.nameInput);
        EditText uidInput = dialogView.findViewById(R.id.uidInput);
        TextView confirmButton = dialogView.findViewById(R.id.confirmButton);

        DialogBox dialogBox = new DialogBox(this, dialogView);
        Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialogBox.setCancelable(true);

        confirmButton.setOnClickListener(v -> {
            gameName = nameInput.getText().toString().trim();
            gameUID = uidInput.getText().toString().trim();

            if (gameName.isEmpty() || gameUID.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                processRegistration(tournament, gameName, gameUID, dialogBox);
                Toast.makeText(this, "Process registeration", Toast.LENGTH_SHORT).show();
            // Handle the registration process here    dialogBox.dismiss();
            }
        });

        dialogBox.show();
    }

    private void processRegistration(TournamentModel tournament, String name, String uid, DialogBox dialogBox) {
        double entryFee = tournament.getEntryFee();
        coinsToDeduct = (int) entryFee;

        // Set the tournamentId globally
        // Declare a global variable to store the tournamentId
        tournamentId = tournament.getTournamentId();

        if (isPaymentEnabled) {
            triggerPaymentFlow(tournament, entryFee, name, uid, dialogBox);
        } else {
            deductUserCoins(entryFee);
            submitParticipantData(tournamentId, gameName, gameUID);
            dialogBox.dismiss();
        }
    }

    private void triggerPaymentFlow(TournamentModel tournament, double entryFee, String name, String uid, DialogBox dialogBox) {
        Intent paymentIntent = new Intent(this, PaymentActivity.class);
        paymentIntent.putExtra("AMOUNT", entryFee);
        paymentIntent.putExtra("Game Name", name);
        paymentIntent.putExtra("Game UID", uid);
        paymentIntent.putExtra("TournamentId", tournament.getTournamentId());  // Pass only tournamentId
        paymentLauncher.launch(paymentIntent);

        dialogBox.dismiss();
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
                double amountPaid = data.getDoubleExtra("AMOUNT", 0.00);
                String name = data.getStringExtra("Game Name");
                String uid = data.getStringExtra("Game UID");
            //    String tournamentId = data.getStringExtra("TournamentId");  // Get tournamentId

                if (tournamentId!= null) {
                    deductUserCoins(amountPaid);
                    submitParticipantData(tournamentId, gameName, gameUID);
                }

                // Show a popup with a Lottie animation
                dialogBox(getString(R.string.payment_successful), "Tournament Joined!" , R.raw.heart_zoom_in_out);

            } else {
                // Payment failed or canceled, keep user as free user
                // Show a popup with a Lottie animation
                dialogBox(getString(R.string.registration_failed), "Please try again .", R.raw.payment_online_animation);
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Payment was canceled
            // Show a popup with a Lottie animation
            dialogBox(getString(R.string.registration_failed), "Please try again ..", R.raw.payment_online_animation);
        } else {
            // Payment failed or unknown error
            // Show a popup with a Lottie animation
            dialogBox(getString(R.string.registration_failed), "Please try again ...", R.raw.payment_online_animation);
        }
    }

    private void deductUserCoins(double entryFee) {
        userCoins -= entryFee;
        updateUserCoinsInFirestore();
    }

    private void updateUserCoinsInFirestore() {
        db.collection("users").document(userUid)
                .update("coins", FieldValue.increment(-coinsToDeduct))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating user coins", e));
    }

    private void submitParticipantData(String tournamentID, String name, String uid) {
        Map<String, Object> participantData = new HashMap<>();
        participantData.put("userId", userUid);
        participantData.put("Coins 4u User Name", userName);
        participantData.put("Game Name", name);
        participantData.put("Game UID", uid);
        participantData.put("Registration Time", FieldValue.serverTimestamp());
        // Get current time in milliseconds for the document name
        long currentTimeMillis = System.currentTimeMillis();

        String documentName = name + "_"  + userName + "_" + currentTimeMillis;

        db.collection("tournaments")
                .document(tournamentID)
                .collection("participants")
                .document(documentName)
                .set(participantData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully joined the tournament!", Toast.LENGTH_SHORT).show();
                    updateFilledSlots(tournamentID);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error submitting participant data", e);
                    Toast.makeText(this, "Failed to join. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFilledSlots(String tournamentID) {
        db.collection("tournaments")
                .document(tournamentID)
                .update("filledSlot", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> fetchTournamentsFromFirestore())
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating filled slots", e));
    }

    private void dialogBox(String title, String body, int animationRes) {
        if (!isFinishing() && !isDestroyed()) {
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(this).inflate(R.layout.dialog_box_layout, null);

            // Set the title and body text directly on the custom layout
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(body);

            // Set the animation directly on the LottieAnimationView
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(animationRes);

            // Create an instance of the custom dialog and pass the custom layout as a parameter
            DialogBox dialogBox = new DialogBox(this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;

            // Set the left button action
            dialogBox.setLeftButton("Home", v -> {
                // Handle left button click
                dialogBox.dismiss();
                startActivity(new Intent(this, MainActivity.class));
            });

            // Set the right button action
            dialogBox.setRightButton("Okay", v -> {
                // Handle right button click
                dialogBox.dismiss();
            });

            // Show the dialog
            dialogBox.show();
        }
    }

    // Method to display toast messages
    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    

}
