package com.example.ftp_client.ui.file;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;
import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class FileListFragment extends Fragment implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerViewFiles;
    private TextView textViewNoFiles;
    private List<FileModel> fileList;

    private String serverIP;
    private int serverPort;
    private String username;
    private String password;

    private AlertDialog serverShutdownDialog;
    private Handler countdownHandler;
    private Runnable countdownRunnable;

    private ProgressBar progressBarReload;

    private Button fabLoadDirectory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);
        recyclerViewFiles = view.findViewById(R.id.recyclerViewFiles);
        textViewNoFiles = view.findViewById(R.id.textViewNoFiles);

        progressBarReload = view.findViewById(R.id.progressBarReload);
        fabLoadDirectory = view.findViewById(R.id.fabReloadDirectory);

        if (getArguments() != null) {
            String jsonFileList = getArguments().getString("fileList");
            Gson gson = new Gson();
            FileModel[] fileArray = gson.fromJson(jsonFileList, FileModel[].class);
            fileList = Arrays.asList(fileArray);
        }

        if (fileList.isEmpty()) {
            textViewNoFiles.setVisibility(View.VISIBLE);
            recyclerViewFiles.setVisibility(View.GONE);
        } else {
            textViewNoFiles.setVisibility(View.GONE);
            recyclerViewFiles.setVisibility(View.VISIBLE);

            FileListAdapter adapter = new FileListAdapter(requireContext(), fileList, this);
            recyclerViewFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerViewFiles.setAdapter(adapter);
        }

        fabLoadDirectory.setOnClickListener(v -> reloadServer());

        return view;
    }


    @Override
    public void onFileClick(FileModel file) {
        if (file.isDirectory()) {
            loadDirectory(file.getPath());
        } else {
            openFile(file);
        }
    }

    @Override
    public void onFileLongClick(FileModel file) {
        BottomSheetFileActionFragment bottomSheet = BottomSheetFileActionFragment.newInstance(file);
        bottomSheet.show(getFragmentManager(), bottomSheet.getTag());
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private void openFile(FileModel file) {
        if (file.getPath() == null || file.getPath().isEmpty()) {
            Log.e("FileListFragment", "File path is null or empty.");
            return;
        }

        Context context = requireContext();

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e("FileListFragment", "External storage is not available or read-only.");
            Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
            return;
        }

        String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ftp-client";
        File fileToOpen = new File(directory, file.getPath());
        if (!fileToOpen.getParentFile().exists()) {
            fileToOpen.getParentFile().mkdirs();
        }
        String mimeType = getMimeType(file.getName());

        try {
            fileToOpen.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FileListFragment", "Failed to create file.");
            Toast.makeText(requireContext(), "Failed to open file.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getApplicationContext().getPackageName() + ".provider",
                fileToOpen);

        // Intent to open the file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Create chooser to handle the intent
        Intent chooserIntent = Intent.createChooser(intent, "Open file with...");

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            Log.e("FileListFragment", "No activity found to handle file.");
            Toast.makeText(requireContext(), "No app installed to open this file.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        String mimeType = "application/octet-stream"; // Default mime type

        if (fileName != null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }

        return mimeType;
    }

    private void loadDirectory(String directoryPath) {
        Bundle bundle = new Bundle();
        bundle.putString("directoryPath", directoryPath);

        FileListFragment fragment = new FileListFragment();
        fragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void reloadServer() {
        runOnUiThread(() -> {
            progressBarReload.setVisibility(View.VISIBLE);
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
                    } else {
                        handleServerShutdown();
                    }
                });
            }
        }
    }

    private void runOnUiThread(Runnable action) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

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
}
