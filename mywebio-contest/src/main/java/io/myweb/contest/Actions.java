package io.myweb.contest;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

import io.myweb.Service;
import io.myweb.api.GET;

/**
 * Created by wkaminski on 14.10.14.
 */
public class Actions {

    public static final String TAG = "Actions";

    @GET("/lose")
    public String buzz(Context ctx) {
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        Log.i("Actions", "The activity is visible and about to be started.");
        v.vibrate(500);

        Log.i(TAG, "/lose called");

        return "";
    }

    @GET("/win")
    public String fanfare(Context ctx) {
//        try {


        final AudioManager am = (AudioManager) ctx.getSystemService(Service.AUDIO_SERVICE);
        final int originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

        MediaPlayer player = MediaPlayer.create(ctx, R.raw.fanfareogg);

        player.start();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                mp.release();
            }
        });

        Log.i(TAG, "/win called");

        return "";
    }
}
