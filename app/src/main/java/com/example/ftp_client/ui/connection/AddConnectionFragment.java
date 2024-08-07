package com.example.ftp_client.ui.connection;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddConnectionFragment extends Fragment {

    private final ThreadLocal<ProgressBar> loadingProgressBar = new ThreadLocal<>();
    private final ThreadLocal<TextView> loadingTextView = new ThreadLocal<>();
    private EditText editTextIPAddress;
    private EditText editTextPort;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;
    private OnConnectionAddedListener listener;
    private Button buttonSave;
    private Button buttonBack;
    private RelativeLayout loadingScreenLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_new_connection, container, false);

        initializeViews(v);
        setListeners();

        return v;
    }

    private void initializeViews(View view) {
        editTextIPAddress = view.findViewById(R.id.editTextIPAddress);
        editTextPort = view.findViewById(R.id.editTextPort);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        btnTogglePassword = view.findViewById(R.id.btnTogglePassword);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonBack = view.findViewById(R.id.btnBack);

        loadingProgressBar.set(view.findViewById(R.id.loadingProgressBar));
        loadingTextView.set(view.findViewById(R.id.loadingTextView));
        loadingScreenLayout = view.findViewById(R.id.loadingScreenLayout);
    }

    private void setListeners() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        buttonSave.setOnClickListener(v -> saveConnection());
        buttonBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnConnectionAddedListener) {
            listener = (OnConnectionAddedListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnConnectionAddedListener");
        }
    }

    private void saveConnection() {
        String ipAddress = editTextIPAddress.getText().toString().trim();
        String port = editTextPort.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (!validateInputFields(ipAddress, port, username, password)) return;

        int _port = getValidatedPort(port);
        if (_port == -1) return;

        ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
        if (connectionList == null) {
            connectionList = new ArrayList<>();
        }
        Map<String, Set<String>> ipUsernameMap = getIpUsernameMap(connectionList);

        if (!validateIpAndUsername(ipUsernameMap, ipAddress, username)) return;

        String email = SharedPreferencesUtil.getEmail(requireContext());

        ConnectionModel connection = new ConnectionModel(ipAddress, _port, username, password, email);

        //Send to Server - Java
        String connectionJson = connection.toJson();
        sendJsonToServer(connectionJson, connection);
    }

    private int getValidatedPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showAlert("Port number must be between 1 and 65535");
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            showAlert("Invalid port number");
            return -1;
        }
    }

    private void sendJsonToServer(String connectionJson, ConnectionModel connection) {
        String ipAddress = editTextIPAddress.getText().toString().trim();
        String port = editTextPort.getText().toString().trim();

        // Show loading screen
        showLoadingScreen();

        new Thread(() -> {
            boolean isConnected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ipAddress, Integer.parseInt(port)), 0);
                if (socket.isConnected()) {
                    try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                         DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

                        outputStream.writeUTF("ADD_USER");
                        outputStream.writeUTF(connectionJson);
                        outputStream.flush();

                        String response = inputStream.readUTF();
                        if ("USER_EXISTS".equals(response)) {
                            showAlertOnUiThread("A connection with this username already exists.");
                        } else if ("CONNECTION_SAVED".equals(response)) {
                            isConnected = true;
                            showAlertOnUiThread("Connection added successfully");

                            // Save the new connection to SharedPreferences only if connected successfully
                            ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
                            if (connectionList == null) {
                                connectionList = new ArrayList<>();
                            }
                            connectionList.add(connection);
                            SharedPreferencesUtil.saveConnectionList(requireContext(), connectionList);

                            if (listener != null) {
                                listener.onConnectionAdded(connection);
                            }

                            if (isAdded()) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                    }
                } else {
                    showAlertOnUiThread("Server is not running");
                }
            } catch (IOException e) {
                e.printStackTrace();
                showAlertOnUiThread("Failed to connect to server: " + e.getMessage());
            } finally {
                hideLoadingScreenOnUiThread();
            }
        }).start();
    }

    private void showAlertOnUiThread(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> showAlert(message));
        }
    }

    private void hideLoadingScreenOnUiThread() {
        if (isAdded()) {
            requireActivity().runOnUiThread(this::hideLoadingScreen);
        }
    }

    private void showLoadingScreen() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                loadingScreenLayout.setVisibility(View.VISIBLE);
                setInteractionEnabled(false);
            });
        }
    }

    private void hideLoadingScreen() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                loadingScreenLayout.setVisibility(View.GONE);
                setInteractionEnabled(true);
            });
        }
    }

    private void setInteractionEnabled(boolean enabled) {
        View rootView = getView();
        if (rootView != null) {
            setViewGroupEnabled((ViewGroup) rootView, enabled);
        }
    }

    private void setViewGroupEnabled(ViewGroup viewGroup, boolean enabled) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                setViewGroupEnabled((ViewGroup) child, enabled);
            }
        }
    }

    private boolean validateInputFields(String ipAddress, String portStr, String username, String password) {
        if (TextUtils.isEmpty(ipAddress) || TextUtils.isEmpty(portStr) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showAlert("Please fill in all fields");
            return false;
        }
        if (!isValidIPAddress(ipAddress)) {
            showAlert("Please enter a valid IP address");
            return false;
        }
        if (!isValidUsername(username)) {
            showAlert("Username can only contain letters, numbers, and underscores (_)");
            return false;
        }
        if (!isValidPassword(password)) {
            showAlert("Password must be 6 to 12 characters long and contain only letters and numbers");
            return false;
        }
        return true;
    }

    private boolean isValidPassword(String password) {
        String passwordRegex = "^[a-zA-Z0-9]{6,12}$";
        return password.matches(passwordRegex);
    }

    private Map<String, Set<String>> getIpUsernameMap(ArrayList<ConnectionModel> connectionList) {
        Map<String, Set<String>> ipUsernameMap = new HashMap<>();
        for (ConnectionModel connection : connectionList) {
            ipUsernameMap.computeIfAbsent(connection.getIpAddress(), k -> new HashSet<>()).add(connection.getUsername());
        }
        return ipUsernameMap;
    }

    private boolean validateIpAndUsername(Map<String, Set<String>> ipUsernameMap, String ipAddress, String username) {
        if (ipUsernameMap.size() >= 10 && !ipUsernameMap.containsKey(ipAddress)) {
            showAlert("You can only add a maximum of 10 different IP addresses.");
            return false;
        }

        Set<String> usernamesForIp = ipUsernameMap.get(ipAddress);

        if (usernamesForIp != null) {
            if (usernamesForIp.size() >= 5 && !usernamesForIp.contains(username)) {
                showAlert("This IP address already has two connections with different usernames.");
                return false;
            }

            if (usernamesForIp.contains(username)) {
                showAlert("A connection with this IP address and username already exists.");
                return false;
            }
        }
        return true;
    }

    private boolean isValidIPAddress(String ip) {
        String ipRegex = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        return ip.matches(ipRegex);
    }

    private boolean isValidUsername(String username) {
        String usernameRegex = "^[a-zA-Z0-9_]+$";
        return username.matches(usernameRegex);
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.baseline_visibility_off);
        } else {
            editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.baseline_visibility);
        }

        editTextPassword.setSelection(editTextPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void showAlert(String message) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> new AlertDialog.Builder(getContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    })
                    .show());
        }
    }

    public interface OnConnectionAddedListener {
        void onConnectionAdded(ConnectionModel connection);
    }
}