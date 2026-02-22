# CloudTracker Android App

An Android app that continuously tracks GPS location and syncs it to the CloudTracker server.

## Setup

1. Open the `android/` folder in **Android Studio**
2. Open `app/src/main/java/com/yourapp/tracker/ApiClient.java`
3. Change `BASE_URL` to your server's address:
   ```java
   private static final String BASE_URL = "http://192.168.1.xxx:3000"; // local dev
   // or
   private static final String BASE_URL = "https://your-domain.com";   // production
   ```
4. Click **Sync Now** when prompted, then press ▶ **Run**

## Features

- Continuous GPS tracking via `FusedLocationProviderClient`
- Offline-first: all points saved locally in SQLite before sending
- Auto-sync when connectivity is restored
- Foreground service with live notification showing coordinates
- JWT authentication with auto-login on next app launch

## Permissions Required

- `ACCESS_FINE_LOCATION` — GPS tracking
- `ACCESS_BACKGROUND_LOCATION` — tracking when app is in background (Android 10+)
- `FOREGROUND_SERVICE` — keeps service alive
- `INTERNET` — cloud sync
- `ACCESS_NETWORK_STATE` — detect online/offline status

## Data Sent to Cloud

Each GPS point is stored under the key `gps:<deviceId>:<timestamp>`:

```json
{
  "timestamp": 1714000000000,
  "device_id": "a1b2c3d4",
  "lat": 48.20849,
  "lng": 16.37208,
  "accuracy_m": 12.5,
  "altitude_m": 170.0,
  "speed_ms": 0.0,
  "bearing": 0.0
}
```

## Adjusting Tracking Interval

In `TrackingService.java`:
```java
private static final long INTERVAL_MS   = 10_000; // 10 seconds (change as needed)
private static final long FAST_INTERVAL =  5_000; // 5 seconds minimum
```

For lower battery usage, increase to 30_000 (30s) or 60_000 (60s).
