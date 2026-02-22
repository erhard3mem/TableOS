package com.yourapp.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Watches for network connectivity changes and flushes the local queue
 * to the cloud server whenever the device comes back online.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    private final Context         ctx;
    private final LocalQueue      queue;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private BroadcastReceiver connectivityReceiver;

    public SyncManager(Context ctx, LocalQueue queue) {
        this.ctx   = ctx;
        this.queue = queue;
    }

    /** Start watching for connectivity changes */
    public void startWatching() {
        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isOnline()) {
                    Log.d(TAG, "Network back online — flushing queue");
                    flushQueue();
                }
            }
        };
        ctx.registerReceiver(
            connectivityReceiver,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        );
    }

    /** Stop watching (call in onDestroy) */
    public void stopWatching() {
        if (connectivityReceiver != null) {
            try {
                ctx.unregisterReceiver(connectivityReceiver);
            } catch (Exception ignored) {}
        }
    }

    /** Upload all pending records to the server */
    public void flushQueue() {
        executor.execute(() -> {
            String token = AuthService.getToken(ctx);
            if (token == null || !isOnline()) return;

            List<LocalQueue.Record> pending = queue.getPending();
            if (pending.isEmpty()) return;

            Log.d(TAG, "Flushing " + pending.size() + " pending record(s)");

            for (LocalQueue.Record record : pending) {
                try {
                    JSONObject payload  = new JSONObject(record.payload);
                    JSONObject response = ApiClient.post("/data/" + record.key, payload, token);

                    if (response != null) {
                        queue.delete(record.id); // only delete after confirmed upload
                        Log.d(TAG, "Synced record id=" + record.id);
                    } else {
                        Log.w(TAG, "Failed to sync record id=" + record.id + " — will retry later");
                        break; // stop if server is unreachable
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Sync error: " + e.getMessage());
                    break;
                }
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm =
            (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
