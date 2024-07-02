package com.example.ftp_client.ui.registration;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ftp_client.MainActivity;
import com.example.ftp_client.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText emailLog, passwordLog;
    private TextView tvRegister, tvForgotPass;
    private Button btnLogin;
    private DatabaseReference databaseReference;
    private boolean isPasswordVisible = false;
    private ImageButton btnShowPassword;

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLog = findViewById(R.id.emailLogin);
        passwordLog = findViewById(R.id.passwordLogin);
        tvRegister = findViewById(R.id.tvRegister);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPass = findViewById(R.id.tvForgotPassword);
        btnShowPassword = findViewById(R.id.togglePasswordLog);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        tvRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        tvForgotPass.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(i);
        });

        btnLogin.setOnClickListener(v -> loginUser());

        btnShowPassword.setOnClickListener(v ->  togglePasswordVisibility());

        checkAndRequestPermissions();
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordLog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnShowPassword.setImageResource(R.drawable.baseline_visibility_off);
        } else {
            passwordLog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnShowPassword.setImageResource(R.drawable.baseline_visibility);
        }
        passwordLog.setSelection(passwordLog.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void loginUser() {
        String email = emailLog.getText().toString().trim();
        String password = passwordLog.getText().toString().trim();

        if (email.isEmpty()) {
            emailLog.setError("Enter your email address");
            emailLog.requestFocus();
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLog.setError("Enter a valid email address");
            emailLog.requestFocus();
        } else if (password.isEmpty()) {
            passwordLog.setError("Enter your password");
            passwordLog.requestFocus();
        } else {
            authenticateUser(email, password);
        }
    }

    private void authenticateUser(String email, String password) {
        Log.d("LoginActivity", "Authenticating user with email: " + email);
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("LoginActivity", "DataSnapshot exists: " + dataSnapshot.exists());
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        UserStorage userStorage = userSnapshot.getValue(UserStorage.class);
                        Log.d("LoginActivity", "User found: " + userStorage);
                        if (userStorage != null && userStorage.password.equals(password)) {
                            Log.d("LoginActivity", "Password matches");
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("LoginActivity", "Invalid email or password");
                            Toast.makeText(LoginActivity.this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.d("LoginActivity", "User not found");
                    Toast.makeText(LoginActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("LoginActivity", "Database error: " + databaseError.getMessage());
                Log.d("LoginActivity", "Details: " + databaseError.getDetails());
                Log.d("LoginActivity", "Code: " + databaseError.getCode());
                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void checkAndRequestPermissions() {
        @SuppressLint("InlinedApi") String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Required")
                        .setMessage("All permissions are required to use this app. Please grant all permissions in order to proceed.")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false) // Prevent dismissing dialog by tapping outside
                        .show();
            }
        }
    }
}