package com.example.ftp_client.ui.file;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ftp_client.R;

import java.io.File;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<FileModel> fileList;
    private Context context;
    private OnFileClickListener fileClickListener;

    public interface OnFileClickListener {
        void onFileClick(FileModel file);
        void onFileLongClick(FileModel file);
    }

    public FileListAdapter(Context context, List<FileModel> fileList, OnFileClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.fileClickListener = listener;
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

        if (file.isDirectory()) {
            holder.imageViewFileType.setImageResource(R.drawable.baseline_folder);
        } else if (file.isImage()) {
            holder.imageViewFileType.setImageResource(R.drawable.baseline_image);
        } else {
            holder.imageViewFileType.setImageResource(R.drawable.baseline_file);
        }

        holder.textViewFileName.setText(file.getName());

        holder.itemView.setOnClickListener(v -> fileClickListener.onFileClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            fileClickListener.onFileLongClick(file);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
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
}
