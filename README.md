# CloudTracker

A full-stack GPS tracking system consisting of:

- **`server/`** — Node.js REST API with SQLite storage
- **`android/`** — Android app with continuous GPS tracking and offline sync

---

## Quick Start

### 1. Start the Server

```bash
cd server
npm install
node server.js
```

Server runs on `http://localhost:3000`

### 2. Open the Android App

1. Open the `android/` folder in Android Studio
2. Edit `ApiClient.java` and set `BASE_URL` to your server IP
3. Press ▶ Run

---

## Architecture

```
Android App
    │
    │  POST /auth/login  →  JWT token
    │  POST /data/:key   →  store GPS point
    │
Node.js Server (Express)
    │
SQLite (cloud.db)
    ├── users
    └── data_store
```

## Project Structure

```
CloudTracker/
├── server/
│   ├── server.js          Node.js REST API
│   ├── package.json
│   └── README.md
└── android/
    ├── README.md
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/yourapp/tracker/
        │   ├── MainActivity.java       Login/Register UI
        │   ├── ApiClient.java          HTTP client
        │   ├── AuthService.java        JWT auth + storage
        │   ├── TrackingService.java    Foreground GPS service
        │   ├── LocalQueue.java         Offline SQLite buffer
        │   └── SyncManager.java        Connectivity-aware sync
        └── res/layout/
            └── activity_main.xml
```
