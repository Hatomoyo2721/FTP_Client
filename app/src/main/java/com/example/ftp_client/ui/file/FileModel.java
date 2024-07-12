package com.example.ftp_client.ui.file;

import android.os.Parcel;
import android.os.Parcelable;

public class FileModel implements Parcelable {
    public static final String TYPE_FILE = "file";
    public static final String TYPE_DIRECTORY = "directory";
    public static final String TYPE_IMAGE = "image";

    private String name;
    private String type;
    private String path;

    public FileModel(String name, String type, String path) {
        this.name = name;
        this.type = type;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public boolean isFile() {
        return type.equals(TYPE_FILE);
    }

    public boolean isDirectory() {
        return type.equals(TYPE_DIRECTORY);
    }

    public boolean isImage() {
        return type.equals(TYPE_IMAGE);
    }

    // Parcelable implementation
    protected FileModel(Parcel in) {
        name = in.readString();
        type = in.readString();
        path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileModel> CREATOR = new Creator<FileModel>() {
        @Override
        public FileModel createFromParcel(Parcel in) {
            return new FileModel(in);
        }

        @Override
        public FileModel[] newArray(int size) {
            return new FileModel[size];
        }
    };
}
