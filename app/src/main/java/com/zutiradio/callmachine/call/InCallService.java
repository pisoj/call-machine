package com.zutiradio.callmachine.call;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.util.Log;
import android.widget.Toast;

import com.zutiradio.callmachine.R;
import com.zutiradio.callmachine.presentation.preferences.PreferencesPresenter;
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

        String phoneNumber = getPhoneNumber(call);
        String contactName = getContactName(getApplicationContext(), phoneNumber);

        if (!shouldProcessCall(call,
                Integer.parseInt(sharedPreferences.getString("contacts_policy", "0")),
                contactName != null,
                sharedPreferences.getStringSet("allowed_country_codes", Set.of()),
                sharedPreferences.getBoolean("is_answering_machine_enabled", false))) return;

        processCall(
                call,
                Integer.parseInt(sharedPreferences.getString("delay_before_answering", "20")),
                new CallCallbackHandler(getApplicationContext(), sharedPreferences, call, buildMessageAttribution(phoneNumber, contactName))
        );
    }

    private void processCall(@NonNull Call call, int delayBeforeAnswering, Call.Callback callCallback) {
        if (delayBeforeAnswering == 0) {
            // Answer right away without launching any new threads
            call.registerCallback(callCallback);
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
            return;
        }
        new Thread(() -> {
            for (int countdown = delayBeforeAnswering; countdown > 0; countdown--) {
                try {
                    Thread.sleep(1000);
                    if (call.getState() != Call.STATE_RINGING) {
                        throw new InterruptedException();
                    }
                } catch (InterruptedException e) {
                    Log.i(InCallService.class.getName(), "Answering machine interrupted.", e);
                    return;
                }
            }
            getApplicationContext().getMainExecutor().execute(() -> {
                call.registerCallback(callCallback);
                call.answer(VideoProfile.STATE_AUDIO_ONLY);
            });
        }).start();
    }

    /**
     * @param allowedCountryCodes List of numbers "1", "44", "42". **Without** the leading `+` symbol.
     */
    private static boolean shouldProcessCall(@NonNull Call call, int contactsPolicy, boolean isInContacts, @NotNull Iterable<String> allowedCountryCodes, boolean isAnsweringMachineEnabled) {
        if (!isAnsweringMachineEnabled) return false;
        if (call.getDetails().getCallDirection() != Call.Details.DIRECTION_INCOMING) return false;

        if (contactsPolicy == PreferencesPresenter.CONTACTS_POLICY_ONLY_KNOWN_NUMBERS && !isInContacts
                || contactsPolicy == PreferencesPresenter.CONTACTS_POLICY_ONLY_UNKNOWN_NUMBERS && isInContacts) return false;

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

    /**
     * @return a string formatted like +12345 (contact_name [if a contact exists])
     */
    @NotNull
    private static String buildMessageAttribution(@NonNull String phoneNumber, @android.support.annotation.Nullable String contactName) {
        return phoneNumber + (contactName != null ? " (" + contactName + ")" : "");
    }

    @NotNull
    private static String getPhoneNumber(@NotNull Call call) {
        if (call.getDetails().getGatewayInfo() != null) {
            return call.getDetails().getGatewayInfo()
                    .getOriginalAddress().getSchemeSpecificPart();
        }
        return call.getDetails().getHandle().getSchemeSpecificPart();
    }

    /**
     * @return NULL if Contacts permission isn't granted or some other error occurred
     */
    @android.support.annotation.Nullable
    @SuppressLint("Range")
    private static String getContactName(@NonNull Context ctx, String phoneNumber) {
        if (!isReadContactsPermissionGranted(ctx)) return null;

        ContentResolver cr = ctx.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(!cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    private static boolean isReadContactsPermissionGranted(@NonNull Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }
}

class CallCallbackHandler extends Call.Callback implements InCallPlayer.InCallPlayerCallback {

    private final Context ctx;

    private final Call call;

    private final String messageAttribution;

    private final InCallRecorder inCallRecorder;

    private final InCallPlayer inCallPlayer;

    @Nullable
    private final WetRadioUploader wetRadioUploader;

    @Nullable
    private final File greeting;

    private boolean hasTimeoutElapsed = false;
    @Nullable
    private final Thread timeoutThread;

    CallCallbackHandler(Context ctx, SharedPreferences sharedPreferences, Call call, @NotNull String messageAttribution) {
        this.ctx = ctx;
        this.call = call;
        this.messageAttribution = messageAttribution;
        this.inCallRecorder = new InCallRecorder(ctx, sharedPreferences, messageAttribution);
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
            Log.i(getClass().getName(), "WetRadioUploader not constructed. This is expected if Wet Radio uploading isn't used.", e);
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
                        Log.i(getClass().getName(), "Timeout counter thread interrupted.", e);
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
                    intent.putExtra(WetRadioUploadService.EXTRA_ATTRIBUTION, messageAttribution);
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
