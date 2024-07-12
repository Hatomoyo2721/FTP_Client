package com.example.ftp_client.ui.file;

public class FileModel {
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
}
