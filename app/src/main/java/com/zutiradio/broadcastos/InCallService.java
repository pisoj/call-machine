package com.zutiradio.broadcastos;

import static com.zutiradio.broadcastos.CallCallbackHandler.getNumber;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class InCallService extends android.telecom.InCallService {

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        call.registerCallback(new CallCallbackHandler(getApplicationContext()));
        call.answer(VideoProfile.STATE_AUDIO_ONLY);

        Log.d(getClass().getTypeName(), "Call added: " + getNumber(call));
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(getClass().getTypeName(), "Call removed");
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
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
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

    protected static String getNumber(Call call) {
        if (call == null) {
            return null;
        }
        if (call.getDetails().getGatewayInfo() != null) {
            return call.getDetails().getGatewayInfo()
                    .getOriginalAddress().getSchemeSpecificPart();
        }
        Uri handle = getHandle(call);
        return handle == null ? null : handle.getSchemeSpecificPart();
    }

    private static Uri getHandle(Call call) {
        return call == null ? null : call.getDetails().getHandle();
    }
}
