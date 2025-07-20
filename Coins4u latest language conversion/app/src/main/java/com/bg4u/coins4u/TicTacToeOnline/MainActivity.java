package com.bg4u.coins4u.TicTacToeOnline;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bg4u.coins4u.ClickSoundHelper;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.TicTacToeModel;
import com.bg4u.coins4u.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    // Declare variables
    private int currentUserCoins;
    private Button onlineBtn, startBtn, joinBtn;
    private TextView onlineWin, onlineLost, onlineDraw;
    private final int Min_Balance = 200;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tictactoe_online);
    
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Play Online");
        
        onlineBtn = findViewById(R.id.onlinebtn);

        startBtn = findViewById(R.id.startbtn);
        joinBtn = findViewById(R.id.joinbtn);
        
        onlineWin = findViewById(R.id.onlineWin);
        onlineDraw= findViewById(R.id.onlineDraw);
        onlineLost= findViewById(R.id.onlineLost);
    
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(); // This is current player uid
    
        fetchAndDisplayFriendData(currentUserUid);
        
        loadAndDisplayOnlineData();
        
        checkMinimumCoins();
        
        // Set click listeners for startBtn and join buttons
        
        onlineBtn.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();

                startActivity(new Intent(this, FiveOnlineActivity.class));
        });

        startBtn.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            // Check minimum coins before allowing the user to join
            if (currentUserCoins >= Min_Balance) {
                startActivity(new Intent(this, StartActivity.class));
            } else {
                startBtn.setAlpha(0.5f);
                Toast.makeText(this, "You need more than " + Min_Balance + " coins.", Toast.LENGTH_SHORT).show();
            }
        });
        
        joinBtn.setOnClickListener(v -> {
            ClickSoundHelper.playClickSound();
            // Check minimum coins before allowing the user to join
            if (currentUserCoins >= Min_Balance) {
                startActivity(new Intent(this, JoinActivity.class));
            } else {
                joinBtn.setAlpha(0.5f);
                Toast.makeText(this, "You need more than " + Min_Balance + " coins.", Toast.LENGTH_LONG).show();
            }
        });

        // Get the OnBackPressedDispatcher
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // Add a callback to handle the back button press
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event here
                finish();
            }
        });
    }

    private void checkMinimumCoins() {
        if(currentUserCoins < Min_Balance){
            startBtn.setAlpha(0.5f);
           // Toast.makeText(this, "You need more than" + " " + Min_Balance + " "  + "coins." , Toast.LENGTH_LONG).show();
        } else {
            startBtn.setAlpha(1f);
        }
    }
    
    private void fetchAndDisplayFriendData(String userUID) {
        FirebaseFirestore.getInstance().collection("users")
                .document(userUID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // You can access the user data here
                            String userName = user.getName();
                            String userCoins = String.valueOf(user.getCoins());
                            String userProfile = user.getProfile();
                            
                            // Display the user data as needed, e.g., set it in TextViews and ImageView
                            
                            currentUserCoins = user.getCoins();
                            checkMinimumCoins(); // Call checkMinimumCoins here
                            
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle the failure to fetch user data
                    Toast.makeText(this, "Failed to fetch friends data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadAndDisplayOnlineData() {
        DocumentReference easyDocRef = FirebaseFirestore.getInstance().collection("TicTacToe").document("Online");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel onlineData = documentSnapshot.toObject(TicTacToeModel.class);
                if (onlineData != null) {
                    // Bind data to the UI views for Easy level using ViewBinding
                    onlineWin.setText(String.valueOf(onlineData.getWin()));
                    onlineDraw.setText(String.valueOf(onlineData.getDraw()));
                    onlineLost.setText(String.valueOf(onlineData.getLost()));
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click here
            finish(); // Finish the current activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
