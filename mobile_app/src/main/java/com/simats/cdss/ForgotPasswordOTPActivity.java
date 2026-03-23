package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.LoginResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * OTP verification screen for Forgot Password flow.
 * Reuses the same layout as VerificationActivity (activity_verification.xml).
 *
 * Flow: ForgotPasswordActivity → ForgotPasswordOTPActivity → ResetPasswordActivity
 */
public class ForgotPasswordOTPActivity extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnVerify;
    private String userRole;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        userRole = getIntent().getStringExtra("role");
        userEmail = getIntent().getStringExtra("email");

        otp1 = findViewById(R.id.otp_1);
        otp2 = findViewById(R.id.otp_2);
        otp3 = findViewById(R.id.otp_3);
        otp4 = findViewById(R.id.otp_4);
        otp5 = findViewById(R.id.otp_5);
        otp6 = findViewById(R.id.otp_6);
        btnVerify = findViewById(R.id.btn_verify);

        setupOtpInputs();

        btnVerify.setOnClickListener(v -> {
            String otp = otp1.getText().toString() + otp2.getText().toString()
                    + otp3.getText().toString() + otp4.getText().toString()
                    + otp5.getText().toString() + otp6.getText().toString();

            if (otp.length() != 6) {
                Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtp(otp);
        });
    }

    /**
     * POST /api/forgot-password/verify-otp/
     * Body: { "email": "...", "otp": "123456", "role": "doctor" or "staff" }
     *
     * If "otp_verified" → navigate to ResetPasswordActivity
     */
    private void verifyOtp(String otp) {
        btnVerify.setEnabled(false);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("otp", otp);
        request.put("role", userRole);

        api.forgotPasswordVerifyOtp(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse verifyResponse = response.body();

                    if ("otp_verified".equals(verifyResponse.getStatus())) {
                        // OTP verified → navigate to Reset Password screen
                        Toast.makeText(ForgotPasswordOTPActivity.this,
                                "OTP verified successfully", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ForgotPasswordOTPActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("role", userRole);
                        intent.putExtra("email", userEmail);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error (invalid OTP, expired, etc.)
                        String message = verifyResponse.getMessage();
                        Toast.makeText(ForgotPasswordOTPActivity.this,
                                message != null ? message : "Invalid OTP",
                                Toast.LENGTH_LONG).show();
                        clearOtpFields();
                    }
                } else {
                    Toast.makeText(ForgotPasswordOTPActivity.this,
                            "Verification failed. Please try again.", Toast.LENGTH_LONG).show();
                    clearOtpFields();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                Toast.makeText(ForgotPasswordOTPActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearOtpFields() {
        otp1.setText("");
        otp2.setText("");
        otp3.setText("");
        otp4.setText("");
        otp5.setText("");
        otp6.setText("");
        otp1.requestFocus();
    }

    private void setupOtpInputs() {
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }
            });

            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[index].getText().toString().isEmpty() && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }
}
