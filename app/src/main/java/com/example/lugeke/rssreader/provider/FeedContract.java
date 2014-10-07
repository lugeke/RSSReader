package com.example.lugeke.rssreader.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by lugeke on 2014/9/30.
 */
public class FeedContract {
    public FeedContract(){}


    public static String CONTENT = "content://";
    public static String AUTHORITY = "com.example.lugeke.rssreader.provider.FeedContract";

    private static final String TEXT = " TEXT ";
    private static final String DATETIME = " DATETIME ";
    private static final String BLOB = " BLOB ";
    private static final String PRIMARY_KEY = " INTEGER PRIMARY KEY AUTOINCREMENT ";

    public static class FeedColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(
                CONTENT).append(AUTHORITY).append("/feeds").toString());

        public static final Uri CONTENT_URI(String feedId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/feeds/").append(feedId).toString());
        }

        public static final Uri CONTENT_URI(long feedId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/feeds/").append(feedId).toString());
        }

        public static final String URL = "url";
        public static final String NAME = "name";
        public static final String LASTUPDATE = "lastupdate";
        public static final String ICON = "icon";
        public static final String REALLASTUPDATE="reallastupdate";
        public static final String[] COLUMNS = new String[] { _ID, URL, NAME,
                LASTUPDATE, ICON, REALLASTUPDATE};

        public static final String[] TYPES = new String[] { PRIMARY_KEY, TEXT,
                TEXT, DATETIME, BLOB,DATETIME };
    }

    public static class EntryColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(
                CONTENT).append(AUTHORITY).append("/entries").toString());

        public static Uri FAVORITES_CONTENT_URI = Uri.parse(new StringBuilder(
                CONTENT).append(AUTHORITY).append("/favorites").toString());

        public static Uri CONTENT_URI(String feedId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/feeds/").append(feedId).append("/entries")
                    .toString());
        }
        public static Uri CONTENT_URI(long feedId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/feeds/").append(feedId).append("/entries")
                    .toString());
        }

        public static Uri ENTRY_CONTENT_URI(String entryId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/entries/").append(entryId).toString());
        }
        public static Uri ENTRY_CONTENT_URI(long entryId) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append("/entries/").append(entryId).toString());
        }

        public static Uri PARENT_URI(String path) {
            return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
                    .append(path.substring(0, path.lastIndexOf('/')))
                    .toString());
        }

        public static final String FEED_ID = "feedid";

        public static final String TITLE = "title";

        public static final String AUTHOR = "author";

        public static final String ABSTRACT = "abstract";

        public static final String DATE = "date";
        public static final String LINK = "link";

        public static final String FAVORITE = "favorite";
        public static final String ISREAD="isread";
        public static final String[] COLUMNS = new String[] { _ID, FEED_ID,
                TITLE, AUTHOR,ABSTRACT, DATE, LINK, FAVORITE ,ISREAD};

        public static final String[] TYPES = new String[] { PRIMARY_KEY, " INT ",
                TEXT, TEXT, TEXT,DATETIME, TEXT, " INTEGER(1) ", " INTEGER(1) "};

    }
}
