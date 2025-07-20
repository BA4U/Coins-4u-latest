package com.bg4u.coins4u.TicTacToeOnline;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.DialogBox;
import com.bg4u.coins4u.TicTacToeModel;
import com.bg4u.coins4u.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class OnlineGameActivity extends AppCompatActivity {
    private TextView player1, player1Coins;
    private TextView player2, player2Coins;
    private String code, friendUid;
    private String ME, OPPONENT;
    private Button restartBtn;
    private String turn;
    private String firstTurn;
    private int MY_SCORE, OPPONENT_SCORE;
    
    private Chronometer player1Timer;
    private Chronometer player2Timer;
    private long startTimePlayer1;
    private long startTimePlayer2;
    private boolean isPlayer1Running = false;
    private boolean isPlayer2Running = false;
    private long elapsedTimePlayer1;
    private long elapsedTimePlayer2;
    
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myGameRef;
    private ValueEventListener gameEventListener;
    private ListenerRegistration registration;
    private String currentUserUid;
    private AppCompatButton[][] buttons;
    private TextView turnText, player1Score, player2Score;
    private CircleImageView player1Pic, player2Pic;
    AppCompatButton one, two, three, four, five, six, seven, eight, nine;
    
    private static final int BOARD_SIZE = 3;
    private static final String EMPTY_CELL = "-";
    private static final String DRAW = "DRAW";
    private final AppCompatButton[] cells = new AppCompatButton[BOARD_SIZE * BOARD_SIZE];
    private final String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
    Boolean gameEnd;
    
    private Handler handler = new Handler();
    private int Win_Coin = 40;
    private int Lost_Coin = -25;
    private int Draw_Coin = 20;
    MediaPlayer startGameMediaPlayer;
    MediaPlayer playerMoveSoundMediaPlayer;
    MediaPlayer computerMoveSoundMediaPlayer;
    MediaPlayer winSoundMediaPlayer;
    MediaPlayer loseSoundMediaPlayer;
    MediaPlayer drawSoundMediaPlayer;
    private boolean showOnlyOnce = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_tictactoe);
    
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Tic-Tac-Toe Online");
    
        Intent intent = getIntent();
        code = intent.getStringExtra("session_code");
        ME = intent.getStringExtra("my_player");
        friendUid = intent.getStringExtra("friendUid");  //  Thia is other Player uid
    
        currentUserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(); // This is current player uid
        myGameRef = database.getReference("games").child(code);
    
        // Deduct user Coins as match fees
        matchFeeSoundDialog();
        
        //  play start game sound when match restarts
        playSoundEffect(startGameMediaPlayer);
        
        player1 = findViewById(R.id.player1);
        player2 = findViewById(R.id.player2);
        player1.setText("You are: " + ME);

        turnText = findViewById(R.id.turn_text);
        player1Score = findViewById(R.id.player1Score);
        player2Score = findViewById(R.id.player2Score);
        
        player1Coins = findViewById(R.id.player1Coins);
        player2Coins = findViewById(R.id.player2Coins);
    
        player1Pic = findViewById(R.id.player1Pic);
        player2Pic = findViewById(R.id.player2Pic);
    
        restartBtn = findViewById(R.id.rematchBtn);
        
        one = findViewById(R.id.one);
        two = findViewById(R.id.two);
        three =findViewById(R.id.three);
        four = findViewById(R.id.four);
        five = findViewById(R.id.five);
        six = findViewById(R.id.six);
        seven =findViewById(R.id.seven);
        eight =findViewById(R.id.eight);
        nine = findViewById(R.id.nine);
        
        if (currentUserUid != null) {
            // Load and display data using uid
            fetchAndDisplayCurrentUserData(currentUserUid);
        }
    
        if (friendUid != null) {
            // Load and display data using uid
            fetchAndDisplayFriendData(friendUid);
        }
    
        whoIsXandWhoIsO();
        loadGameData();
        
        initializeMediaPlayer();
        initializeCells();
        initializeRestartButton();
        
        startLocal();
        startFB();
        updateUI();

        myGameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateLocal(dataSnapshot);
                
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.w("Cancelled", "Failed to read value.", databaseError.toException());
            }
        });
    
        setCellClickListeners();
    
        checkIfUserLeft();
    
    }
    
    
    
    private void startPlayer1Timer() {
        startTimePlayer1 = System.currentTimeMillis();
        isPlayer1Running = true;
        handler.postDelayed(updateTimerPlayer1, 0);
    }
    
    private void pausePlayer1Timer() {
        isPlayer1Running = false;
        elapsedTimePlayer1 = System.currentTimeMillis() - startTimePlayer1;
    }
    
    private void resumePlayer1Timer() {
        startTimePlayer1 = System.currentTimeMillis() - elapsedTimePlayer1;
        isPlayer1Running = true;
        handler.postDelayed(updateTimerPlayer1, 0);
    }
    
    private void resetPlayer1Timer() {
        isPlayer1Running = false;
        player1Timer.setText("00:00");
    }
    
    private void startPlayer2Timer() {
        startTimePlayer2 = System.currentTimeMillis();
        isPlayer2Running = true;
        handler.postDelayed(updateTimerPlayer2, 0);
    }
    
    private void pausePlayer2Timer() {
        isPlayer2Running = false;
        elapsedTimePlayer2 = System.currentTimeMillis() - startTimePlayer2;
    }
    
    private void resumePlayer2Timer() {
        startTimePlayer2 = System.currentTimeMillis() - elapsedTimePlayer2;
        isPlayer2Running = true;
        handler.postDelayed(updateTimerPlayer2, 0);
    }
    
    private void resetPlayer2Timer() {
        isPlayer2Running = false;
        player2Timer.setText("00:00");
    }
    
    private Runnable updateTimerPlayer1 = new Runnable() {
        @Override
        public void run() {
            if (isPlayer1Running) {
                long elapsedTime = System.currentTimeMillis() - startTimePlayer1;
                int seconds = (int) (elapsedTime / 1000);
                player1Timer.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                handler.postDelayed(this, 1000);
            }
        }
    };
    
    private Runnable updateTimerPlayer2 = new Runnable() {
        @Override
        public void run() {
            if (isPlayer2Running) {
                long elapsedTime = System.currentTimeMillis() - startTimePlayer2;
                int seconds = (int) (elapsedTime / 1000);
                player2Timer.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                handler.postDelayed(this, 1000);
            }
        }
    };
    
    private void calculateTimerWinner() {
        // Get the text from the timer text views
        String timePlayer1 = player1Timer.getText().toString();
        String timePlayer2 = player2Timer.getText().toString();
        
        // Split the time strings into minutes and seconds
        String[] partsPlayer1 = timePlayer1.split(":");
        String[] partsPlayer2 = timePlayer2.split(":");
        
        // Convert the minutes and seconds to total seconds
        int totalSecondsPlayer1 = Integer.parseInt(partsPlayer1[0]) * 60 + Integer.parseInt(partsPlayer1[1]);
        int totalSecondsPlayer2 = Integer.parseInt(partsPlayer2[0]) * 60 + Integer.parseInt(partsPlayer2[1]);
        
        // Compare the times and determine the winner
        if (totalSecondsPlayer1 < totalSecondsPlayer2) {
            // Player 1 is the winner
            System.out.println("Player 1 is the winner!");
        } else if (totalSecondsPlayer2 < totalSecondsPlayer1) {
            // Player 2 is the winner
            System.out.println("Player 2 is the winner!");
        } else {
            // It's a tie
            System.out.println("It's a tie!");
        }
    }
    
    
    private void whoIsXandWhoIsO() {
        if (ME.equals("X")) {
            OPPONENT = "O";
            player1Pic.setBorderColor(ContextCompat.getColor(OnlineGameActivity.this, R.color.green));
            player1Score.setTextColor(getColor(R.color.green));
            player1.setTextColor(getColor(R.color.green));
            
            player2Pic.setBorderColor(ContextCompat.getColor(OnlineGameActivity.this, R.color.yellow));
            player2Score.setTextColor(getColor(R.color.yellow));
            player2.setTextColor(getColor(R.color.yellow));
    
        } else {
            OPPONENT = "X";
            player2Pic.setBorderColor(ContextCompat.getColor(OnlineGameActivity.this, R.color.green));
            player2Score.setTextColor(getColor(R.color.green));
            player2.setTextColor(getColor(R.color.green));
            
            player1Pic.setBorderColor(ContextCompat.getColor(OnlineGameActivity.this, R.color.yellow));
            player1Score.setTextColor(getColor(R.color.yellow));
            player1.setTextColor(getColor(R.color.yellow));
        }
    
    }
    private void loadGameData() {
        DocumentReference easyDocRef = FirebaseFirestore.getInstance().collection("TicTacToe").document("Online");
        
        easyDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                TicTacToeModel onlineData = documentSnapshot.toObject(TicTacToeModel.class);
                if (onlineData != null) {
                    // Update the class-level instance variables with the retrieved values
                    Win_Coin = onlineData.getWin();
                    Draw_Coin = onlineData.getDraw();
                    Lost_Coin = onlineData.getLost();
                    showToast("winner coin : " + Win_Coin);
                }
            }
        });
    }
    
    private void initializeCells() {
        cells[0] = one;
        cells[1] = two;
        cells[2] = three;
        cells[3] = four;
        cells[4] = five;
        cells[5] = six;
        cells[6] = seven;
        cells[7] = eight;
        cells[8] = nine;
        
        // Initialize the buttons array here
        buttons = new AppCompatButton[][]{
                {one, two, three},
                {four, five, six},
                {seven, eight, nine}
        };
    }
    
    private void initializeRestartButton() {
        restartBtn.setOnClickListener(v -> restartGame());
    }
    
    private void initializeMediaPlayer() {
        // Initialize MediaPlayer instances for sound effects
        startGameMediaPlayer = MediaPlayer.create(this, R.raw.start_game_sound);
        playerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.player_move_sound);
        computerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.computer_move_sound);
        winSoundMediaPlayer = MediaPlayer.create(this, R.raw.win_sound);
        loseSoundMediaPlayer = MediaPlayer.create(this, R.raw.lose_sound);
        drawSoundMediaPlayer = MediaPlayer.create(this, R.raw.draw_sound);
    }
    
    private void restartGame() {
        if (!gameEnd) {
            showToast("The game is not finished yet.");
            return;
        }
        
        myGameRef.child("restart").child(OPPONENT).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean restartValue = dataSnapshot.getValue(Boolean.class);
                if (restartValue != null && restartValue) {
                
                }else {
                    showToast("Opponent is not ready yet...");
                }
            }
        
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle failure
            }
        });
    
        myGameRef.child("restart").child(ME).setValue(true);
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void setCellClickListeners() {
        for (int i = 0; i < cells.length; i++) {
            final int cellIndex = i;
            cells[i].setOnClickListener(v -> playLocal(cellIndex + 1));
        }
    }
    private void playLocal(int i) {
        if(!gameEnd) {
            if(turn.equals(ME)) {
                if(is_valid_move(i)) {
                    board[(i-1)/3][(i-1)%3] = ME;
                    turn = ME.equals("X") ? "O" : "X";
                    updateUI();
                    updateFB(i);
                    checkWinning();
                }
            }
        }
    }

    private void updateFB(int i) {
        myGameRef.child("board").child(String.valueOf(i)).setValue(board[(i-1)/3][(i-1)%3]);
        myGameRef.child("turn").setValue(turn);
    }

    private void startFB() {
        for(int i = 1; i <= 9; i++) {
            myGameRef.child("board").child(String.valueOf(i)).setValue("-");
        }
        myGameRef.child("turn").setValue("X");
        myGameRef.child("scores").child("X").setValue(0);
        myGameRef.child("scores").child("O").setValue(0);
        myGameRef.child("restart").child("X").setValue(false);
        myGameRef.child("restart").child("O").setValue(false);
        myGameRef.child("first_turn").setValue("X");
    }

    private void startLocal() {
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                board[i][j] = "-";
            }
        }
    
        turn = firstTurn = "X";
        MY_SCORE = OPPONENT_SCORE = 0;
        gameEnd = false;
    }

    private void updateLocal(DataSnapshot dataSnapshot) {
        if(dataSnapshot.child("restart").child("X").getValue(Boolean.class) &&
                dataSnapshot.child("restart").child("O").getValue(Boolean.class)) {
            // Clear Board
            for(int i = 0; i < 3; i++) {
                for(int j = 0; j < 3; j++) {
                    board[i][j] = "-";
                }
            }
    
            restartBtn.setAlpha(0.5f);
            
            for(int i = 1; i <= 9; i++) {
                myGameRef.child("board").child(String.valueOf(i)).setValue("-");
            }
            // First Turn
            firstTurn = firstTurn.equals("X") ? "O" : "X";
            myGameRef.child("first_turn").setValue(firstTurn);
            // Turn
            turn = firstTurn;
            myGameRef.child("turn").setValue(turn);
            // Restart
            gameEnd = false;
            myGameRef.child("restart").child("X").setValue(false);
            myGameRef.child("restart").child("O").setValue(false);
            // Scores
            MY_SCORE = dataSnapshot.child("scores").child(ME).getValue(Integer.class);
            OPPONENT_SCORE = dataSnapshot.child("scores").child(OPPONENT).getValue(Integer.class);
            // Restart Button
            updateUI();
        }

        if(!turn.equals(ME)) {
            for(int i = 0; i < 3; i++) {
                for(int j = 0; j < 3; j++) {
                    if (!board[i][j].equals(dataSnapshot.child("board").child(String.valueOf((i * 3) + j + 1))
                            .getValue(String.class))) {
                        turn = ME;
                        board[i][j] = dataSnapshot.child("board").child(String.valueOf((i * 3) + j + 1))
                                .getValue(String.class);
                    }
                }
            }
        }
        updateUI();
    }
    
    // Update the UI to reflect the current game state
    private void updateUI() {
        player1Score.setText(ME + " - " + MY_SCORE);
        player2Score.setText(OPPONENT + " - " + OPPONENT_SCORE);
        turnText.setText(turn);
    
        // Update ImageViews based on board state
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                switch (board[i][j]) {
                    case "X":
                        playSoundEffect(playerMoveSoundMediaPlayer);
                        buttons[i][j].setText("X");
                        buttons[i][j].setBackground(ContextCompat.getDrawable(this, R.drawable.option_right));
                        break;
                    case "O":
                        playSoundEffect(computerMoveSoundMediaPlayer);
                        buttons[i][j].setText("O");
                        buttons[i][j].setBackground(ContextCompat.getDrawable(this, R.drawable.option_yellow));
                        break;
                    default:
                        buttons[i][j].setText(" ");
                        buttons[i][j].setBackground(ContextCompat.getDrawable(this, R.drawable.option_unselected));
                        break;
                }
            }
        }
    }
    
    private boolean is_valid_move(int i) {
        if(i < 1 || i > 9) return false;

        return board[(i - 1) / 3][(i - 1) % 3].equals("-");
    }
    
    private boolean checkWinning() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2]) && !board[i][0].equals("-")) {
                endGame(board[i][0]);
                
                buttons[i][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[i][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[i][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                return true;
            }
        }
        
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i]) && !board[0][i].equals("-")) {
                endGame(board[0][i]);
                
                buttons[0][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[1][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                buttons[2][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                return true;
            }
        }
        
        // Check diagonals
        if (board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2]) && !board[0][0].equals("-")) {
            endGame(board[0][0]);
            
            buttons[0][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[1][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[2][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            return true;
        }
        
        if (board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0]) && !board[0][2].equals("-")) {
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
                if (board[i][j].equals("-")) {
                    isBoardFull = false;
                    break;
                }
            }
        }
        
        if (isBoardFull) {
            endGame("-");
            return true;
        }
        
        return false;
    }
    
    private void endGame(String winner) {
        restartBtn.setEnabled(true);
        restartBtn.setAlpha(1f);
        gameEnd = true;
        
        if (showOnlyOnce) {
            // Activity is already finishing or has been destroyed, don't proceed further
            return;
        }
        if (winner.equals(ME)) {
            turnText.setText(ME + " Winner");
            MY_SCORE += 1;
    
            player1Score.setText(ME + " - " + MY_SCORE);
            myGameRef.child("scores").child(ME).setValue(MY_SCORE);
    
            // Add sound effects
            winSoundStatDialog();
            
        } else if (winner.equals(OPPONENT)) {
            turnText.setText(OPPONENT + " Winner");
            OPPONENT_SCORE += 1;
    
            player2Score.setText(OPPONENT + " - " + OPPONENT_SCORE);
            myGameRef.child("scores").child(OPPONENT).setValue(OPPONENT_SCORE);
    
    
            // Show a popup with a Lottie animation
            lostSoundStatDialog();
        } else if(winner.equals("-")) {
            restartBtn.setAlpha(1f);
            turnText.setText("DRAW!");
            drawSoundStatDialog();
        }
        showOnlyOnce = true;
    }
    
    private void fetchAndDisplayFriendData(String userUID) {
        registration = FirebaseFirestore.getInstance().collection("users")
                .document(userUID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // You can access the user data here
                            String userName = user.getName();
                            String userCoins = String.valueOf(user.getCoins());
                            String userProfile = user.getProfile();
                            
                            //  already set you are My_player
                            player2.setText(userName);
                            player2Coins.setText(userCoins);
                            
                            if (userProfile != null) {
                                // Load and display the profile picture using the URL
                                Picasso.get().load(userProfile).into(player2Pic);
                            } else {
                                // Handle the case where profilePicUrl is null, load default pic
                                Picasso.get().load(R.drawable.easy_bot).into(player2Pic);
                            }
                        }
                    }
                });
    }
    
    private void showCongratsDialog(String title, String body, int animationRes, String leftButtonLabel, String rightButtonLabel) {
        if (!isFinishing()) {
            // Inflate your custom layout for the dialog content
            View customView = LayoutInflater.from(OnlineGameActivity.this).inflate(R.layout.dialog_box_layout, null);
            
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
            dialogBox.setLeftButton(leftButtonLabel, v -> {
                // Handle left button click
                dialogBox.dismiss();
    
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure to leave ? Please tell your friends to leave the match")
                        .setTitle("Leave Match")
                        .setIcon(R.drawable.logout)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            if (!gameEnd) {
                                // Deduct user Coins
                                updateUserStats(Lost_Coin, 0, 1, 0);
                                // Add sound effects
                                playSoundEffect(loseSoundMediaPlayer);
                    
                                myGameRef.child("restart").child("X").setValue(true);
                                myGameRef.child("restart").child("O").setValue(true);
                            }
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
    
            });
            
            // Set the right button action
            dialogBox.setRightButton(rightButtonLabel, v -> {
                // Handle right button click
                dialogBox.dismiss();
                restartGame();
            });
            
            // Show the dialog
            dialogBox.show();
        }
    }
    
    private void fetchAndDisplayCurrentUserData(String userUID) {
        registration = FirebaseFirestore.getInstance().collection("users")
                .document(userUID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // You can access the user data here
                            String userName = user.getName();
                            String userCoins = String.valueOf(user.getCoins());
                            String userProfile = user.getProfile();
                            
                            //  already set you are My_player
                            //    player1.setText(userName);
                            player1Coins.setText(userCoins);
                            
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
    
    private void updateUserStats(int incrementAmount, int wins, int losses, int draws) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        
        if (currentUserUid != null) {
            DocumentReference userRef = database.collection("users").document(currentUserUid);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("coins", FieldValue.increment(incrementAmount));
            updates.put("onlineWin", FieldValue.increment(wins));
            updates.put("onlineLost", FieldValue.increment(losses));
            updates.put("onlineDraw", FieldValue.increment(draws));
            
            userRef.update(updates)
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update coins.", Toast.LENGTH_SHORT).show());
        }
        
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
    private void matchFeeSoundDialog() {
        // Add sound effects
        playSoundEffect(startGameMediaPlayer);
    
        // Deduct user Coins
        updateUserStats(Lost_Coin, 0, 0, 0);
    
        // Show a popup with a Lottie animation
        showCongratsDialog(getString(R.string.match_started), getString(R.string.match_fee)  + " " + Math.abs(Lost_Coin) + " " + getString(R.string.coins), R.raw.payment_online_animation, "Exit", "Ok");
    }
    private void winSoundStatDialog() {
        // Add sound effects
        playSoundEffect(winSoundMediaPlayer);
    
        // Deduct user Coins
        updateUserStats(Win_Coin, 1, 0, 0);
    
        // Show a popup with a Lottie animation
        showCongratsDialog(getString(R.string.congratulations), getString(R.string.You) + " " + getString(R.string.won)  + " " + Win_Coin + " " + getString(R.string.coins), R.raw.win_animation, "Exit", "Rematch");
    }
    private void drawSoundStatDialog() {
        // Add sound effects
        playSoundEffect(drawSoundMediaPlayer);
    
        // Deduct user Coins
        updateUserStats(Draw_Coin, 0, 0, 1);
    
        // Show a popup with a Lottie animation
        showCongratsDialog(getString(R.string.draw), getString(R.string.You)+" " +getString(R.string.won)  + " " + Draw_Coin + " " + getString(R.string.coins), R.raw.draw_animation, "Exit", "Rematch");
    }
    private void lostSoundStatDialog() {
        // Add sound effects
        playSoundEffect(loseSoundMediaPlayer);
    
        // Deduct user Coins
        updateUserStats(Lost_Coin, 0, 1, 0);
    
        // Show game over popup with a Lottie animation
        showCongratsDialog(getString(R.string.Game_over), getString(R.string.You)+" " +getString(R.string.lost)  + " " + Math.abs(Lost_Coin) + " " + getString(R.string.coins), R.raw.lose_animation, "Exit", "Rematch");
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
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayers();
        deleteSession();
        if (!gameEnd){
            // Deduct user Coins
            updateUserStats(Lost_Coin, 0, 1, 0);
        
            gameEnd = true;
            myGameRef.child("restart").child(ME).setValue(true);
            myGameRef.child("restart").child(OPPONENT).setValue(true);
        }
    
        // Remove the ValueEventListener from myGameRef
        if (myGameRef != null && gameEventListener != null) {
            myGameRef.removeEventListener(gameEventListener);
        }
    
        // Remove any callbacks that were posted to the handler
        handler.removeCallbacksAndMessages(null);
    }
    
    private void checkIfUserLeft() {
        gameEventListener = myGameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    gameEnd = true;
                    // Deduct user Coins
                    updateUserStats(Math.abs(Lost_Coin), 0, 0, 0);
                    showUserLeftAlert();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Database Error", databaseError.getMessage());
            }
        });
    }
    
    private void showUserLeftAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(player2.getText().toString() + " " + "Left")
                .setMessage(player2.getText().toString() + " " + "has left the match. You will get your" + Lost_Coin + " " + "Entry fees coins back.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> finish());
        
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        
        // Automatically dismiss the alert and finish the activity after 5 seconds
        handler.postDelayed(() -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
                finish();
            }
        }, 8000);
    }
    
    private void deleteSession() {
        if (code != null && !code.isEmpty()) {
            myGameRef.child("code").removeValue();
            Toast.makeText(this, "Game session deleted ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
}
