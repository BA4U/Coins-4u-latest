package com.bg4u.coins4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityTicTacToeBinding;
import com.bg4u.coins4u.TicTacToeOnline.MainActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class TicTacToeActivity extends AppCompatActivity implements View.OnClickListener {

    private OnBackPressedCallback onBackPressedCallback;
    private FirebaseFirestore database;
    private ActivityTicTacToeBinding binding; // Declare the ViewBinding object
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicTacToeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.tic_tac_toe);
    
        database = FirebaseFirestore.getInstance();
        
        // Assuming you have initialized Firebase in your app
//        TicTacToeModel easyData = new TicTacToeModel(); // Example data
//        TicTacToeModel mediumData = new TicTacToeModel(); // Example data
//        TicTacToeModel hardData = new TicTacToeModel(); // Example data
//        TicTacToeModel onlineData = new TicTacToeModel(); // Example data
    
        // Load and display data for each level
        loadAndDisplayEasyData();
        loadAndDisplayMediumData();
        loadAndDisplayHardData();
        loadAndDisplayOnlineData();
    
        Button easyButton = binding.easyButton;
        Button mediumButton = binding.mediumButton;
        Button hardButton = binding.hardButton;

        Button onlineButton = binding.onlineTicTacToe;

        Button unbeatableButton = binding.unbeatableTicTacToe;

        easyButton.setOnClickListener(this);
        mediumButton.setOnClickListener(this);
        hardButton.setOnClickListener(this);
        
        onlineButton.setOnClickListener(this);

        unbeatableButton.setOnClickListener(this);

        // Handle back button press with onBackPressedDispatcher
        // Get the OnBackPressedDispatcher from the activity
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // Add a callback to the dispatcher
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Take the user to the MainActivity
                Intent intent = new Intent(TicTacToeActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Finish activities in the stack
            }
        };
        dispatcher.addCallback(onBackPressedCallback);
    }
    
    @Override
    public void onClick(View v) {
        // Determine which button was clicked and start the corresponding activity
        if (v.getId() == R.id.easyButton) {
            startActivity(new Intent(this, EasyActivity.class));
        } else if (v.getId() == R.id.mediumButton) {
        //    showInterstitialAd();  // Show ads before going to medium activity
            startActivity(new Intent(this, MediumActivity.class));
        } else if (v.getId() == R.id.hardButton) {
            startActivity(new Intent(this, HardActivity.class));
        } else if (v.getId() == R.id.onlineTicTacToe) {
        //    showInterstitialAd();  // Show ads before going to medium activity
            startActivity(new Intent(this, MainActivity.class));
        }else if (v.getId() == R.id.unbeatableTicTacToe) {
        //    showInterstitialAd();  // Show ads before going to medium activity
            startActivity(new Intent(this, UnbeatableActivity.class));
        }
    }
    
    private void loadAndDisplayEasyData() {
        DocumentReference easyDocRef = database.collection("TicTacToe").document("Easy");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel easyData = documentSnapshot.toObject(TicTacToeModel.class);
                if (easyData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.easyWin.setText(String.valueOf(easyData.getWin()));
                    binding.easyDraw.setText(String.valueOf(easyData.getDraw()));
                    binding.easyLost.setText(String.valueOf(easyData.getLost()));
                }
            }
        });
    }
    
    private void loadAndDisplayMediumData() {
        DocumentReference easyDocRef = database.collection("TicTacToe").document("Medium");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel mediumData = documentSnapshot.toObject(TicTacToeModel.class);
                if (mediumData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.mediumWin.setText(String.valueOf(mediumData.getWin()));
                    binding.mediumDraw.setText(String.valueOf(mediumData.getDraw()));
                    binding.mediumLost.setText(String.valueOf(mediumData.getLost()));
                }
            }
        });
    }
    
    private void loadAndDisplayHardData() {
        DocumentReference easyDocRef = database.collection("TicTacToe").document("Hard");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel hardData = documentSnapshot.toObject(TicTacToeModel.class);
                if (hardData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.hardWin.setText(String.valueOf(hardData.getWin()));
                    binding.hardDraw.setText(String.valueOf(hardData.getDraw()));
                    binding.hardLost.setText(String.valueOf(hardData.getLost()));
                }
            }
        });
    }
    
    private void loadAndDisplayOnlineData() {
        DocumentReference easyDocRef = database.collection("TicTacToe").document("Online");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel onlineData = documentSnapshot.toObject(TicTacToeModel.class);
                if (onlineData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    binding.onlineWin.setText(String.valueOf(onlineData.getWin()));
                    binding.onlineDraw.setText(String.valueOf(onlineData.getDraw()));
                    binding.onlineLost.setText(String.valueOf(onlineData.getLost()));
                }
            }
        });
    }
    
    // Use this method to show the interstitial ad

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Get the OnBackPressedDispatcher from the activity
            OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

            // Add a callback to the dispatcher
            dispatcher.addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Handle the back button click here
                    // For example, you can finish the activity
                     finish();
                }
            });

            // Trigger the callback
            dispatcher.onBackPressed();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
