package com.zutiradio.broadcastos;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telecom.Call;

import org.jetbrains.annotations.NotNull;

public interface CallHelpers {

    /**
     * @return a string formatted like +12345 (Contact Name If Contact Exists)
     */
    @NotNull
    static String getAttribution(@NotNull Context ctx, @NotNull Call call) {
        String phoneNumber = getPhoneNumber(call);
        String contactName = getContactName(ctx, phoneNumber);
        return phoneNumber + (contactName != null ? " (" + contactName + ")" : "");
    }

    @NotNull
    static String getPhoneNumber(@NotNull Call call) {
        if (call.getDetails().getGatewayInfo() != null) {
            return call.getDetails().getGatewayInfo()
                    .getOriginalAddress().getSchemeSpecificPart();
        }
        return call.getDetails().getHandle().getSchemeSpecificPart();
    }

    @Nullable
    @SuppressLint("Range")
    static String getContactName(@NonNull Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
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
}
