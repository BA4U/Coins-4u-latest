package com.bg4u.coins4u;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import com.bg4u.coins4u.R;

public class ClickSoundHelper {
    private static SoundPool soundPool;
    private static int clickSound;
    
    public static void initialize(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }
        
        // Make sure to handle the case where soundPool is null
        if (soundPool != null) {
            clickSound = soundPool.load(context, R.raw.click_sound, 1);
        }
    }
    
    public static void playClickSound() {
        if (soundPool != null && clickSound != 0) {
            soundPool.play(clickSound, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
}
