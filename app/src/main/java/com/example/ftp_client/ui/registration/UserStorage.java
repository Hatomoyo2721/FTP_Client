package com.example.ftp_client.ui.registration;

public class UserStorage {
    public String email;
    public String password;

    public UserStorage() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public UserStorage(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
