package com.example.ftp_client.ui.connection;

import android.app.AlertDialog;
import android.os.AsyncTask;
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
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class AddExistingConnectionFragment extends Fragment {

    private ProgressBar loadingProgressBar;
    private TextView loadingTextView;
    private EditText editTextIPAddress;
    private EditText editTextPort;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;
    private Button buttonSave;
    private RelativeLayout loadingScreenLayout;

    public AddExistingConnectionFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_existing_connection, container, false);

        initializeViews(v);
        setListeners();

        return v;
    }

    private void initializeViews(View v) {
        editTextIPAddress = v.findViewById(R.id.editTextIPAddress);
        editTextPort = v.findViewById(R.id.editTextPort);
        editTextUsername = v.findViewById(R.id.editTextUsername);
        editTextPassword = v.findViewById(R.id.editTextPassword);
        btnTogglePassword = v.findViewById(R.id.btnTogglePasswordForExist);
        buttonSave = v.findViewById(R.id.buttonSave);
        loadingScreenLayout = v.findViewById(R.id.loadingScreenLayoutExisting);
        loadingProgressBar = v.findViewById(R.id.loadingProgressBarExisting);
        loadingTextView = v.findViewById(R.id.loadingTextViewExisting);
    }

    private void setListeners() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        buttonSave.setOnClickListener(v -> saveExistConnection());
    }

    private void saveExistConnection() {
        String ipAddress = editTextIPAddress.getText().toString().trim();
        String portStr = editTextPort.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (!validateInputFields(ipAddress, portStr, username)) return;

        int port = Integer.parseInt(portStr);

        ConnectToServerTask task = new ConnectToServerTask();
        task.execute(ipAddress, port, username, password);
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

    private boolean validateInputFields(String ipAddress, String portStr, String username) {
        if (TextUtils.isEmpty(ipAddress)) {
            showFieldAlert("IP address cannot be empty");
            return false;
        }
        if (TextUtils.isEmpty(portStr)) {
            showFieldAlert("Port number cannot be empty");
            return false;
        }
        if (TextUtils.isEmpty(username)) {
            showFieldAlert("Username cannot be empty");
            return false;
        }
        if (!isValidIPAddress(ipAddress)) {
            showFieldAlert("Please enter a valid IP address");
            return false;
        }
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showFieldAlert("Port must be between 1 and 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            showFieldAlert("Invalid port number");
            return false;
        }
        if (!isValidUsername(username)) {
            showFieldAlert("Username can only contain letters, numbers, and underscores (_)");
            return false;
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

    private void showFieldAlert(String message) {
        if (isAdded() && getActivity() != null) {
            new AlertDialog.Builder(getContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    })
                    .show();
        }
    }

    private class ConnectToServerTask extends AsyncTask<Object, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingScreenLayout.setVisibility(View.VISIBLE);
            loadingTextView.setText("Connecting...");
        }

        @Override
        protected String doInBackground(Object... params) {
            String ipAddress = (String) params[0];
            int port = (int) params[1];
            String username = (String) params[2];
            String password = (String) params[3];

            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;
            String response = null;

            try {
                socket = new Socket(ipAddress, port);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                // Send request type
                dataOutputStream.writeUTF("EXISTED_CONNECTION");
                dataOutputStream.flush();

                // Send JSON data
                String json = "{\"username\":\"" + username + "\", \"password\":\"" +
                        password + "\"}";
                dataOutputStream.writeUTF(json);
                dataOutputStream.flush();

                // Receive response
                response = dataInputStream.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dataOutputStream != null) dataOutputStream.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loadingScreenLayout.setVisibility(View.GONE);

            if (result != null) {
                if ("INVALID_USER".equals(result)) {
                    showAlertOnUiThread("Invalid user or password. Please try again.");
                } else if ("EXIST_USER".equals(result)) {
                    String ipAddress = editTextIPAddress.getText().toString().trim();
                    int port = Integer.parseInt(editTextPort.getText().toString().trim());
                    String username = editTextUsername.getText().toString().trim();

                    ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
                    ConnectionModel newConnection = new ConnectionModel(ipAddress, port, username);

                    boolean isExist = false;
                    for (ConnectionModel existingConnection : connectionList) {
                        if (existingConnection.equals(newConnection)) {
                            isExist = true;
                            break;
                        }
                    }

                    if (!isExist) {
                        connectionList.add(newConnection);
                        SharedPreferencesUtil.saveConnectionList(requireContext(), connectionList);
                        showAlertOnUiThread("Connection added successfully. Welcome back.");

                        editTextIPAddress.setText("");
                        editTextPort.setText("");
                        editTextUsername.setText("");

                        if (isAdded()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    } else {
                        showAlertOnUiThread("Connection already exists in the list.");
                    }
                } else {
                    Toast.makeText(getActivity(), "Connection error", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void showAlertOnUiThread(String message) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> showAlert(message));
            }
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
    }
}
