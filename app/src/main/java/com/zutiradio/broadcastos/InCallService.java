package com.zutiradio.broadcastos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.telecom.CallEndpoint;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@SuppressLint("NewApi")
public class InCallService extends android.telecom.InCallService {

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        onSilenceRinger();

        call.registerCallback(new CallCallbackHandler(getApplicationContext()));

        Log.d(getClass().getTypeName(), "Call added");
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(getClass().getTypeName(), "Call removed");
    }

    @Override
    public void onAvailableCallEndpointsChanged(@NonNull List<CallEndpoint> availableEndpoints) {
        super.onAvailableCallEndpointsChanged(availableEndpoints);
        Log.d(getClass().getTypeName(), "Avaiable call endpoints changed to: %s".formatted(availableEndpoints.toString()));
    }

    @Override
    public void onConnectionEvent(Call call, String event, Bundle extras) {
        super.onConnectionEvent(call, event, extras);
        Log.d(getClass().getTypeName(), "Connection event: %s".formatted(event));
    }

    @Override
    public void onSilenceRinger() {
        super.onSilenceRinger();
        Log.d(getClass().getTypeName(), "Silent ringer triggered");
    }

    @Override
    public void onCanAddCallChanged(boolean canAddCall) {
        super.onCanAddCallChanged(canAddCall);
        Log.d(getClass().getTypeName(), "Can add more calls change to: %b".formatted(canAddCall));
    }
}

@SuppressLint("NewApi")
class CallCallbackHandler extends Call.Callback {

    private final Context ctx;

    private MediaRecorder recorder;

    CallCallbackHandler(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onStateChanged(Call call, int state) {
        super.onStateChanged(call, state);
        Log.d(getClass().getTypeName(), "Call state changed: %s".formatted(state));
        switch (state) {
            case Call.STATE_ACTIVE:
                startRecording(call);
                break;
            case Call.STATE_DISCONNECTING:
                stopRecording();
                break;
        }
    }

    @Override
    public void onCallDestroyed(Call call) {
        super.onCallDestroyed(call);
        Log.d(getClass().getTypeName(), "Call destroyed");
    }

    private void startRecording(Call call) {

        Log.d(getClass().getTypeName(), "External media directories: %s".formatted(Arrays.toString(ctx.getExternalMediaDirs())));

        File file = new File(ctx.getFilesDir(),
                call.getDetails().getCallerDisplayName()
                        + ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
                        + ".ogg"
        );

        recorder = new MediaRecorder();
        recorder.setOutputFile(file);
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(getClass().getTypeName(), "Failed to prepare audio recorder", e);
        } finally {
            recorder.start();
            Log.d(getClass().getTypeName(), "Recording to: " + file.getAbsolutePath());
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        Log.d(getClass().getTypeName(), "Recording stopped");
    }
}
