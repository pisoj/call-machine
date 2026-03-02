package com.zutiradio.broadcastos;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InCallPlayer {

    private final Context ctx;

    public InCallPlayer(Context context) {
        this.ctx = context;
    }

    public void execute() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(48000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build();

        int bufferSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);


        AudioTrack track = new AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STREAM, 0);

        setupAlsa();

        playMessage(track, bufferSize);

        cleanupAlsa();
    }

    private void playMessage(AudioTrack track, int bufferSize) {
        InputStream inputStream = ctx.getResources().openRawResource(R.raw.zuco_stereo_48k);

        try {
            inputStream.skip(44); // Blindly assume we need to skip the first 44 bytes of WAV header, TODO: make this smart
            byte[] buffer = new byte[bufferSize];
            int readBytesCount;

            track.play();
            while ((readBytesCount = inputStream.read(buffer)) != -1) {
                track.write(buffer, 0, readBytesCount);
            }

            inputStream.close();
        } catch (IOException e) {
            Log.e(getClass().getName(), "Failed to manipulate with media resource", e);
        }
    }

    private void setupAlsa() {
        try {
            String result = "";
            result += runCommand(buildRootTinymixCommand("Incall_Music Audio Mixer MultiMedia1", "1 1"));
            result += runCommand(buildRootTinymixCommand("TX_AIF1_CAP Mixer DEC1", "0"));
            result += runCommand(buildRootTinymixCommand("EAR PA GAIN", "G_0_DB"));
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
            result += runCommand(buildRootTinymixCommand("Incall_Music Audio Mixer MultiMedia1", "0 0"));
            result += runCommand(buildRootTinymixCommand("TX_AIF1_CAP Mixer DEC1", "12"));
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
        return new String[]{"su", "-c", "/system/bin/com.zutiradio.broadcastos.tinymix", '"' + name + '"', value};
    }
}
