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


    public long insertData(String index, String leftOrRight, String date, String duration,
                           String amount, String delta_roll, String delta_tilt) {

        SQLiteDatabase db = mHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(CustomSQLOpenHelper.INDEX, index);
        contentValues.put(CustomSQLOpenHelper.LEFT_OR_RIGHT, leftOrRight);
        contentValues.put(CustomSQLOpenHelper.DATE, date);
        contentValues.put(CustomSQLOpenHelper.DURATION, duration);
        contentValues.put(CustomSQLOpenHelper.AMOUNT, amount);
        contentValues.put(CustomSQLOpenHelper.DELTA_ROLL, delta_roll);
        contentValues.put(CustomSQLOpenHelper.DELTA_TILT, delta_tilt);

        long id = db.insert(CustomSQLOpenHelper.TABLE_NAME, null, contentValues);
        return id;
    }

    public ArrayList<HistoryFragment.HistoryEntry> getHistory() {
        SQLiteDatabase db = mHelper.getReadableDatabase();

        String[] columns = {CustomSQLOpenHelper.UID, CustomSQLOpenHelper.LEFT_OR_RIGHT,
                CustomSQLOpenHelper.DATE, CustomSQLOpenHelper.DURATION, CustomSQLOpenHelper.AMOUNT,
                CustomSQLOpenHelper.DELTA_ROLL, CustomSQLOpenHelper.DELTA_TILT};

        Cursor cursor = db.query(CustomSQLOpenHelper.TABLE_NAME, columns, null, null, null, null, null);

        ArrayList<HistoryFragment.HistoryEntry> history = new ArrayList<HistoryFragment.HistoryEntry>();
        

        while (cursor.moveToNext()) {
            int index0 = cursor.getColumnIndex(CustomSQLOpenHelper.UID);
            int index1 = cursor.getColumnIndex(CustomSQLOpenHelper.LEFT_OR_RIGHT);
            int index2 = cursor.getColumnIndex(CustomSQLOpenHelper.DATE);
            int index3 = cursor.getColumnIndex(CustomSQLOpenHelper.DURATION);
            int index4 = cursor.getColumnIndex(CustomSQLOpenHelper.AMOUNT);
            int index5 = cursor.getColumnIndex(CustomSQLOpenHelper.DELTA_ROLL);
            int index6 = cursor.getColumnIndex(CustomSQLOpenHelper.DELTA_TILT);

            int uid = cursor.getInt(index0);
            String leftOrRight = cursor.getString(index1);
            String dateString = cursor.getString(index2);
            int duration = cursor.getInt(index3);
            int amount = cursor.getInt(index4);
            int delta_roll = cursor.getInt(index5);
            int delta_tilt = cursor.getInt(index6);

            history.add(new HistoryFragment.HistoryEntry(uid, leftOrRight, dateString,
                    duration, amount, delta_roll, delta_tilt));

        }

        cursor.close();

        return history;
    }

    public void clearTable() {
        mHelper.clearTable(CustomSQLOpenHelper.TABLE_NAME);
    }

    static class CustomSQLOpenHelper extends SQLiteOpenHelper {

        private static final String LOG_TAG = "CustomSQLOpenHelper";

        private static final String DATABASE_NAME = "momilk_db";
        private static final String TABLE_NAME = "measurements_history";
        private static final int DATABASE_VERSION = 1;
        private static final String UID = "_id";
        private static final String INDEX = "Measurement_Index";
        private static final String LEFT_OR_RIGHT = "LeftOrRight";
        private static final String DATE = "Date";
        private static final String DURATION = "Duration";
        private static final String AMOUNT = "Amount";
        private static final String DELTA_ROLL = "Delta_roll";
        private static final String DELTA_TILT = "Delta_tilt";
        private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
                UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                INDEX + " INTEGER, " +
                LEFT_OR_RIGHT + " VARCHAR(10), " +
                DATE + " VARCHAR(20), " +
                DURATION + " INTEGER, " +
                AMOUNT + " INTEGER," +
                DELTA_ROLL + " INTEGER," +
                DELTA_TILT + " INTEGER" + ");";
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

        public void clearTable(String tableName) {
            Log.i(LOG_TAG, "clearTable was called for table: " + tableName);
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            db.execSQL(CREATE_TABLE);
        }

    }
}