package com.zutiradio.callmachine.call;

import static com.zutiradio.callmachine.call.CallHelpers.getPhoneNumber;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.util.Log;
import android.widget.Toast;

import com.zutiradio.callmachine.R;
import com.zutiradio.callmachine.wetradio.WetRadioUploadService;
import com.zutiradio.callmachine.wetradio.WetRadioUploader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

public class InCallService extends android.telecom.InCallService {

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (!shouldProcessCall(call,
                sharedPreferences.getStringSet("allowed_country_codes", Set.of()),
                sharedPreferences.getBoolean("is_answering_machine_enabled", false))) return;

        call.registerCallback(new CallCallbackHandler(getApplicationContext(), sharedPreferences, call));
        call.answer(VideoProfile.STATE_AUDIO_ONLY);
    }

    /**
     * @param allowedCountryCodes List of numbers "1", "44", "42". **Without** the leading `+` symbol.
     */
    private static boolean shouldProcessCall(@NonNull Call call, @NotNull Iterable<String> allowedCountryCodes, boolean isAnsweringMachineEnabled) {
        if (!isAnsweringMachineEnabled) return false;
        if (call.getDetails().getCallDirection() != Call.Details.DIRECTION_INCOMING) return false;

        boolean isAllowedCountryCode = false;
        boolean areCountryCodesEmpty = true;
        String callNumber = getPhoneNumber(call);
        for (String countryCode: allowedCountryCodes) {
            areCountryCodesEmpty = false;
            if (!callNumber.startsWith('+' + countryCode)) continue;
            isAllowedCountryCode = true;
            break;
        }

        return isAllowedCountryCode || areCountryCodesEmpty;
    }
}

class CallCallbackHandler extends Call.Callback implements InCallPlayer.InCallPlayerCallback {

    private final Context ctx;

    private final Call call;

    private final InCallRecorder inCallRecorder;

    private final InCallPlayer inCallPlayer;

    @Nullable
    private final WetRadioUploader wetRadioUploader;

    @Nullable
    private final File greeting;

    private boolean hasTimeoutElapsed = false;
    @Nullable
    private final Thread timeoutThread;

    CallCallbackHandler(Context ctx, SharedPreferences sharedPreferences, Call call) {
        this.ctx = ctx;
        this.call = call;
        this.inCallRecorder = new InCallRecorder(ctx, sharedPreferences, call);
        this.inCallPlayer = new InCallPlayer(ctx, sharedPreferences, this);
        
        WetRadioUploader wetRadioUploader;
        try {
            wetRadioUploader = new WetRadioUploader(
                    sharedPreferences.getBoolean("is_wet_radio_uploading_enabled", false),
                    sharedPreferences.getBoolean("wet_radio_delete_after_upload", false),
                    sharedPreferences.getString("wet_radio_base_url", ""),
                    sharedPreferences.getString("wet_radio_target_send_id", ""),
                    sharedPreferences.getString("wet_radio_recorder_field_index", ""),
                    sharedPreferences.getString("wet_radio_attribution_field_index", null),
                    sharedPreferences.getString("wet_radio_privileged_sender_token", null)
            );
        } catch (Exception e) {
            wetRadioUploader = null;
            Log.w(getClass().getName(), "Failed to construct WetRadioUploader.", e);
        }
        this.wetRadioUploader = wetRadioUploader;

        File greetingFile = new File(ctx.getFilesDir(), "greeting");
        if (greetingFile.exists()) {
            this.greeting = greetingFile;
        } else {
            Toast.makeText(ctx, R.string.greeting_file_does_not_exist, Toast.LENGTH_LONG).show();
            this.greeting = null;
        }


        int timeoutSec = Integer.parseInt(sharedPreferences.getString("message_max_duration", "0"));
        if (timeoutSec > 0) {
            File timeoutFile = new File(ctx.getFilesDir(), "timeout");
            this.timeoutThread = new Thread(() -> {
                for (int timeout = timeoutSec; timeout > 0; timeout--) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.i(getClass().getName(), "Timeout counter thread interupted.", e);
                        return;
                    }
                }
                this.hasTimeoutElapsed = true;
                if (timeoutFile.exists()) {
                    this.inCallPlayer.play(timeoutFile);
                } else {
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(@NonNull Message message) {
                            Toast.makeText(ctx, R.string.timeout_file_does_not_exist, Toast.LENGTH_LONG).show();
                        }
                    };
                    call.disconnect();
                }
            });
        } else {
            this.timeoutThread = null;
        }
    }

    @Override
    public void onStateChanged(Call call, int state) {
        super.onStateChanged(call, state);
        switch (state) {
            case Call.STATE_ACTIVE:
                if (greeting != null) {
                    inCallPlayer.play(greeting);
                } else {
                    startRecording();
                }
                break;
            case Call.STATE_DISCONNECTED:
                inCallPlayer.stop();
                inCallPlayer.cleanup();
                boolean wasSomethingRecorded = inCallRecorder.stopRecording();
                if (wetRadioUploader != null && wasSomethingRecorded) {
                    Intent intent = new Intent(ctx, WetRadioUploadService.class);
                    intent.putExtra(WetRadioUploadService.EXTRA_WET_RADIO_UPLOADER, wetRadioUploader);
                    intent.putExtra(WetRadioUploadService.EXTRA_AUDIO_FILE_PATH, inCallRecorder.getOutputFile().getAbsolutePath());
                    intent.putExtra(WetRadioUploadService.EXTRA_ATTRIBUTION, CallHelpers.getAttribution(ctx, call));
                    ctx.startService(intent);
                }
                if (timeoutThread != null) timeoutThread.interrupt();
                break;
        }
    }

    @Override
    public void onFinishedPlaying() {
        if (hasTimeoutElapsed) {
            call.disconnect();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        inCallRecorder.startRecording();
        if (timeoutThread != null) timeoutThread.start();
    }
}
