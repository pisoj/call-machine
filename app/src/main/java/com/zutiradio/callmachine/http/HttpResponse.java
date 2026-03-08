package com.zutiradio.callmachine.http;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public class HttpResponse {

    public final int statusCode;

    @NotNull
    public final String response;

    public HttpResponse(int statusCode, @NonNull String response) {
        this.statusCode = statusCode;
        this.response = response;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
