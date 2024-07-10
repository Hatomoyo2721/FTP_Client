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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileModel fileModel = (FileModel) o;

        if (type != fileModel.type) return false;
        return name != null ? name.equals(fileModel.name) : fileModel.name == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + type;
        return result;
    }

    public static FileModel fromString(String line) {
        if (line.startsWith("FILE")) {
            String[] parts = line.split("\\s+", 2); // Split by whitespace, maximum 2 parts
            String fileName = parts[1]; // Extract file name
            return new FileModel(fileName, TYPE_FILE);
        } else if (line.startsWith("DIR")) {
            String[] parts = line.split("\\s+", 2); // Split by whitespace, maximum 2 parts
            String dirName = parts[1]; // Extract directory name
            return new FileModel(dirName, TYPE_DIRECTORY);
        } else {
            throw new IllegalArgumentException("Invalid line format for FileModel: " + line);
        }
    }
}