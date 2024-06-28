package com.example.ftp_client.ui.registration;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ftp_client.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText inputEmailVerify;
    Button sendEmailVerify;
    TextView backToLogin, tvCountDown;
    private CountDownTimer countDownTimer;
    private static final long RESEND_DELAY = 30000; // 30 seconds delay
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        inputEmailVerify = findViewById(R.id.inputEmailVerify);
        sendEmailVerify = findViewById(R.id.btnSendForgotPass);
        backToLogin = findViewById(R.id.backToLogin);
        tvCountDown = findViewById(R.id.tvCountdown);
        mAuth = FirebaseAuth.getInstance();

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        sendEmailVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmailVerify.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    inputEmailVerify.setError("Enter your email address");
                    inputEmailVerify.requestFocus();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    inputEmailVerify.setError("Enter a valid email address");
                    inputEmailVerify.requestFocus();
                } else {
                    sendPasswordResetEmail(email); // Gửi email reset mật khẩu
                }
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                        startResendCountdown();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendCountdown() {
        sendEmailVerify.setEnabled(false);
        tvCountDown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(RESEND_DELAY, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String countdownText = "Resend available in: " + millisUntilFinished / 1000 + "s";
                tvCountDown.setText(countdownText);
            }

            @Override
            public void onFinish() {
                sendEmailVerify.setEnabled(true);
                tvCountDown.setVisibility(View.GONE);
            }
        }.start();
    }
}
