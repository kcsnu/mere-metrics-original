package com.example.cs360_charlton_molloy_keir;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DB_NAME = "cs360_weight_tracker.db";
    private static final int DB_VERSION = 2;

    // Users table and columns
    public static final String TABLE_USERS = "Users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";

    // Password columns
    public static final String COL_PASSWORD_SALT = "password_salt";
    public static final String COL_PASSWORD_HASH = "password_hash";

    // Daily weights table and columns
    public static final String TABLE_DAILY_WEIGHTS = "DailyWeights";
    public static final String COL_WEIGHT_ID = "id";
    public static final String COL_WEIGHT_USER_ID = "user_id";
    public static final String COL_ENTRY_DATE = "entry_date";
    public static final String COL_WEIGHT_VALUE = "weight";

    // Goal weight table and columns
    public static final String TABLE_GOAL_WEIGHT = "GoalWeight";
    public static final String COL_GOAL_USER_ID = "user_id";
    public static final String COL_GOAL_VALUE = "goal_weight";

    public AppDatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // Enforce foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create Users table
        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " ("
                        + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_USERNAME + " TEXT NOT NULL UNIQUE, "
                        + COL_PASSWORD_SALT + " TEXT NOT NULL, "
                        + COL_PASSWORD_HASH + " TEXT NOT NULL"
                        + ")"
        );

        // Create DailyWeights table
        db.execSQL(
                "CREATE TABLE " + TABLE_DAILY_WEIGHTS + " ("
                        + COL_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_WEIGHT_USER_ID + " INTEGER NOT NULL, "
                        + COL_ENTRY_DATE + " TEXT NOT NULL, "
                        + COL_WEIGHT_VALUE + " REAL NOT NULL, "
                        + "FOREIGN KEY(" + COL_WEIGHT_USER_ID + ") REFERENCES "
                        + TABLE_USERS + "(" + COL_USER_ID + ")"
                        + ")"
        );

        // Create GoalWeight table
        db.execSQL(
                "CREATE TABLE " + TABLE_GOAL_WEIGHT + " ("
                        + COL_GOAL_USER_ID + " INTEGER PRIMARY KEY, "
                        + COL_GOAL_VALUE + " REAL NOT NULL, "
                        + "FOREIGN KEY(" + COL_GOAL_USER_ID + ") REFERENCES "
                        + TABLE_USERS + "(" + COL_USER_ID + ")"
                        + ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop and recreate tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOAL_WEIGHT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Users

    // Function to validate login (returns userId or -1)
    public long validateLogin(String username, String password) {
        if (username == null || password == null) {
            return -1;
        }

        String safeUsername = username.trim();
        if (safeUsername.isEmpty()) {
            return -1;
        }

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID, COL_PASSWORD_SALT, COL_PASSWORD_HASH},
                COL_USERNAME + "=?",
                new String[]{safeUsername},
                null, null, null
        )) {

            // If the user exists, verify the entered password using the stored salt and hash
            if (cursor.moveToFirst()) {
                long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID));
                String saltB64 = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_SALT));
                String hashB64 = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_HASH));

                boolean valid = PasswordUtil.verifyPassword(password, saltB64, hashB64);
                return valid ? userId : -1;
            }
        } catch (Exception e) {
            return -1;
        }

        return -1;
    }

    // Function to create a new user (returns rowId or -1 on failure)
    public long createUser(String username, String password) {
        if (username == null || password == null) {
            return -1;
        }

        String safeUsername = username.trim();
        if (safeUsername.isEmpty()) {
            return -1;
        }

        // Hash the password with a random salt for storage
        PasswordUtil.HashResult result = PasswordUtil.hashPassword(password);
        if (result == null) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, safeUsername);
        values.put(COL_PASSWORD_SALT, result.saltB64);
        values.put(COL_PASSWORD_HASH, result.hashB64);

        try {
            return db.insert(TABLE_USERS, null, values);
        } catch (Exception e) {
            return -1;
        }
    }

    // Function to check if a username already exists
    public boolean usernameExists(String username) {
        if (username == null) {
            return false;
        }

        String safeUsername = username.trim();
        if (safeUsername.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=?",
                new String[]{safeUsername},
                null, null, null
        )) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }

    // Goal weight

    // Function to insert/update the goal weight for a user
    public void upsertGoalWeight(long userId, double goalWeight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_GOAL_USER_ID, userId);
        values.put(COL_GOAL_VALUE, goalWeight);

        // Replace keeps one goal row per user
        db.insertWithOnConflict(TABLE_GOAL_WEIGHT, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Function to get the goal weight for a user (returns null if not set)
    public Double getGoalWeight(long userId) {
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_GOAL_WEIGHT,
                new String[]{COL_GOAL_VALUE},
                COL_GOAL_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(cursor.getColumnIndexOrThrow(COL_GOAL_VALUE));
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    // Daily weight entries

    // Function to add a daily weight entry (Create)
    public long addDailyWeight(long userId, String date, double weight) {
        if (date == null) {
            return -1;
        }

        String safeDate = date.trim();
        if (safeDate.isEmpty()) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_WEIGHT_USER_ID, userId);
        values.put(COL_ENTRY_DATE, safeDate);
        values.put(COL_WEIGHT_VALUE, weight);

        try {
            return db.insert(TABLE_DAILY_WEIGHTS, null, values);
        } catch (Exception e) {
            return -1;
        }
    }

    // Function to get daily weight entries for a user (Read)
    public List<WeightEntry> getDailyWeights(long userId) {
        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_DAILY_WEIGHTS,
                new String[]{COL_WEIGHT_ID, COL_ENTRY_DATE, COL_WEIGHT_VALUE},
                COL_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_WEIGHT_ID + " DESC"
        )) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_WEIGHT_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_ENTRY_DATE));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT_VALUE));
                entries.add(new WeightEntry(id, date, weight));
            }
        } catch (Exception e) {
            return entries;
        }

        return entries;
    }

    // Function to update an entry by id for a specific user (Update)
    public boolean updateDailyWeight(long userId, long entryId, String newDate, double newWeight) {
        if (newDate == null) {
            return false;
        }

        String safeDate = newDate.trim();
        if (safeDate.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ENTRY_DATE, safeDate);
        values.put(COL_WEIGHT_VALUE, newWeight);

        int rows = db.update(
                TABLE_DAILY_WEIGHTS,
                values,
                COL_WEIGHT_ID + "=? AND " + COL_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(entryId), String.valueOf(userId)}
        );

        return rows > 0;
    }

    // Function to delete an entry by id for a specific user (Delete)
    public boolean deleteDailyWeight(long userId, long entryId) {
        SQLiteDatabase db = getWritableDatabase();

        int rows = db.delete(
                TABLE_DAILY_WEIGHTS,
                COL_WEIGHT_ID + "=? AND " + COL_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(entryId), String.valueOf(userId)}
        );

        return rows > 0;
    }
}