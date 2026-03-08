package com.zutiradio.callmachine.files;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;

public class FileProvider extends ContentProvider {
    @Override
    public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        File file = new File(getContext().getCacheDir(), uri.getPath());
        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        throw new FileNotFoundException(uri.getPath());
    }

    @Override
    public Cursor query(@NonNull Uri u, String[] s, String s1, String[] s2, String s3) { return null; }
    @Override
    public String getType(@NonNull Uri uri) { return "text/html"; }
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues v) { return null; }
    @Override
    public int delete(@NonNull Uri uri, String s, String[] s1) { return 0; }
    @Override
    public int update(@NonNull Uri uri, ContentValues v, String s, String[] s1) { return 0; }

    public static void openHtmlFromPrivateCache(@NonNull Context ctx, String fileName) {
        String authority = ctx.getPackageName() + ".files.cache";
        Uri uri = Uri.parse("content://" + authority + "/" + fileName);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }
}
