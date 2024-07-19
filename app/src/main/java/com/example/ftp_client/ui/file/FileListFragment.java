package com.example.ftp_client.ui.file;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;
import com.example.ftp_client.ui.connection.ConnectionModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FileListFragment extends Fragment implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerViewFiles;
    private TextView textViewNoFiles;
    private List<FileModel> fileList;

    private FloatingActionButton fabUploadFile;
    private FloatingActionButton fabCreateFolder;
    private FloatingActionButton fabOpenDrawer;
    private LinearLayout fabNavigationDrawerFileAction;

    private ProgressBar progressBarReload;

    private FloatingActionButton fabBackToPreviousLayout;

    private String serverIP;
    private int serverPort;
    private String username;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_list, container, false);
        recyclerViewFiles = v.findViewById(R.id.recyclerViewFiles);
        textViewNoFiles = v.findViewById(R.id.textViewNoFiles);

        progressBarReload = v.findViewById(R.id.progressBarReload);
        fabBackToPreviousLayout = v.findViewById(R.id.fabBackToFileTransferLayout);

        fabUploadFile = v.findViewById(R.id.fabUploadFile);
        fabCreateFolder = v.findViewById(R.id.fabCreateDirectory);
        fabOpenDrawer = v.findViewById(R.id.fabOpenMenuFileAction);
        fabNavigationDrawerFileAction = v.findViewById(R.id.fabNavigationDrawerFileAction);

        fileList = new ArrayList<>();

        Bundle args = getArguments();
        if (args != null && args.containsKey("fileList")) {
            String jsonFileList = args.getString("fileList");
            serverIP = args.getString("serverIP");
            serverPort = args.getInt("serverPort");
            username = args.getString("username");
            if (jsonFileList != null && !jsonFileList.isEmpty()) {
                Gson gson = new Gson();
                FileModel[] fileArray = gson.fromJson(jsonFileList, FileModel[].class);
                if (fileArray != null) {
                    fileList.addAll(Arrays.asList(fileArray));
                }
            }
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

        fabBackToPreviousLayout.setOnClickListener(view -> getParentFragmentManager().popBackStack());

        fabOpenDrawer.setOnClickListener(view -> toggleNavigationDrawerFileAction());

        return v;
    }

    private void toggleNavigationDrawerFileAction() {
        if (fabNavigationDrawerFileAction.getVisibility() == View.GONE) {
            fabNavigationDrawerFileAction.setVisibility(View.VISIBLE);
        } else {
            fabNavigationDrawerFileAction.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFileClick(FileModel file) {
        if (file.isDirectory()) {
            loadDirectory(file.getPath());
        }
        openFileFromServer(file);
    }

    @Override
    public void onFileLongClick(FileModel file) {
        BottomSheetFileActionFragment bottomSheet = BottomSheetFileActionFragment.newInstance(file);
        bottomSheet.show(requireFragmentManager(), bottomSheet.getTag());
    }

    private void loadDirectory(String directoryPath) {
        Bundle bundle = new Bundle();
        bundle.putString("serverIP", serverIP);
        bundle.putInt("serverPort", serverPort);
        bundle.putString("username", username);
        bundle.putString("directoryPath", directoryPath);

        FileListFragment fragment = new FileListFragment();
        fragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openFileFromServer(FileModel file) {
        if (file.isDirectory()) {
            loadDirectory(file.getPath());
        } else {
            new DownloadFileTask().execute(file);
        }
    }

    private class DownloadFileTask extends AsyncTask<FileModel, Void, File> {
        @Override
        protected File doInBackground(FileModel... fileModels) {
            FileModel fileModel = fileModels[0];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            FileOutputStream fileOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Send request to server to download file
                dataOutputStream.writeUTF("DOWNLOAD_FILE");
                dataOutputStream.writeUTF(fileModel.getName());
                dataOutputStream.writeUTF(new Gson().toJson(new ConnectionModel(serverIP, serverPort, username)));
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                if (response.equals("FILE_FOUND")) {
                    long fileSize = dataInputStream.readLong();
                    byte[] fileData = new byte[(int) fileSize];
                    int bytesRead;
                    int totalBytesRead = 0;

                    while (totalBytesRead < fileSize && (bytesRead = dataInputStream.read(fileData, totalBytesRead,
                            (int) (fileSize - totalBytesRead))) != -1) {
                        totalBytesRead += bytesRead;
                    }

                    if (totalBytesRead != fileSize) {
                        Log.e("DownloadFileTask", "Mismatch in file size. Expected: " + fileSize + ", but read: " + totalBytesRead);
                        return null;
                    }

                    File outputFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileModel.getName());

                    fileOutputStream = new FileOutputStream(outputFile);
                    fileOutputStream.write(fileData, 0, (int) fileSize);

                    return outputFile;
                } else {
                    Log.e("DownloadFileTask", "Server response: " + response);
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("DownloadFileTask", "IOException: " + e.getMessage());
                return null;
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file != null) {
                openFile(file);
            } else {
                Toast.makeText(requireContext(), "Failed to download file", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openFile(File fileToOpen) {
        if (fileToOpen == null || !fileToOpen.exists()) {
            Log.e("FileListFragment", "File is null or doesn't exist.");
            Toast.makeText(requireContext(), "File doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }

        String mimeType = getMimeType(fileToOpen.getName());

        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getApplicationContext().getPackageName() + ".provider",
                    fileToOpen);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(intent, "Open file with...");
            startActivity(chooserIntent);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FileListFragment", "Failed to open file: " + e.getMessage());
            Toast.makeText(requireContext(), "Failed to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        String mimeType = "application/octet-stream"; // Default mime type

        if (fileName != null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null) {
                MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
                mimeType = mimeMap.getMimeTypeFromExtension(extension.toLowerCase());

                if (mimeType == null) {
                    mimeType = "application/octet-stream"; // Fallback to default
                }
            }
        }

        return mimeType;
    }
}