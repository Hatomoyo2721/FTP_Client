package com.example.ftp_client.ui.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ftp_client.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;

public class BottomSheetFileActionFragment extends BottomSheetDialogFragment {

    private static final String ARG_FILE = "file";

    private FileModel file;

    public static BottomSheetFileActionFragment newInstance(FileModel file) {
        BottomSheetFileActionFragment fragment = new BottomSheetFileActionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILE, (Serializable) file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            file = (FileModel) getArguments().getSerializable(ARG_FILE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_file_action_fragment, container, false);

        TextView textViewRename = view.findViewById(R.id.textViewRename);
        TextView textViewDelete = view.findViewById(R.id.textViewDelete);

        textViewRename.setOnClickListener(v -> {
            // Handle rename action
            dismiss();
        });

        textViewDelete.setOnClickListener(v -> {
            // Handle delete action
            dismiss();
        });

        return view;
    }
}
