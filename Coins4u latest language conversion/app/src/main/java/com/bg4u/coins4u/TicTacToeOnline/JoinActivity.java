package com.bg4u.coins4u.TicTacToeOnline;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JoinActivity extends AppCompatActivity {
    private static final long ONE_DAY_IN_MILLIS = 86400000;
    private DatabaseReference myRef;
    private String currentUserUid, friendUid;
    private DataSnapshot dataSnapshot;
    private ProgressDialog pd;
    private RecyclerView recyclerView;
    private ValueEventListener availableMatchesListener;
    private List<MatchModel> availableMatches;
    private FirebaseAuth mAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Join Game");

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

        mAuth = FirebaseAuth.getInstance();
        currentUserUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        
        // Retrieve and show data of other player
        fetchAndDisplayFriendData(currentUserUid);
        
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("sessions");
        
        recyclerView = findViewById(R.id.onlineTicTacToeMatchList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        availableMatches = new ArrayList<>();
        
        availableMatchesListener = myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dataSnapshot = snapshot;
                updateMatchesList();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w("Cancelled", "Failed to read value.", error.toException());
            }
        });
        
        Button submitBtn = findViewById(R.id.submitCode);
        final EditText codeEditText = findViewById(R.id.enteredCode);
        
        submitBtn.setOnClickListener(v -> {
            String enteredCode = codeEditText.getText().toString().trim();
            if (validateCode(enteredCode)) {
                showProgressDialog(); // Show a loading dialog while processing
                handleValidCode(enteredCode);
            } else {
                Toast.makeText(getBaseContext(), "Invalid Code!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateMatchesList() {
        hideProgressDialog();
        availableMatches.clear();
        
        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
            if (!childSnapshot.child("p2").exists()) {
                // Retrieve necessary information from childSnapshot
                String code = childSnapshot.getKey();
                String player1Uid = childSnapshot.child("p1").getValue(String.class);
                
                // Fetch the user's data from the database
                FirebaseFirestore.getInstance().collection("users").document(player1Uid).addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        
                        if (user != null) {
                            // Create a MatchModel instance and add it to availableMatches
                            MatchModel match = new MatchModel();
                            match.setCode(code);
                            match.setFriendUid(player1Uid);
                            match.setCurrentUserUid(currentUserUid);
                            match.setUserName(user.getName());
                            match.setUserProfilePic(user.getProfile());
                            availableMatches.add(match);
                            
                            // Update the RecyclerView with availableMatches
                            MatchAdapter adapter = new MatchAdapter(JoinActivity.this, availableMatches);
                            recyclerView.setAdapter(adapter);
                        } else {
                            Log.e("User Data Error", "User data is null");
                        }
                    }
                });
            }
        }
    }
    
    private boolean validateCode(String code) {
        return code.length() == 4 && dataSnapshot.child(code).exists();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (availableMatchesListener != null) {
            myRef.removeEventListener(availableMatchesListener);
        }
        hideProgressDialog();
    }
    
    private void showProgressDialog() {
        if (pd == null) {
            pd = new ProgressDialog(JoinActivity.this);
            pd.setCancelable(false);
            pd.setCanceledOnTouchOutside(false);
            pd.setMessage("Please wait");
        }
        pd.show();
    }
    
    private void hideProgressDialog() {
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }
    
    private void handleValidCode(String code) {
        if (dataSnapshot.child(code).child("p2").exists()) {
            hideProgressDialog(); // Hide loading dialog
            Toast.makeText(this, "The Game has already started. Please generate a new code.", Toast.LENGTH_LONG).show();
        } else {
            if (currentUserUid != null) {
                friendUid = dataSnapshot.child(code).child("p1").getValue(String.class);
                
                if (currentUserUid.equals(friendUid)) {
                    hideProgressDialog(); // Hide loading dialog
                    Toast.makeText(this, "You can't join a game started by you.", Toast.LENGTH_LONG).show();
                } else {
                    myRef.child(code).child("p2").setValue(currentUserUid);
                    hideProgressDialog(); // Hide loading dialog
                    Toast.makeText(this, "Game Started!", Toast.LENGTH_LONG).show();
                    
                    // Pass necessary information to the GameActivity
                    Intent intent = new Intent(getBaseContext(), GameActivity.class);
                    intent.putExtra("session_code", code);
                    intent.putExtra("my_player", "O");
                    intent.putExtra("friendUid", friendUid);         // Pass the friend user's UID
                    intent.putExtra("currentUserUid", currentUserUid); // Pass the current user's UID
                    startActivity(intent);
                    
                    finishAffinity();
                }
            } else {
                // Handle the case where current user UID is null
                hideProgressDialog(); // Hide loading dialog
                Toast.makeText(getBaseContext(), "Unable to retrieve current user UID.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void fetchAndDisplayFriendData(String userUID) {
        FirebaseFirestore.getInstance().collection("users")
                .document(userUID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to fetch friends data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        
                        if (user != null) {
                            // You can access the user data here
                            String userName = user.getName();
                            String userCoins = String.valueOf(user.getCoins());
                            String userProfile = user.getProfile();
                            
                            // Display the user data as needed, e.g., set it in TextViews and ImageView
                            TextView player1 = findViewById(R.id.player1);
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