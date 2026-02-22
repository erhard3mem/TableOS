package com.yourapp.tracker;

import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // *** CHANGE THIS to your server's address ***
    // For local development on the same WiFi: "http://192.168.1.xxx:3000"
    // For production: "https://your-domain.com"
    private static final String BASE_URL = "http://192.168.1.100:3000";

    private static final String TAG = "ApiClient";

    /**
     * Generic POST request.
     * @param endpoint  e.g. "/auth/login" or "/data/mykey"
     * @param body      JSON payload
     * @param token     JWT token (pass null for unauthenticated routes)
     * @return          Response as JSONObject, or null on failure
     */
    public static JSONObject post(String endpoint, JSONObject body, String token) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null)
                conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            return new JSONObject(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "POST " + endpoint + " failed: " + e.getMessage());
            return null;
        }
    }
}
