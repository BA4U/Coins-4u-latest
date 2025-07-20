package com.bg4u.coins4u.chat4u;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bg4u.coins4u.AdsModel;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.chat.SettingsActivity;
import com.bg4u.coins4u.chat.TabsAccessorAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        // UI customization
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorBlue));

        View decorView = window.getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Firebase init
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Chat 4u New");

        // ViewPager and Tabs
        ViewPager2 viewPager = findViewById(R.id.main_tabs_pager);
        TabsAccessorAdapter tabsAdapter = new TabsAccessorAdapter(this);
        viewPager.setAdapter(tabsAdapter);

        TabLayout tabLayout = findViewById(R.id.main_tabs);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Find Friends");
                            break;
                        case 1:
                            tab.setText("Requests");
                            break;
                        case 2:
                            tab.setText("Chats");
                            break;
                    }
                }).attach();

        // AdView setup
        mAdView = findViewById(R.id.adView); // Make sure this ID is present in your XML
        adsData();
    }

    private void adsData() {
        DocumentReference docRef = db.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("MainActivity", "Firestore error: " + error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus() && mAdView != null) {
                        AdRequest adRequest = new AdRequest.Builder().build();
                        mAdView.loadAd(adRequest);
                    }
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
            verifyUserExistence();
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

    private void verifyUserExistence() {
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || !snapshot.contains("name")) {
                        sendUserToSettingsActivity();
                    }
                });
    }

    private void sendUserToSettingsActivity() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUserStatus(String state) {
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        String saveCurrentTime, saveCurrentDate;
        Calendar calendar = Calendar.getInstance();

        DateFormat currentDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        saveCurrentDate = currentDate.format(calendar.getTime());

        DateFormat currentTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String, Object> statusMap = new HashMap<>();
        statusMap.put("time", saveCurrentTime);
        statusMap.put("date", saveCurrentDate);
        statusMap.put("state", state);

        db.collection("users")
                .document(currentUserId)
                .collection("userState")
                .document("status")
                .set(statusMap)
                .addOnSuccessListener(unused -> Log.d("Firestore", "Status updated"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating status: ", e));
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
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return true;
    }
}
