package com.centigrade.browser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    
    private static final String DB_NAME = "browser_history.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_HISTORY = "history";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_URL = "url";
    private static final String COL_VISITED = "visited_at";
    
    private static HistoryManager instance;
    private SQLiteDatabase db;
    
    public static class HistoryItem {
        public long id;
        public String title;
        public String url;
        public long visitedAt;
        
        public HistoryItem(long id, String title, String url, long visitedAt) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.visitedAt = visitedAt;
        }
    }
    
    private HistoryManager(Context context) {
        HistoryDbHelper helper = new HistoryDbHelper(context.getApplicationContext());
        db = helper.getWritableDatabase();
    }
    
    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context);
        }
        return instance;
    }
    
    private static class HistoryDbHelper extends SQLiteOpenHelper {
        HistoryDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_URL + " TEXT NOT NULL, " +
                    COL_VISITED + " INTEGER DEFAULT 0)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            onCreate(db);
        }
    }
    
    public long addHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.equals("about:blank")) return -1;
        
        db.delete(TABLE_HISTORY, COL_URL + "=?", new String[]{url});
        
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title != null ? title : url);
        values.put(COL_URL, url);
        values.put(COL_VISITED, System.currentTimeMillis());
        return db.insert(TABLE_HISTORY, null, values);
    }
    
    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                COL_VISITED + " DESC", "500");
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL));
                long visitedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_VISITED));
                list.add(new HistoryItem(id, title, url, visitedAt));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    
    public List<HistoryItem> searchHistory(String keyword) {
        List<HistoryItem> list = new ArrayList<>();
        String likePattern = "%" + keyword + "%";
        Cursor cursor = db.query(TABLE_HISTORY, null,
                COL_TITLE + " LIKE ? OR " + COL_URL + " LIKE ?",
                new String[]{likePattern, likePattern},
                null, null, COL_VISITED + " DESC", "100");
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL));
                long visitedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_VISITED));
                list.add(new HistoryItem(id, title, url, visitedAt));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    
    public int deleteHistory(long id) {
        return db.delete(TABLE_HISTORY, COL_ID + "=?", new String[]{String.valueOf(id)});
    }
    
    public int deleteAllHistory() {
        return db.delete(TABLE_HISTORY, null, null);
    }
    
    public int getHistoryCount() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}
