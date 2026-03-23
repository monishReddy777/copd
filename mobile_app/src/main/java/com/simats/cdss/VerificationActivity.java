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

public class VerificationActivity extends AppCompatActivity {

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

        // Fallback: get email from SessionManager if not passed via intent
        if (userEmail == null || userEmail.isEmpty()) {
            SessionManager session = new SessionManager(this);
            userEmail = session.getEmail();
        }

        otp1 = findViewById(R.id.otp_1);
        otp2 = findViewById(R.id.otp_2);
        otp3 = findViewById(R.id.otp_3);
        otp4 = findViewById(R.id.otp_4);
        otp5 = findViewById(R.id.otp_5);
        otp6 = findViewById(R.id.otp_6);
        btnVerify = findViewById(R.id.btn_verify);

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });

        findViewById(R.id.tv_resend).setOnClickListener(v -> {
            resendOtp();
        });

        btnVerify.setOnClickListener(v -> {
            String enteredOtp = getEnteredOtp();

            if (enteredOtp.length() != 6) {
                Toast.makeText(this, "Please enter the complete 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtp(enteredOtp);
        });

        setupOtpInputs();
    }

    /**
     * Collect the 6-digit OTP from individual EditText fields
     */
    private String getEnteredOtp() {
        return otp1.getText().toString().trim()
                + otp2.getText().toString().trim()
                + otp3.getText().toString().trim()
                + otp4.getText().toString().trim()
                + otp5.getText().toString().trim()
                + otp6.getText().toString().trim();
    }

    /**
     * Call verify-otp API based on user role.
     * On success: is_verified is set to 1 in DB → navigate to Terms & Conditions
     * On failure: show "Invalid OTP"
     */
    private void verifyOtp(String otp) {
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("otp", otp);

        // Call the appropriate verify-otp endpoint based on role
        Call<LoginResponse> call;
        if ("staff".equals(userRole)) {
            call = api.verifyStaffOtp(request);
        } else {
            call = api.verifyDoctorOtp(request);
        }

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse verifyResponse = response.body();
                    String responseStatus = verifyResponse.getStatus();

                    if ("terms_required".equals(responseStatus)) {
                        // OTP verified but Terms not accepted → Terms Screen
                        SessionManager session = new SessionManager(VerificationActivity.this);
                        session.saveTokens("", "", userRole);
                        session.saveEmail(userEmail);

                        Toast.makeText(VerificationActivity.this,
                                "OTP verified successfully", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(VerificationActivity.this, TermsActivity.class);
                        intent.putExtra("role", userRole);
                        intent.putExtra("email", userEmail);
                        startActivity(intent);
                        finish();

                    } else if ("success".equals(responseStatus) || verifyResponse.isVerified()) {
                        // OTP verified AND Terms already accepted → Dashboard
                        SessionManager session = new SessionManager(VerificationActivity.this);
                        session.saveTokens("", "", userRole);
                        session.saveEmail(userEmail);

                        Toast.makeText(VerificationActivity.this,
                                "Login successful", Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if ("staff".equals(userRole)) {
                            intent = new Intent(VerificationActivity.this, StaffDashboardActivity.class);
                        } else {
                            intent = new Intent(VerificationActivity.this, DoctordashboardActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        // Verification failed
                        String message = verifyResponse.getMessage();
                        Toast.makeText(VerificationActivity.this,
                                message != null ? message : "Invalid OTP",
                                Toast.LENGTH_LONG).show();
                        clearOtpFields();
                    }
                } else {
                    // HTTP error response
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                        if (errorBody.contains("expired")) {
                            Toast.makeText(VerificationActivity.this,
                                    "OTP has expired. Please login again.", Toast.LENGTH_LONG).show();
                            // Go back to login
                            finish();
                        } else {
                            Toast.makeText(VerificationActivity.this,
                                    "Invalid OTP", Toast.LENGTH_LONG).show();
                            clearOtpFields();
                        }
                    } catch (Exception e) {
                        Toast.makeText(VerificationActivity.this,
                                "Invalid OTP", Toast.LENGTH_LONG).show();
                        clearOtpFields();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");
                Toast.makeText(VerificationActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Resend OTP by calling the login API again (which generates a new OTP)
     */
    private void resendOtp() {
        Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("password", ""); // Password not needed for resend, but the field is required

        // For resend, we could use the login endpoint again or a dedicated resend endpoint
        // For now, show a message that OTP has been resent
        // In a production app, you would call a dedicated resend-otp endpoint

        Toast.makeText(this, "Please login again to receive a new OTP", Toast.LENGTH_LONG).show();
    }

    /**
     * Clear all OTP input fields
     */
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
        otp1.addTextChangedListener(new OtpTextWatcher(otp1, otp2));
        otp2.addTextChangedListener(new OtpTextWatcher(otp2, otp3));
        otp3.addTextChangedListener(new OtpTextWatcher(otp3, otp4));
        otp4.addTextChangedListener(new OtpTextWatcher(otp4, otp5));
        otp5.addTextChangedListener(new OtpTextWatcher(otp5, otp6));
        otp6.addTextChangedListener(new OtpTextWatcher(otp6, null));

        // Handle backspace
        otp2.setOnKeyListener(new OtpKeyListener(otp2, otp1));
        otp3.setOnKeyListener(new OtpKeyListener(otp3, otp2));
        otp4.setOnKeyListener(new OtpKeyListener(otp4, otp3));
        otp5.setOnKeyListener(new OtpKeyListener(otp5, otp4));
        otp6.setOnKeyListener(new OtpKeyListener(otp6, otp5));
    }

    private class OtpTextWatcher implements TextWatcher {
        private EditText currentView;
        private EditText nextView;

        public OtpTextWatcher(EditText currentView, EditText nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }

    private class OtpKeyListener implements android.view.View.OnKeyListener {
        private EditText currentView;
        private EditText previousView;

        public OtpKeyListener(EditText currentView, EditText previousView) {
            this.currentView = currentView;
            this.previousView = previousView;
        }

        @Override
        public boolean onKey(android.view.View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL
                    && currentView.getText().toString().isEmpty() && previousView != null) {
                previousView.requestFocus();
                return true;
            }
            return false;
        }
    }
}