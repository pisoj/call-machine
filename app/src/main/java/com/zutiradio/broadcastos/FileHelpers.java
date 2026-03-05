package com.zutiradio.broadcastos;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHelpers {

    private static final Handler handler = new Handler();

    public static void copyUriToFileAsync(Context ctx, Uri src, File dst, CopyFilesAsyncCallback callback) {
        handler.post(() -> {
            try {
                copyUriToFile(ctx, src, dst);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public static void copyUriToFile(Context ctx, Uri src, File dst) {
        try (InputStream inputStream = ctx.getContentResolver().openInputStream(src);
             OutputStream outputStream = new FileOutputStream(dst)) {
            if (inputStream == null) return;

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface CopyFilesAsyncCallback {

        void onSuccess();

        void onError(Throwable e);
    }
}
