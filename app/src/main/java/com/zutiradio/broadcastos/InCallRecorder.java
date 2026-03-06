package com.zutiradio.broadcastos;

import static com.zutiradio.broadcastos.CallHelpers.getContactName;
import static com.zutiradio.broadcastos.CallHelpers.getPhoneNumber;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InCallRecorder {

    private final File outputFile;

    private final int audioEncoder;

    private final int containerFormat;

    private final int bitrate; // 0 means use default

    private final int sampleRate; // 0 means use default

    private final MediaRecorder recorder;

    public InCallRecorder(@NotNull Context ctx, @NonNull SharedPreferences sharedPreferences, @NotNull Call call) {
        String[] audioFormatSetting = sharedPreferences.getString("message_encoding", "7/11").split("/"); // Default opus/ogg
        audioEncoder = Integer.parseInt(audioFormatSetting[0]);
        containerFormat = Integer.parseInt(audioFormatSetting[1]);

        bitrate = Integer.parseInt(sharedPreferences.getString("message_bitrate", "0"));
        sampleRate = Integer.parseInt(sharedPreferences.getString("message_sample_rate", "0"));

        outputFile = new File(sharedPreferences.getString("destination_directory", ctx.getFilesDir().getAbsolutePath()),
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss"))
                        + " " + CallHelpers.getAttribution(ctx, call)
                        + "." + getContainerExtension(containerFormat)
        );

        recorder = new MediaRecorder();
    }

    public void startRecording() {
        recorder.setOutputFile(outputFile);
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK);
        recorder.setOutputFormat(containerFormat);
        recorder.setAudioEncoder(audioEncoder);
        if (bitrate != 0) recorder.setAudioEncodingBitRate(bitrate);
        if (sampleRate != 0) recorder.setAudioSamplingRate(sampleRate);
        try {
            recorder.prepare();
            recorder.start();
            Log.i(getClass().getTypeName(), "Recording to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(getClass().getTypeName(), "Failed to prepare audio recorder", e);
        }
    }

    /**
     * @return Was something recorded.
     */
    public boolean stopRecording() {
        boolean wasSomethingRecorded = false;
        try {
            recorder.stop();
            wasSomethingRecorded = true;
            Log.i(getClass().getTypeName(), "Recording stopped");
        } catch (RuntimeException e) {
            Log.e(getClass().getName(), "Failed to stop audio recorder.", e);
        } finally {
            recorder.release();
        }
        return wasSomethingRecorded;
    }

    @NotNull
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * @return file extension without dot
     */
    @NonNull
    private static String getContainerExtension(int containerFormat) {
        switch (containerFormat) {
            case MediaRecorder.OutputFormat.AAC_ADTS:
                return "aac";
            case MediaRecorder.OutputFormat.OGG:
                return "ogg";
            case MediaRecorder.OutputFormat.AMR_NB:
            case MediaRecorder.OutputFormat.AMR_WB:
                return "amr";
            case MediaRecorder.OutputFormat.MPEG_2_TS:
                return "ts";
            case MediaRecorder.OutputFormat.MPEG_4:
                return "m4a";
            case MediaRecorder.OutputFormat.THREE_GPP:
                return "3gpp";
            case MediaRecorder.OutputFormat.WEBM:
                return "webm";
            default:
                throw new IllegalStateException("Container format "  + containerFormat + " not supported");
        }
    }
}
