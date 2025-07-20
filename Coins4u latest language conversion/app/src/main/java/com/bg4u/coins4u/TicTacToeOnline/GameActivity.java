package com.bg4u.coins4u.TicTacToeOnline;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;
import com.bg4u.coins4u.DialogBox;
import com.bg4u.coins4u.SubscriptionActivity;
import com.bg4u.coins4u.TicTacToeActivity;
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


public class GameActivity extends AppCompatActivity {
    private OnBackPressedCallback onBackPressedCallback;
    private TextView player1, player1Coins;
    private TextView player2, player2Coins;
    private String code, friendUid;
    private String ME, OPPONENT;
    private Button restartBtn;
    private String turn;
    private String firstTurn;
    private int MY_SCORE, OPPONENT_SCORE;
    
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myGameRef;
    private ValueEventListener gameEventListener;
    private ListenerRegistration registration;
    private String currentUserUid;
    private AppCompatButton[][] buttons;
    private TextView turnText;
    private TextView player1Score, player2Score;
    
    private Chronometer player1Timer;
    private Chronometer player2Timer;
    private long startTimePlayer1;
    private long startTimePlayer2;
    private boolean isPlayer1Running = false;
    private boolean isPlayer2Running = false;
    private long elapsedTimePlayer1;
    private long elapsedTimePlayer2;
    
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
    private boolean showOnlyOnce = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_tictactoe);
        
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Tic-Tac-Toe Online");

        // Handle back button press with onBackPressedDispatcher
        // Get the OnBackPressedDispatcher from the activity
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();

        // Add a callback to the dispatcher
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button action here
                showExitDialog();
            }
        };
        dispatcher.addCallback(onBackPressedCallback);

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback);

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
        
        player1Timer = findViewById(R.id.player1Timer);
        player2Timer = findViewById(R.id.player2Timer);
        player1Timer.setFormat("%s");
        player2Timer.setFormat("%s");
        player1Timer.setBase(SystemClock.elapsedRealtime());
        player2Timer.setBase(SystemClock.elapsedRealtime());
        
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
                if (checkWinning().equals(EMPTY_CELL)) {  // Use showOnlyOnce here
                    if(check_draw()) {
                        turnText.setText("Draw");
                        gameEnd = true;
                        if (showOnlyOnce) {
                            calculateTimerWinner();
                            showOnlyOnce = false;
                        }
                    }
                } else {
                    String winner = checkWinning();
                    if (winner.equals(ME)) {
                        MY_SCORE = dataSnapshot.child("scores").child(ME).getValue(Integer.class);
                        player1Score.setText(ME + " - " + MY_SCORE);
                        
                        turnText.setText(player1.getText().toString() + " WON!");
                        if(showOnlyOnce){
                            winSoundStatDialog();
                            showOnlyOnce = false;
                        }
                    } else {
                        OPPONENT_SCORE = dataSnapshot.child("scores").child(OPPONENT).getValue(Integer.class);
                        player2Score.setText(OPPONENT + " - " + OPPONENT_SCORE);
                        
                        turnText.setText(player1.getText().toString() + " Lost");
                        if(showOnlyOnce){
                            lostSoundStatDialog();
                            showOnlyOnce = false;
                        }
                    }
                    turnText.setText(winner + " WON!");
                    gameEnd = true;
                    pausePlayer1Timer();
                    pausePlayer2Timer();
                }
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
    
    
    public void startPlayer1Timer() {
        if (!isPlayer1Running) {
            player1Timer.setBase(SystemClock.elapsedRealtime() - elapsedTimePlayer1);
            player1Timer.start();
            isPlayer1Running = true;
        }
    }
    
    public void pausePlayer1Timer() {
        if (isPlayer1Running) {
            player1Timer.stop();
            elapsedTimePlayer1 = SystemClock.elapsedRealtime() - player1Timer.getBase();
            isPlayer1Running = false;
        }
    }
    
    public void resetPlayer1Timer() {
        player1Timer.setBase(SystemClock.elapsedRealtime());
        elapsedTimePlayer1 = 0;
        isPlayer1Running = false;
    }
    
    public void startPlayer2Timer() {
        if (!isPlayer2Running) {
            player2Timer.setBase(SystemClock.elapsedRealtime() - elapsedTimePlayer2);
            player2Timer.start();
            isPlayer2Running = true;
        }
    }
    
    public void pausePlayer2Timer() {
        if (isPlayer2Running) {
            player2Timer.stop();
            elapsedTimePlayer2 = SystemClock.elapsedRealtime() - player2Timer.getBase();
            isPlayer2Running = false;
        }
    }
    
    public void resetPlayer2Timer() {
        player2Timer.setBase(SystemClock.elapsedRealtime());
        elapsedTimePlayer2 = 0;
        isPlayer2Running = false;
    }
    
    private void calculateTimerWinner() {
        pausePlayer1Timer();
        pausePlayer2Timer();
        
        long player1Time = elapsedTimePlayer1;
        long player2Time = elapsedTimePlayer2;
    
        restartBtn.setAlpha(1f);
        
        if (isPlayer1Running) {
            player1Time = SystemClock.elapsedRealtime() - player1Timer.getBase();
        }
        if (isPlayer2Running) {
            player2Time = SystemClock.elapsedRealtime() - player2Timer.getBase();
        }
        
        if (player1Time < player2Time) {
            turnText.setText(ME + " Winner");
            MY_SCORE += 1;
            Toast.makeText(this, ME + " BOOYAH", Toast.LENGTH_SHORT).show();
            if (showOnlyOnce) {
                winSoundStatDialog();
                showOnlyOnce = false;
            }
            player1Score.setText(ME + " - " + MY_SCORE);
            myGameRef.child("scores").child(checkWinning()).setValue(MY_SCORE);
        } else if (player1Time > player2Time) {
            turnText.setText(OPPONENT + " Winner");
            OPPONENT_SCORE += 1;
            Toast.makeText(this, ME +" DEFEAT!", Toast.LENGTH_SHORT).show();
            if (showOnlyOnce) {
                lostSoundStatDialog();
                showOnlyOnce = false;
            }
            player2Score.setText(OPPONENT + " - " + OPPONENT_SCORE);
            myGameRef.child("scores").child(checkWinning()).setValue(OPPONENT_SCORE);
        } else {
            turnText.setText("Match Draw!");
            gameEnd = true;
            Toast.makeText(this, "It's a tie!", Toast.LENGTH_SHORT).show();
            if (showOnlyOnce) {
                drawSoundStatDialog();
                showOnlyOnce = false;
            }
        }
    }
    
    private void whoIsXandWhoIsO() {
        if (ME.equals("X")) {
            OPPONENT = "O";
            player1Pic.setBorderColor(ContextCompat.getColor(GameActivity.this, R.color.green));
            player1Score.setTextColor(getColor(R.color.green));
            player1.setTextColor(getColor(R.color.green));
            
            player2Pic.setBorderColor(ContextCompat.getColor(GameActivity.this, R.color.yellow));
            player2Score.setTextColor(getColor(R.color.yellow));
            player2.setTextColor(getColor(R.color.yellow));
            
        } else {
            OPPONENT = "X";
            player2Pic.setBorderColor(ContextCompat.getColor(GameActivity.this, R.color.green));
            player2Score.setTextColor(getColor(R.color.green));
            player2.setTextColor(getColor(R.color.green));
            
            player1Pic.setBorderColor(ContextCompat.getColor(GameActivity.this, R.color.yellow));
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
                } else {
                    showToast("Opponent is not ready yet...");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle failure
            }
        });
    
        myGameRef.child("restart").child(ME).setValue(true);
        resetPlayer1Timer();
        resetPlayer2Timer();
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
                    if(checkWinning().equals("-")) {
                        if(check_draw()) {
                            calculateTimerWinner();
                        }
                    } else{
                        turnText.setText(checkWinning() + " WON!");
                        gameEnd = true;
                        showOnlyOnce = true;
                        restartBtn.setAlpha(1f);
    
                        if(checkWinning().equals(ME)) {
                            turnText.setText(ME + " Winner");
                            MY_SCORE += 1;
                            player1Score.setText(ME + " - " + MY_SCORE);
                            myGameRef.child("scores").child(checkWinning()).setValue(MY_SCORE);
                            
                        }else {
                            turnText.setText(OPPONENT + " Winner");
                            OPPONENT_SCORE += 1;
                            if(showOnlyOnce){
                                lostSoundStatDialog();
                            }
                            player2Score.setText(OPPONENT + " - " + OPPONENT_SCORE);
                            myGameRef.child("scores").child(checkWinning()).setValue(OPPONENT_SCORE);
                        }
                    }
                }
            }
        }
    }
    
    private void updateFB(int i) {
        myGameRef.child("board").child(String.valueOf(i)).setValue(board[(i-1)/3][(i-1)%3]);
        myGameRef.child("turn").setValue(turn);
        myGameRef.child("timer").child(ME).setValue(elapsedTimePlayer1);
        myGameRef.child("timer").child(OPPONENT).setValue(elapsedTimePlayer2);
    
        showOnlyOnce = true;
//        if(Objects.equals(turn, ME)){
//            pausePlayer2Timer();
//            resumePlayer1Timer();
//        } else if (Objects.equals(turn, OPPONENT)) {
//            pausePlayer1Timer();
//            resumePlayer2Timer();
//        }
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
            
            if(showOnlyOnce) {
                matchFeeSoundDialog();
                showOnlyOnce = false;
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
            
            // Timer
            myGameRef.child("timer").child(ME).setValue(0);
            myGameRef.child("timer").child(OPPONENT).setValue(0);
            
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
        if(Objects.equals(turn, ME)){
            startPlayer1Timer();
            pausePlayer2Timer();
        }else if(turn.equals(OPPONENT)){
            startPlayer2Timer();
            pausePlayer1Timer();
        }
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
        if (i < 1 || i > 9) return false;
        return board[(i - 1) / 3][(i - 1) % 3].equals("-");
    }
    
    private String checkWinning() {
        for(int i = 0; i < 3; i++) {
            if(board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2]) && ! board[i][0].equals("-")) {
                
                    buttons[i][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                    buttons[i][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                    buttons[i][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                
                return board[i][0];
            }
            if(board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i]) && ! board[0][i].equals("-")) {
                
                    buttons[0][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                    buttons[1][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                    buttons[2][i].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
                
                return board[0][i];
            }
        }
        if(board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2]) && ! board[0][0].equals("-")) {
            
            buttons[0][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[1][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[2][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            return board[0][0];
        }
        if(board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0]) && ! board[0][2].equals("-")) {
            
            buttons[0][2].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[1][1].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            buttons[2][0].setBackground(ContextCompat.getDrawable(this, R.drawable.option_wrong));
            return board[0][2];
        }
        return "-";
    }
    
    private boolean check_draw() {
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                if(board[i][j].equals("-")) {
                    return false;
                }
            }
        }
        return true;
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
            View customView = LayoutInflater.from(GameActivity.this).inflate(R.layout.dialog_box_layout, null);
            
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
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        String message;
        if (!gameEnd) {
            message = "If you leave this match, Your" + " " +  Math.abs(Lost_Coin) + " " + "Coins will be deducted";
        } else {
            message = "Are you sure you want to left this match";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Leave Match")
                .setIcon(R.drawable.logout)
                .setPositiveButton("Yes", (dialog, id) -> {
                    if (!gameEnd) {
                        // Deduct user Coins
                        updateUserStats(Lost_Coin, 0, 1, 0);
                        
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
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to leave this match...")
                .setTitle("Leave Match")
                .setIcon(R.drawable.logout)
                .setPositiveButton("Yes", (dialog, id) -> {
                    Intent intent = new Intent(this, TicTacToeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, id) -> {
                    // User cancelled the dialog, do nothing
                })
                .setOnCancelListener(dialog -> {
                    // Handle the back button press properly
                        finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
