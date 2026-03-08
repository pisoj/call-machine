package com.zutiradio.callmachine.http;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;

/**
 * Used for building an HTTP multipart request as a {@link DataOutputStream}.<br><br>
 * At the beginning use {@link #writeContentType} to inform the server a multipart request is coming.<br>
 * At the end use {@link #writeEndSequence()} to finish the request correctly.
 */
public class MultipartRequest {

    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private static final String BOUNDARY = "*****";

    private final DataOutputStream request;

    public MultipartRequest(@NotNull DataOutputStream request) {
        this.request = request;
    }

    /**
     * Adds what would be a single &ltinput type="text"&gt in HTML
     * @param name The field identifier, i.e. what would be &ltinput name=""&gt in HTML.
     * @param value The field value
     */
    public void writeText(@NotNull String name, @NotNull String value) throws IOException {
        request.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
        request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF);
        request.writeBytes(CRLF);
        request.writeBytes(value);
        request.writeBytes(CRLF);
    }

    /**
     * Adds what would be a single &ltinput type="text"&gt in HTML
     * @param name The field identifier, i.e. what would be &ltinput name=""&gt in HTML.
     * @param file The file which contents should be uploaded.
     */
    public void writeFile(@NotNull String name, @NotNull File file) throws IOException {
        request.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
        request.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName() + "\"" + CRLF);
        request.writeBytes("Content-Type: " + Files.probeContentType(file.toPath()) + CRLF);
        request.writeBytes(CRLF);
        try (InputStream fileStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                request.write(buffer, 0, bytesRead);
            }
        }
        request.writeBytes(CRLF);
    }

    /**
     * Sets the content type header of the provided httpConnection to multipart/form-data.
     * Also sets the boundary that will be used for the request.
     */
    public static void writeContentType(@NotNull HttpURLConnection httpConnection) {
        httpConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
    }

    /**
     * Writes the end sequence of a multipart request in order to finish the request correctly.
     */
    public void writeEndSequence() throws IOException {
        request.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS);
    }
}
