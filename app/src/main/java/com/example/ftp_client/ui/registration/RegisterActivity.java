package com.example.ftp_client.ui.registration;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ftp_client.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword, inputConfirmPassword, phoneCountryCode, phoneNumber;
    private TextView tvLogin;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;
    private ImageButton togglePasswordReg, toggleConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        phoneCountryCode = findViewById(R.id.countryCode);
        phoneNumber = findViewById(R.id.inputPhone);
        tvLogin = findViewById(R.id.tvLogin);
        btnRegister = findViewById(R.id.btnRegister);
        togglePasswordReg = findViewById(R.id.togglePasswordReg);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);

        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> validateDataAndRegister());

        tvLogin.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
        });

        togglePasswordReg.setOnClickListener(v -> togglePasswordVisibility());
        toggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            togglePasswordReg.setImageResource(R.drawable.baseline_visibility_off);
        } else {
            inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            togglePasswordReg.setImageResource(R.drawable.baseline_visibility);
        }
        inputPassword.setSelection(inputPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void toggleConfirmPasswordVisibility() {
        if (isPasswordVisible) {
            inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleConfirmPassword.setImageResource(R.drawable.baseline_visibility_off);
        } else {
            inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleConfirmPassword.setImageResource(R.drawable.baseline_visibility);
        }
        inputConfirmPassword.setSelection(inputConfirmPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void validateDataAndRegister() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        inputEmail.setError(null);
        inputPassword.setError(null);
        inputConfirmPassword.setError(null);

        if (!isNetworkAvailable()) {
            Toast.makeText(this,
                    "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email address");
            inputEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() <= 7) {
            inputPassword.setError("Password must be at least 8 characters");
            inputPassword.requestFocus();
            return;
        }

        if (!confirmPassword.equals(password)) {
            inputConfirmPassword.setError("Passwords do not match");
            inputConfirmPassword.requestFocus();
            return;
        }

        if (phoneCountryCode.getText().toString().isEmpty() || phoneNumber.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter your phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = "+" + phoneCountryCode.getText().toString().trim() + phoneNumber.getText().toString().trim();
        Intent intent = new Intent(RegisterActivity.this, VerifyPhone.class);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        startActivity(intent);
    }
}