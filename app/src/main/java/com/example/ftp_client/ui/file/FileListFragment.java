package com.example.ftp_client.ui.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {

    private RecyclerView recyclerViewFiles;
    private FileListAdapter fileListAdapter;
    private List<FileModel> fileList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);

        recyclerViewFiles = view.findViewById(R.id.recyclerViewFiles);
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(getContext()));

        fileList = new ArrayList<>();

        // Retrieve the file list from the bundle arguments
        if (getArguments() != null) {
            String jsonFileList = getArguments().getString("fileList");
            fileList = new Gson().fromJson(jsonFileList, new TypeToken<List<FileModel>>() {}.getType());
        }

        fileListAdapter = new FileListAdapter(requireContext(), fileList);
        recyclerViewFiles.setAdapter(fileListAdapter);

        return view;
    }
}