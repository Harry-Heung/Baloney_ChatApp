package com.example.ea_vtc_chat_project;

import android.content.Context;
import android.content.SharedPreferences;

public class MemoryData {

    private static final String SHARED_PREF_NAME = "ea_vtc_chat_project_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LAST_MSG_TS = "last_msg_ts";
    private static final String PREF_NAME = "MyAppPrefs";
    private static final String USER_ID_KEY = "UniqueName";

    public static void saveName(String name, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public static String getName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    public static void saveLastMsgTS(String timestamp, String chatKey, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_MSG_TS + "_" + chatKey, timestamp);
        editor.apply();
    }

    public static String getLastMsgTS(Context context, String chatKey) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_LAST_MSG_TS + "_" + chatKey, "0");
    }

    public static String getUniqueName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        return sharedPreferences.getString("unique_name", null);  // 修改为 unique_name
    }

    public static void saveUniqueName(String UniqueName, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("unique_name", UniqueName);  // 修改为 unique_name
        editor.apply();
    }

}
