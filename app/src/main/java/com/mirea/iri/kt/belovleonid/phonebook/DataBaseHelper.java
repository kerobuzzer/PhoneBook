package com.mirea.iri.kt.belovleonid.phonebook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;


public class DataBaseHelper extends SQLiteOpenHelper {


    private static String TAG = "DataBaseHelper";
    private Context context;
    private static final String DATABASE_NAME = "PhoneBook.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "my_contacts";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_FN = "contact_full_name";
    private static final String COLUMN_TEL = "contact_telephone_number";
    private static final String COLUMN_AVATAR = "contact_avatar";

    public DataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query =
                "CREATE TABLE " + TABLE_NAME +
                        " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_FN + " TEXT, " +
                        COLUMN_TEL + " TEXT, " +
                        COLUMN_AVATAR + " TEXT);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addContacts(JsonArray jsonContacts) throws RuntimeException {

        Log.i(TAG, jsonContacts.toString());
        SQLiteDatabase db = this.getWritableDatabase();

        for(JsonElement element: jsonContacts) {
            ContentValues cv = new ContentValues();

            JsonObject elementObject = element.getAsJsonObject();
            String fullName = elementObject.get("name").toString().replaceAll("\"", "");
            String telephoneNumber = elementObject.get("phone").toString().replaceAll("\"", "");
            String avatar = elementObject.get("avatar").toString().replaceAll("\"", "");

            Log.i(TAG, fullName + telephoneNumber + avatar);
            cv.put(COLUMN_FN, fullName);
            cv.put(COLUMN_TEL, telephoneNumber);
            cv.put(COLUMN_AVATAR, avatar);

            int patienceCounter = 3;
            while (patienceCounter >= 0){
                Log.d(TAG, "Inserting contact: " + fullName + ", " + telephoneNumber + ", " + avatar);
                long result = db.insert(TABLE_NAME, null, cv);
                if(result != -1){
                    break;
                } else {
                    Log.e(TAG, "Failed try of writing data to DB");
                    patienceCounter--;
                }
            }
            if (patienceCounter <= 0){
                Log.e(TAG, "Database couldn't write data");
                RuntimeException ex = new RuntimeException("Database couldn't write data");
                throw ex;
            }
        }
        db.close();
    }

    public void removeContact(int id, String avatar) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE _id = " + id;

        if (avatar != null && !avatar.isEmpty()) {
            File avatarFile = new File(avatar);
            if (avatarFile.exists()) {
                avatarFile.delete();
            }
        }
        db.execSQL(sql);
        db.close();

        Log.i(TAG, "deleted contact with id: " + id);
    }
    public void removeContact(boolean selectAll){
        SQLiteDatabase db = this.getWritableDatabase();
        if (selectAll) {
            db.execSQL("DELETE FROM " + TABLE_NAME);
            Log.i(TAG, "DB was cleared");
        }
        db.close();

        File avatarsDir = new File(context.getFilesDir(), "avatars");
        if (avatarsDir.exists() && avatarsDir.isDirectory()) {
            boolean dirDeleted = avatarsDir.delete();
            Log.i(TAG, "Avatars dir deleted: " + dirDeleted);
        }
    }

    public int getLastId() throws RuntimeException {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + COLUMN_ID + ") FROM " + TABLE_NAME;
        int patienceCounter = 3;
        int lastId = 0;

        while (patienceCounter > 0) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(query, null);
                if (cursor != null && cursor.moveToFirst()) {
                    lastId = cursor.getInt(0);
                    cursor.close();
                    return lastId;
                } else {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return lastId;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching last ID " + e.getMessage());
                patienceCounter--;
                if (patienceCounter == 0) {
                    RuntimeException ex = new RuntimeException("Failed to fetch last ID after 3 attempts :(");
                    throw ex;
                }
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        db.close();
        return 0;
    }

    Cursor readAllData(){
        String query = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = null;
        if(db != null){
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }
}
