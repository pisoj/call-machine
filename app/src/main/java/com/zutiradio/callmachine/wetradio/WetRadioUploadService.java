package com.zutiradio.callmachine.wetradio;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.zutiradio.callmachine.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class WetRadioUploadService extends Service {

    /// Required
    public static final String EXTRA_WET_RADIO_UPLOADER = "wet_radio_uploader";

    ///  Required
    public static final String EXTRA_AUDIO_FILE_PATH = "audio_file_path";

    /// Optional
    public static final String EXTRA_ATTRIBUTION = "attribution";

    private final AtomicInteger activeThreadCount = new AtomicInteger(0);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();

        WetRadioUploader uploader = intent.getParcelableExtra(EXTRA_WET_RADIO_UPLOADER);
        File audioFile = new File(intent.getStringExtra(EXTRA_AUDIO_FILE_PATH));
        String attribution = intent.getStringExtra(EXTRA_ATTRIBUTION);

        uploadToWetRadioAsync(uploader, audioFile, attribution);

        return START_NOT_STICKY;
    }

    private void uploadToWetRadioAsync(@NotNull WetRadioUploader uploader, @NotNull File recording, @org.jetbrains.annotations.Nullable String attribution) {
        new Thread(() -> {
            activeThreadCount.incrementAndGet();
            Log.i(getClass().getName(), "Uploading: " + recording.getName() + " Attribution: " + attribution);
            try {
                int statusCode = uploader.uploadAudio(recording, attribution, new File(getApplicationContext().getCacheDir(), WetRadioUploader.LAST_RESPONSE_FILE_NAME));
                if (statusCode < 200 || statusCode >= 300) {
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(@NonNull Message message) {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.wet_radio_uploading_failed_with_status_code_x, Integer.toString(statusCode)), Toast.LENGTH_LONG).show();
                        }
                    };
                } else {
                    Log.i(getClass().getName(), "Successfully uploaded to Wet Radio.");
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message message) {
                        Toast.makeText(getApplicationContext(), R.string.wet_radio_uploading_failed_message, Toast.LENGTH_LONG).show();
                    }
                };
                Log.e(getClass().getName(), "Failed to upload recording to WetRadio", e);
            } finally {
                onThreadFinished();
            }
        }).start();
    }

    private void onThreadFinished() {
        if (activeThreadCount.decrementAndGet() <= 0) {
            Log.i(getClass().getName(), "Stopping Wet Radio Uploading service.");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void showNotification() {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.wet_radio_uploading_service))
                .build();
        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.wet_radio_uploading_service), importance);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private static final String CHANNEL_ID = "wet_radio_upload";
    private static final int NOTIFICATION_ID = 1;
}
