package com.example.ftp_client.ui.file;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class FileDiffCallback extends DiffUtil.Callback {

    private final List<FileModel> oldList;
    private final List<FileModel> newList;

    public FileDiffCallback(List<FileModel> oldList, List<FileModel> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getName().equals(newList.get(newItemPosition).getName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}

