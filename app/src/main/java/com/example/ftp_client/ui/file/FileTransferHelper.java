package com.example.ftp_client.ui.file;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.activity.HistoryItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
    private FloatingActionButton fabOpenDrawer;
    private LinearLayout fabNavigationDrawer;

    private TextView textViewStatus;
    private ProgressBar progressBarReload;

    private ImageView userDirectory;
    private TextView nameDirectory;

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
        nameDirectory.setText(username);
        return view;
    }

    private void initializeViews(View view) {
        fabSendFile = view.findViewById(R.id.fabSendFile);
        fabSelectFile = view.findViewById(R.id.fabSelectFile);
        fabReloadServer = view.findViewById(R.id.fabReloadServer);
        fabDisconnect = view.findViewById(R.id.fabDisconnect);
        fabOpenDrawer = view.findViewById(R.id.fabOpenDrawer);
        fabNavigationDrawer = view.findViewById(R.id.fabNavigationDrawer);

        userDirectory = view.findViewById(R.id.user_directory);
        nameDirectory = view.findViewById(R.id.name_directory);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        progressBarReload = view.findViewById(R.id.progressBarReload);
    }

    private void setButtonClickListeners() {
        fabSendFile.setOnClickListener(v -> sendFile());
        fabSelectFile.setOnClickListener(v -> selectFile());
        fabReloadServer.setOnClickListener(v -> reloadServer());
        fabDisconnect.setOnClickListener(v -> disconnect());
        fabOpenDrawer.setOnClickListener(v -> toggleNavigationDrawer());
        nameDirectory.setOnClickListener(v -> loadDirectory(username));
    }

    private void toggleNavigationDrawer() {
        fabNavigationDrawer.setVisibility(fabNavigationDrawer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void saveSentFileDetails(String ipAddress, String fileName, Uri fileUri) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MessageHistory", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = sharedPreferences.getString("messages", "");
        List<HistoryItem> historyItems;

        if (json.isEmpty()) {
            historyItems = new ArrayList<>();
        } else {
            try {
                JsonElement jsonElement = JsonParser.parseString(json); // Check if JSON is an array
                if (jsonElement.isJsonArray()) {
                    Type type = new TypeToken<ArrayList<HistoryItem>>() {
                    }.getType();
                    historyItems = gson.fromJson(json, type);
                } else {
                    historyItems = new ArrayList<>(); // Handle the case where JSON is not an array
                }
            } catch (JsonSyntaxException e) {
                historyItems = new ArrayList<>(); // Handle JSON parsing error
            }
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        HistoryItem newHistoryItem = new HistoryItem(ipAddress, fileName, fileUri.toString(), timestamp);
        historyItems.add(newHistoryItem);
        editor.putString("messages", gson.toJson(historyItems));
        editor.apply();
    }

    //===========================//
    /* Send file to Server */
    private void sendFile() {
        if (serverIP == null || selectedFileUri == null) {
            runOnUiThread(() -> textViewStatus.setText("No connection to server or file not selected"));
            return;
        }
        runOnUiThread(() -> progressBarReload.setVisibility(View.VISIBLE));
        try {
            long fileSize = getFileSize(selectedFileUri);
            if (fileSize > MAX_FILE_SIZE) {
                runOnUiThread(() -> textViewStatus.setText("File exceeds maximum allowed size"));
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> textViewStatus.setText("Error reading file: " + e.getMessage()));
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
                outputStream.writeUTF("SEND_FILE");
                outputStream.writeUTF(fileName);
                outputStream.writeLong(getFileSize(fileUri)); // Send file size
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
                runOnUiThread(() -> {
                    textViewStatus.setText(result);
                    progressBarReload.setVisibility(View.GONE);
                });
            }
        }
    }

    //===========================//
    /* Handle Server shut down */
    private void handleServerShutdown() {
        if (isAdded()) {
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Server Shutdown")
                        .setMessage("Server has shutdown. Go back in 5 seconds.")
                        .setCancelable(false)
                        .setPositiveButton("Ok", (dialog, which) -> {
                            dialog.dismiss();
                            new Handler().postDelayed(() ->
                                    requireActivity().getSupportFragmentManager().popBackStack(), 2000);
                        });

                serverShutdownDialog = builder.create();
                serverShutdownDialog.show();
                countdownHandler = new Handler();
                countdownRunnable = new Runnable() {
                    int secondsLeft = 5;

                    @Override
                    public void run() {
                        if (secondsLeft > 0) {
                            secondsLeft--;
                            new Handler().postDelayed(this, 1000);
                        } else {
                            serverShutdownDialog.dismiss();
                            countdownHandler.removeCallbacks(this);
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                };
                countdownHandler.post(countdownRunnable);
            });
        }
    }

    //===========================//
    /* Handle with Directory */
    private void loadDirectory(String username) {
        if (isNetworkConnected()) {
            new LoadDirectoryTask().execute(username);
        } else {
            Toast.makeText(requireContext(), "No network connection", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private class LoadDirectoryTask extends AsyncTask<String, Void, List<FileModel>> {
        private String errorMessage = null;

        @Override
        protected List<FileModel> doInBackground(String... usernames) {
            List<FileModel> fileItems = new ArrayList<>();
            try (Socket socket = new Socket(serverIP, serverPort);
                 DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                outputStream.writeUTF("LOAD_DIRECTORY");
                outputStream.writeUTF(usernames[0]);
                outputStream.flush();

                int responseCode = inputStream.readInt();
                if (responseCode == 0) { // Assuming 0 indicates success
                    String readFiles = inputStream.readUTF();
                    fileItems = new Gson().fromJson(readFiles, fileItems.getClass());
                } else if (responseCode == 1) {
                    errorMessage = "Your account or directory has been deleted or encountered an error. Please contact support.";
                } else {
                    errorMessage = "An unknown error occurred. Please try again later.";
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMessage = "Failed to connect to the server. Please check your network connection and try again.";
            }
            return fileItems;
        }

        @Override
        protected void onPostExecute(List<FileModel> fileItems) {
            super.onPostExecute(fileItems);
            progressBarReload.setVisibility(View.GONE);

            if (errorMessage != null) {
                showAlert(errorMessage);
            } else {
                showFileListFragment(fileItems);
            }
        }

        private void showAlert(String message) {
            if (isAdded()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }

        private void showFileListFragment(List<FileModel> fileList) {
            FileListFragment fragment = new FileListFragment();
            Gson gson = new Gson();
            String jsonFileList = gson.toJson(fileList);
            Bundle args = new Bundle();
            args.putString("fileList", jsonFileList);
            fragment.setArguments(args);

            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    //===========================//
    /* Helper functions */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            textViewStatus.setText(String.format("File selected: %s", getFileName(selectedFileUri)));
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        Cursor cursor = requireContext().getContentResolver().query(
                uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            fileName = cursor.getString(displayNameIndex);
            cursor.close();
        }
        return fileName;
    }

    private long getFileSize(Uri fileUri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
        long fileSize = inputStream != null ? inputStream.available() : 0;
        if (inputStream != null) {
            inputStream.close();
        }
        return fileSize;
    }

    private void reloadServer() {
        runOnUiThread(() -> {
            progressBarReload.setVisibility(View.VISIBLE);
            textViewStatus.setText("Waiting for server to reload, please wait a moment...");
        });

        new ReloadServerTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class ReloadServerTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            Socket socket = null;
            try {
                socket = new Socket(serverIP, serverPort);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("RELOAD_SERVER");
                outputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (isAdded()) {
                runOnUiThread(() -> {
                    progressBarReload.setVisibility(View.GONE);
                    if (success) {
                        textViewStatus.setText("Server is running.");
                    } else {
                        textViewStatus.setText("Server has shut down.");
                        handleServerShutdown();
                    }
                });
            }
        }
    }

    private void disconnect() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void runOnUiThread(Runnable action) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        if (serverShutdownDialog != null && serverShutdownDialog.isShowing()) {
            serverShutdownDialog.dismiss();
        }
    }
}