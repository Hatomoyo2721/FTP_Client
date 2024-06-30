package com.example.ftp_client.ui.connection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;

import java.util.ArrayList;

public class ConnectionAdapter extends RecyclerView.Adapter<ConnectionAdapter.ConnectionViewHolder> {

    private final Context context;
    private final ArrayList<ConnectionModel> connectionList;
    private final OnConnectionClickListener listener;

    public ConnectionAdapter(Context context, ArrayList<ConnectionModel> connectionList, OnConnectionClickListener listener) {
        this.context = context;
        this.connectionList = connectionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConnectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_connection, parent, false);
        return new ConnectionViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectionViewHolder holder, int position) {
        holder.bind(connectionList.get(position));
    }

    @Override
    public int getItemCount() {
        return connectionList.size();
    }

    public static class ConnectionViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewIpAddress;
        private final ImageButton imageButtonActions;
        private ConnectionModel connection;

        public ConnectionViewHolder(@NonNull View itemView, OnConnectionClickListener listener) {
            super(itemView);
            textViewIpAddress = itemView.findViewById(R.id.textViewIpAddress);
            imageButtonActions = itemView.findViewById(R.id.imageButtonActions);

            itemView.setOnClickListener(v -> {
                if (connection != null) {
                    listener.onConnectionClick(connection);
                }
            });

            imageButtonActions.setOnClickListener(v -> {
                // No actions needed for now
            });
        }

        public void bind(ConnectionModel connection) {
            this.connection = connection;
            textViewIpAddress.setText(connection.getIpAddress());
        }
    }

    public interface OnConnectionClickListener {
        void onConnectionClick(ConnectionModel connection);
    }
}
