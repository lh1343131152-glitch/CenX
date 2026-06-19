package com.centigrade.browser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {
    
    private static final String DB_NAME = "browser_bookmarks.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_URL = "url";
    private static final String COL_CREATED = "created_at";
    
    private static BookmarkManager instance;
    private SQLiteDatabase db;
    
    public static class BookmarkItem {
        public long id;
        public String title;
        public String url;
        public long createdAt;
        
        public BookmarkItem(long id, String title, String url, long createdAt) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.createdAt = createdAt;
        }
    }
    
    private BookmarkManager(Context context) {
        BookmarkDbHelper helper = new BookmarkDbHelper(context.getApplicationContext());
        try {
            db = helper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static synchronized BookmarkManager getInstance(Context context) {
        if (instance == null) {
            instance = new BookmarkManager(context);
        }
        return instance;
    }
    
    private static class BookmarkDbHelper extends SQLiteOpenHelper {
        BookmarkDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARKS + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_URL + " TEXT NOT NULL UNIQUE, " +
                    COL_CREATED + " INTEGER DEFAULT 0)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
            onCreate(db);
        }
    }
    
    public long addBookmark(String title, String url) {
        if (url == null || url.isEmpty()) return -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COL_TITLE, title != null ? title : url);
            values.put(COL_URL, url);
            values.put(COL_CREATED, System.currentTimeMillis());
            return db.insertOrThrow(TABLE_BOOKMARKS, null, values);
        } catch (Exception e) {
            // 可能已存在（UNIQUE冲突）或其他错误
            e.printStackTrace();
            return -1;
        }
    }
    
    public boolean isBookmarked(String url) {
        if (url == null || db == null) return false;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARKS, new String[]{COL_ID},
                    COL_URL + "=?", new String[]{url}, null, null, null);
            return cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    public int deleteBookmark(long id) {
        if (db == null) return 0;
        try {
            return db.delete(TABLE_BOOKMARKS, COL_ID + "=?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public int deleteBookmarkByUrl(String url) {
        if (url == null || db == null) return 0;
        try {
            return db.delete(TABLE_BOOKMARKS, COL_URL + "=?", new String[]{url});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public List<BookmarkItem> getAllBookmarks() {
        List<BookmarkItem> list = new ArrayList<>();
        if (db == null) return list;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null,
                    COL_CREATED + " DESC");
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                    String url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL));
                    long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED));
                    list.add(new BookmarkItem(id, title, url, createdAt));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }
    
    public int getBookmarkCount() {
        if (db == null) return 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BOOKMARKS, null);
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    public void updateBookmark(long id, String newTitle, String newUrl) {
        if (db == null) return;
        ContentValues values = new ContentValues();
        if (newTitle != null) values.put(COL_TITLE, newTitle);
        if (newUrl != null) values.put(COL_URL, newUrl);
        if (values.size() > 0) {
            try {
                db.update(TABLE_BOOKMARKS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        instance = null;
    }
}