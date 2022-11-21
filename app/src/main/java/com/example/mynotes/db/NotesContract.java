package com.example.mynotes.db;

import android.provider.BaseColumns;

public class NotesContract {
    public static final class NotesEntry implements BaseColumns{
        public static final String TABLE_NAME = "note";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_DATA = "data";
        public static final String COLUMN_PRIORITY = "priority";

        public static final String TYPE_TEXT = "TEXT";
        public static final String TYPE_INTEGER = "INTEGER";

        public static final String CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS "+
                TABLE_NAME + " ("+_ID+" "+TYPE_INTEGER+
                " PRIMARY KEY AUTOINCREMENT, "+ COLUMN_ID +
                " "+TYPE_INTEGER+", "+COLUMN_TITLE+" "+TYPE_TEXT+", "+COLUMN_DESCRIPTION+" "+TYPE_TEXT+", "+
                COLUMN_DATA+" "+TYPE_TEXT+", "+COLUMN_PRIORITY+" "+TYPE_INTEGER+ ")";

        public  static final String DROP_COMMAND = "DROP TABLE IF EXISTS "+TABLE_NAME;
    }
}
