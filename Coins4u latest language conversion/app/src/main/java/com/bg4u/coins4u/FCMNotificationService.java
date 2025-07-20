package com.bg4u.coins4u;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMNotificationService extends FirebaseMessagingService {

    // Override the onNewToken method to receive the FCM token
    @Override
    public void onNewToken(String token) {
        // Log the FCM token
        Log.d("FCM", "Refreshed token: " + token);

        // Send the token to your server to store it
        sendRegistrationToServer(token);
    }

    // Override the onMessageReceived method to handle incoming messages
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            // Handle data messages
            handleDataMessage(remoteMessage.getData());
        }

        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            // Handle notification messages
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    // Method to send the FCM token to your server
    private void sendRegistrationToServer(String token) {
        // Implement your server-side logic to store the token
    }

    // Method to handle data messages
    private void handleDataMessage(Map<String, String> data) {
        // Implement your logic to process data messages
    }

    // Method to handle notification messages
    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        // Implement your logic to display notifications
    }
}

