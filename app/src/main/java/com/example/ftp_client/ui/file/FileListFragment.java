package com.example.ftp_client.ui.file;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
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

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileListFragment extends Fragment implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerViewFiles;
    private TextView textViewNoFiles;
    private List<FileModel> fileList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);
        recyclerViewFiles = view.findViewById(R.id.recyclerViewFiles);
        textViewNoFiles = view.findViewById(R.id.textViewNoFiles);

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

    private void openFile(FileModel file) {
        if (file.getPath() == null || file.getPath().isEmpty()) {
            Log.e("FileListFragment", "File path is null or empty.");
            return;
        }

        File fileToOpen = new File(file.getPath());

        // Use FileProvider to get a content URI
        Uri fileUri = FileProvider.getUriForFile(requireContext(),
                "com.example.ftp_client.fileprovider", fileToOpen);

        String mimeType = getMimeType(file.getName());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

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
}
