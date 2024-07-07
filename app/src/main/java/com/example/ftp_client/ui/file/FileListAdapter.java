package com.example.ftp_client.ui.file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;

import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<FileModel> fileList;
    private Context context;

    public FileListAdapter(List<FileModel> fileList, Context context) {
        this.fileList = fileList != null ? fileList : new ArrayList<>();
        this.context = context;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileModel file = fileList.get(position);

        holder.imageViewFileType.setImageResource(file.isFile() ? R.drawable.baseline_file : R.drawable.baseline_folder);
        holder.textViewFileName.setText(file.getName());
    }

    @Override
    public int getItemCount() {
        return fileList != null ? fileList.size() : 0;
    }

    public class FileViewHolder extends RecyclerView.ViewHolder {

        ImageView imageViewFileType;
        TextView textViewFileName;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewFileType = itemView.findViewById(R.id.imageViewFileType);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
        }
    }

    public void updateFileList(List<FileModel> newFileList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FileDiffCallback(this.fileList, newFileList));
        this.fileList.clear();
        this.fileList.addAll(newFileList);
        diffResult.dispatchUpdatesTo(this);
    }

}