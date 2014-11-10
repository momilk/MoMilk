package com.momilk.momilk;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;


public class CustomDatabaseAdapter {

    private static final String LOG_TAG = "CustomDatabaseAdapter";

    private CustomSQLOpenHelper mHelper;

    public CustomDatabaseAdapter(Context context) {
        mHelper = new CustomSQLOpenHelper(context);
    }


    public long insertData(String date, String duration, String amount) {

        SQLiteDatabase db = mHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(CustomSQLOpenHelper.DATE, date);
        contentValues.put(CustomSQLOpenHelper.DURATION, duration);
        contentValues.put(CustomSQLOpenHelper.AMOUNT, amount);
        Log.i(LOG_TAG, "inserting into DB: date: " + date + " duration: " + duration + " amount " + amount);

        long id = db.insert(CustomSQLOpenHelper.TABLE_NAME, null, contentValues);
        return id;
    }

    public ArrayList<HistoryFragment.HistoryEntry> getHistory() {
        SQLiteDatabase db = mHelper.getReadableDatabase();

        String[] columns = {CustomSQLOpenHelper.UID, CustomSQLOpenHelper.DATE,
                CustomSQLOpenHelper.DURATION, CustomSQLOpenHelper.AMOUNT};

        Cursor cursor = db.query(CustomSQLOpenHelper.TABLE_NAME, columns, null, null, null, null, null);

        ArrayList<HistoryFragment.HistoryEntry> history = new ArrayList<HistoryFragment.HistoryEntry>();
        

        while (cursor.moveToNext()) {
            int index1 = cursor.getColumnIndex(CustomSQLOpenHelper.UID);
            int index2 = cursor.getColumnIndex(CustomSQLOpenHelper.DATE);
            int index3 = cursor.getColumnIndex(CustomSQLOpenHelper.DURATION);
            int index4 = cursor.getColumnIndex(CustomSQLOpenHelper.AMOUNT);

            int uid = cursor.getInt(index1);
            String dateString = cursor.getString(index2);
            int duration = cursor.getInt(index3);
            int amount = cursor.getInt(index4);

            history.add(new HistoryFragment.HistoryEntry(uid, dateString, duration, amount));

        }

        cursor.close();

        return history;
    }

    public void deleteTable() {
        mHelper.deleteTable(CustomSQLOpenHelper.TABLE_NAME);
    }


    public void deleteDB() {
        mHelper.deleteDB(CustomSQLOpenHelper.DATABASE_NAME);
    }


    static class CustomSQLOpenHelper extends SQLiteOpenHelper {

        private static final String LOG_TAG = "CustomSQLOpenHelper";

        private static final String DATABASE_NAME = "momilk_db";
        private static final String TABLE_NAME = "measurements_history";
        private static final int DATABASE_VERSION = 1;
        private static final String UID = "_id";
        private static final String DATE = "Date";
        private static final String DURATION = "Duration";
        private static final String AMOUNT = "Amount";
        private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
                UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DATE + " VARCHAR(20), " +
                DURATION + " INTEGER, " +
                AMOUNT + " INTEGER" + ");";
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        private Context mContext;


        public CustomSQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(LOG_TAG, "onCreate is called");
            try {
                db.execSQL(CREATE_TABLE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(LOG_TAG, "onUpgrade is called. Old ver: " + oldVersion + "new ver: " + newVersion);
            try {
                db.execSQL(DROP_TABLE);
                onCreate(db);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void deleteTable(String tableName) {
            Log.i(LOG_TAG, "deleteTable was called for table: " + tableName);
            getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tableName);
        }

        public void deleteDB(String dbName) {
            Log.i(LOG_TAG, "deleteDB was called for db: " + dbName);
            mContext.deleteDatabase(dbName);
        }

    }
}