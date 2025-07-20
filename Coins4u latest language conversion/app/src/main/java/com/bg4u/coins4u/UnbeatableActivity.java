package com.bg4u.coins4u;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class UnbeatableActivity extends AppCompatActivity {

    // Constants representing cell states.
    private static final int EMPTY = 0;
    private static final int HUMAN = 1;
    private static final int BOT = 2;

    // Board size and winning streak requirement.
    private static final int BOARD_SIZE = 5;
    private static final int WIN_LENGTH = 4;  // Four in a row wins.

    // The game board array and corresponding UI button grid.
    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private Button[][] buttons = new Button[BOARD_SIZE][BOARD_SIZE];

    // Adjustable AI "mistake" probability.
    // For the hardest difficulty (best AI), set to 0.0.
    private double aiMistakeChance = 0.0;

    // Scoreboard coin values.
    private int Win_Coin = 100;
    private int Lost_Coin = -10;
    private int Draw_Coin = 15;

    // Game state flags.
    private boolean gameOver = false;
    // Flag to ensure only one move per turn.
    private boolean isUserTurn = true;

    private Random random = new Random();

    // Firebase instances.
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;

    // MediaPlayer instances for sound effects.
    private MediaPlayer startGameMediaPlayer;
    private MediaPlayer playerMoveSoundMediaPlayer;
    private MediaPlayer computerMoveSoundMediaPlayer;
    private MediaPlayer winSoundMediaPlayer;
    private MediaPlayer loseSoundMediaPlayer;
    private MediaPlayer drawSoundMediaPlayer;

    // AdView for banner ads.
    private AdView mAdView;

    // Logging tag.
    private static final String TAG = "UnbeatableActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_tic_tac_toe_5x5); // Your 5×5 grid layout

        // Initialize Firebase instances.
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize sound effects.
        startGameMediaPlayer = MediaPlayer.create(this, R.raw.start_game_sound);
        playerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.player_move_sound);
        computerMoveSoundMediaPlayer = MediaPlayer.create(this, R.raw.computer_move_sound);
        winSoundMediaPlayer = MediaPlayer.create(this, R.raw.win_sound);
        loseSoundMediaPlayer = MediaPlayer.create(this, R.raw.lose_sound);
        drawSoundMediaPlayer = MediaPlayer.create(this, R.raw.draw_sound);

        // Play start game sound.
        startGameMediaPlayer.start();

        // Initialize AdView (ensure your layout has an AdView with id "adView").
        // mAdView = findViewById(R.id.adView);

        // Initialize the grid buttons and reset the board.
        initButtons();
        resetBoard();

        // Reset button listener to restart the game.
        Button resetBtn = findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetBoard();
            }
        });
    }

    /**
     * Initializes the button grid by iterating over the TableLayout rows and cells.
     * An onClick listener is added to each cell, which processes the user's move
     * only if the game is not over, it is the user's turn, and the cell is empty.
     */
    private void initButtons() {
        TableLayout tableLayout = findViewById(R.id.tableLayout);
        for (int i = 0; i < BOARD_SIZE; i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j] = (Button) row.getChildAt(j);
                final int rowIdx = i;
                final int colIdx = j;
                buttons[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Process move only if the game is active, it's the user's turn, and the cell is empty.
                        if (!gameOver && isUserTurn && board[rowIdx][colIdx] == EMPTY) {
                            // Lock the board until the AI has made its move.
                            isUserTurn = false;
                            makeMove(rowIdx, colIdx, HUMAN);
                            playerMoveSoundMediaPlayer.start();

                            // Check for a win for HUMAN.
                            List<int[]> winLine = getWinningLine(HUMAN);
                            if (winLine != null) {
                                gameOver = true;
                                highlightWinningLine(winLine);
                                winSoundMediaPlayer.start();
                                // Update user stats: win.
                                updateUserStats(Win_Coin, 1, 0, 0);
                                showCongratsDialog("Congratulations!", "You win!", R.raw.win_animation, "Quit", null);
                            } else if (isBoardFull()) {
                                gameOver = true;
                                drawSoundMediaPlayer.start();
                                // Update user stats: draw.
                                updateUserStats(Draw_Coin, 0, 0, 1);
                                // Show ads on draw.
                                adsData();
                                showCongratsDialog("Draw!", "It's a draw!", R.raw.draw_animation, "Quit", null);
                            } else {
                                // Delay AI move to simulate thinking.
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        botTurn();
                                    }
                                }, 500);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Resets the game board and state.
     * Also randomly selects which side (user or AI) goes first.
     * If the AI is chosen to start, it makes its move after a short delay.
     */
    private void resetBoard() {
        gameOver = false;
        // Randomly choose the first turn.
        isUserTurn = random.nextBoolean();

        // Clear the board state and update UI.
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                // Reset each cell's background.
                buttons[i][j].setBackground(ContextCompat.getDrawable(this, R.drawable.option_unselected));
            }
        }

        // If the AI is selected to start, schedule its move.
        if (!isUserTurn) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    botTurn();
                }
            }, 500);
        }
    }

    /**
     * Makes a move for the given player at the specified cell.
     * Updates the internal board, sets the marker ("X" for HUMAN, "O" for BOT) in deep black,
     * disables the cell, and changes the background (yellow for HUMAN, green for BOT).
     *
     * @param row    Row index.
     * @param col    Column index.
     * @param player HUMAN or BOT.
     */
    private void makeMove(int row, int col, int player) {
        board[row][col] = player;
        buttons[row][col].setText(player == HUMAN ? "X" : "O");
        buttons[row][col].setTextColor(Color.BLACK);
        buttons[row][col].setEnabled(false);
        if (player == HUMAN) {
            buttons[row][col].setBackground(ContextCompat.getDrawable(this, R.drawable.option_yellow));
        } else {
            buttons[row][col].setBackground(ContextCompat.getDrawable(this, R.drawable.option_green));
        }
    }

    /**
     * Executes the AI's move using a minimax algorithm with alpha–beta pruning
     * (with a search depth of 4) and a heuristic evaluation of the board.
     * With a probability (aiMistakeChance), the AI may choose a random move.
     * On game loss or draw, ads are loaded.
     */
    private void botTurn() {
        if (gameOver) return;

        int[] move;
        if (random.nextDouble() < aiMistakeChance) {
            move = chooseRandomMove();
        } else {
            move = chooseBestMove();
        }

        if (move != null) {
            makeMove(move[0], move[1], BOT);
            computerMoveSoundMediaPlayer.start();
            List<int[]> winLine = getWinningLine(BOT);
            if (winLine != null) {
                gameOver = true;
                highlightWinningLine(winLine);
                loseSoundMediaPlayer.start();
                // Update user stats: loss.
                updateUserStats(Lost_Coin, 0, 1, 0);
                // Load ads on loss.
                adsData();
                showCongratsDialog("Oh no!", "Bot wins!", R.raw.lose_animation, "Quit", null);
            } else if (isBoardFull()) {
                gameOver = true;
                drawSoundMediaPlayer.start();
                // Update user stats: draw.
                updateUserStats(Draw_Coin, 0, 0, 1);
                // Load ads on draw.
                adsData();
                showCongratsDialog("Draw!", "It's a draw!", R.raw.draw_animation, "Quit", null);
            } else {
                // Unlock the board for the user's move.
                isUserTurn = true;
            }
        }
    }

    /**
     * Returns a random available move from the board.
     *
     * @return An array [row, col] representing the move, or null if none are available.
     */
    private int[] chooseRandomMove() {
        List<int[]> availableMoves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    availableMoves.add(new int[]{i, j});
                }
            }
        }
        if (!availableMoves.isEmpty()) {
            return availableMoves.get(random.nextInt(availableMoves.size()));
        }
        return null;
    }

    /**
     * Uses minimax with alpha–beta pruning (search depth 4) to choose the best move for BOT.
     *
     * @return An array [row, col] representing the best move.
     */
    private int[] chooseBestMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        int depth = 4;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = BOT;
                    int score = minimax(depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    board[i][j] = EMPTY;
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new int[]{i, j};
                    }
                }
            }
        }
        if (bestMove == null) {
            bestMove = chooseRandomMove();
        }
        return bestMove;
    }

    /**
     * Implements the minimax algorithm with alpha–beta pruning.
     * Terminal conditions:
     * - Returns a high positive value if BOT wins.
     * - Returns a high negative value if HUMAN wins.
     * - Otherwise, returns a heuristic evaluation of the board.
     *
     * @param depth        Remaining search depth.
     * @param isMaximizing True if it's BOT's turn.
     * @param alpha        Current alpha value.
     * @param beta         Current beta value.
     * @return The evaluation score.
     */
    private int minimax(int depth, boolean isMaximizing, int alpha, int beta) {
        if (getWinningLine(BOT) != null) {
            return 1000;
        }
        if (getWinningLine(HUMAN) != null) {
            return -1000;
        }
        if (depth == 0 || isBoardFull()) {
            return evaluateBoard();
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == EMPTY) {
                        board[i][j] = BOT;
                        int eval = minimax(depth - 1, false, alpha, beta);
                        board[i][j] = EMPTY;
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            return maxEval; // Beta cutoff.
                        }
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == EMPTY) {
                        board[i][j] = HUMAN;
                        int eval = minimax(depth - 1, true, alpha, beta);
                        board[i][j] = EMPTY;
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if (beta <= alpha) {
                            return minEval; // Alpha cutoff.
                        }
                    }
                }
            }
            return minEval;
        }
    }

    /**
     * A heuristic evaluation function for the current board state.
     * It scans every possible line (rows, columns, and diagonals of length WIN_LENGTH)
     * and awards or deducts points if the line contains only BOT or only HUMAN markers.
     *
     * @return The heuristic score.
     */
    private int evaluateBoard() {
        int score = 0;
        // Evaluate rows.
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j <= BOARD_SIZE - WIN_LENGTH; j++) {
                int botCount = 0, humanCount = 0;
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i][j + k] == BOT) botCount++;
                    if (board[i][j + k] == HUMAN) humanCount++;
                }
                if (botCount > 0 && humanCount == 0) {
                    score += getLineScore(botCount);
                } else if (humanCount > 0 && botCount == 0) {
                    score -= getLineScore(humanCount);
                }
            }
        }
        // Evaluate columns.
        for (int j = 0; j < BOARD_SIZE; j++) {
            for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
                int botCount = 0, humanCount = 0;
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j] == BOT) botCount++;
                    if (board[i + k][j] == HUMAN) humanCount++;
                }
                if (botCount > 0 && humanCount == 0) {
                    score += getLineScore(botCount);
                } else if (humanCount > 0 && botCount == 0) {
                    score -= getLineScore(humanCount);
                }
            }
        }
        // Evaluate main diagonals.
        for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
            for (int j = 0; j <= BOARD_SIZE - WIN_LENGTH; j++) {
                int botCount = 0, humanCount = 0;
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j + k] == BOT) botCount++;
                    if (board[i + k][j + k] == HUMAN) humanCount++;
                }
                if (botCount > 0 && humanCount == 0) {
                    score += getLineScore(botCount);
                } else if (humanCount > 0 && botCount == 0) {
                    score -= getLineScore(humanCount);
                }
            }
        }
        // Evaluate anti-diagonals.
        for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
            for (int j = WIN_LENGTH - 1; j < BOARD_SIZE; j++) {
                int botCount = 0, humanCount = 0;
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j - k] == BOT) botCount++;
                    if (board[i + k][j - k] == HUMAN) humanCount++;
                }
                if (botCount > 0 && humanCount == 0) {
                    score += getLineScore(botCount);
                } else if (humanCount > 0 && botCount == 0) {
                    score -= getLineScore(humanCount);
                }
            }
        }
        return score;
    }

    /**
     * Returns a score based on the number of markers in a pure line.
     * Example: 1 marker = 1 point, 2 markers = 10 points, 3 markers = 50 points, and 4 markers = 1000 points.
     *
     * @param count Number of markers in the line.
     * @return The line score.
     */
    private int getLineScore(int count) {
        switch (count) {
            case 1: return 1;
            case 2: return 10;
            case 3: return 50;
            case 4: return 1000;
            default: return 0;
        }
    }

    /**
     * Checks whether the specified player has a winning line (four in a row).
     * If found, returns a list of cell coordinates forming that winning line; otherwise, returns null.
     *
     * @param player HUMAN or BOT.
     * @return A list of int[] coordinates, or null.
     */
    private List<int[]> getWinningLine(int player) {
        // Check rows.
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j <= BOARD_SIZE - WIN_LENGTH; j++) {
                boolean win = true;
                List<int[]> line = new ArrayList<>();
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i][j + k] != player) {
                        win = false;
                        break;
                    }
                    line.add(new int[]{i, j + k});
                }
                if (win) return line;
            }
        }
        // Check columns.
        for (int j = 0; j < BOARD_SIZE; j++) {
            for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
                boolean win = true;
                List<int[]> line = new ArrayList<>();
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j] != player) {
                        win = false;
                        break;
                    }
                    line.add(new int[]{i + k, j});
                }
                if (win) return line;
            }
        }
        // Check main diagonals.
        for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
            for (int j = 0; j <= BOARD_SIZE - WIN_LENGTH; j++) {
                boolean win = true;
                List<int[]> line = new ArrayList<>();
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j + k] != player) {
                        win = false;
                        break;
                    }
                    line.add(new int[]{i + k, j + k});
                }
                if (win) return line;
            }
        }
        // Check anti-diagonals.
        for (int i = 0; i <= BOARD_SIZE - WIN_LENGTH; i++) {
            for (int j = WIN_LENGTH - 1; j < BOARD_SIZE; j++) {
                boolean win = true;
                List<int[]> line = new ArrayList<>();
                for (int k = 0; k < WIN_LENGTH; k++) {
                    if (board[i + k][j - k] != player) {
                        win = false;
                        break;
                    }
                    line.add(new int[]{i + k, j - k});
                }
                if (win) return line;
            }
        }
        return null;
    }

    /**
     * Highlights the winning line by changing the background of those cells to red.
     *
     * @param winLine A list of int[] coordinates representing the winning cells.
     */
    private void highlightWinningLine(List<int[]> winLine) {
        for (int[] cell : winLine) {
            int row = cell[0];
            int col = cell[1];
            buttons[row][col].setBackground(ContextCompat.getDrawable(this, R.drawable.option_red));
        }
    }

    /**
     * Checks whether the board is completely full.
     *
     * @return True if there are no empty cells, false otherwise.
     */
    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) return false;
            }
        }
        return true;
    }

    /**
     * Updates the user’s statistics in Firestore.
     * The provided increments are applied to the coins and game stats fields.
     *
     * @param incrementAmount The coin increment (positive or negative).
     * @param wins            The win count increment.
     * @param losses          The loss count increment.
     * @param draws           The draw count increment.
     */
    private void updateUserStats(int incrementAmount, int wins, int losses, int draws) {
        // Get Firestore instance and current user ID.
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
                    .addOnSuccessListener(aVoid -> Toast.makeText(UnbeatableActivity.this, "Stats updated.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(UnbeatableActivity.this, "Failed to update stats.", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Loads ads based on Firestore settings. It listens to the "app_updates/ads" document.
     * If ads are enabled, it loads a banner ad (and rewarded ad) into the appropriate views.
     */
    private void adsData() {
        DocumentReference docRef = database.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(UnbeatableActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore error: " + error.getMessage());
                    return;
                }
                if (value != null && value.exists()) {
                    AdsModel data = value.toObject(AdsModel.class);
                    if (data != null && data.getAdsStatus()) {
                        Log.d(TAG, "Ads data fetched: " + data.toString());
                        if (mAdView != null) {
                            AdRequest adRequest = new AdRequest.Builder().build();
                            mAdView.loadAd(adRequest);
                            // Load rewarded ad if desired.
                            loadRewardedAd();
                        } else {
                            Log.e(TAG, "AdView is not initialized.");
                        }
                    } else {
                        Log.e(TAG, "Ads disabled or AdsModel null.");
                    }
                } else {
                    Log.e(TAG, "Document does not exist.");
                }
            }
        });
    }

    /**
     * Placeholder method to load a rewarded ad.
     * Implement your rewarded ad loading logic here.
     */
    private void loadRewardedAd() {
        // Your rewarded ad logic goes here.
    }

    /**
     * Displays a custom congratulatory dialog with a title, body text, and Lottie animation.
     * The dialog provides two buttons: one to quit (or navigate to another activity) and one to restart the game.
     *
     * @param title           The title text.
     * @param body            The body text.
     * @param animationRes    The Lottie animation resource.
     * @param leftButtonLabel The label for the left button.
     * @param activityClass   If not null, the activity to navigate to when the left button is pressed.
     */
    private void showCongratsDialog(String title, String body, int animationRes, String leftButtonLabel, Class<?> activityClass) {
        if (!isFinishing()) {
            View customView = LayoutInflater.from(UnbeatableActivity.this).inflate(R.layout.dialog_box_layout, null);
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogBody = customView.findViewById(R.id.dialog_body);
            dialogTitle.setText(title);
            dialogBody.setText(body);
            LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
            lottieAnimation.setAnimation(animationRes);
            DialogBox dialogBox = new DialogBox(UnbeatableActivity.this, customView);
            Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;

            dialogBox.setLeftButton(leftButtonLabel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogBox.dismiss();
                    if (activityClass != null) {
                        finish();
                        startActivity(new Intent(UnbeatableActivity.this, activityClass));
                    }
                }
            });

            dialogBox.setRightButton("Restart", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogBox.dismiss();
                    resetBoard();
                }
            });

            dialogBox.show();
        }
    }

    /**
     * Optional helper method to reset the game (can be called from dialog callbacks).
     */
    private void resetGame() {
        resetBoard();
    }
}
