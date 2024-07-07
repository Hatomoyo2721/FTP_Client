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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private FileListAdapter fileListAdapter;

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
                // Check if JSON is an array
                JsonElement jsonElement = JsonParser.parseString(json);
                if (jsonElement.isJsonArray()) {
                    Type type = new TypeToken<ArrayList<HistoryItem>>() {
                    }.getType();
                    historyItems = gson.fromJson(json, type);
                } else {
                    // Handle the case where JSON is not an array
                    historyItems = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                // Handle JSON parsing error
                historyItems = new ArrayList<>();
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
    @SuppressLint("StaticFieldLeak")
    private class LoadDirectoryTask extends AsyncTask<String, Void, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarReload.setVisibility(View.VISIBLE);
            textViewStatus.setText("Loading directory...");
        }

        @Override
        protected List<String> doInBackground(String... params) {
            String username = params[0];
            List<String> files = new ArrayList<>();

            try (Socket socket = new Socket(serverIP, serverPort);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                outputStream.writeUTF("LOAD_DIRECTORY");
                outputStream.writeUTF(username);
                outputStream.flush();

                String response = reader.readLine();
                files = new Gson().fromJson(response, new TypeToken<List<String>>() {}.getType());

            } catch (IOException e) {
                e.printStackTrace();
                publishProgress(); // Notify UI thread about error
            }
            return files;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            progressBarReload.setVisibility(View.GONE);
            textViewStatus.setText("Error loading directory");
        }

        @Override
        protected void onPostExecute(List<String> files) {
            super.onPostExecute(files);
            progressBarReload.setVisibility(View.GONE);
            textViewStatus.setText("Directory loaded: " + username);

            // Update fileListAdapter with new files
            fileListAdapter.updateFileList(convertToFileModels(files)); // Assuming you have a method to convert strings to FileModel

            // Transition to FileListFragment and pass the file list
            Bundle args = new Bundle();
            args.putString("fileList", new Gson().toJson(files));

            FileListFragment fileListFragment = new FileListFragment();
            fileListFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fileListFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadDirectory(String username) {
        new LoadDirectoryTask().execute(username);
    }

    private List<FileModel> convertToFileModels(List<String> files) {
        List<FileModel> fileModels = new ArrayList<>();
        for (String file : files) {
            fileModels.add(new FileModel(file, FileModel.TYPE_FILE));
        }
        return fileModels;
    }

    //===========================//
    /* Helper functions */

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            String fileName = getFileName(Objects.requireNonNull(selectedFileUri));
            if (selectedFileUri != null) {
                textViewStatus.setText("Selected file: " + fileName);
            }
        }
    }

    private String getFileName(Uri uri) {
        Cursor cursor = requireContext().getContentResolver().query(
                uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                String fileName = cursor.getString(nameIndex);
                cursor.close();
                return fileName;
            }
            cursor.close();
        }
        return null;
    }

    private long getFileSize(Uri uri) throws IOException {
        try (Cursor cursor = requireContext().getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    return size;
                }
                cursor.close();
            }
        }
        return 0;
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

    private void reloadServer() {
        runOnUiThread(() -> {
            progressBarReload.setVisibility(View.VISIBLE);
            textViewStatus.setText("Waiting for server to reload, please wait a moment...");
            disableButtons();
        });

        new ReloadServerTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class ReloadServerTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try (Socket socket = new Socket(serverIP, serverPort);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream()))
            {
                outputStream.writeUTF("RELOAD_SERVER");
                outputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
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
                    enableButtons();
                });
            }
        }
    }

    private void disconnect() {
        if (serverIP == null) {
            runOnUiThread(() ->
                    textViewStatus.setText("No server connection to disconnect from"));
            return;
        }

        new DisconnectTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class DisconnectTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try (Socket socket = new Socket(serverIP, serverPort);
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
                outputStream.writeUTF("DISCONNECT");
                outputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (isAdded()) {
                runOnUiThread(() -> {
                    if (success) {
                        textViewStatus.setText("Disconnected from server in 5 seconds");
                        new Handler().postDelayed(() -> requireActivity().getSupportFragmentManager().popBackStack(), 5000);
                        disableButtons();
                    } else {
                        textViewStatus.setText("Error disconnecting from server. Go back in 5 seconds");
                        new Handler().postDelayed(() -> requireActivity().getSupportFragmentManager().popBackStack(), 5000);
                        disableButtons();
                    }
                });
            }
        }
    }
    private void disableButtons() {
        runOnUiThread(() -> {
            fabDisconnect.setClickable(false);
            fabOpenDrawer.setClickable(false);
            fabReloadServer.setClickable(false);
            fabSelectFile.setClickable(false);
            fabSendFile.setClickable(false);
            nameDirectory.setClickable(false);
        });
    }
    private void enableButtons() {
        runOnUiThread(() -> {
            fabDisconnect.setClickable(true);
            fabOpenDrawer.setClickable(true);
            fabReloadServer.setClickable(true);
            fabSelectFile.setClickable(true);
            fabSendFile.setClickable(true);
            nameDirectory.setClickable(true);
        });
    }
    private void runOnUiThread(Runnable action) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }
}
