package com.example.ftp_client.ui.registration;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ftp_client.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerifyPhone extends AppCompatActivity {

    EditText otpNumberOne, otpNumberTwo, otpNumberThree, otpNumberFour, otpNumberFive, otpNumberSix;
    Button verifyPhone;
    TextView resendOTP, resendOTPText;
    boolean otpValid = true;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    String verificationId;
    String phone, email, password;
    PhoneAuthProvider.ForceResendingToken token;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthCredential storedCredential;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        Intent data = getIntent();
        phone = data.getStringExtra("phone");
        email = data.getStringExtra("email");
        password = data.getStringExtra("password");

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        otpNumberOne = findViewById(R.id.otpNumberOne);
        otpNumberTwo = findViewById(R.id.otpNumberTwo);
        otpNumberThree = findViewById(R.id.otpNumberThree);
        otpNumberFour = findViewById(R.id.otpNumberFour);
        otpNumberFive = findViewById(R.id.otpNumberFive);
        otpNumberSix = findViewById(R.id.otpNumberSix);

        verifyPhone = findViewById(R.id.verifyPhoneBtn);
        resendOTP = findViewById(R.id.resendOTP);
        resendOTPText = findViewById(R.id.resendOTPText);

        verifyPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateOtpFields();
                if (otpValid) {
                    String otp = otpNumberOne.getText().toString().trim() +
                            otpNumberTwo.getText().toString().trim() +
                            otpNumberThree.getText().toString().trim() +
                            otpNumberFour.getText().toString().trim() +
                            otpNumberFive.getText().toString().trim() +
                            otpNumberSix.getText().toString().trim();

                    if (storedCredential != null) {
                        verifyAuthentication(storedCredential);
                    } else {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
                        verifyAuthentication(credential);
                    }
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationId = s;
                token = forceResendingToken;
                startCountdownTimer();
                resendOTP.setVisibility(View.GONE);
                resendOTPText.setText("You can regenerate the OTP after 60 seconds.");
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                resendOTP.setVisibility(View.VISIBLE);
                resendOTPText.setText("You can regenerate the OTP now.");
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                storedCredential = phoneAuthCredential;
                String smsCode = phoneAuthCredential.getSmsCode();
                if (smsCode != null) {
                    char[] codeArray = smsCode.toCharArray();
                    otpNumberOne.setText(String.valueOf(codeArray[0]));
                    otpNumberTwo.setText(String.valueOf(codeArray[1]));
                    otpNumberThree.setText(String.valueOf(codeArray[2]));
                    otpNumberFour.setText(String.valueOf(codeArray[3]));
                    otpNumberFive.setText(String.valueOf(codeArray[4]));
                    otpNumberSix.setText(String.valueOf(codeArray[5]));
                }
                resendOTP.setVisibility(View.GONE);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(VerifyPhone.this, "OTP Verification Failed.", Toast.LENGTH_SHORT).show();
            }
        };

        sendOTP(phone);

        resendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resendOTP(phone);
            }
        });
    }

    private void sendOTP(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, this, mCallbacks);
    }

    private void resendOTP(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, this, mCallbacks, token);
    }

    private void validateOtpFields() {
        otpValid = !(otpNumberOne.getText().toString().isEmpty() ||
                otpNumberTwo.getText().toString().isEmpty() ||
                otpNumberThree.getText().toString().isEmpty() ||
                otpNumberFour.getText().toString().isEmpty() ||
                otpNumberFive.getText().toString().isEmpty() ||
                otpNumberSix.getText().toString().isEmpty());
        if (!otpValid) {
            Toast.makeText(this, "Please enter valid OTP", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyAuthentication(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        saveUserToDatabase(email, password);
                        Toast.makeText(VerifyPhone.this,
                                "Account Created and Linked Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(VerifyPhone.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(VerifyPhone.this, "Failed to verify OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void saveUserToDatabase(String email, String password) {
        String userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        UserStorage userStorage = new UserStorage(email, password);
        databaseReference.child(userId).setValue(userStorage)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(VerifyPhone.this, "User data saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(VerifyPhone.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                resendOTPText.setText("You can regenerate the OTP in " + seconds + " seconds.");
            }

            public void onFinish() {
                resendOTPText.setText("You can regenerate the OTP now.");
                resendOTP.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

