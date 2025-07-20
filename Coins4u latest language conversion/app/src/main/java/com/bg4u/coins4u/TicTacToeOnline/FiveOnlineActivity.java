package com.bg4u.coins4u.TicTacToeOnline;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.bg4u.coins4u.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class FiveOnlineActivity extends AppCompatActivity {

    // Game Constants
    private static final int BOARD_SIZE = 5;
    private static final int WIN_LENGTH = 4;
    private static final String EMPTY_CELL = "";

    // UI Components
    private Toolbar toolbar;
    private CircleImageView player1Pic, player2Pic;
    private TextView player1, player2, player1Coins, player2Coins;
    private TextView player1Score, player2Score, turnText;
    private Chronometer player1Timer, player2Timer;
    private AppCompatButton[][] buttons = new AppCompatButton[BOARD_SIZE][BOARD_SIZE];
    private Button rematchBtn;

    // Game State
    private String matchId, currentUserId, opponentId;
    private String currentPlayerSymbol = "X";
    private boolean isMyTurn = false;
    private boolean isGameActive = true;

    // Firebase
    private FirebaseFirestore db;
    private DocumentReference matchRef;
    private ListenerRegistration matchListener;

    // Media
    private MediaPlayer moveSound, winSound, loseSound, drawSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_five_online_tictactoe);

        initializeFirebase();
        initializeUI();
        initializeGame();
        setupBoardListeners();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        matchId = getIntent().getStringExtra("matchId");
        opponentId = getIntent().getStringExtra("opponentId");
    }

    private void initializeUI() {
        // Toolbar
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Player Views
        player1 = findViewById(R.id.player1);
        player2 = findViewById(R.id.player2);
        player1Coins = findViewById(R.id.player1Coins);
        player2Coins = findViewById(R.id.player2Coins);
        player1Score = findViewById(R.id.player1Score);
        player2Score = findViewById(R.id.player2Score);
        turnText = findViewById(R.id.turn_text);

        // Player Images
        player1Pic = findViewById(R.id.player1Pic);
        player2Pic = findViewById(R.id.player2Pic);

        // Timers
        player1Timer = findViewById(R.id.player1Timer);
        player2Timer = findViewById(R.id.player2Timer);

        // Game Board
        initializeBoardButtons();

        // Rematch Button
        rematchBtn = findViewById(R.id.rematchBtn);
        rematchBtn.setOnClickListener(v -> handleRematch());

        // Media Players
        moveSound = MediaPlayer.create(this, R.raw.player_move_sound);
        winSound = MediaPlayer.create(this, R.raw.win_sound);
        loseSound = MediaPlayer.create(this, R.raw.lose_sound);
        drawSound = MediaPlayer.create(this, R.raw.draw_sound);
    }

    private void initializeBoardButtons() {
        // Initialize 5x5 grid buttons
        buttons[0][0] = findViewById(R.id.button_00);
        buttons[0][1] = findViewById(R.id.button_01);
        buttons[0][2] = findViewById(R.id.button_02);
        buttons[0][3] = findViewById(R.id.button_03);
        buttons[0][4] = findViewById(R.id.button_04);

        buttons[1][0] = findViewById(R.id.button_10);
        buttons[1][1] = findViewById(R.id.button_11);
        buttons[1][2] = findViewById(R.id.button_12);
        buttons[1][3] = findViewById(R.id.button_13);
        buttons[1][4] = findViewById(R.id.button_14);

        buttons[2][0] = findViewById(R.id.button_20);
        buttons[2][1] = findViewById(R.id.button_21);
        buttons[2][2] = findViewById(R.id.button_22);
        buttons[2][3] = findViewById(R.id.button_23);
        buttons[2][4] = findViewById(R.id.button_24);

        buttons[3][0] = findViewById(R.id.button_30);
        buttons[3][1] = findViewById(R.id.button_31);
        buttons[3][2] = findViewById(R.id.button_32);
        buttons[3][3] = findViewById(R.id.button_33);
        buttons[3][4] = findViewById(R.id.button_34);

        buttons[4][0] = findViewById(R.id.button_40);
        buttons[4][1] = findViewById(R.id.button_41);
        buttons[4][2] = findViewById(R.id.button_42);
        buttons[4][3] = findViewById(R.id.button_43);
        buttons[4][4] = findViewById(R.id.button_44);
    }

    private void setupBoardListeners() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                final int row = i;
                final int col = j;
                buttons[i][j].setOnClickListener(v -> handleMove(row, col));
            }
        }
    }

    private void initializeGame() {
        if (matchId == null) {
            createNewMatch();
        } else {
            joinExistingMatch();
        }
        loadPlayerProfiles();
    }

    private void createNewMatch() {
        Map<String, Object> match = new HashMap<>();
        match.put("player1", currentUserId);
        match.put("player2", "");
        match.put("board", createEmptyBoard());
        match.put("currentPlayer", currentUserId);
        match.put("status", "waiting");
        match.put("createdAt", FieldValue.serverTimestamp());

        db.collection("matches").add(match)
                .addOnSuccessListener(ref -> {
                    matchId = ref.getId();
                    matchRef = ref;
                    setupMatchListener();
                })
                .addOnFailureListener(e -> showError("Match creation failed"));
    }

    private void joinExistingMatch() {
        matchRef = db.collection("matches").document(matchId);
        matchRef.update(
                "player2", currentUserId,
                "status", "playing"
        ).addOnSuccessListener(aVoid -> setupMatchListener());
    }

    private void setupMatchListener() {
        matchListener = matchRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                showError("Connection error");
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                updateGameState(snapshot);
            } else {
                handleMatchDeleted();
            }
        });
    }

    private void handleMatchDeleted() {
        Toast.makeText(this, "Match no longer exists", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateGameState(DocumentSnapshot snapshot) {
        String status = snapshot.getString("status");
        if ("ended".equals(status)) {
            handleGameEnd(snapshot);
            return;
        }

        updateBoardUI((ArrayList<ArrayList<String>>) snapshot.get("board"));
        updateTurnIndicator(snapshot.getString("currentPlayer"));
        updateScores((Map<String, Long>) snapshot.get("scores"));
    }

    private void handleGameEnd(DocumentSnapshot snapshot) {
        String winner = snapshot.getString("winner");
        if (winner != null) {
            if (winner.equals(currentUserId)) {
                showGameResult("You Win!");
            } else if (winner.equals("draw")) {
                showGameResult("Game Draw!");
            } else {
                showGameResult("You Lose!");
            }
        }
        rematchBtn.setEnabled(true);
    }

    private void updateTurnIndicator(String currentPlayerId) {
        isMyTurn = currentPlayerId.equals(currentUserId);
        turnText.setText(isMyTurn ? "Your Turn" : "Opponent's Turn");
        turnText.setTextColor(isMyTurn ? Color.GREEN : Color.RED);
    }

    private void updateScores(Map<String, Long> scores) {
        if (scores != null) {
            player1Score.setText(String.format("X: %d", scores.get("X")));
            player2Score.setText(String.format("O: %d", scores.get("O")));
        }
    }

    private boolean isBoardFull(ArrayList<ArrayList<String>> board) {
        for (ArrayList<String> row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void deleteMatchAfterDelay() {
        new Handler().postDelayed(() -> {
            if (matchRef != null) {
                matchRef.delete();
            }
        }, 300000); // 5 minutes
    }


    private void updateBoardUI(ArrayList<ArrayList<String>> boardData) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                String cellValue = boardData.get(i).get(j);
                buttons[i][j].setText(cellValue);
                updateCellAppearance(i, j, cellValue);
            }
        }
    }

    private void updateCellAppearance(int row, int col, String value) {
        int bgRes = R.drawable.option_unselected;
        if (value.equals("X")) {
            bgRes = R.drawable.option_right;
        } else if (value.equals("O")) {
            bgRes = R.drawable.option_yellow;
        }

        buttons[row][col].setBackgroundResource(bgRes);
        buttons[row][col].setEnabled(value.isEmpty() && isMyTurn);
    }

    private void handleMove(int row, int col) {
        if (!isMyTurn) return;

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(matchRef);
                    ArrayList<ArrayList<String>> board = (ArrayList<ArrayList<String>>) snapshot.get("board");

                    if (!board.get(row).get(col).isEmpty()) {
                        throw new FirebaseFirestoreException("Invalid move",
                                FirebaseFirestoreException.Code.ABORTED);
                    }

                    board.get(row).set(col, currentPlayerSymbol);
                    String nextPlayer = snapshot.getString("player1").equals(currentUserId) ?
                            snapshot.getString("player2") : snapshot.getString("player1");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("board", board);
                    updates.put("currentPlayer", nextPlayer);
                    transaction.update(matchRef, updates);

                    return null;
                }).addOnSuccessListener(aVoid -> checkGameStatus())
                .addOnFailureListener(e -> showError("Move failed"));
    }

    private void checkGameStatus() {
        matchRef.get().addOnSuccessListener(snapshot -> {
            ArrayList<ArrayList<String>> board = (ArrayList<ArrayList<String>>) snapshot.get("board");
            if (checkWinCondition(board, currentPlayerSymbol)) {
                endGame(currentUserId);
            } else if (isBoardFull(board)) {
                endGame("draw");
            }
        });
    }

    // Implement 4-in-a-row win check for 5x5 grid
    private boolean checkWinCondition(ArrayList<ArrayList<String>> board, String symbol) {
        // Add your win checking logic here
        return false;
    }

    private void endGame(String result) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "ended");
        updates.put("winner", result);
        updates.put("endedAt", FieldValue.serverTimestamp());

        matchRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    isGameActive = false;
                    showGameResult(result);
                    deleteMatchAfterDelay();
                    updatePlayerStats(result);
                });
    }

    private void showGameResult(String result) {
        if (result.equals(currentUserId)) {
            playSound(winSound);
            Toast.makeText(this, "You Win!", Toast.LENGTH_LONG).show();
        } else if (result.equals("draw")) {
            playSound(drawSound);
            Toast.makeText(this, "Game Draw!", Toast.LENGTH_LONG).show();
        } else {
            playSound(loseSound);
            Toast.makeText(this, "You Lose!", Toast.LENGTH_LONG).show();
        }
    }

    private void updatePlayerStats(String result) {
        Map<String, Object> updates = new HashMap<>();
        int coins = 0;

        if (result.equals(currentUserId)) {
            coins = 100;
            updates.put("wins", FieldValue.increment(1));
        } else if (result.equals("draw")) {
            coins = 50;
            updates.put("draws", FieldValue.increment(1));
        } else {
            coins = -25;
            updates.put("losses", FieldValue.increment(1));
        }

        updates.put("coins", FieldValue.increment(coins));
        db.collection("users").document(currentUserId).update(updates);
    }

    private void handleRematch() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("restartRequested", true);
        updates.put("status", "waiting");
        updates.put("board", createEmptyBoard());

        matchRef.update(updates)
                .addOnSuccessListener(aVoid -> resetGame())
                .addOnFailureListener(e -> showError("Rematch failed"));
    }

    private void resetGame() {
        isGameActive = true;
        currentPlayerSymbol = currentPlayerSymbol.equals("X") ? "O" : "X";
        initializeBoardButtons();
    }

    private ArrayList<ArrayList<String>> createEmptyBoard() {
        ArrayList<ArrayList<String>> board = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            ArrayList<String> row = new ArrayList<>();
            for (int j = 0; j < BOARD_SIZE; j++) {
                row.add(EMPTY_CELL);
            }
            board.add(row);
        }
        return board;
    }

    private void loadPlayerProfiles() {
        loadUserProfile(currentUserId, player1Pic, player1, player1Coins);
        loadUserProfile(opponentId, player2Pic, player2, player2Coins);
    }

    private void loadUserProfile(String userId, CircleImageView imageView, TextView nameView, TextView coinsView) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        nameView.setText(snapshot.getString("name"));
                        coinsView.setText(String.valueOf(snapshot.getLong("coins")));
                        Picasso.get().load(snapshot.getString("profileUrl")).into(imageView);
                    }
                });
    }

    private void playSound(MediaPlayer player) {
        if (player != null) {
            player.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchListener != null) matchListener.remove();
        releaseMediaPlayers();
        if (isGameActive) endGame("abandoned");
    }

    private void releaseMediaPlayers() {
        if (moveSound != null) moveSound.release();
        if (winSound != null) winSound.release();
        if (loseSound != null) loseSound.release();
        if (drawSound != null) drawSound.release();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}