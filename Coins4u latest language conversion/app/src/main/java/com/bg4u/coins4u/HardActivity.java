package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class HardActivity extends AppCompatActivity implements View.OnClickListener {

    private OnBackPressedCallback onBackPressedCallback;
    private RewardedAd rewardedAd;
    private AdView mAdView;
    private final boolean adLoaded = false;
    private boolean isRewardedAdLoaded = false;
    private int Win_Coin = 100;
    private int Lost_Coin = -10;
    private int Draw_Coin = 15;
    private User user;
    
    // Set the Random Move probability factor here (0.05 means 5% probability of random move)
    private static final double PROBABILITY_FACTOR = 0.05;
    
    // Set the Show AD probability factor here (0.85 means 85% probability of showing Ad)
    private static double AD_PROBABILITY_FACTOR = 0.75;
    // Add a global variable to keep track of the number of moves made by the computer
    private int computerMovesCount = 0;
    private int userMovesCount = 0;
    
    private enum Player {
        USER, COMPUTER
    }
    
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private boolean isPremiumUser = false;
    
    // Initialize the InterstitialAd instance
    
    private String userName; // Declare userName as a field
    private Player currentPlayer;
    private TextView textViewUser;
    private TextView textViewComputer;
    private AppCompatButton[][] buttons;
    private Button resetButton;
    private Handler computerMoveHandler = new Handler();
    
    MediaPlayer startGameMediaPlayer;
    MediaPlayer playerMoveSoundMediaPlayer;
    MediaPlayer computerMoveSoundMediaPlayer;
    MediaPlayer winSoundMediaPlayer;
    MediaPlayer loseSoundMediaPlayer;
    MediaPlayer drawSoundMediaPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_tic_tac_toe);
    
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.hard_level);

        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Find the AdView and load an ad
        mAdView = findViewById(R.id.adView);

        // Find the AdView and load an ad
        adsData();

        // Get the OnBackPressedDispatcher from the activity
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // Add a callback to the dispatcher
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show the confirmation dialog
                showConfirmationDialog();
            }
        };
        dispatcher.addCallback(onBackPressedCallback);

        // Initialize 'user' object
        String currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        database.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        user = document.toObject(User.class);
                        if (user != null) {
                            isPremiumUser = user.isSubscription();
                            subscriptionFeature();
                        }
                    }
                });
                
        // Find the views by their IDs
        
        textViewUser = findViewById(R.id.name);
        textViewComputer = findViewById(R.id.computer);
        resetButton = findViewById(R.id.resetBtn);
        resetButton.setOnClickListener(v -> resetGame());
    
        textViewUser.setSelected(true);
        textViewComputer.setSelected(true);
        ImageView userProfilePic = findViewById(R.id.userPic);
        ImageView computerProfilePic = findViewById(R.id.botPic);
        
        // Retrieve current user from MyApplication
        User currentUser = MyApplication.getCurrentUser();
        String profilePicUrl = MyApplication.getProfilePicUrl();
        if (profilePicUrl != null) {
            // Load and display the profile picture using the URL
            Picasso.get().load(profilePicUrl).into(userProfilePic);
        } else {
            // Handle the case where profilePicUrl is null, load default pic
            Picasso.get().load(R.drawable.user_icon_default).into(userProfilePic);
        }
    
        // Display user name
        if (currentUser != null) {
            userName = currentUser.getName();
            textViewUser.setText(userName);
        }
    
        buttons = new AppCompatButton[3][3];
        buttons[0][0] = findViewById(R.id.button_00);
        buttons[0][1] = findViewById(R.id.button_01);
        buttons[0][2] = findViewById(R.id.button_02);
        buttons[1][0] = findViewById(R.id.button_10);
        buttons[1][1] = findViewById(R.id.button_11);
        buttons[1][2] = findViewById(R.id.button_12);
        buttons[2][0] = findViewById(R.id.button_20);
        buttons[2][1] = findViewById(R.id.button_21);
        buttons[2][2] = findViewById(R.id.button_22);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setOnClickListener(this);
            }
        }
    
        // Initialize MediaPlayer instances for sound effects
        startGameMediaPlayer = MediaPlayer.create(this, R.raw.start_game_sound);
        playerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.player_move_sound);
        computerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.computer_move_sound);
        winSoundMediaPlayer = MediaPlayer.create(this, R.raw.win_sound);
        loseSoundMediaPlayer = MediaPlayer.create(this, R.raw.lose_sound);
        drawSoundMediaPlayer = MediaPlayer.create(this, R.raw.draw_sound);
    
        loadHardData();
        
        initializeGame();
        
    }

    private void adsData() {
        DocumentReference docRef = database.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(HardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore error: " + error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus()) {
                        Log.d(TAG, "Ads data fetched: " + data.toString());

                        // Ensure AdView is initialized
                        if (mAdView != null) {
                            // Create an ad request
                            AdRequest adRequest = new AdRequest.Builder().build();
                            // Load the ad into the AdView
                            mAdView.loadAd(adRequest);

                            //  Load rewarded ad
                            loadRewardedAd();

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

    private void loadHardData() {
        DocumentReference easyDocRef = database.collection("TicTacToe").document("Hard");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel hardData = documentSnapshot.toObject(TicTacToeModel.class);
                if (hardData != null) {
                    // Update the class-level instance variables with the retrieved values
                    Win_Coin = hardData.getWin();
                    Draw_Coin = hardData.getDraw();
                    Lost_Coin = hardData.getLost();
                }
            }
        });
    }
    
    private void subscriptionFeature() {
    
        if (user.isSubscription() && (user.isPremiumPlan() || user.isStandardPlan() || user.isBasicPlan())){
        
            SubscriptionModel subscriptionPlan;
            if (user.isPremiumPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("premium");
                double coinMultiplier = subscriptionPlan.getCoinMultiplier();
                
                // Set the Show AD probability factor here (0.60 means 60% probability of showing Ad)
                 AD_PROBABILITY_FACTOR = 0.10;
                 
                // Convert the result to int using type casting
                Win_Coin = (int) (coinMultiplier * Win_Coin);
                Lost_Coin = (int) (coinMultiplier * Lost_Coin);
                Draw_Coin = (int) (coinMultiplier * Draw_Coin);
            
            } else if (user.isStandardPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("standard");
                double coinMultiplier = subscriptionPlan.getCoinMultiplier();
                
                // Set the Show AD probability factor here (0.40 means 40% probability of showing Ad)
                AD_PROBABILITY_FACTOR = 0.40;
    
                // Convert the result to int using type casting
                Win_Coin = (int) (coinMultiplier * Win_Coin);
                Lost_Coin = (int) (coinMultiplier * Lost_Coin);
                Draw_Coin = (int) (coinMultiplier * Draw_Coin);
            
            } else if (user.isBasicPlan()) {
                subscriptionPlan = SubscriptionAdapter.getSubscriptionPlan("basic");
                double coinMultiplier = subscriptionPlan.getCoinMultiplier();
    
                // Set the Show AD probability factor here (0.10 means 10% probability of showing Ad)
                AD_PROBABILITY_FACTOR = 0.50;
    
                // Convert the result to int using type casting
                Win_Coin = (int) (coinMultiplier * Win_Coin);
                Lost_Coin = (int) (coinMultiplier * Lost_Coin);
                Draw_Coin = (int) (coinMultiplier * Draw_Coin);
            
            }
        }
    }
    
    private void initializeGame() {
        // Add sound effects
        playSoundEffect(startGameMediaPlayer);
        currentPlayer = getRandomPlayer();
        textViewUser.setText(userName);
        textViewComputer.setText(getString(R.string.anuj));
        textViewUser.setTextColor(Color.YELLOW);
        textViewComputer.setTextColor(Color.GRAY);
        resetButton.setEnabled(false);
        resetButton.setAlpha(0.5f);
        
        clearButtons();
        
        if (currentPlayer == Player.COMPUTER) {
            makeComputerMove();
        }
    }
    
    private Player getRandomPlayer() {
        Random random = new Random();
        int randomNum = random.nextInt(2);
        return randomNum == 0 ? Player.USER : Player.COMPUTER;
    }
    
    private void clearButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackgroundColor(Color.WHITE);
                buttons[i][j].setBackground(ContextCompat.getDrawable(this, R.drawable.option_unselected));
            }
        }
    }
    
    @Override
    public void onClick(View v) {
        AppCompatButton button = (AppCompatButton) v;
        // Add sound effects
        playSoundEffect(playerMoveSoundMediaPlayer);
        button.setText("O"); // Change "X" to "O" for user move
        button.setEnabled(false);
        button.setBackground(ContextCompat.getDrawable(this, R.drawable.option_yellow));
        userMovesCount++;
        
        currentPlayer = Player.COMPUTER;
        textViewUser.setTextColor(Color.LTGRAY);
        textViewComputer.setTextColor(Color.GREEN);
        textViewUser.setAlpha(0.5f);
        textViewComputer.setAlpha(1f);
        textViewUser.setTextSize(18); // set text size to 18sp
        textViewComputer.setTextSize(28); // set text size to 20sp
        
        if (!checkGameStatus()) {
            makeComputerMove();
        }
    }
    
    private void makeComputerMove() {
        // Disable all buttons during the computer's move
        disableAllButtons();
        
        // After 3 moves by the computer, randomize the move occasionally
        if (computerMovesCount >= 3 && shouldRandomizeMove()) {
            makeRandomMove();
        } else {
            // Find the computer's move using Minimax algorithm
            int[] newBestMove = findBestMove(); // Create a copy of the bestMove array
            
            int newRow = newBestMove[0];
            int newCol = newBestMove[1];
            
            // Set the computer's move to be invisible initially
            buttons[newRow][newCol].setText("");
            buttons[newRow][newCol].setEnabled(false);
            buttons[newRow][newCol].setBackground(ContextCompat.getDrawable(this, R.drawable.option_unselected));
    
            // Delay updating the computer's move text and color by 600 milliseconds
            computerMoveHandler.removeCallbacksAndMessages(null); // Remove any previously posted callbacks
            computerMoveHandler.postDelayed(() -> {
                // Add sound effects
                playSoundEffect(computerMoveSoundMediaPlayer);
                // Update the computer's move after the delay
                buttons[newRow][newCol].setText("X"); // Change "O" to "X" for computer move
                buttons[newRow][newCol].setBackground(ContextCompat.getDrawable(this, R.drawable.option_right));
                
                currentPlayer = Player.USER;
                textViewUser.setTextColor(Color.YELLOW);
                textViewComputer.setTextColor(Color.GRAY);
                textViewUser.setAlpha(1f);
                textViewComputer.setAlpha(0.5f);
                textViewUser.setTextSize(28); // set text size to 28sp
                textViewComputer.setTextSize(18); // set text size to 18sp
    
                // Enable all buttons after the computer's move is complete
                enableAllButtons();
                
                checkGameStatus();
                
            }, 600);
        }
        // Increment the computer moves count
        computerMovesCount++;
    }
    
    private boolean checkGameStatus() {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }
        
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2]) && !board[i][0].equals("")) {
                endGame(board[i][0]);
    
                buttons[i][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[i][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[i][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                return true;
            }
        }
        
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i]) && !board[0][i].equals("")) {
                endGame(board[0][i]);
    
                buttons[0][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[1][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[2][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                return true;
            }
        }
        
        // Check diagonals
        if (board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2]) && !board[0][0].equals("")) {
            endGame(board[0][0]);
    
            buttons[0][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[1][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[2][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            return true;
        }
        
        if (board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0]) && !board[0][2].equals("")) {
            endGame(board[0][2]);
    
            buttons[0][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[1][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[2][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            return true;
        }
        
        // Check for a draw
        boolean isBoardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    isBoardFull = false;
                    break;
                }
            }
        }
        
        if (isBoardFull) {
            endGame("draw");
            return true;
        }
        
        return false;
    }
    
    private void endGame(String winner) {
        resetButton.setEnabled(true);
        resetButton.setAlpha(1f);
        textViewUser.setTextColor(Color.YELLOW);
        textViewComputer.setTextColor(Color.GREEN);
        boolean isFinishing = isFinishing();
    
        // Cancel the delayed handler when the game ends
        computerMoveHandler.removeCallbacksAndMessages(null);
        if (isFinishing) {
            // Activity is already finishing or has been destroyed, don't proceed further
            disableAllButtons();
            return;
        }
        if (winner.equals("O")) {
            textViewUser.setText(user.getName() + " " + getString(R.string.won));
            textViewComputer.setText(getString(R.string.anuj)+ " " + getString(R.string.lost));
            textViewUser.setTextSize(20); // set text size to 20sp
            textViewComputer.setTextSize(20); // set text size to 20sp
            // Add Coins to user
            updateUserStats(Win_Coin, 1, 0, 0);
            // Add sound effects
            playSoundEffect(winSoundMediaPlayer);
            
            // Show a popup with a Lottie animation
            showCongratsDialog(getString(R.string.congratulations), getString(R.string.You) + " " + getString(R.string.won)  + " " + Win_Coin + " " + getString(R.string.coins), R.raw.win_animation, "Hard", HardActivity.class);
            
        } else if (winner.equals("X")) {
            textViewUser.setText(user.getName()+ " " + getString(R.string.lost));
            textViewComputer.setText(getString(R.string.anuj)+ " " + getString(R.string.won));
            textViewUser.setTextSize(20); // set text size to 20sp
            textViewComputer.setTextSize(20); // set text size to 20sp
            // Add Coins to user
            updateUserStats(Lost_Coin, 0, 1, 0);
            // Add sound effects
            playSoundEffect(loseSoundMediaPlayer);
    
            // Show a popup with a Lottie animation
            showCongratsDialog(getString(R.string.Game_over), getString(R.string.You) + " " + getString(R.string.lost)  + " " + Math.abs(Lost_Coin) + " " + getString(R.string.coins), R.raw.lose_animation, "Easy", EasyActivity.class);
            
        } else {
            textViewUser.setText(R.string.draw);
            textViewComputer.setText(R.string.draw);
            textViewUser.setTextSize(20); // set text size to 24sp
            textViewComputer.setTextSize(20); // set text size to 24sp
            // Add sound effects
            playSoundEffect(drawSoundMediaPlayer);
    
            // Show interstitial ad if it's loaded
            showAd();
            
            // Show a popup with a Lottie animation
            showCongratsDialog(getString(R.string.draw), getString(R.string.You)+" " +getString(R.string.won)  + " " + Draw_Coin + " " + getString(R.string.coins), R.raw.draw_animation, "Medium", MediumActivity.class);
            
        }
        
        disableAllButtons();
    }
    
    private void playSoundEffect(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    private void releaseMediaPlayers() {
        if (startGameMediaPlayer != null) {
            startGameMediaPlayer.release();
            startGameMediaPlayer = null;
        }
        if (playerMoveSoundMediaPlayer != null) {
            playerMoveSoundMediaPlayer.release();
            playerMoveSoundMediaPlayer = null;
        }
        if (computerMoveSoundMediaPlayer != null) {
            computerMoveSoundMediaPlayer.release();
            computerMoveSoundMediaPlayer = null;
        }
        if (winSoundMediaPlayer != null) {
            winSoundMediaPlayer.release();
            winSoundMediaPlayer = null;
        }
        if (loseSoundMediaPlayer != null) {
            loseSoundMediaPlayer.release();
            loseSoundMediaPlayer = null;
        }
        if (drawSoundMediaPlayer != null) {
            drawSoundMediaPlayer.release();
            drawSoundMediaPlayer = null;
        }
    }
    
    private void updateUserStats(int incrementAmount, int wins, int losses, int draws) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getUid();
        
        if (userId != null) {
            DocumentReference userRef = database.collection("users").document(userId);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("coins", FieldValue.increment(incrementAmount));
            updates.put("hardWin", FieldValue.increment(wins));
            updates.put("hardLost", FieldValue.increment(losses));
            updates.put("hardDraw", FieldValue.increment(draws));
            
            userRef.update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(HardActivity.this, "Coins added to your account.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(HardActivity.this, "Failed to update coins.", Toast.LENGTH_SHORT).show());
        }
    }
    
    private void disableAllButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }
    // Method to enable all buttons after the computer's move is complete
    private void enableAllButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().toString().isEmpty()) {
                    buttons[i][j].setEnabled(true);
                }
            }
        }
    }
    private void showCongratsDialog(String title, String body, int animationRes, String leftButtonLabel, Class<?> activityClass) {
        if (!isFinishing()) {
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(HardActivity.this).inflate(R.layout.dialog_box_layout, null);
            
            // Set the title and body text directly on the custom layout
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(body);
            
            // Set the animation directly on the LottieAnimationView
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(animationRes);
            
            // Create an instance of the custom dialog and pass the custom layout as a parameter
            DialogBox dialogBox = new DialogBox(HardActivity.this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            
            // Set the left button action
            dialogBox.setLeftButton(leftButtonLabel, v -> {
                // Handle left button click
                dialogBox.dismiss();
                if (activityClass != null) {
                    finish(); // Finish the current activity
                    startActivity(new Intent(HardActivity.this, activityClass));
                }
                // resetGame();
            });
            
            // Set the right button action
            dialogBox.setRightButton("Restart", v -> {
                // Handle right button click
                dialogBox.dismiss();
                resetGame();
            });
            
            // Show the dialog
            dialogBox.show();
        }
    }
    
    private void resetGame() {
        // Reset move counts for both user and computer
        userMovesCount = 0;
        computerMovesCount = 0;
        initializeGame();
    }
    
    // Find the best move for the computer player
    private int[] findBestMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = new int[]{-1, -1};
        
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    board[i][j] = "X";
                    int score = minimax(board, 0, false);
                    board[i][j] = "";
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove[0] = i;
                        bestMove[1] = j;
                    }
                }
            }
        }
        
        return bestMove;
    }
    
    private int minimax(String[][] board, int depth, boolean isMaximizingPlayer) {
        String result = checkResult(board);
        if (!result.equals("")) {
            return evaluateScore(result, depth);
        }
        
        int bestScore;
        if (isMaximizingPlayer) {
            bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "X";
                        int score = minimax(board, depth + 1, false);
                        board[i][j] = "";
                        bestScore = Math.max(score, bestScore);
                    }
                }
            }
        } else {
            bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].equals("")) {
                        board[i][j] = "O";
                        int score = minimax(board, depth + 1, true);
                        board[i][j] = "";
                        bestScore = Math.min(score, bestScore);
                    }
                }
            }
        }
        return bestScore;
    }
    
    private String checkResult(String[][] board) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2]) && !board[i][0].equals("")) {
                return board[i][0];
            }
        }
        
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i]) && !board[0][i].equals("")) {
                return board[0][i];
            }
        }
        
        // Check diagonals
        if (board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2]) && !board[0][0].equals("")) {
            return board[0][0];
        }
        
        if (board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0]) && !board[0][2].equals("")) {
            return board[0][2];
        }
        
        // Check for a draw
        boolean isBoardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals("")) {
                    isBoardFull = false;
                    break;
                }
            }
        }
        
        if (isBoardFull) {
            return "draw";
        }
        
        return "";
    }
    
    private int evaluateScore(String result, int depth) {
        if (result.equals("X")) {
            return 10 - depth;
        } else if (result.equals("O")) {
            return depth - 10;
        } else {
            return 0;
        }
    }
    
    // Method to make a random move for the computer
    private void makeRandomMove() {
        // Check if the game is over or no more moves are available
        if (checkGameStatus() || computerMovesCount >= 5) {
            return;
        }
        Random random = new Random();
        int row, col;
        
        // Keep generating random positions until an empty position is found
        do {
            row = random.nextInt(3);
            col = random.nextInt(3);
        } while (!buttons[row][col].getText().toString().isEmpty());
        
        // Make the random move for the computer
        buttons[row][col].setText("");
        buttons[row][col].setEnabled(false);
        buttons[row][col].setBackground(ContextCompat.getDrawable(this, R.drawable.option_unselected));
    
        // Delay updating the computer's move text and color by 600 milliseconds
        computerMoveHandler.removeCallbacksAndMessages(null); // Remove any previously posted callbacks
        int finalRow = row;
        int finalCol = col;
        computerMoveHandler.postDelayed(() -> {
            // Add sound effects
            playSoundEffect(computerMoveSoundMediaPlayer);
            // Update the computer's move after the delay
            buttons[finalRow][finalCol].setText("X");
            buttons[finalRow][finalCol].setBackground(ContextCompat.getDrawable(this, R.drawable.option_right));
        
            currentPlayer = Player.USER;
            textViewUser.setTextColor(Color.YELLOW);
            textViewComputer.setTextColor(Color.GRAY);
            textViewUser.setTextSize(28); // set text size to 28sp
            textViewComputer.setTextSize(18); // set text size to 18sp
            
            // Enable all buttons after the computer's move is complete
            enableAllButtons();
            
            checkGameStatus();
        
        }, 600);
    }
    
    // Method to decide whether the computer should make a random move
    private boolean shouldRandomizeMove() {
        return Math.random() < PROBABILITY_FACTOR;
    }
    
    private void showAd() {
        if(showAdProbability()){
            showRewardedAd();
        }
    }
    private boolean showAdProbability() {
        return Math.random() < AD_PROBABILITY_FACTOR;
    }

    private void showRewardedAd() {
        if (isRewardedAdLoaded) {
            Activity activityContext = HardActivity.this; // Make sure to use the correct activity context

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "onAdShowedFullScreenContent");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                    adsData(); // Instead of using  loadRewardedAd() we are using adsData() if ads are enable from server or not
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "onAdDismissedFullScreenContent");
                    // Set the flag to false after ad is dismissed
                    isRewardedAdLoaded = false;
                    adsData(); // Instead of using  loadRewardedAd() we are using adsData() if ads are enable from server or not
                }
            });

            rewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    // Add Coins to user
                    updateUserStats(Win_Coin, 1, 0, 0);
                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet or failed to load.");
            // Load a new rewarded ad
            adsData(); // Instead of using  loadRewardedAd() we are using adsData() if ads are enable from server or not
        }
    }

    private void loadRewardedAd() {
        if (!isRewardedAdLoaded) {
            AdRequest adRequest = new AdRequest.Builder().build();
            String REWARDED_AD_UNIT_ID = getString(R.string.HARD_REWARDED_AD_UNIT_ID); // Get rewarded ad

            RewardedAd.load(
                    this,
                    REWARDED_AD_UNIT_ID,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                            isRewardedAdLoaded = true; // Set the flag to true when ad is loaded
                            Log.d(TAG, "onAdLoaded");

                            // Set up the FullScreenContentCallback here
                            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    Log.d(TAG, "Ad dismissed fullscreen content.");
                                    rewardedAd = null;
                                    // Don't load a new ad here; it will be loaded when necessary
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    // Called when ad fails to show.
                                    Log.e(TAG, "Ad failed to show fullscreen content: " + adError.getMessage());
                                    // Don't set rewardedAd to null here
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    // Called when ad is shown.
                                    Log.d(TAG, "Ad showed fullscreen content.");
                                }
                            });

                            // Proceed with showing the ad if needed
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            Log.d(TAG, "Ad failed to load: " + loadAdError.getMessage());
                            // Set the flag to false if ad failed to load
                            isRewardedAdLoaded = false;
                            // You can add your error handling logic here
                            Toast.makeText(HardActivity.this, "Oops! you may get less rewards", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayers();
        // Remove the callback when the activity is destroyed
        onBackPressedCallback.remove();
    }

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

    private void showConfirmationDialog() {
        if (!isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to leave this game?")
                    .setTitle("Quit Game!")
                    .setIcon(R.drawable.logout)
                    .setPositiveButton("Yes", (dialog, id) -> {
                        // Show interstitial ad if it's loaded (optional)
                        showAd();
                        // Take the user to the MainActivity
                            Intent intent = new Intent(HardActivity.this, TicTacToeActivity.class);
                        startActivity(intent);
                        finish(); // Finish activities in the stack
                    })
                    .setNegativeButton("No", (dialog, id) -> {
                        // User cancelled the dialog, do nothing
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    
}
