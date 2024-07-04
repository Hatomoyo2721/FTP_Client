package com.example.ftp_client.ui.file;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.activity.HistoryItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileTransferHelper extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    private static final int BUFFER_SIZE = 4096;
    private Uri selectedFileUri;

    private FloatingActionButton fabSendFile;
    private FloatingActionButton fabSelectFile;
    private FloatingActionButton fabReloadServer;
    private FloatingActionButton fabDisconnect;
    private TextView textViewStatus;
    private ProgressBar progressBarReload;

    private String serverIP;
    private int serverPort;
    private String username;
    private String password;

    private AlertDialog serverShutdownDialog;
    private Handler countdownHandler;
    private Runnable countdownRunnable;

    public FileTransferHelper() {
        // Required empty public constructor
    }

    public void setConnectionDetails(String serverIP, int serverPort, String username, String password) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_transfer_layout, container, false);

        setHasOptionsMenu(true);

        initializeViews(view);
        setButtonClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        fabSendFile = view.findViewById(R.id.fabSendFile);
        fabSelectFile = view.findViewById(R.id.fabSelectFile);
        fabReloadServer = view.findViewById(R.id.fabReloadServer);
        fabDisconnect = view.findViewById(R.id.fabDisconnect);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        progressBarReload = view.findViewById(R.id.progressBarReload);
    }

    private void setButtonClickListeners() {
        fabSendFile.setOnClickListener(v -> sendFile());
        fabSelectFile.setOnClickListener(v -> selectFile());
        fabReloadServer.setOnClickListener(v -> reloadServer());
        fabDisconnect.setOnClickListener(v -> disconnect());
    }

    private void saveSentFileDetails(String ipAddress, String fileName, Uri fileUri) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MessageHistory", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = sharedPreferences.getString("messages", "");

        List<HistoryItem> historyItems;

        if (json.isEmpty()) {
            historyItems = new ArrayList<>();
            Log.d("History", "No existing history, creating new list.");
        } else {
            try {
                // Check if JSON is an array
                JsonElement jsonElement = JsonParser.parseString(json);
                if (jsonElement.isJsonArray()) {
                    Type type = new TypeToken<ArrayList<HistoryItem>>() {
                    }.getType();
                    historyItems = gson.fromJson(json, type);
                    Log.d("History", "Loaded existing history: " + historyItems.size() + " items.");
                } else {
                    // Handle the case where JSON is not an array
                    historyItems = new ArrayList<>();
                    Log.d("History", "Existing history is not an array, creating new list.");
                }
            } catch (JsonSyntaxException e) {
                // Handle JSON parsing error
                historyItems = new ArrayList<>();
                Log.e("History", "JSON parsing error: " + e.getMessage());
            }
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        HistoryItem newHistoryItem = new HistoryItem(ipAddress, fileName, fileUri.toString(), timestamp);
        historyItems.add(newHistoryItem);

        String updatedJson = gson.toJson(historyItems);
        editor.putString("messages", updatedJson);
        editor.apply();
    }


    @SuppressLint("SetTextI18n")
    private void sendFile() {
        if (serverIP == null || selectedFileUri == null) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> textViewStatus.setText
                        ("No connection to server or file not selected"));
            }
            return;
        }

        progressBarReload.setVisibility(View.VISIBLE);

        try {
            long fileSize = getFileSize(selectedFileUri);
            if (fileSize > MAX_FILE_SIZE) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> textViewStatus.setText
                            ("File exceeds maximum allowed size"));
                }
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> textViewStatus.setText
                        ("Error reading file: " + e.getMessage()));
            }
            return;
        }

        new SendFileTask().execute(selectedFileUri);
    }

    @SuppressLint("StaticFieldLeak")
    private class SendFileTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            Uri fileUri = uris[0];
            try (Socket socket = new Socket(serverIP, serverPort);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                 InputStream fileInputStream = requireContext().getContentResolver().openInputStream(fileUri)) {

                String fileName = getFileName(fileUri);
                if (fileName == null) {
                    fileName = "Untitled";
                }

                // Send file type and file name to server
                outputStream.writeUTF("FILE");
                outputStream.writeUTF(fileName);

                // Send file size
                outputStream.writeLong(getFileSize(fileUri));

                // Send file content
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = Objects.requireNonNull(fileInputStream).read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Save for history view
                saveSentFileDetails(serverIP, fileName, fileUri);

                return "File sent: " + fileName;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error sending file: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (isAdded()) {
                textViewStatus.setText(result);
            }
        }
    }

    private void handleServerShutdown() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Server Shutdown")
                        .setMessage("The server has shut down.")
                        .setPositiveButton("Ok", (dialog, which) -> {
                            dialog.dismiss();
                            new Handler().postDelayed(() ->
                                    requireActivity().getSupportFragmentManager().popBackStack(), 2000);
                        });

                serverShutdownDialog = builder.create();
                serverShutdownDialog.show();
            });
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            String fileName = getFileName(Objects.requireNonNull(selectedFileUri));
            if (isAdded()) {
                textViewStatus.setText("Selected file: " + fileName);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void reloadServer() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() ->
                    textViewStatus.setText("Waiting for server to reload, please wait a moment..."));
        }

        new Thread(() -> {
            try (Socket socket = new Socket(serverIP, serverPort)) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> textViewStatus.setText("Server is running."));
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        textViewStatus.setText("Server has shut down: " + e.getMessage());
                        handleServerShutdown();
                    });
                }
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void disconnect() {
        if (serverIP == null) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        textViewStatus.setText("No server connection to disconnect from"));
            }
            return;
        }

        new Thread(() -> {
            try (Socket socket = new Socket(serverIP, serverPort);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                outputStream.writeUTF("DISCONNECT");
                outputStream.flush();

                socket.close();

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        textViewStatus.setText("Disconnected from server");
                        new Handler().postDelayed(() -> {
                            // Navigate back to ConnectionListFragment
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }, 2000); // 2 seconds delay
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> textViewStatus.setText("Error disconnecting: " + e.getMessage()));
                    handleServerShutdown();
                }
            }
        }).start();
    }


    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private long getFileSize(Uri uri) throws IOException {
        long fileSize = -1;
        Cursor cursor = requireContext().getContentResolver().query(
                uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex >= 0) {
                fileSize = cursor.getLong(sizeIndex);
            }
            cursor.close();
        }
        return fileSize;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources, listeners, and handlers
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        if (serverShutdownDialog != null && serverShutdownDialog.isShowing()) {
            serverShutdownDialog.dismiss();
        }
    }
}
