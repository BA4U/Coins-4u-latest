package com.bg4u.coins4u;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("tournament_channel", "Tournament Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "tournament_channel")
                .setSmallIcon(R.drawable.coinslogolowsize)
                .setContentTitle("Tournament Reminder")
                .setContentText("Your Free Fire tournament starts in 10 minutes!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Trigger notification
        notificationManager.notify(1001, builder.build());
    }
}
