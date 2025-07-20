package com.bg4u.coins4u;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bg4u.coins4u.R;
import com.bg4u.coins4u.databinding.ActivityQuizBinding;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
public class QuizActivity extends AppCompatActivity {


    private RewardedAd rewardedAd;
    private AdView mAdView;
    private boolean isRewardedAdLoaded = false;
    private OnBackPressedCallback onBackPressedCallback;
    ActivityQuizBinding binding;
    ArrayList<Question> questions;

    // Initialize MediaPlayer objects at the class level
    MediaPlayer correctSoundEffect;
    MediaPlayer wrongSoundEffect;

    int index = 0;
    private final boolean backPressedOnce = false;
    boolean answered = false;
    Question question;
    CountDownTimer timer;
    FirebaseFirestore database;
    int correctAnswers = 0;
    int wrongAnswers = 0; // New field for tracking the number of wrong answers
    int totalWrongAnswers = 0; // Initialize the counter for total wrong answers to show ads
    int totalRightAnswers = 0; // Initialize the counter for total wrong answers to show ads

    int catCoin = 1;
    int catLostCoin = 1;
    String catName = "";
    String catImage = "";
    private boolean isActivityValid = false; // Add this flag at the class level

    // Create a method to transition to the ResultActivity
    private void goToResultActivity() {

        // Create an intent to navigate to the ResultActivity
        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtra("correct", correctAnswers);
        intent.putExtra("wrong", wrongAnswers);
        intent.putExtra("total", questions.size()); // Sum of right and wrong answers
        intent.putExtra("catCoin", catCoin); // Pass the catCoin value to the ResultActivity
        intent.putExtra("catLostCoin", catLostCoin); // Pass the catLostCoin value to the ResultActivity
        intent.putExtra("catName", catName); // Pass the catName to the ResultActivity
        intent.putExtra("catImage", catImage); // Pass the catImage to the ResultActivity
        startActivity(intent);

        finishAffinity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());

        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

        // Show the status bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(binding.getRoot());

        isActivityValid = true;

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

        binding.quitBtn.setOnClickListener(view -> onBackPressed());

        questions = new ArrayList<>();
        database = FirebaseFirestore.getInstance();

        // Find the AdView and load an ad
        mAdView = findViewById(R.id.adView);

        // Find the AdView and load an ad
        adsData();

        final String catId = getIntent().getStringExtra("catId");
        catCoin = getIntent().getIntExtra("catCoin", 0);
        catLostCoin = getIntent().getIntExtra("catLostCoin", 0);

        catName = getIntent().getStringExtra("catName");
        catImage = getIntent().getStringExtra("catImage"); // Get the quiz Image category Image

        binding.quizTitle.setText(catName); // Set the quiz title with the category name
        binding.catWinCoins.setText(String.valueOf(catCoin)); // Set the quiz title with the category name
        binding.catLostCoins.setText(String.valueOf(catLostCoin)); // Set the quiz title with the category name

        Random random = new Random();
        final int rand = random.nextInt(11);

