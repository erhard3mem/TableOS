package com.yourapp.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import org.json.JSONObject;

public class AuthService {

    private static final String PREFS     = "tracker_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER  = "username";

    public interface AuthCallback {
        void onSuccess(String token);
        void onFailure(String error);
    }

    public static void saveToken(Context ctx, String username, String token) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, username)
            .apply();
    }

    public static String getToken(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                  .getString(KEY_TOKEN, null);
    }

    public static String getUsername(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                  .getString(KEY_USER, null);
    }

    public static void clearToken(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().clear().apply();
    }

    public static boolean isLoggedIn(Context ctx) {
        return getToken(ctx) != null;
    }

    /** Login — runs network call off the main thread */
    public static void login(Context ctx, String username, String password, AuthCallback cb) {
        AsyncTask.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);

                JSONObject response = ApiClient.post("/auth/login", body, null);

                if (response != null && response.has("token")) {
                    String token = response.getString("token");
                    saveToken(ctx, username, token);
                    cb.onSuccess(token);
                } else {
                    String err = response != null
                        ? response.optString("error", "Login failed")
                        : "No response from server";
                    cb.onFailure(err);
                }
            } catch (Exception e) {
                cb.onFailure(e.getMessage());
            }
        });
    }

    /** Register, then auto-login on success */
    public static void register(Context ctx, String username, String password, AuthCallback cb) {
        AsyncTask.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);

                JSONObject response = ApiClient.post("/auth/register", body, null);

                if (response != null && response.optBoolean("success", false)) {
                    login(ctx, username, password, cb);
                } else {
                    String err = response != null
                        ? response.optString("error", "Registration failed")
                        : "No response from server";
                    cb.onFailure(err);
                }
            } catch (Exception e) {
                cb.onFailure(e.getMessage());
            }
        });
    }
}
