package com.example.ftp_client.ui.activity;

public class HistoryItem {
    private String ipAddress;
    private String fileName;
    private String fileUri;
    private String timestamp;

    public HistoryItem(String ipAddress, String fileName, String fileUri, String timestamp) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.fileUri = fileUri;
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUri() {
        return fileUri;
    }

    public String getTimestamp() {
        return timestamp;
    }
}