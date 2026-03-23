package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import com.simats.cdss.models.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class DoctorLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });

        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> {
            Intent intent = new Intent(DoctorLoginActivity.this, ForgotPasswordActivity.class);
            intent.putExtra("role", "doctor");
            startActivity(intent);
        });

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            EditText etEmail = findViewById(R.id.et_email);
            EditText etPassword = findViewById(R.id.et_password);
            
            String username = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "please enter mail id and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(username, password);
        });

        findViewById(R.id.tv_signup).setOnClickListener(v -> {
            startActivity(new Intent(DoctorLoginActivity.this, SignupActivity.class));
        });
    }

    private void performLogin(String username, String password) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        Map<String, String> request = new HashMap<>();
        request.put("email", username);
        request.put("password", password);

        api.doctorLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    String responseStatus = loginResponse.getStatus();

                    SessionManager session = new SessionManager(DoctorLoginActivity.this);
                    session.saveEmail(username);
                    if (loginResponse.getName() != null) {
                        session.saveName(loginResponse.getName());
                    }

                    if ("otp_sent".equals(responseStatus)) {
                        // FIRST-TIME LOGIN → Navigate to OTP Verification Screen
                        Toast.makeText(DoctorLoginActivity.this, 
                            loginResponse.getMessage(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(DoctorLoginActivity.this, VerificationActivity.class);
                        intent.putExtra("role", "doctor");
                        intent.putExtra("email", username);
                        startActivity(intent);
                        finish();

                    } else if ("terms_required".equals(responseStatus)) {
                        // VERIFIED but Terms not accepted → Navigate to Terms Screen
                        session.saveTokens("", "", "doctor");

                        Intent intent = new Intent(DoctorLoginActivity.this, TermsActivity.class);
                        intent.putExtra("role", "doctor");
                        intent.putExtra("email", username);
                        startActivity(intent);
                        finish();

                    } else if ("success".equals(responseStatus)) {
                        // FULLY VERIFIED → Direct Dashboard (skip OTP & Terms)
                        session.saveTokens("", "", "doctor");

                        Toast.makeText(DoctorLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(DoctorLoginActivity.this, DoctordashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    }
                } else {
                    if (response.code() == 403) {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                            if (errorBody.contains("disabled by admin")) {
                                Toast.makeText(DoctorLoginActivity.this, "Can't able to access. Your account is disabled by admin", Toast.LENGTH_LONG).show();
                            } else if (errorBody.contains("not approved")) {
                                Toast.makeText(DoctorLoginActivity.this, "Your account is not approved yet", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(DoctorLoginActivity.this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(DoctorLoginActivity.this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        handleApiError(response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(DoctorLoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleApiError(int code) {
        if (code == 401) {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_LONG).show();
        } else if (code == 403) {
            Toast.makeText(this, "Can't able to access. Your account is disabled by admin", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error: " + code, Toast.LENGTH_SHORT).show();
        }
    }
}
