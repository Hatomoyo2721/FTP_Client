package com.example.ftp_client.ui.connection;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ConnectionModel implements Parcelable {
    private UUID id;
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private String email;
    private String creationDate;

    public ConnectionModel(String ipAddress, int port, String username, String password, String email) {
        this.id = UUID.randomUUID();
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.email = email;
        this.creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    protected ConnectionModel(Parcel in) {
        id = UUID.fromString(in.readString());
        ipAddress = in.readString();
        port = in.readInt();
        username = in.readString();
        password = in.readString();
    }

    public ConnectionModel(String ipAddress, int port, String username) {
        this.id = UUID.randomUUID();
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id.toString());
        dest.writeString(ipAddress);
        dest.writeInt(port);
        dest.writeString(username);
        dest.writeString(password);
    }

    public static final Creator<ConnectionModel> CREATOR = new Creator<ConnectionModel>() {
        @Override
        public ConnectionModel createFromParcel(Parcel in) {
            return new ConnectionModel(in);
        }

        @Override
        public ConnectionModel[] newArray(int size) {
            return new ConnectionModel[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConnectionModel that = (ConnectionModel) obj;
        return port == that.port &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}