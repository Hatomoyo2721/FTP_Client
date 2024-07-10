package com.example.ftp_client.ui.connection;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ftp_client.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetConnectionActions extends BottomSheetDialogFragment {

    private static final String ARG_CONNECTION = "arg_connection";
    private ConnectionModel connection;
    private ConnectionActionsListener listener;

    public static BottomSheetConnectionActions newInstance(ConnectionModel connection) {
        BottomSheetConnectionActions fragment = new BottomSheetConnectionActions();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONNECTION, connection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            connection = getArguments().getParcelable(ARG_CONNECTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_fragment_actions_connection, container, false);

        view.findViewById(R.id.buttonConnect).setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnectToServer(connection);
                dismiss();
            }
        });

        view.findViewById(R.id.buttonDelete).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteConnection(connection);
                dismiss();
            }
        });

        view.findViewById(R.id.buttonProperties).setOnClickListener(v -> showProperties());

        return view;
    }

    private void showProperties() {
        if (connection != null) {
            View propertiesLayout = requireView().findViewById(R.id.layoutProperties);
            if (propertiesLayout != null) {
                TextView textViewIP = propertiesLayout.findViewById(R.id.textViewIP);
                TextView textViewPort = propertiesLayout.findViewById(R.id.textViewPort);
                TextView textViewUsername = propertiesLayout.findViewById(R.id.textViewUsername);
                TextView textViewPassword = propertiesLayout.findViewById(R.id.textViewPassword);

                textViewIP.setText("IP Address: " + connection.getIpAddress());
                textViewPort.setText("Port: " + connection.getPort());
                textViewUsername.setText("Username: " + connection.getUsername());

                propertiesLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setConnectionActionsListener(ConnectionActionsListener listener) {
        this.listener = listener;
    }

    public interface ConnectionActionsListener {
        void onConnectToServer(ConnectionModel connection);
        void onDeleteConnection(ConnectionModel connection);
    }
}