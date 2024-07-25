package com.example.ftp_client.ui.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.ftp_client.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetDirectoryActionFragment extends BottomSheetDialogFragment {

    private FileModel file;
    private BottomSheetListener listener;

    public interface BottomSheetListener {
        void onRenameClick(FileModel file);
        void onDeleteClick(FileModel file);
    }

    public static BottomSheetDirectoryActionFragment newInstance(FileModel file) {
        BottomSheetDirectoryActionFragment fragment = new BottomSheetDirectoryActionFragment();
        Bundle args = new Bundle();
        args.putParcelable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_directory_action, container, false);

        if (getArguments() != null) {
            file = getArguments().getParcelable("file");
        }

        Button buttonRename = view.findViewById(R.id.buttonRename);
        Button buttonDelete = view.findViewById(R.id.buttonDelete);

        buttonRename.setOnClickListener(v -> {
            listener.onRenameClick(file);
            dismiss();
        });

        buttonDelete.setOnClickListener(v -> {
            listener.onDeleteClick(file);
            dismiss();
        });

        return view;
    }

    public void setBottomSheetListener(BottomSheetListener listener) {
        this.listener = listener;
    }
}
