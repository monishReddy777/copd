package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
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

public class StaffLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_login);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            EditText etEmail = findViewById(R.id.et_email);
            EditText etPassword = findViewById(R.id.et_password);
            
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "please enter mail id and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, password);
        });

        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            intent.putExtra("role", "staff");
            startActivity(intent);
        });

        findViewById(R.id.tv_signup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });
    }

    private void performLogin(String email, String password) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        api.staffLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    String responseStatus = loginResponse.getStatus();

                    SessionManager session = new SessionManager(StaffLoginActivity.this);
                    session.saveEmail(email);
                    if (loginResponse.getName() != null) {
                        session.saveName(loginResponse.getName());
                    }

                    if ("otp_sent".equals(responseStatus)) {
                        // FIRST-TIME LOGIN → Navigate to OTP Verification Screen
                        Toast.makeText(StaffLoginActivity.this,
                            loginResponse.getMessage(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(StaffLoginActivity.this, VerificationActivity.class);
                        intent.putExtra("role", "staff");
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();

                    } else if ("terms_required".equals(responseStatus)) {
                        // VERIFIED but Terms not accepted → Navigate to Terms Screen
                        session.saveTokens("", "", "staff");

                        Intent intent = new Intent(StaffLoginActivity.this, TermsActivity.class);
                        intent.putExtra("role", "staff");
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();

                    } else if ("success".equals(responseStatus)) {
                        // FULLY VERIFIED → Direct Dashboard (skip OTP & Terms)
                        session.saveTokens("", "", "staff");

                        Toast.makeText(StaffLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(StaffLoginActivity.this, StaffDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    }
                } else {
                    if (response.code() == 403) {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                            if (errorBody.contains("disabled by admin")) {
                                Toast.makeText(StaffLoginActivity.this, "Can't able to access. Your account is disabled by admin", Toast.LENGTH_LONG).show();
                            } else if (errorBody.contains("not approved")) {
                                Toast.makeText(StaffLoginActivity.this, "Your account is not approved yet", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(StaffLoginActivity.this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(StaffLoginActivity.this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        handleApiError(response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(StaffLoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleApiError(int code) {
        if (code == 401) {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_LONG).show();
        } else if (code == 403) {
            Toast.makeText(this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error: " + code, Toast.LENGTH_SHORT).show();
        }
    }
}
