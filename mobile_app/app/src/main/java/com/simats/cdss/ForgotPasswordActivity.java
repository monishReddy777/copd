package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.simats.cdss.models.LoginResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private String userRole;
    private EditText etEmail;
    private MaterialButton btnSendResetLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Get role from intent (doctor or staff)
        userRole = getIntent().getStringExtra("role");
        if (userRole == null || userRole.isEmpty()) {
            userRole = "doctor"; // Default fallback
        }

        etEmail = findViewById(R.id.et_email);
        btnSendResetLink = findViewById(R.id.btn_send_reset_link);

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });

        btnSendResetLink.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            sendOtp(email);
        });
    }

    /**
     * POST /api/forgot-password/
     * Body: { "email": "...", "role": "doctor" or "staff" }
     *
     * Generates 6-digit OTP, saves in DB, sends OTP to email.
     * On success → navigate to ForgotPasswordOTPActivity
     */
    private void sendOtp(String email) {
        btnSendResetLink.setEnabled(false);
        btnSendResetLink.setText("Sending...");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("role", userRole);

        api.forgotPassword(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnSendResetLink.setEnabled(true);
                btnSendResetLink.setText("Send Reset Link");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse forgotResponse = response.body();

                    if ("otp_sent".equals(forgotResponse.getStatus())) {
                        // OTP sent → show success popup, then navigate to OTP screen
                        showSuccessBottomSheet(email);
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                forgotResponse.getMessage() != null ? forgotResponse.getMessage() : "Failed to send OTP",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Handle error responses (e.g., 404 email not registered)
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("not registered")) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Email not registered", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Failed to send OTP. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed to send OTP", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSendResetLink.setEnabled(true);
                btnSendResetLink.setText("Send Reset Link");
                Toast.makeText(ForgotPasswordActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccessBottomSheet(String email) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_success_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);

        view.findViewById(R.id.btn_done).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // Navigate to OTP verification screen for forgot password
            Intent intent = new Intent(ForgotPasswordActivity.this, ForgotPasswordOTPActivity.class);
            intent.putExtra("role", userRole);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        });

        bottomSheetDialog.show();
    }
}
