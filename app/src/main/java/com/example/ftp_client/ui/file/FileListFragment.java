package com.example.ftp_client.ui.file;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.example.ftp_client.ui.connection.ConnectionModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FileListFragment extends Fragment implements FileListAdapter.OnFileClickListener, FileListAdapter.OnFolderLongClickListener {
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private Uri selectedFileUri;

    private RecyclerView recyclerViewFiles;
    private TextView textViewNoFiles;
    private List<FileModel> fileList;

    private FloatingActionButton fabUploadFile;
    private FloatingActionButton fabCreateFolder;
    private FloatingActionButton fabOpenDrawer;
    private LinearLayout fabNavigationDrawerFileAction;
    private FloatingActionButton fabBackToPreviousLayout;
    private ProgressBar progressBar;
    private View blockingView;

    private String serverIP;
    private int serverPort;
    private String username;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_list, container, false);

        initializeViews(v);

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

            FileListAdapter adapter = new FileListAdapter(requireContext(), fileList, this, this);
            recyclerViewFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerViewFiles.setAdapter(adapter);
        }

        setListeners();

        return v;
    }

    private void initializeViews(View v) {
        recyclerViewFiles = v.findViewById(R.id.recyclerViewFiles);
        textViewNoFiles = v.findViewById(R.id.textViewNoFiles);
        fabBackToPreviousLayout = v.findViewById(R.id.fabBackToFileTransferLayout);
        fabUploadFile = v.findViewById(R.id.fabUploadFile);
        fabCreateFolder = v.findViewById(R.id.fabCreateDirectory);
        fabOpenDrawer = v.findViewById(R.id.fabOpenMenuFileAction);
        fabNavigationDrawerFileAction = v.findViewById(R.id.fabNavigationDrawerFileAction);
        progressBar = v.findViewById(R.id.progressBar);
        blockingView = v.findViewById(R.id.blockingView);
        fileList = new ArrayList<>();
//        progressDialog = new ProgressDialog(requireContext());
//        progressDialog.setTitle("Downloading File");
//        progressDialog.setMessage("Please wait...");
//        progressDialog.setCancelable(false);
    }

    private void setListeners() {
        fabUploadFile.setOnClickListener(v -> selectFile());
        fabCreateFolder.setOnClickListener(v -> showCreateFolderDialog());
        fabBackToPreviousLayout.setOnClickListener(view -> getParentFragmentManager().popBackStack());
        fabOpenDrawer.setOnClickListener(view -> toggleNavigationDrawerFileAction());
    }

    private void toggleNavigationDrawerFileAction() {
        if (fabNavigationDrawerFileAction.getVisibility() == View.GONE) {
            fabNavigationDrawerFileAction.setVisibility(View.VISIBLE);
        } else {
            fabNavigationDrawerFileAction.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFolderLongClick(FileModel file) {
        if (file.isDirectory()) {
            BottomSheetDirectoryActionFragment bottomSheet = BottomSheetDirectoryActionFragment.newInstance(file);
            bottomSheet.setBottomSheetListener(new BottomSheetDirectoryActionFragment.BottomSheetListener() {
                @Override
                public void onRenameClick(FileModel file) {
                    showRenameFolderBox(file);
                }

                @Override
                public void onDeleteClick(FileModel file) {
                    deleteFolder(file);
                }
            });
            bottomSheet.show(requireFragmentManager(), bottomSheet.getTag());
        }
    }

    @Override
    public void onFileClick(FileModel file) {
        if (!file.isDirectory()) {
            BottomSheetFileActionFragment bottomSheet = BottomSheetFileActionFragment.newInstance(file);
            bottomSheet.setBottomSheetListener(new BottomSheetFileActionFragment.BottomSheetListener() {
                @Override
                public void onRenameClick(FileModel file) {
                    showRenameDialog(file);
                }

                @Override
                public void onDeleteClick(FileModel file) {
                    deleteFile(file);
                }

                @Override
                public void onDownloadClick(FileModel file) {
                    openFileFromServer(file);
                }

                @Override
                public void onShareClick(FileModel file) {

                }
            });
            bottomSheet.show(requireFragmentManager(), bottomSheet.getTag());
        } else {
            loadDirectory(file.getPath());
        }
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

    private void deleteFile(FileModel file) {
        new DeleteFileTask().execute(file);
    }

    private class DeleteFileTask extends AsyncTask<FileModel, Void, Boolean> {
        @Override
        protected Boolean doInBackground(FileModel... fileModels) {
            FileModel fileModel = fileModels[0];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("DELETE_FILE_DIR_USER");
                dataOutputStream.writeUTF(fileModel.getPath());
                dataOutputStream.flush();

                // Nhận phản hồi từ server
                String response = dataInputStream.readUTF();
                return response.equals("DELETE_SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) socket.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(requireContext(), "Deleted file successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void renameFile(FileModel file, String newName) {
        new RenameFileTask().execute(file, newName);
    }

    private void showRenameDialog(FileModel file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename File");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rename_file, (ViewGroup) getView(), false);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText(file.getName());
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newName = input.getText().toString();
            renameFile(file, newName);
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private class RenameFileTask extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            FileModel file = (FileModel) params[0];
            String newName = (String) params[1];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("RENAME_FILE");
                dataOutputStream.writeUTF(file.getPath());
                dataOutputStream.writeUTF(newName);
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                return response.equals("RENAME_SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) socket.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(requireContext(), "Renamed file successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to rename file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Folder");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_folder, (ViewGroup) getView(), false);
        final EditText input = viewInflated.findViewById(R.id.input);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            String folderName = input.getText().toString().trim();
            if (isFolderNameValid(folderName)) {
                createFolder(folderName);
            } else {
                Toast.makeText(requireContext(), "Folder name is invalid. Please use only alphanumeric characters and spaces.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean isFolderNameValid(String folderName) {
        String regex = "^[^/\\\\:*?\"<>|]+$";
        return folderName.matches(regex);
    }


    private void createFolder(String folderName) {
        new CreateFolderTask().execute(username, folderName);
    }

    private class CreateFolderTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String parentDirPath = params[0];
            String folderName = params[1];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("CREATE_NEW_DIR");
                dataOutputStream.writeUTF(parentDirPath);
                dataOutputStream.writeUTF(folderName);
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                return response.equals("CREATE_SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) socket.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(requireContext(), "Folder created successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFileFromServer(FileModel file) {
        if (file.isDirectory()) {
            loadDirectory(file.getPath());
        } else {
            new DownloadFileTask().execute(file);
        }
    }

    private class DownloadFileTask extends AsyncTask<FileModel, Integer, File> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            blockingView.setVisibility(View.VISIBLE);
            recyclerViewFiles.setClickable(false);
            fabOpenDrawer.setClickable(false);
            fabNavigationDrawerFileAction.setClickable(false);
        }

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

                    File outputFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileModel.getName());

                    if (totalBytesRead != fileSize) {
                        Log.e("DownloadFileTask", "Mismatch in file size. Expected: " + fileSize + ", but read: "
                                + totalBytesRead);
                        return null;
                    }

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
                    if (socket != null) { socket.close(); }
                    if (dataInputStream != null) { dataInputStream.close(); }
                    if (dataOutputStream != null) { dataOutputStream.close(); }
                    if (fileOutputStream != null) { fileOutputStream.close(); }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            progressBar.setVisibility(View.GONE);
            blockingView.setVisibility(View.GONE);
            recyclerViewFiles.setClickable(true);
            fabOpenDrawer.setClickable(true);
            fabNavigationDrawerFileAction.setClickable(true);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            uploadFile();
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        Cursor cursor = requireContext().getContentResolver().query(
                uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (columnIndex >= 0) {
                fileName = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return fileName;
    }

    private long getFileSize(Uri fileUri) {
        InputStream inputStream = null;
        try {
            inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            long fileSize = inputStream != null ? inputStream.available() : 0;
            if (inputStream != null) {
                inputStream.close();
            }
            return fileSize;
        } catch (IOException e) {
            return -1;
        }
    }

    private void uploadFile() {
        new UploadFileTask().execute(getFileSize(selectedFileUri));
    }

    private class UploadFileTask extends AsyncTask<Long, Void, Boolean> {
        @SuppressLint("Recycle")
        @Override
        protected Boolean doInBackground(Long... params) {
            long fileSize = params[0];

            InputStream inputStream;
            try {
                inputStream = requireContext().getContentResolver().openInputStream(selectedFileUri);
            } catch (FileNotFoundException e) {
                return false;
            }
            if (inputStream == null) {
                return false;
            }
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Send request to server to upload file
                dataOutputStream.writeUTF("UPLOAD_FILE");
                dataOutputStream.writeUTF(getFileNameFromUri(selectedFileUri));
                dataOutputStream.writeUTF(username);
                dataOutputStream.writeLong(fileSize);
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                if (response.equals("READY_TO_RECEIVE")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        Log.d("", "doInBackground: " + bytesRead + " bytes");
                        dataOutputStream.write(buffer, 0, bytesRead);
                        dataOutputStream.flush();
                    }

                    response = dataInputStream.readUTF();
                    return response.equals("UPLOAD_SUCCESS");
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    inputStream.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(requireContext(), "File uploaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to upload file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showRenameFolderBox(FileModel folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Folder");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rename_file, (ViewGroup) getView(), false);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText(folder.getName());
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newName = input.getText().toString();
            renameFolder(folder, newName);
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void renameFolder(FileModel folder, String newName) {
        new RenameFolderTask().execute(folder, newName);
    }

    private class RenameFolderTask extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            FileModel folder = (FileModel) params[0];
            String newName = (String) params[1];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("RENAME_DIR");
                dataOutputStream.writeUTF(folder.getPath());
                dataOutputStream.writeUTF(newName);
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                return response.equals("RENAME_SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) socket.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(getActivity(), "Renamed folder successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to rename folder", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void deleteFolder(FileModel folder) {
        new DeleteFolderTask().execute(folder);
    }

    private class DeleteFolderTask extends AsyncTask<FileModel, Void, Boolean> {
        @Override
        protected Boolean doInBackground(FileModel... params) {
            FileModel folder = params[0];
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                socket = new Socket(serverIP, serverPort);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("DELETE_DIR");
                dataOutputStream.writeUTF(folder.getPath());
                dataOutputStream.flush();

                String response = dataInputStream.readUTF();
                return response.equals("DELETE_SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (socket != null) socket.close();
                    if (dataInputStream != null) dataInputStream.close();
                    if (dataOutputStream != null) dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(getActivity(), "Deleted folder successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to delete folder", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

//        private long previousTime;
//        private long previousBytesRead;

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressDialog.show();
//            previousTime = System.currentTimeMillis();
//            previousBytesRead = 0;
//        }

//                    File outputFile = new File(Environment.getExternalStoragePublicDirectory(
//                            Environment.DIRECTORY_DOWNLOADS), fileModel.getName());
//                    fileOutputStream = new FileOutputStream(outputFile);
//
//                    long previousBytesRead = 0;
//
//                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
//                        fileOutputStream.write(buffer, 0, bytesRead);
//                        totalBytesRead += bytesRead;

//                        long currentTime = System.currentTimeMillis();
//                        long elapsedTime = currentTime - previousTime;
//
//                        if (elapsedTime > 1000) { // Update progress every second
//                            int progress = (int) (totalBytesRead * 100 / fileSize);
//
//                            long bytesReadInLastSecond = totalBytesRead - previousBytesRead;
//                            double speedBps = bytesReadInLastSecond * 1000.0 / elapsedTime; // Bytes per second
//                            double speedKBps = speedBps / 1024.0; // Convert to KB/s
//                            double speedMBps = speedKBps / 1024.0; // Convert to MB/s
//
//                            publishProgress(progress, (totalBytesRead / (1024 * 1024)),
//                                    (int) (fileSize / (1024 * 1024)),
//                                    (int) speedMBps);
//
//                            previousBytesRead = totalBytesRead;
//                            previousTime = currentTime;
//                        }
//                    }


//        @SuppressLint("DefaultLocale")
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            super.onProgressUpdate(values);
//            int progress = values[0];
//            int downloadedMB = values[1];
//            int totalMB = values[2];
//            double speedMBps = values[3];
//            String speedText = String.format("%.1f MB/s", speedMBps); // Display MB/s with one decimal place
//
//            progressDialog.setProgress(progress);
//            progressDialog.setMessage(String.format("Downloading: %dMB / %dMB\nSpeed: %s", downloadedMB, totalMB, speedText));
//            if (progressDialog.isShowing()) {
//                progressDialog.dismiss();
//            }
//        }
