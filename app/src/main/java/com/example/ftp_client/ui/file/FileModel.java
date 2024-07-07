package com.example.ftp_client.ui.file;

public class FileModel {

    public static final int TYPE_FILE = 1;
    public static final int TYPE_DIRECTORY = 2;

    private String name;
    private int type;

    public FileModel(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public boolean isFile() {
        return type == TYPE_FILE;
    }

    public boolean isDirectory() {
        return type == TYPE_DIRECTORY;
    }
}