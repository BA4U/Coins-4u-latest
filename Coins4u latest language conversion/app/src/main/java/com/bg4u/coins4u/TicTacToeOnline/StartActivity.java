package com.bg4u.coins4u.TicTacToeOnline;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class StartActivity extends AppCompatActivity {
    int entryFee = 25; // Set your entry fee value
    boolean isPrivate = false; // Set your private value
    private static final long GAME_START_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds
    
    private DatabaseReference myRef;
    private boolean isCodeGenerated = false;
    private int sessionCode = 0;
    private String currentUserUid, friendUid;
    private String currentUserCoins;
    private final boolean doubleBackToExitPressedOnce = false;
    private AlertDialog dialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Tic Tac Toe Room");
    
        currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(); // This is current player uid
    
        // Retrieve and show data of other player
        fetchAndDisplayFriendData(currentUserUid);
        
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("sessions");
        
        generateCode();
        
        Button sharebtn = findViewById(R.id.share_btn);
        sharebtn.setOnClickListener(v -> {
            if (isCodeGenerated) {
                shareCode();
            }
        });
    }
    
    private void generateCode() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            
                while (!isCodeGenerated) {
                
                    Random random = new Random();
                    int CODE;
                    CODE = random.nextInt(9000) + 1000;
                
                    if(dataSnapshot.child(String.valueOf(CODE)).exists()) {
                        if(dataSnapshot.child(String.valueOf(CODE)).child("p1").exists()) {
                            Calendar c = Calendar.getInstance();
                            if(dataSnapshot.child(String.valueOf(CODE)).child("start_time")
                                    .getValue(Long.class) - c.getTimeInMillis() >= 86400000) {
                                startSession(CODE);
                            }
                        } else {
                            startSession(CODE);
                        }
                    } else {
                        startSession(CODE);
                    }
                }
    
                if (dataSnapshot.child(String.valueOf(sessionCode)).child("p2").exists()) {
                    friendUid = dataSnapshot.child(String.valueOf(sessionCode)).child("p2").getValue(String.class);
                    Toast.makeText(getBaseContext(), "Game Started!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getBaseContext(), GameActivity.class)
                            .putExtra("session_code", String.valueOf(sessionCode))
                            .putExtra("my_player", "X")
                            .putExtra("friendUid", friendUid)       // Pass the friend user's UID
                            .putExtra("currentUserUid", currentUserUid)     // Pass the current user's UID
                    );
                    finish();
                }
            }
        
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w("Cancelled", "Failed to read value.", error.toException());
            }
        });
    
    }
    
    private void startGameActivity(int sessionCode, String myPlayer) {
        Toast.makeText(getBaseContext(), "Game Started!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getBaseContext(), GameActivity.class)
                .putExtra("session_code", String.valueOf(sessionCode))
                .putExtra("my_player", myPlayer)
                .putExtra("friendUid", friendUid)       // Pass the friend user's UID
                .putExtra("currentUserUid", currentUserUid)     // Pass the current user's UID
        );
        finish();
    }
    
    private void startSession(int CODE) {
        if (currentUserUid != null) {
            isCodeGenerated = true;
            sessionCode = CODE;
            myRef.child(String.valueOf(CODE)).child("p1").setValue(currentUserUid);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date());
    
            myRef.child(String.valueOf(CODE)).child("start_time").setValue(formattedDate);
            myRef.child(String.valueOf(CODE)).child("start_time").setValue(formattedDate);
            
            // Add new data entry fee and private room or not
            myRef.child(String.valueOf(CODE)).child("entry_fee").setValue(entryFee);
            myRef.child(String.valueOf(CODE)).child("is_private").setValue(isPrivate);
    
            TextView textView = findViewById(R.id.code_text);
            textView.setText(String.valueOf(CODE));
            
            // Start a timer to check if the game has started
            // startGameStartTimer();
            
            checkGameStarted();
        }
    }
    
    private void startGameStartTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isCodeGenerated) {
                // Game hasn't started within the timeout, delete the session
                deleteSession();
            }
        }, GAME_START_TIMEOUT);
    }
    
    private void checkGameStarted() {
        myRef.child(String.valueOf(sessionCode)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("p2").exists()) {
                    String player1Uid = dataSnapshot.child("p1").getValue(String.class);
                    String player2Uid = dataSnapshot.child("p2").getValue(String.class);
                    
                    // Determine the player roles
                    String myPlayer;
                    String opponentPlayer;
                    if (Objects.requireNonNull(player1Uid).equals(currentUserUid)) {
                        myPlayer = "X";
                        opponentPlayer = "O";
                    } else {
                        myPlayer = "O";
                        opponentPlayer = "X";
                    }
                    
                    startGameActivity(sessionCode, myPlayer);
                    finishAffinity();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w("Cancelled", "Failed to read value.", error.toException());
            }
        });
    }
    
    private void deleteSession() {
        if (isCodeGenerated) {
            myRef.child(String.valueOf(sessionCode)).removeValue();
            Toast.makeText(this, "Game session deleted ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void shareCode() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Tic-Tac-Toe Friends Code");
        
        String appDownloadLink = "https://play.google.com/store/apps/details?id=com.bg4u.coins4u"; // Replace with your app's Play Store link
        String shareMessage = "Hey! Let's play Tic-Tac-Toe game Online in Coins 4u. Join my game with this code: " + sessionCode
                + "\n\nDownload the app from here: " + appDownloadLink;
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
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
                            TextView player1 = findViewById(R.id.player1);
                            currentUserCoins = userCoins;
                            ImageView player1Pic = findViewById(R.id.player1Pic);
                            
                            player1.setText(userName);
                            
                            if (userProfile != null) {
                                // Load and display the profile picture using the URL
                                Picasso.get().load(userProfile).into(player1Pic);
                            } else {
                                // Handle the case where profilePicUrl is null, load default pic
                                Picasso.get().load(R.drawable.easy_bot).into(player1Pic);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle the failure to fetch user data
                    Toast.makeText(this, "Failed to fetch friends data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to leave this room?")
                .setTitle("Delete Room")
                .setIcon(R.drawable.logout)
                .setPositiveButton("Yes", (dialog, id) -> {
                    deleteSession();
                    finish(); // Finish activities in the stack
                })
                .setNegativeButton("No", (dialog, id) -> {
                    // User cancelled the dialog, do nothing
                })
                .setOnCancelListener(dialog -> {
                    // Handle the back button press properly
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    
        super.onBackPressed();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteSession();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
}