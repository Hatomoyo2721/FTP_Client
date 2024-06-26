package com.example.ftp_client.ui.connection;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import java.util.UUID;

public class ConnectionModel implements Parcelable {
    private UUID id;
    private String ipAddress;
    private int port;
    private String username;
    private String password;

    public ConnectionModel(String ipAddress, int port, String username, String password) {
        this.id = UUID.randomUUID();
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    protected ConnectionModel(Parcel in) {
        id = UUID.fromString(in.readString());
        ipAddress = in.readString();
        port = in.readInt();
        username = in.readString();
        password = in.readString();
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
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
