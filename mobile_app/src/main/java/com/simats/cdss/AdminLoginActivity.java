package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import com.simats.cdss.models.LoginRequest;
import com.simats.cdss.models.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            EditText etUsername = findViewById(R.id.et_username);
            EditText etPassword = findViewById(R.id.et_password);
            
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(username, password);
        });
    }

    private void performLogin(String username, String password) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        
        java.util.HashMap<String, String> requestBody = new java.util.HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        api.adminLogin(requestBody).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    
                    String role = response.body().getRole();
                    
                    if (role != null && role.equals("admin")) {
                        String accessToken = response.body().getAccess();
                        String refreshToken = response.body().getRefresh();
                        
                        SessionManager session = new SessionManager(AdminLoginActivity.this);
                        session.saveTokens(accessToken, refreshToken, role);
                        
                        Toast.makeText(AdminLoginActivity.this, "Welcome Admin", Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AdminLoginActivity.this, "Access denied. Not an Admin.", Toast.LENGTH_LONG).show();
                    }

                } else {
                    if (response.code() == 401) {
                        Toast.makeText(AdminLoginActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
                    } else if (response.code() == 403) {
                        Toast.makeText(AdminLoginActivity.this, "Your account is waiting for admin approval", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AdminLoginActivity.this, "Login failed", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(AdminLoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}