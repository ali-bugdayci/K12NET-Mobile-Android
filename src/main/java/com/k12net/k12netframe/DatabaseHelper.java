package com.k12net.k12netframe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private SQLiteDatabase db = null;

    public DatabaseHelper(Context context, Integer version) {
        super(context, "K12NETFRAME.db", null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE UserProfile (ID INTEGER PRIMARY KEY AUTOINCREMENT, Username TEXT, Password TEXT, LastLoginTime INTEGER)");
        db.execSQL("CREATE TABLE Instance (ID INTEGER PRIMARY KEY AUTOINCREMENT, Url TEXT, IsSelected INTEGER)");

        this.db = db;

        this.addInstance("https://okul.k12net.com", false);
        this.addInstance("https://sis.moe.gov.ae", false);
        this.addInstance("https://nsis.sec.gov.qa", false);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS UserProfile");
        db.execSQL("DROP TABLE IF EXISTS Instance");
        onCreate(db);
    }

    public List<UserProfile> GetUserProfiles(){
        List<UserProfile> list = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM UserProfile ORDER BY LastLoginTime DESC", null);

        cursor.moveToFirst();

        while (!cursor.isAfterLast()){
            UserProfile userProfile = new UserProfile();

            userProfile.ID=cursor.getInt(0);
            userProfile.Username = cursor.getString(1);
            userProfile.Password = cursor.getString(2);
            userProfile.LastLoginTime = cursor.getLong(3);

            list.add(userProfile);

            cursor.moveToNext();
        }

        return list;
    }

    public void addUserProfile(String username, String password){
        ContentValues values = new ContentValues();

        values.put("Username", username);
        values.put("Password", password);

        SQLiteDatabase db = getWritableDatabase();

        db.insert("UserProfile", null, values);
        db.close();
    }

    public void update(UserProfile userProfile){
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("UPDATE UserProfile SET Username = '" + userProfile.Username + "', Password = '" + userProfile.Password + "', LastLoginTime = " + userProfile.LastLoginTime.toString() + " WHERE ID = " + userProfile.ID.toString());
        db.close();
    }

    public void delete(UserProfile userProfile){
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("DELETE FROM UserProfile WHERE ID = " + userProfile.ID.toString());
        db.close();
    }

    public List<Instance> GetInstances(){
        List<Instance> list = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM Instance ORDER BY IsSelected DESC", null);

        cursor.moveToFirst();

        while (!cursor.isAfterLast()){
            Instance instance = new Instance();

            instance.ID=cursor.getInt(0);
            instance.Url = cursor.getString(1);
            instance.IsSelected = cursor.getInt(2) == 1;

            list.add(instance);

            cursor.moveToNext();
        }

        return list;
    }

    public void addInstance(String url, Boolean isSelected){
        ContentValues values = new ContentValues();

        values.put("Url", url);
        values.put("IsSelected", isSelected ? 1 : 0);

        Boolean isdbCreated = false;

        if(db == null) {
            db = getWritableDatabase();
            isdbCreated = true;
        }

        if(isSelected){
            db.execSQL("UPDATE Instance SET IsSelected = 0");
        }

        db.insert("Instance", null, values);

        if(isdbCreated) db.close();
    }

    public void setAsDefault(Instance instance){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE Instance SET IsSelected = 0");
        db.execSQL("UPDATE Instance SET IsSelected = 1 WHERE ID = " + instance.ID.toString());
        db.close();
    }

    public class UserProfile {
        public Integer ID;
        public String Username;
        public String Password;
        public Long LastLoginTime;
    }

    public class Instance {
        public Integer ID;
        public String Url;
        public Boolean IsSelected;
    }
}


