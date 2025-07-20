package com.bg4u.coins4u;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.bg4u.coins4u.R;

import java.util.Objects;

public class DialogBox extends Dialog {
    private TextView dialogTitle;
    private TextView dialogBody;
    private LottieAnimationView lottieAnimation;
    private Button leftButton;
    private Button rightButton;
    private MediaPlayer notificationSound;
    
    public DialogBox(Context context, View customView) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    
        // Hide the navigation bar
        View decorView = Objects.requireNonNull(getWindow()).getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    
        // Show the status bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    
        setContentView(customView);
        
        dialogTitle = customView.findViewById(R.id.dialog_title);
        dialogBody = customView.findViewById(R.id.dialog_body);
        lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
        leftButton = customView.findViewById(R.id.btn_left);
        rightButton = customView.findViewById(R.id.btn_right);
        
        // Initialize the notification sound
        notificationSound = MediaPlayer.create(context, R.raw.notification_sound);
    
    }
    
    // Method to set the title of the dialog
    public void setTitle(String title) {
        dialogTitle.setText(title);
    }
    
    // Method to set the body text of the dialog
    public void setBody(String body) {
        dialogBody.setText(body);
    }
    
    // Method to set the Lottie animation of the dialog
    public void setAnimation(int animationRes) {
        lottieAnimation.setAnimation(animationRes);
    }
    
    // Method to set the text and click action of the left button
    public void setLeftButton(String buttonText, View.OnClickListener listener) {
        leftButton.setText(buttonText);
        leftButton.setOnClickListener(listener);
    }
    
    // Method to set the text and click action of the right button
    public void setRightButton(String buttonText, View.OnClickListener listener) {
        rightButton.setText(buttonText);
        rightButton.setOnClickListener(listener);
    }
    
    // Method to show the dialog with notification sound
    @Override
    public void show() {
        // Play the notification sound
        if (notificationSound != null) {
            notificationSound.start();
        }
        super.show();
    }
    
    // Method to dismiss the dialog and release the notification sound
    @Override
    public void dismiss() {
        super.dismiss();
        
        // Release the notification sound
        if (notificationSound != null) {
            notificationSound.release();
            notificationSound = null;
        }
    }
    // Other methods remain the same as before
    
}

// How to use dialog box
//    private void showDialog(String title, String body, int animationResId, String leftButtonText, String rightButtonText, Class<?> activityLeftClass, Class<?> activityRightClass ) {
//        // Inflate your custom layout for the dialog content
//        View customView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_box_layout, null);
//        // Set the title and body text directly on the custom layout
//        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
//        TextView dialogBody = customView.findViewById(R.id.dialog_body);
//        dialogTitle.setText(title);
//        dialogBody.setText(body);
//        // Set the animation directly on the LottieAnimationView
//        LottieAnimationView lottieAnimation = customView.findViewById(R.id.lottie_dialog_animation);
//        lottieAnimation.setAnimation(animationResId);
//        // Create an instance of the custom dialog and pass the custom layout as a parameter
//        DialogBox dialogBox = new DialogBox(MainActivity.this, customView);
//        Objects.requireNonNull(dialogBox.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
//        dialogBox.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
//
//        // Set the dialog to be not cancelable
//
//        // Set the left button action
//        dialogBox.setLeftButton(leftButtonText, v -> {
//            dialogBox.dismiss();
//            // Handle left button click
//            if (activityLeftClass == null) {
//                // Show interstitial ad if it's loaded
//                showInterstitialVideoAd();
//                // Add 7 days trial peroid method
//            }else {
//                startActivity(new Intent(this, SpinnerActivity.class));
//            }
//        });
//
//        // Set the right button action
//        dialogBox.setRightButton(rightButtonText, v -> {
//            // Handle right button click
//            if (activityRightClass != null) {
//                // Launch PaymentActivity with a custom amount
//                double customAmount = 49.00; // Change this to your desired amount
//                Intent paymentIntent = new Intent(MainActivity.this, activityRightClass);
//                paymentIntent.putExtra("AMOUNT", customAmount);
//                paymentLauncher.launch(paymentIntent);
//            } else {
//                dialogBox.dismiss();
//            }
//        });
//
//        // Show the dialog
//        dialogBox.show();
//    }
//