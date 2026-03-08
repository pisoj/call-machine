package com.zutiradio.callmachine.call;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class InCallPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private final Context ctx;

    @Nullable
    private final InCallPlayerCallback callback;

    private final String incallMusicBusNumber;

    private MediaPlayer player;

    public InCallPlayer(Context ctx, @NonNull SharedPreferences sharedPreferences, @Nullable InCallPlayerCallback callback) {
        this.ctx = ctx;
        this.callback = callback;
        this.incallMusicBusNumber = sharedPreferences.getString("incall_music_bus_number", "1");
    }

    public void play(File file) {
        maxVolume();
        MediaPlayer player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // Must be USAGE_MEDIA in order to route to the incall_music sink
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                // Bypass ducking during call of what system thinks is music because we've set USAGE_MEDIA
                .setFlags(0x1 << 6) // AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY - Flag requesting audible playback even under limited interruptions., marked with @hide
                .build()
        );
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);

        try {
            player.setDataSource(ctx, Uri.fromFile(file));
            player.prepareAsync();
            this.player = player;
        } catch (IOException e) {
            Log.e(getClass().getName(), "Failed to initialize media player.", e);
        }
    }

    @Override
    public void onPrepared(@NonNull MediaPlayer player) {
        setupAlsa();
        player.start();
    }

    public void stop() {
        if (player == null) return;
        player.stop();
        player.release();
        this.player = null;
    }

    public void cleanup() {
        cleanupAlsa();
    }

    @Override
    public void onCompletion(@NonNull MediaPlayer player) {
        player.release();
        this.player = null;
        if (callback == null) return;
        callback.onFinishedPlaying();
    }

    private void maxVolume() {
        // System volume effects how loud will the message be at the other end
        for (int streamType: new int[]{AudioManager.STREAM_MUSIC, AudioManager.STREAM_VOICE_CALL}) {
            AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            audioManager.setStreamVolume(streamType, maxVolume, 0);
        }
    }

    private void setupAlsa() {
        try {
            String result = "";
            result += runCommand(buildRootTinymixCommand("Incall_Music Audio Mixer MultiMedia" + incallMusicBusNumber, "1 1")); // Forward playing media to phone call
            result += runCommand(buildRootTinymixCommand("TX_AIF1_CAP Mixer DEC1", "0")); // Mute the microphone
            // I haven't found a setting to completely silence the earpiece while still being abe to record call audio
            result += runCommand(buildRootTinymixCommand("EAR PA GAIN", "G_0_DB")); // At least try to lower the gain so earpiece isn't as loud
            if (!result.isBlank()) {
                Log.i(getClass().getName(), "ALSA setup result:\n" + result);
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), "Failed to setup ALSA mixer settings", e);
        }
    }

    private void cleanupAlsa() {
        try {
            String result = "";
            result += runCommand(buildRootTinymixCommand("Incall_Music Audio Mixer MultiMedia" + incallMusicBusNumber, "0 0"));
            result += runCommand(buildRootTinymixCommand("TX_AIF1_CAP Mixer DEC1", "1"));
            result += runCommand(buildRootTinymixCommand("EAR PA GAIN", "G_6_DB"));
            if (!result.isBlank()) {
                Log.i(getClass().getName(), "ALSA cleanup result:\n" + result);
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), "Failed to cleanup ALSA mixer settings", e);
        }
    }

    private static String runCommand(@NonNull String... cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String output = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            output += inputLine + '\n';
        }

        in.close();
        process.waitFor();
        process.destroy();

        return output;
    }

    @NonNull
    private static String[] buildRootTinymixCommand(@NonNull String name, @NonNull String value) {
        return new String[]{"su", "-c", "/system/bin/com.zutiradio.callmachine.tinymix", '"' + name + '"', value};
    }

    public interface InCallPlayerCallback {

        void onFinishedPlaying();
    }
}
