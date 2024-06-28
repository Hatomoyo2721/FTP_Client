package com.example.ftp_client.ui.connection;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.utils.SharedPreferencesUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
    private Button btnBack;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

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
        btnBack = view.findViewById(R.id.btnBack);
    }

    private void setListeners() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        buttonSave.setOnClickListener(v -> saveConnection());
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
            Toast.makeText(getContext(), "Invalid port number", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<ConnectionModel> connectionList = SharedPreferencesUtil.loadConnectionList(requireContext());
        Map<String, Set<String>> ipUsernameMap = getIpUsernameMap(connectionList);

        if (!validateIpAndUsername(ipUsernameMap, ipAddress, username)) return;

        ConnectionModel connection = new ConnectionModel(ipAddress, _port, username, password);

        if (listener != null) {
            listener.onConnectionAdded(connection);
        }

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private boolean validateInputFields(String ipAddress, String portStr, String username, String password) {
        if (TextUtils.isEmpty(ipAddress) || TextUtils.isEmpty(portStr) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidIPAddress(ipAddress)) {
            Toast.makeText(getContext(), "Please enter a valid IP address", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(),
                    "You can only add a maximum of 10 different IP addresses.", Toast.LENGTH_SHORT).show();
            return false;
        }

        Set<String> usernamesForIp = ipUsernameMap.get(ipAddress);

        if (usernamesForIp != null) {
            if (usernamesForIp.size() >= 2 && !usernamesForIp.contains(username)) {
                Toast.makeText(getContext(),
                        "This IP address already has two connections with different usernames.", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (usernamesForIp.contains(username)) {
                Toast.makeText(getContext(),
                        "A connection with this IP address and username already exists.", Toast.LENGTH_SHORT).show();
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

    public interface OnConnectionAddedListener {
        void onConnectionAdded(ConnectionModel connection);
    }
}
