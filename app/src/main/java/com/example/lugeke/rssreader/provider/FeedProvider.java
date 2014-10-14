package com.example.lugeke.rssreader.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
/**
 * Created by lugeke on 2014/9/30.
 */
public class FeedProvider extends ContentProvider {
    private static final int URI_FEEDS = 1;

    private static final int URI_FEED = 2;

    private static final int URI_ENTRIES = 3;

    private static final int URI_ENTRY = 4;

    private static final int URI_ALLENTRIES = 5;

    private static final int URI_ALLENTRIES_ENTRY = 6;

    private static final int URI_FAVORITES = 7;

    private static final int URI_FAVORITES_ENTRY = 8;

    protected static final String TABLE_FEEDS = "feeds";

    private static final String TABLE_ENTRIES = "entries";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "FeedReader.db";

    private static UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "feeds", URI_FEEDS);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "feeds/#", URI_FEED);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "feeds/#/entries",
                URI_ENTRIES);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "feeds/#/entries/#",
                URI_ENTRY);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "entries",
                URI_ALLENTRIES);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "entries/#",
                URI_ALLENTRIES_ENTRY);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "favorites",
                URI_FAVORITES);
        URI_MATCHER.addURI(FeedContract.AUTHORITY, "favorites/#",
                URI_FAVORITES_ENTRY);

    }

    private static class FeedReaderDbHelper extends SQLiteOpenHelper {

        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            Log.i("create ", "database");
        }

        private static String SQL_CREATE_FEED = createTable(TABLE_FEEDS,
                FeedContract.FeedColumns.COLUMNS,
                FeedContract.FeedColumns.TYPES);
        private static String SQL_CREATE_ENTRY = createTable(TABLE_ENTRIES,
                FeedContract.EntryColumns.COLUMNS,
                FeedContract.EntryColumns.TYPES);

        private static String createTable(String table, String[] columns,
                                          String[] types) {
            StringBuilder result = new StringBuilder("CREATE TABLE ");
            result.append(table).append(" ( ");
            for (int i = 0; i < columns.length; ++i) {

                if(i!=0)
                    result.append(",");
                result.append(columns[i]).append(" ").append(types[i]);
            }
            result.append(" )");
            return result.toString();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_FEED);
            db.execSQL(SQL_CREATE_ENTRY);
            Log.i("create", "tables");
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            return;
        }

    }

    private FeedReaderDbHelper db;

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {

        String table = null;

        StringBuilder where = new StringBuilder();

        SQLiteDatabase database = db.getWritableDatabase();

        switch (URI_MATCHER.match(arg0)) {
            case URI_FEED: {// *******feeds/#
                table = TABLE_FEEDS;

                final String feedId = arg0.getPathSegments().get(1);

                new Thread() {
                    public void run() {
                        delete(FeedContract.EntryColumns.CONTENT_URI(feedId),
                                null, null);
                    }
                }.start();

                where.append(FeedContract.FeedColumns._ID).append('=')
                        .append(feedId);

                break;
            }
            case URI_FEEDS: {
                table = TABLE_FEEDS;
                break;
            }
            case URI_ENTRY: {// ****feeds/#/entries/#
                table = TABLE_ENTRIES;
                where.append(FeedContract.EntryColumns._ID).append('=')
                        .append(arg0.getPathSegments().get(3));
                break;
            }
            case URI_ENTRIES: {// ***feeds/#/entries
                table = TABLE_ENTRIES;
                where.append(FeedContract.EntryColumns.FEED_ID).append('=')
                        .append(arg0.getPathSegments().get(1));
                break;
            }
            case URI_ALLENTRIES: {
                table = TABLE_ENTRIES;
                break;
            }
            case URI_ALLENTRIES_ENTRY: {
                table = TABLE_ENTRIES;
                where.append(FeedContract.EntryColumns._ID).append('=')
                        .append(arg0.getPathSegments().get(1));
                break;
            }
        }
        if (!TextUtils.isEmpty(arg1)) {
            if (where.length() > 0)
                where.append(" and ");
            where.append(arg1);
        }
        return database.delete(table, where.toString(), arg2);

    }

    @Override
    public String getType(Uri arg0) {
        switch (URI_MATCHER.match(arg0)) {
            case URI_FEEDS:
                return "vnd.android.cursor.dir/vnd.feeddata.add_feed";
            case URI_FEED:
                return "vnd.android.cursor.item/vnd.feeddata.add_feed";
            case URI_FAVORITES:
            case URI_ALLENTRIES:
            case URI_ENTRIES:
                return "vnd.android.cursor.dir/vnd.feeddata.entry";
            case URI_FAVORITES_ENTRY:
            case URI_ALLENTRIES_ENTRY:
            case URI_ENTRY:
                return "vnd.android.cursor.item/vnd.feeddata.entry";
            default:
                throw new IllegalArgumentException("Unknown URI: " + arg0);
        }
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        long newId = -1;
        SQLiteDatabase database = db.getWritableDatabase();
        switch (URI_MATCHER.match(arg0)) {
            case URI_FEEDS:
                newId = database.insert(TABLE_FEEDS, null, arg1);
                break;
            case URI_ENTRIES:
                arg1.put(FeedContract.EntryColumns.FEED_ID, arg0
                        .getPathSegments().get(1));
                newId = database.insert(TABLE_ENTRIES, null, arg1);
                break;
            case URI_ALLENTRIES:
                newId = database.insert(TABLE_ENTRIES, null, arg1);
                break;
            default:
                throw new IllegalArgumentException("Illegal insert");
        }
        if (newId > -1) {
            getContext().getContentResolver().notifyChange(arg0, null);
            return ContentUris.withAppendedId(arg0, newId);
        } else {
            throw new SQLException("Could not insert row into " + arg0);
        }
    }

    @Override
    public boolean onCreate() {
        db = new FeedReaderDbHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
                        String arg4) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(arg0)) {
            case URI_FEEDS://feeds
                queryBuilder.setTables(TABLE_FEEDS);
                break;
            case URI_ENTRIES://feeds/#/entries
                queryBuilder.setTables(TABLE_ENTRIES);
                queryBuilder.appendWhere(new StringBuilder(
                        FeedContract.EntryColumns.FEED_ID).append('=')
                        .append(arg0.getPathSegments().get(1)));
                break;
            case URI_ENTRY://feeds/#/entries/#
                queryBuilder.setTables(TABLE_ENTRIES);
                queryBuilder.appendWhere(new StringBuilder(
                        FeedContract.EntryColumns._ID).append('=').append(
                        arg0.getPathSegments().get(3)));
                break;
            case URI_ALLENTRIES_ENTRY://entries/#
                queryBuilder.setTables(TABLE_ENTRIES);
                queryBuilder.appendWhere(new StringBuilder(
                        FeedContract.EntryColumns._ID).append("=").append(
                        arg0.getPathSegments().get(1)));


        }
        SQLiteDatabase database = db.getReadableDatabase();

        Cursor cursor = queryBuilder.query(database, arg1, arg2, arg3, null,
                null, arg4);
        cursor.setNotificationUri(getContext().getContentResolver(), arg0);
        return cursor;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        StringBuilder where = new StringBuilder();
        String table = null;

        SQLiteDatabase database = db.getWritableDatabase();
        switch (URI_MATCHER.match(arg0)) {
            case URI_FEED:// feeds/#
                table = TABLE_FEEDS;

                long feedId = Long.parseLong(arg0.getPathSegments().get(1));
                where.append(FeedContract.FeedColumns._ID).append('=')
                        .append(feedId);
                if (!TextUtils.isEmpty(arg2)) {

                    where.append(" and ").append(arg2);
                }
                return database.update(table, arg1, where.toString(), arg3);
            case URI_ALLENTRIES_ENTRY:// entries/#
                table = TABLE_ENTRIES;
                long entryId = Long.parseLong(arg0.getPathSegments().get(1));
                where.append(FeedContract.EntryColumns._ID).append('=')
                        .append(entryId);
                if (!TextUtils.isEmpty(arg2)) {
                    where.append(" and ").append(arg2);
                }

                return database.update(table, arg1, where.toString(), arg3);
        }
        return 0;

    }
}
