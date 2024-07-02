package com.example.ftp_client.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ftp_client.ui.activity.HistoryItem;
import com.example.ftp_client.ui.connection.ConnectionModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesUtil {

    private static final String PREF_NAME = "FTPClientPreferences";
    private static final String KEY_CONNECTION_LIST = "ConnectionList";
    private static final String KEY_EMAIL = "UserEmail";

    public static void saveConnectionList(Context context, ArrayList<ConnectionModel> connectionList) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = new Gson().toJson(connectionList);
        editor.putString(KEY_CONNECTION_LIST, json);
        editor.apply();
    }

    public static ArrayList<ConnectionModel> loadConnectionList(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_CONNECTION_LIST, null);
        Type type = new TypeToken<ArrayList<ConnectionModel>>() {}.getType();
        return new Gson().fromJson(json, type != null ? type : new TypeToken<ArrayList<ConnectionModel>>() {}.getType());
    }

    public static void saveHistoryList(Context context, List<HistoryItem> historyList) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MessageHistory", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = new Gson().toJson(historyList);
        editor.putString("messages", json);
        editor.apply();
    }

    public static List<HistoryItem> loadHistoryList(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MessageHistory", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("messages", null);
        if (json == null) {
            Log.d("History", "No history found in SharedPreferences.");
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<HistoryItem>>() {}.getType();
        List<HistoryItem> historyItems = new Gson().fromJson(json, type);
        Log.d("History", "History loaded from SharedPreferences: " + historyItems.size() + " items.");
        return historyItems;
    }

    public static void deleteHistoryItem(Context context, List<HistoryItem> historyList, HistoryItem historyItemToDelete) {
        historyList.removeIf(historyItem ->
                historyItem.getIpAddress().equals(historyItemToDelete.getIpAddress()) &&
                        historyItem.getFileName().equals(historyItemToDelete.getFileName()) &&
                        historyItem.getFileUri().equals(historyItemToDelete.getFileUri()) &&
                        historyItem.getTimestamp().equals(historyItemToDelete.getTimestamp()));
        saveHistoryList(context, historyList);
    }

    public static void saveEmail(Context context, String email) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public static String getEmail(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_EMAIL, null);
    }
}