package com.example.ftp_client.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyItems;
    private ImageButton btnDeleteAll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historyItems = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyItems, this::showDeleteConfirmationDialog);
        recyclerView.setAdapter(historyAdapter);

        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmationDialog());

        loadHistory();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadHistory() {
        List<HistoryItem> loadedHistoryItems = SharedPreferencesUtil.loadHistoryList(this);
        if (loadedHistoryItems != null) {
            historyItems.clear();
            historyItems.addAll(loadedHistoryItems);
            historyAdapter.notifyDataSetChanged();
        }
    }

    private void deleteItem(int position) {
        if (historyItems.isEmpty()) {
            Log.e("History", "History items list is empty.");
            return;
        }

        if (position < 0 || position >= historyItems.size()) {
            Log.e("History", "Invalid position: " + position);
            return;
        }

        HistoryItem itemToDelete = historyItems.get(position);
        historyItems.remove(position);
        SharedPreferencesUtil.deleteHistoryItem(this, historyItems, itemToDelete);
        historyAdapter.updateHistoryItems(historyItems);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteAllItems() {
        historyItems.clear();
        historyAdapter.updateHistoryItems(historyItems);
        SharedPreferencesUtil.saveHistoryList(this, historyItems);
    }

    private void showDeleteConfirmationDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this history item?")
                .setPositiveButton("Yes", (dialog, which) -> deleteItem(position))
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete all history items?")
                .setPositiveButton("Yes", (dialog, which) -> deleteAllItems())
                .setNegativeButton("No", null)
                .show();
    }
}
