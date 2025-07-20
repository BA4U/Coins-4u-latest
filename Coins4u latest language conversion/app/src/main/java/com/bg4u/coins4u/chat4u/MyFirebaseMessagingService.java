package com.bg4u.coins4u.chat4u;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.bg4u.coins4u.MainActivity;
import com.bg4u.coins4u.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Service for handling Firebase Cloud Messaging (FCM) notifications
 * - Updates user token when refreshed
 * - Creates notifications for chat messages & friend requests
 * - Handles notification when app is in background
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "Chat Notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Update token in Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users").document(currentUser.getUid())
                    .update("token", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update token", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            // Handle notification payload
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Handle data payload
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String message = data.get("message");

            if (title != null && message != null) {
                sendNotification(title, message, data);
            }
        }
    }

    /**
     * Create and show notification
     * @param title Notification title
     * @param messageBody Notification message body
     * @param data Additional data for notification handling
     */
    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        Intent intent;

        // Determine which activity to open based on notification type
        if (data != null && data.containsKey("type")) {
            String type = data.get("type");
            String senderId = data.get("senderId");

            if ("chat_message".equals(type) && senderId != null) {
                // Open chat activity for chat messages
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra("visit_user_id", senderId);
                intent.putExtra("visit_user_name", data.get("senderName"));
                intent.putExtra("visit_image", data.get("senderImage"));
            } else if ("friend_request".equals(type)) {
                // Open request fragment for friend requests
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("open_fragment", "requests");
            } else {
                // Default to main activity
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // Default to main activity
            intent = new Intent(this, MainActivity.class);
        }

        // Add flags to clear activities on top
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification_coins_app_logo)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Show notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}