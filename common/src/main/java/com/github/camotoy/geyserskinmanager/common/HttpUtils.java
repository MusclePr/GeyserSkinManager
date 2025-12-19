package com.github.camotoy.geyserskinmanager.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class HttpUtils {
    private static final Gson GSON = new Gson();
    private static final String USER_AGENT = "Geyser/SkinManager";
    private static final String CONNECTION_STRING = "--";
    private static final String BOUNDARY = "******";
    private static final String END = "\r\n";

    public static HttpResponse get(String urlString) {
        HttpURLConnection connection;

        try {
            URL url = URI.create(urlString).toURL();
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        try {
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("ContentType", "application/json");
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        }

        return readResponse(connection);
    }

    public static HttpResponse post(String urlString, BufferedImage... images) {
        HttpURLConnection connection;

        try {
            URL url = URI.create(urlString).toURL();
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data;boundary=" + BOUNDARY);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                writeDataFor(outputStream, images);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        }

        return readResponse(connection);
    }

    private static HttpResponse readResponse(HttpURLConnection connection) {
        try {
            int responseCode = connection.getResponseCode();
            try (InputStream stream = (responseCode >= 200 && responseCode < 400)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
                    InputStreamReader streamReader = new InputStreamReader(stream)) {

                JsonObject response = GSON.fromJson(streamReader, JsonObject.class);

                return new HttpResponse(responseCode, response);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read response", exception);
        }
    }

    public static void writeDataFor(DataOutputStream outputStream, BufferedImage... images) {
        try {
            for (int i = 0; i < images.length; i++) {
                outputStream.writeBytes(CONNECTION_STRING + BOUNDARY + END);
                outputStream.writeBytes(
                        "Content-Disposition:form-data;name=file;filename=image" + i + ".png");
                outputStream.writeBytes(END);
                outputStream.writeBytes(END);
                fileDataForImage(outputStream, images[i]);
                outputStream.writeBytes(END);
            }
            outputStream.writeBytes(CONNECTION_STRING + BOUNDARY + CONNECTION_STRING + END);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void fileDataForImage(OutputStream outputStream, BufferedImage image) {
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static final class HttpResponse {
        private final int httpCode;
        private final JsonObject response;

        public HttpResponse(int httpCode, JsonObject response) {
            this.httpCode = httpCode;
            this.response = response;
        }

        public int getHttpCode() {
            return httpCode;
        }

        public JsonObject getResponse() {
            return response;
        }
    }
}
