package com.yourapp.tracker;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.*;
import org.json.JSONObject;
import java.util.UUID;

/**
 * A foreground service that continuously tracks GPS location,
 * saves each point to a local queue, and syncs to the cloud when online.
 */
public class TrackingService extends Service {

    private static final String TAG           = "TrackingService";
    private static final String CHANNEL_ID    = "tracker_channel";
    private static final int    NOTIF_ID      = 1;

    // Tracking interval — adjust to balance accuracy vs battery life
    private static final long INTERVAL_MS   = 10_000; // 10 seconds
    private static final long FAST_INTERVAL =  5_000; // 5 seconds (fastest allowed)

    private FusedLocationProviderClient locationClient;
    private LocationCallback            locationCallback;
    private LocalQueue                  localQueue;
    private SyncManager                 syncManager;
    private String                      deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("Starting GPS tracker..."));

        // Use a stable device ID stored in prefs (avoids needing READ_PHONE_STATE)
        deviceId = getOrCreateDeviceId();

        localQueue  = new LocalQueue(this);
        syncManager = new SyncManager(this, localQueue);
        syncManager.startWatching();

        setupLocationTracking();
    }

    private void setupLocationTracking() {
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest request = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(INTERVAL_MS)
            .setFastestInterval(FAST_INTERVAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (android.location.Location loc : result.getLocations()) {
                    handleLocation(loc);
                }
            }
        };

        try {
            locationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            );
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted: " + e.getMessage());
            updateNotification("Error: location permission missing");
        }
    }

    private void handleLocation(android.location.Location loc) {
        try {
            // Key format: gps:<deviceId>:<timestamp>
            String key = "gps:" + deviceId + ":" + System.currentTimeMillis();

            JSONObject data = new JSONObject();
            data.put("timestamp",  System.currentTimeMillis());
            data.put("device_id",  deviceId);
            data.put("lat",        loc.getLatitude());
            data.put("lng",        loc.getLongitude());
            data.put("accuracy_m", loc.getAccuracy());
            data.put("altitude_m", loc.getAltitude());
            data.put("speed_ms",   loc.getSpeed());    // metres per second
            data.put("bearing",    loc.getBearing());  // degrees

            // Always save locally first — never lose a point
            localQueue.enqueue(key, data);

            // Attempt immediate sync if online
            if (syncManager.isOnline()) {
                syncManager.flushQueue();
            }

            updateNotification(String.format(
                "%.5f, %.5f  |  Queue: %d",
                loc.getLatitude(), loc.getLongitude(), localQueue.pendingCount()
            ));

        } catch (Exception e) {
            Log.e(TAG, "handleLocation error: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private String getOrCreateDeviceId() {
        android.content.SharedPreferences prefs =
            getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE);
        String id = prefs.getString("device_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CloudTracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Tracker", NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    // --- Lifecycle ---

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // restart automatically if killed by the OS
    }

    @Override
    public void onDestroy() {
        if (locationClient != null && locationCallback != null)
            locationClient.removeLocationUpdates(locationCallback);
        syncManager.stopWatching();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
