package com.bg4u.coins4u.chat;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bg4u.coins4u.AdsModel;
import com.bg4u.coins4u.HardActivity;
import com.bg4u.coins4u.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private OnBackPressedCallback onBackPressedCallback;
    private RewardedAd rewardedAd;
    private AdView mAdView;
    private FirebaseFirestore database;
    
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";
    private static final String USERS = "Users";
    private static final String USER_STATE = "userState";
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);
    
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlue));
    
        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    
        // Show the status bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // adsData();
        
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    
        Toolbar toolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Chat 4u");
    
        ViewPager2 myviewPager = findViewById(R.id.main_tabs_pager);
        TabsAccessorAdapter mytabsAccessorAdapter = new TabsAccessorAdapter(this);
        myviewPager.setAdapter(mytabsAccessorAdapter);


    
        TabLayout mytabLayout = findViewById(R.id.main_tabs);
        new TabLayoutMediator(mytabLayout, myviewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Find Friends");
                            break;
                        //case 1:
                        //    tab.setText("Groups");
                        case 1:
                            tab.setText("Requests");
                            break;
                        case 2:
                            tab.setText("Chats");
                            break;
                    }
                }
        ).attach();
         
    }

    private void adsData() {
        DocumentReference docRef = database.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Firestore error: " + error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus()) {
                        Log.d(TAG, "Ads data fetched: " + data);

                        // Ensure AdView is initialized
                        if (mAdView != null) {
                            // Create an ad request
                            AdRequest adRequest = new AdRequest.Builder().build();
                            // Load the ad into the AdView
                            mAdView.loadAd(adRequest);
                        } else {
                            Log.e(TAG, "AdView is not initialized.");
                        }
                    } else {
                        Log.e(TAG, "Ads status is false or AdsModel data is null.");
                    }
                } else {
                    Log.e(TAG, "Document does not exist.");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            sendUserToSettingsActivity();
        } else {
            updateUserStatus(STATUS_ONLINE);
            VerifyUserExistence();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            updateUserStatus(STATUS_OFFLINE);
        }
    }
    
    private void VerifyUserExistence() {
        String currentUserID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        DatabaseReference currentUserRef = databaseReference.child(USERS).child(currentUserID);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.child("name").exists()) {
                    sendUserToSettingsActivity();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle failure if needed
            }
        });
    }
    
    
    private void sendUserToSettingsActivity() {
        Intent settingsintent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsintent);
        finish(); // Optional: Finish the current activity to prevent going back to it
    }
    
    private void updateUserStatus(String state) {
        String savecurrentTime, savecurrentDate;
        Calendar calendar = Calendar.getInstance();
        
        DateFormat currentDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        savecurrentDate = currentDate.format(calendar.getTime());
        
        DateFormat currentTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        savecurrentTime = currentTime.format(calendar.getTime());
        
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("time", savecurrentTime);
        hashMap.put("date", savecurrentDate);
        hashMap.put("state", state);
    
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        DatabaseReference userStateRef = databaseReference.child(USERS).child(currentUserId).child(USER_STATE);
        userStateRef.updateChildren(hashMap);
        // Add this line to declare currentStatus
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.main_settings_menu) {
            sendUserToSettingsActivity();
        }
        if (id == android.R.id.home) {
            // Handle the "up" or "back" button click here
            onBackPressed();
            return true;
        }
       /* else if(id==R.id.main_create_group_menu)
        {
            RequestNewGroup();
        }*/
        return true;
    }

}
