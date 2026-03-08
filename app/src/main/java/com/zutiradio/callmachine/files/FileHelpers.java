package com.zutiradio.callmachine.files;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHelpers {

    public interface CopyFilesAsyncCallback {

        void onSuccess();

        void onError(Throwable e);
    }

    public static void copyUriToFileAsync(Context ctx, Uri src, File dst, CopyFilesAsyncCallback callback) {
        Handler handler = new Handler();
        handler.post(() -> {
            try {
                copyUriToFile(ctx, src, dst);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public static void copyUriToFile(Context ctx, Uri src, File dst) throws IOException {
        try (InputStream inputStream = ctx.getContentResolver().openInputStream(src)) {
            if (inputStream == null) return;
            writeStreamToFile(inputStream, dst);
        }
    }

    public static void writeStreamToFile(@NotNull InputStream src, @NotNull File dst) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = src.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
