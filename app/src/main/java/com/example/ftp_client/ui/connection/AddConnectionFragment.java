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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;

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

    private EditText editTextIPAddress;
    private EditText editTextPort;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;
    private OnConnectionAddedListener listener;
    private Button buttonSave;
    private Button buttonBack;

    private final ThreadLocal<ProgressBar> loadingProgressBar = new ThreadLocal<>();
    private final ThreadLocal<TextView> loadingTextView = new ThreadLocal<>();
    private RelativeLayout loadingScreenLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_connection, container, false);

        initializeViews(view);
        setListeners();

        return view;
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

        int _port;
        try {
            _port = validatePort(port);
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(),"Invalid port number", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
        Map<String, Set<String>> ipUsernameMap = getIpUsernameMap(connectionList);

        if (!validateIpAndUsername(ipUsernameMap, ipAddress, username)) return;

        String email = SharedPreferencesUtil.getEmail(requireContext());

        ConnectionModel connection = new ConnectionModel(ipAddress, _port, username, password, email);

        //Send to Server - Java
        String connectionJson = connection.toJson();
        sendJsonToServer(connectionJson, connection);
    }

    private void sendJsonToServer(String connectionJson, ConnectionModel connection) {
        String ipAddress = editTextIPAddress.getText().toString().trim();
        String port = editTextPort.getText().toString().trim();

        // Show loading screen
        showLoadingScreen();

        new Thread(() -> {
            boolean isConnected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ipAddress, Integer.parseInt(port)), 5000);
                if (socket.isConnected()) {
                    try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                        outputStream.writeUTF("CONNECTION");
                        outputStream.writeUTF(connectionJson);
                        outputStream.flush();
                        isConnected = true;
                        showAlertOnUiThread("Connection added successfully");

                        // Save the new connection to SharedPreferences only if connected successfully
                        ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
                        connectionList.add(connection);
                        SharedPreferencesUtil.saveConnectionList(requireContext(), connectionList);

                        if (listener != null) {
                            listener.onConnectionAdded(connection);
                        }

                        if (isAdded()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
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
            Toast.makeText(getActivity(),"Please fill in all fields", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!isValidIPAddress(ipAddress)) {
            Toast.makeText(getActivity(),"Please enter a valid IP address", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!isValidUsername(username)) {
            Toast.makeText(getActivity(), "Username can only contain letters, numbers, and underscores (_)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private int validatePort(String port) throws NumberFormatException {
        int _port = Integer.parseInt(port);
        if (_port < 1 || _port > 65535) {
            throw new NumberFormatException("Port out of range");
        }
        return _port;
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
                        if (isAdded()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    })
                    .show());
        }
    }

    public interface OnConnectionAddedListener {
        void onConnectionAdded(ConnectionModel connection);
    }
}