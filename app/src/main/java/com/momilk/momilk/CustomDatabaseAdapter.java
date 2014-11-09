package com.momilk.momilk;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;




public class CustomDatabaseAdapter {

    private CustomSQLOpenHelper mHelper;

    public CustomDatabaseAdapter(Context context) {
        mHelper = new CustomSQLOpenHelper(context);
    }


    public long insertData(Long date, Long time) {

        SQLiteDatabase db = mHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(CustomSQLOpenHelper.DATE, date);
        contentValues.put(CustomSQLOpenHelper.TIME, time);


        long id = db.insert(CustomSQLOpenHelper.TABLE_NAME, null, contentValues);
        return id;
    }

    static class CustomSQLOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "momilk_db";
        private static final String TABLE_NAME = "measurements_history";
        private static final int DATABASE_VERSION = 1;
        private static final String UID = "_id";
        private static final String DATE = "Date";
        private static final String TIME = "Time";
        private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
                UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DATE + " INTEGER, " +
                TIME + " INTEGER" + ");";
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        private Context mContext;


        public CustomSQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(CREATE_TABLE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i2) {
            try {
                db.execSQL(DROP_TABLE);
                onCreate(db);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}