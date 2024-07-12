package com.example.ftp_client.ui.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ftp_client.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetFileActionFragment extends BottomSheetDialogFragment {

    private static final String ARG_FILE = "file";
    private FileModel file;
    private BottomSheetListener mListener;

    public interface BottomSheetListener {
        void onRenameClick(FileModel file);
        void onDeleteClick(FileModel file);
        void onDownloadClick(FileModel file);
    }

    public static BottomSheetFileActionFragment newInstance(FileModel file) {
        BottomSheetFileActionFragment fragment = new BottomSheetFileActionFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            file = getArguments().getParcelable(ARG_FILE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_file_action_fragment, container, false);

        Button btnRename = view.findViewById(R.id.buttonRename);
        Button btnDelete = view.findViewById(R.id.buttonDelete);
        Button btnDownload = view.findViewById(R.id.buttonDownload);

        btnRename.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRenameClick(file);
            }
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDeleteClick(file);
            }
            dismiss();
        });

        btnDownload.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDownloadClick(file);
            }
            dismiss();
        });

        return view;
    }

    public void setBottomSheetListener(BottomSheetListener listener) {
        mListener = listener;
    }
}
