package com.zutiradio.broadcastos;

import static com.zutiradio.broadcastos.CallHelpers.getPhoneNumber;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

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
        String callNumber = getPhoneNumber(call);
        for (String countryCode: allowedCountryCodes) {
            if (!callNumber.startsWith('+' + countryCode)) continue;
            isAllowedCountryCode = true;
            break;
        }

        return isAllowedCountryCode;
    }
}

class CallCallbackHandler extends Call.Callback implements InCallPlayer.InCallPlayerCallback {

    private final InCallRecorder inCallRecorder;

    private final InCallPlayer inCallPlayer;

    private final File greeting;

    CallCallbackHandler(Context ctx, SharedPreferences sharedPreferences, Call call) {
        this.inCallRecorder = new InCallRecorder(ctx, sharedPreferences, call);
        this.inCallPlayer = new InCallPlayer(ctx, sharedPreferences, this);

        File greetingFile = new File(ctx.getFilesDir(), "greeting");
        if (greetingFile.exists()) {
            this.greeting = greetingFile;
        } else {
            Toast.makeText(ctx, R.string.greeting_file_does_not_exist, Toast.LENGTH_LONG).show();
            this.greeting = null;
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
                    inCallRecorder.startRecording();
                }
                break;
            case Call.STATE_DISCONNECTING:
                inCallRecorder.stopRecording();
                break;
        }
    }

    @Override
    public void onFinishedPlaying() {
        inCallRecorder.startRecording();
    }
}
