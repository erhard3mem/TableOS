package com.yourapp.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A local SQLite queue that buffers GPS records when the device is offline.
 * Records are sent to the cloud server and deleted from this queue once confirmed.
 */
public class LocalQueue extends SQLiteOpenHelper {

    private static final String DB_NAME    = "tracker_queue.db";
    private static final int    DB_VERSION = 1;
    private static final String TABLE      = "pending_records";

    public static class Record {
        public long   id;
        public String key;
        public String payload;
    }

    public LocalQueue(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE " + TABLE + " (" +
            "  id      INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  key     TEXT NOT NULL," +
            "  payload TEXT NOT NULL," +
            "  created INTEGER NOT NULL" +
            ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** Add a new record to the queue */
    public void enqueue(String key, JSONObject payload) {
        ContentValues cv = new ContentValues();
        cv.put("key",     key);
        cv.put("payload", payload.toString());
        cv.put("created", System.currentTimeMillis());
        getWritableDatabase().insert(TABLE, null, cv);
    }

    /** Get all pending records, oldest first */
    public List<Record> getPending() {
        List<Record> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(
            TABLE, null, null, null, null, null, "created ASC"
        );
        while (c.moveToNext()) {
            Record r = new Record();
            r.id      = c.getLong(c.getColumnIndexOrThrow("id"));
            r.key     = c.getString(c.getColumnIndexOrThrow("key"));
            r.payload = c.getString(c.getColumnIndexOrThrow("payload"));
            list.add(r);
        }
        c.close();
        return list;
    }

    /** Remove a successfully synced record */
    public void delete(long id) {
        getWritableDatabase().delete(TABLE, "id = ?", new String[]{String.valueOf(id)});
    }

    /** Number of records waiting to be synced */
    public int pendingCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }
}