        assert catId != null;
        database.collection("categories")
                .document(catId)
                .collection("questions")
                //      .whereGreaterThanOrEqualTo("index", rand)
                //      .orderBy("index")  // Remove the ordering by index
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.getDocuments().size() < 10) {
                        database.collection("categories")
                                .document(catId)
                                .collection("questions")
                                //        .whereLessThanOrEqualTo("index", rand)
                                //        .orderBy("index")
                                .limit(10)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                    ArrayList<Question> tempQuestions = new ArrayList<>();
                                    for (DocumentSnapshot snapshot : queryDocumentSnapshots1) {
                                        Question question = snapshot.toObject(Question.class);
                                        tempQuestions.add(question);
                                    }
                                    Collections.shuffle(tempQuestions); // Shuffle the questions
                                    questions.addAll(tempQuestions);
                                    setNextQuestion();
                                })
                                .addOnFailureListener(e -> {
                                    // Handle query failure
                                    Toast.makeText(QuizActivity.this, "Error loading questions", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        ArrayList<Question> tempQuestions = new ArrayList<>();
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                            Question question = snapshot.toObject(Question.class);
                            tempQuestions.add(question);
                        }
                        Collections.shuffle(tempQuestions); // Shuffle the questions
                        questions.addAll(tempQuestions);
                        setNextQuestion();
                    }
                });

        // Initialize the MediaPlayer objects
        correctSoundEffect = MediaPlayer.create(this, R.raw.correct_sound_effect);
        wrongSoundEffect = MediaPlayer.create(this, R.raw.wrong_sound_effect);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Handle back button press
            }
        };

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void adsData() {
        DocumentReference docRef = database.collection("app_updates").document("ads");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(QuizActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

    void resetTimer() {
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.timer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                // Move to the next question
                if (index < questions.size()) {
                    index++;
                    wrongAnswers++;
                    totalWrongAnswers++;
                    reset();
                    setNextQuestion();
                } else {
                    goToResultActivity();
                }
            }
        };
        timer.start(); // Start the timer
    }

    void showAnswer() {
        // Get the correct answer from the current question
        String correctAnswer = question.getAnswer();
        // Set the background drawable of the correct answer option
        if (correctAnswer.equals(binding.option1.getText().toString())) {
            setCorrectBackground(binding.option1);
        } else if (correctAnswer.equals(binding.option2.getText().toString())) {
            setCorrectBackground(binding.option2);
        } else if (correctAnswer.equals(binding.option3.getText().toString())) {
            setCorrectBackground(binding.option3);
        } else if (correctAnswer.equals(binding.option4.getText().toString())) {
            setCorrectBackground(binding.option4);
        }
    }

    void setNextQuestion() {
        if (index < questions.size() && isActivityValid) {
            binding.questionCounter.setText(String.format(Locale.getDefault(), "%d/%d", (index + 1), questions.size()));
            question = questions.get(index);
            binding.question.setText(question.getQuestion());
            binding.option1.setText(question.getOption1());
            binding.option2.setText(question.getOption2());
            binding.option3.setText(question.getOption3());
            binding.option4.setText(question.getOption4());

            // Shuffle the options
            ArrayList<String> options = new ArrayList<>();
            options.add(question.getOption1());
            options.add(question.getOption2());
            options.add(question.getOption3());
            options.add(question.getOption4());
            Collections.shuffle(options);

            binding.option1.setText(options.get(0));
            binding.option2.setText(options.get(1));
            binding.option3.setText(options.get(2));
            binding.option4.setText(options.get(3));

            resetTimer(); // Reset the timer for the next question
            reset(); // Reset the button for the next question
            answered = false; // Reset the answered flag

            // Enable selecting Option
            binding.option1.setEnabled(true);
            binding.option2.setEnabled(true);
            binding.option3.setEnabled(true);
            binding.option4.setEnabled(true);

            // Load the image for the question
            if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
                binding.questionImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(question.getImageUrl())
                        .into(binding.questionImage);
            } else {
                binding.questionImage.setVisibility(View.GONE);
            }
            index++; // Move to the next question
        } else {
            goToResultActivity();
        }
    }

    void checkAnswer(TextView textView) {
        if (!isActivityValid || question == null) {
            return;
        }
        answered = true; // Set the answered flag to true
        String selectedAnswer = textView.getText().toString();

        // Count wrong answers and show the interstitial ad if total wrong answers is a multiple of 3
        if (answered && !selectedAnswer.equals(question.getAnswer())) { // Check if user answered and the answer is wrong
            totalWrongAnswers++;
            totalRightAnswers = 0; // Reset the counter if user gives a correct answer

            if (totalWrongAnswers % 2 == 0) { //    Check if total wrong answers is a multiple of 3
                if(isRewardedAdLoaded){
                    showRewardedAd();
                }
            }
        } else {
            totalWrongAnswers = 0; // Reset the counter if user gives a correct answer
            totalRightAnswers ++; // count correct answer

            if (totalRightAnswers % 4 == 0) { // Check if total right answers is a multiple of 3
                if(isRewardedAdLoaded){
                    showRewardedAd();
                }
            }
        }

        if (selectedAnswer.equals(question.getAnswer())) {
            correctAnswers++;
            setCorrectBackground(textView);
            // Play the correct sound effect if the MediaPlayer object is not null
            if (correctSoundEffect != null) {
                correctSoundEffect.start();
            }
        } else {
            wrongAnswers++;
            showAnswer();
            setWrongBackground(textView);
            // Vibrate when the user answers incorrectly
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(200); // Vibrate for 200 milliseconds
            }
            // Play the wrong sound effect if the MediaPlayer object is not null
            if (wrongSoundEffect != null) {
                wrongSoundEffect.start();
            }
        }
    }

    void reset() {
        // Reset the background of all options
        setUnselectedBackground(binding.option1);
        setUnselectedBackground(binding.option2);
        setUnselectedBackground(binding.option3);
        setUnselectedBackground(binding.option4);
    }

    public void onClick(View view) {
        int viewId = view.getId();

        if (viewId == R.id.option_1 || viewId == R.id.option_2 || viewId == R.id.option_3 || viewId == R.id.option_4) {
            // Disable selecting other options after selecting one
            if (!answered) { // Check if the user has not answered yet
                // Disable selecting other options after selecting one
                binding.option1.setEnabled(false);
                binding.option2.setEnabled(false);
                binding.option3.setEnabled(false);
                binding.option4.setEnabled(false);
                if (timer != null) {
                    timer.cancel();
                }
                TextView selected = (TextView) view;
                checkAnswer(selected);  // check the correct answer and if the answer is wrong call show answer in check answer

                // Move to the next question after a delay of 200 milliseconds
                //binding.nextBtn.postDelayed(this::setNextQuestion, 2000); // 200 milliseconds delay
            }
        } else if (viewId == R.id.nextBtn) {
            if (answered) { // Check if the user has answered the current question
                // Add a delay of 400 milliseconds before moving to the next question
                binding.nextBtn.postDelayed(this::setNextQuestion, 200); // 200 milliseconds delay
            }  else {
                // Show a toast message asking the user to select an option
                Toast.makeText(QuizActivity.this, "Please select an option", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.quitBtn) {
            // Quit the activity without adding coins or updating answers data
            showExitDialog();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setUnselectedBackground(TextView textView) {
        Drawable unselectedBackground = ContextCompat.getDrawable(this, R.drawable.option_unselected);
        textView.setBackground(unselectedBackground);
        binding.quizImageView.setVisibility(View.VISIBLE);
        binding.lostCoinView.setVisibility(View.GONE);
        binding.linearLayoutLostCoin.setVisibility(View.GONE);
        binding.winCoinView.setVisibility(View.GONE);
        binding.linearLayoutWinCoin.setVisibility(View.GONE);
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private void setCorrectBackground(TextView textView) {
        Drawable correctBackground = ContextCompat.getDrawable(this, R.drawable.option_right);
        textView.setBackground(correctBackground);
        binding.quizImageView.setVisibility(View.GONE);
        binding.winCoinView.setVisibility(View.VISIBLE);
        binding.linearLayoutWinCoin.setVisibility(View.VISIBLE);
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private void setWrongBackground(TextView textView) {
        Drawable wrongBackground = ContextCompat.getDrawable(this, R.drawable.option_wrong);
        textView.setBackground(wrongBackground);
        binding.quizImageView.setVisibility(View.GONE);
        binding.lostCoinView.setVisibility(View.VISIBLE);
        binding.linearLayoutLostCoin.setVisibility(View.VISIBLE);
    }

    private void showRewardedAd() {
        if (isRewardedAdLoaded) {
            Activity activityContext = QuizActivity.this; // Make sure to use the correct activity context

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "onAdShowedFullScreenContent");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                    loadRewardedAd(); // Load a new ad for future use
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "onAdDismissedFullScreenContent");
                    // Set the flag to false after ad is dismissed
                    isRewardedAdLoaded = false;
                    loadRewardedAd(); // Load a new ad for future use
                }
            });

            rewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    // Add Coins to user
                    // BonusCoin(rewardItem);
                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet or failed to load.");
            // Load a new rewarded ad
            loadRewardedAd();
        }
    }

    private void loadRewardedAd() {
        if (!isRewardedAdLoaded) {
            AdRequest adRequest = new AdRequest.Builder().build();
            String REWARDED_AD_UNIT_ID = getString(R.string.QUIZ_REWARDED_AD_UNIT_ID); // Get rewarded ad

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
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up the onBackPressed callback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    private void releaseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityValid = false;

        // Release MediaPlayer resources
        releaseMediaPlayer(correctSoundEffect);
        releaseMediaPlayer(wrongSoundEffect);
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to leave this quiz?")
                .setTitle("Leaving Quiz")
                .setIcon(R.drawable.logout)
                .setPositiveButton("Yes", (dialog, id) -> {
                    goToResultActivity();
                    if (timer != null) {
                        timer.cancel();
                    }
                    finish();
                })
                .setNegativeButton("No", (dialog, id) -> {
                    // User cancelled the dialog, do nothing
                })
                .setOnCancelListener(dialog -> {
                    // Handle the back button press properly
                    if (!answered) {
                        if (timer != null) {
                            timer.cancel();
                        }
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
