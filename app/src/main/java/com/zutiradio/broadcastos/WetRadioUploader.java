package com.zutiradio.broadcastos;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.zutiradio.broadcastos.files.FileHelpers;
import com.zutiradio.broadcastos.http.MultipartRequest;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WetRadioUploader implements Parcelable {

    public static final String LAST_RESPONSE_FILE_NAME = "WetRadio_lastResponse.html";

    public final boolean isUploadingEnabled;
    public final boolean shouldDeleteAfterUpload;
    public final URL sendUrl;
    public final String targetSendId;
    public final String recorderFieldIndex;
    public final String attributionFieldIndex;
    public final String privilegedSenderToken;

    public WetRadioUploader(boolean isUploadingEnabled,
                            boolean shouldDeleteAfterUpload,
                            @NonNull String baseUrl,
                            String targetSendId,
                            String recorderFieldIndex,
                            String attributionFieldIndex,
                            String privilegedSenderToken) throws MalformedURLException {
        this.isUploadingEnabled = isUploadingEnabled;
        this.shouldDeleteAfterUpload = shouldDeleteAfterUpload;
        this.sendUrl = new URL(baseUrl + (baseUrl.endsWith("/") ? "send.php" : "/send.php"));
        this.targetSendId = targetSendId;
        this.recorderFieldIndex = recorderFieldIndex;
        this.attributionFieldIndex = attributionFieldIndex;
        this.privilegedSenderToken = privilegedSenderToken;
    }

    /**
     * @param responseDstFile File in which to save response body. Null to discard it.
     * @return HTTP status code received from the server.
     */
    int uploadAudio(@NotNull File audioFile, @Nullable String attribution, @Nullable File responseDstFile) throws IOException {

        HttpURLConnection httpConnection = (HttpURLConnection) sendUrl.openConnection();
        httpConnection.setUseCaches(false);
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Connection", "Keep-Alive");
        httpConnection.setRequestProperty("Cache-Control", "no-cache");
        MultipartRequest.writeContentType(httpConnection);

        DataOutputStream request = new DataOutputStream(httpConnection.getOutputStream());
        MultipartRequest multipartRequest = new MultipartRequest(request);
        multipartRequest.writeText("id", targetSendId);
        if (privilegedSenderToken != null) {
            multipartRequest.writeText("privileged_sender_token", privilegedSenderToken);
        }
        if (attributionFieldIndex != null && attribution != null) {
            multipartRequest.writeText(attributionFieldIndex, attribution);
        }
        multipartRequest.writeFile(recorderFieldIndex, audioFile);
        multipartRequest.writeEndSequence();
        request.flush();
        request.close();

        int responseCode = httpConnection.getResponseCode();
        if (responseDstFile != null) {
            InputStream responseStream;
            if (responseCode >= 200 && responseCode < 300) {
                responseStream = httpConnection.getInputStream();
            } else {
                responseStream = httpConnection.getErrorStream();
            }

            Log.d(getClass().getName(), "Response file loc: " + responseDstFile.getAbsolutePath());
            FileHelpers.writeStreamToFile(responseStream, responseDstFile);
            responseStream.close();
        }
        return responseCode;
    }

    @NonNull
    @Contract("_ -> new")
    public static File getLastResponseFile(@NotNull Context ctx) {
        return new File(ctx.getCacheDir(), WetRadioUploader.LAST_RESPONSE_FILE_NAME);
    }


    // Parcelable

    protected WetRadioUploader(@NonNull Parcel in) {
        isUploadingEnabled = in.readByte() != 0;
        shouldDeleteAfterUpload = in.readByte() != 0;

        String urlString = in.readString();
        URL tempUrl = null;
        try {
            tempUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(getClass().getName(), "Failed to restore URL from parcelable.", e);
        }
        sendUrl = tempUrl;

        targetSendId = in.readString();
        recorderFieldIndex = in.readString();
        attributionFieldIndex = in.readString();
        privilegedSenderToken = in.readString();
    }

    public static final Creator<WetRadioUploader> CREATOR = new Creator<WetRadioUploader>() {
        @Override
        public WetRadioUploader createFromParcel(Parcel in) {
            return new WetRadioUploader(in);
        }

        @Override
        public WetRadioUploader[] newArray(int size) {
            return new WetRadioUploader[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByte((byte) (isUploadingEnabled ? 1 : 0));
        dest.writeByte((byte) (shouldDeleteAfterUpload ? 1 : 0));

        dest.writeString(sendUrl != null ? sendUrl.toString() : null);

        dest.writeString(targetSendId);
        dest.writeString(recorderFieldIndex);
        dest.writeString(attributionFieldIndex);
        dest.writeString(privilegedSenderToken);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
