package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.simats.cdss.models.LoginResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private MaterialButton btnResetPassword;
    private String userRole;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        userRole = getIntent().getStringExtra("role");
        userEmail = getIntent().getStringExtra("email");

        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnResetPassword = findViewById(R.id.btn_reset_password);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        btnResetPassword.setOnClickListener(v -> {
            if (validatePassword()) {
                String newPassword = etNewPassword.getText().toString();
                resetPassword(newPassword);
            }
        });
    }

    /**
     * POST /api/reset-password/
     * Body: { "email": "...", "new_password": "...", "role": "doctor" or "staff" }
     *
     * Updates password securely and clears OTP.
     */
    private void resetPassword(String newPassword) {
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Updating...");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("new_password", newPassword);
        request.put("role", userRole);

        api.resetPassword(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Update Password & Login");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse resetResponse = response.body();

                    if ("success".equals(resetResponse.getStatus())) {
                        // Password reset successful → go to login
                        Toast.makeText(ResetPasswordActivity.this,
                                "Password reset successful! Please login with your new password.",
                                Toast.LENGTH_LONG).show();

                        // Navigate back to the login screen based on role
                        Intent intent;
                        if ("staff".equals(userRole)) {
                            intent = new Intent(ResetPasswordActivity.this, StaffLoginActivity.class);
                        } else {
                            intent = new Intent(ResetPasswordActivity.this, DoctorLoginActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = resetResponse.getMessage();
                        Toast.makeText(ResetPasswordActivity.this,
                                message != null ? message : "Failed to reset password",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ResetPasswordActivity.this,
                            "Failed to reset password. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Update Password & Login");
                Toast.makeText(ResetPasswordActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validatePassword() {
        String password = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (password.length() < 8) {
            etNewPassword.setError("Minimum 8 characters required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            etNewPassword.setError("At least 1 uppercase letter required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            etNewPassword.setError("At least 1 lowercase letter required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            etNewPassword.setError("At least 1 digit required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!Pattern.compile("[!@#$%^&*(),.?\":{}|<>_]").matcher(password).find()) {
            etNewPassword.setError("At least 1 special character required");
            etNewPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }
}