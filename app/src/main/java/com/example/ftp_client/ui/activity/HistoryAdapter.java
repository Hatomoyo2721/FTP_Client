package com.example.ftp_client.ui.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyItems;
    private OnDeleteClickListener onDeleteClickListener;

    public HistoryAdapter(List<HistoryItem> historyItems, OnDeleteClickListener onDeleteClickListener) {
        this.historyItems = historyItems;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem historyItem = historyItems.get(position);
        holder.textViewIpAddress.setText(historyItem.getIpAddress());
        holder.textViewFileName.setText(historyItem.getFileName());
        holder.textViewFileUri.setText(historyItem.getFileUri());
        holder.textViewTimestamp.setText(historyItem.getTimestamp());
        holder.btnDeleteHistory.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewIpAddress;
        TextView textViewFileName;
        TextView textViewFileUri;
        TextView textViewTimestamp;
        ImageButton btnDeleteHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewIpAddress = itemView.findViewById(R.id.textViewIpAddress);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            textViewFileUri = itemView.findViewById(R.id.textViewFileUri);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            btnDeleteHistory = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public void updateHistoryItems(List<HistoryItem> newHistoryItems) {
        this.historyItems = newHistoryItems;
        notifyDataSetChanged();
    }
}
