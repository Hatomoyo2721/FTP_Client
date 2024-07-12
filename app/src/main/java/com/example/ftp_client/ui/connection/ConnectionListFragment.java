package com.example.ftp_client.ui.connection;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.file.FileListFragment;
import com.example.ftp_client.ui.file.FileTransferHelper;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionListFragment extends Fragment implements ConnectionAdapter.OnConnectionClickListener,
        BottomSheetConnectionActions.ConnectionActionsListener {

    private ArrayList<ConnectionModel> connectionList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ConnectionAdapter adapter;
    private TextView textViewNoConnections;
    private Button buttonAddConnection;

    private static final int CONNECTION_DELAY = 2000;
    private final ThreadLocal<ProgressBar> loadingConnectionView = new ThreadLocal<>();
    private final ThreadLocal<TextView> textViewLoading = new ThreadLocal<>();
    private View loadingView;
    private View overlayView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connection_list, container, false);

        // Initialize RecyclerView and other views
        recyclerView = view.findViewById(R.id.recyclerViewConnectionList);
        textViewNoConnections = view.findViewById(R.id.textViewNoConnections);
        buttonAddConnection = view.findViewById(R.id.buttonAddConnection);

        // Setup loading view
        loadingView = inflater.inflate(R.layout.loading_screen_layout, container, false);
        loadingConnectionView.set(loadingView.findViewById(R.id.loadingProgressBar));
        textViewLoading.set(loadingView.findViewById(R.id.loadingTextView));
        overlayView = view.findViewById(R.id.overlayView);

        // Load connection list from SharedPreferences
        connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());

        adapter = new ConnectionAdapter(getContext(), connectionList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        buttonAddConnection.setOnClickListener(v -> {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
            transaction.replace(R.id.fragment_container, new AddConnectionFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        updateConnectionList();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionList();
    }
    
    @Override
    public void onConnectionClick(ConnectionModel connection) {
        BottomSheetConnectionActions bottomSheet = BottomSheetConnectionActions.newInstance(connection);
        bottomSheet.setConnectionActionsListener(this);
        bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onConnectToServer(ConnectionModel connection) {
        if (connection != null) {
            showLoadingScreen();
            new ConnectToServerTask().execute(connection);
        } else {
            showErrorDialog("No connection selected or your account is not exist.");
        }
    }

    public void addConnection(ConnectionModel connection) {
        connectionList.add(connection);
        SharedPreferencesUtil.saveConnectionList(requireContext(), connectionList);
        updateConnectionList();
    }

    @Override
    public void onDeleteConnection(ConnectionModel connection) {
        connectionList.remove(connection);
        SharedPreferencesUtil.saveConnectionList(requireContext(), connectionList);
        updateConnectionList();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateConnectionList() {
        connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
        if (connectionList == null) {
            connectionList = new ArrayList<>();
        }
        adapter = new ConnectionAdapter(getContext(), connectionList, this); //Update list
        adapter.notifyDataSetChanged(); //Announce adapter changing
        recyclerView.setAdapter(adapter);

        if (connectionList.isEmpty()) {
            textViewNoConnections.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewNoConnections.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Method to show loading screen
    private void showLoadingScreen() {
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView != null) {
            if (loadingView.getParent() != null) {
                ((ViewGroup) loadingView.getParent()).removeView(loadingView);
            }
            rootView.addView(loadingView);
            overlayView.setVisibility(View.VISIBLE);
            setInteractionEnabled(false);
        }
    }

    // Method to hide loading screen
    private void hideLoadingScreen() {
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView != null) {
            rootView.removeView(loadingView);
            overlayView.setVisibility(View.GONE);
            setInteractionEnabled(true);

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setInteractionEnabled(boolean enabled) {
        recyclerView.setClickable(enabled);
        buttonAddConnection.setClickable(enabled);
        if (!enabled) {
            recyclerView.setOnTouchListener((v, event) -> true);
            buttonAddConnection.setOnTouchListener((v, event) -> true);
        } else {
            recyclerView.setOnTouchListener(null);
            buttonAddConnection.setOnTouchListener(null);
        }
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Connection Error")
                .setMessage(message)
                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @SuppressLint("StaticFieldLeak")
    private class ConnectToServerTask extends AsyncTask<ConnectionModel, Void, Boolean> {
        private ConnectionModel connection;
        private String errorMessage;

        @Override
        protected Boolean doInBackground(ConnectionModel... params) {
            connection = params[0];
            String ipAddress = connection.getIpAddress();
            int port = connection.getPort();

            try {
                Socket socket = new Socket(ipAddress, port);
                socket.close();
                return true;
            } catch (IOException e) {
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isConnected) {
            hideLoadingScreen();
            if (isConnected) {
                new Handler().postDelayed(() -> {
                    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

                    FileTransferHelper fileTransferHelper = new FileTransferHelper();
                    fileTransferHelper.setConnectionDetails(
                            connection.getIpAddress(), connection.getPort(),
                            connection.getUsername(), connection.getPassword());

                    transaction.replace(R.id.fragment_container, fileTransferHelper);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }, CONNECTION_DELAY);
            } else {
                showErrorDialog("Failed to connect to the server. " + errorMessage);
            }
        }
    }
}