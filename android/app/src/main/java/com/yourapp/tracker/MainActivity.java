package com.yourapp.tracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_LOCATION            = 1;
    private static final int PERM_BACKGROUND_LOCATION = 2;

    private EditText etUsername, etPassword;
    private Button   btnLogin, btnRegister;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already logged in — skip straight to tracking
        if (AuthService.isLoggedIn(this)) {
            startTrackingService();
            return;
        }

        setContentView(R.layout.activity_main);
        etUsername  = findViewById(R.id.etUsername);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvStatus    = findViewById(R.id.tvStatus);

        requestLocationPermission();

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                tvStatus.setText("Please fill in all fields");
                return;
            }
            setLoading(true, "Logging in...");
            AuthService.login(this, user, pass, new AuthService.AuthCallback() {
                @Override public void onSuccess(String token) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Logged in! Starting tracker...");
                        startTrackingService();
                    });
                }
                @Override public void onFailure(String error) {
                    runOnUiThread(() -> {
                        setLoading(false, "Error: " + error);
                    });
                }
            });
        });

        btnRegister.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                tvStatus.setText("Please fill in all fields");
                return;
            }
            setLoading(true, "Registering...");
            AuthService.register(this, user, pass, new AuthService.AuthCallback() {
                @Override public void onSuccess(String token) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Registered! Starting tracker...");
                        startTrackingService();
                    });
                }
                @Override public void onFailure(String error) {
                    runOnUiThread(() -> setLoading(false, "Error: " + error));
                }
            });
        });
    }

    private void startTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        startForegroundService(intent);
        finish(); // close UI — service keeps running in background
    }

    private void setLoading(boolean loading, String status) {
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        tvStatus.setText(status);
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERM_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERM_LOCATION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                // Now request background location (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERM_BACKGROUND_LOCATION);
                }
            } else {
                if (tvStatus != null)
                    tvStatus.setText("Location permission is required for tracking.");
            }
        }
    }
}
