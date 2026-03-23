package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
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

public class TermsActivity extends AppCompatActivity {

    private String userRole;
    private String userEmail;
    private Button btnAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        userRole = getIntent().getStringExtra("role");
        userEmail = getIntent().getStringExtra("email");

        // Fallback: get from SessionManager if not passed via intent
        if (userRole == null || userRole.isEmpty()) {
            SessionManager session = new SessionManager(this);
            userRole = session.getRole();
        }
        if (userEmail == null || userEmail.isEmpty()) {
            SessionManager session = new SessionManager(this);
            userEmail = session.getEmail();
        }

        CheckBox cbAgree = findViewById(R.id.cb_agree);
        btnAccept = findViewById(R.id.btn_accept);

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            onBackPressed();
        });

        cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAccept.setEnabled(isChecked);
        });

        btnAccept.setOnClickListener(v -> {
            // Call API to update terms_accepted = 1 in database
            acceptTerms();
        });
    }

    /**
     * POST /api/accept-terms/
     * Body: { "email": "...", "role": "doctor" or "staff" }
     *
     * Updates terms_accepted = 1 in the respective table.
     * On success → navigate to Dashboard.
     */
    private void acceptTerms() {
        btnAccept.setEnabled(false);
        btnAccept.setText("Accepting...");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, String> request = new HashMap<>();
        request.put("email", userEmail);
        request.put("role", userRole);

        api.acceptTerms(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnAccept.setEnabled(true);
                btnAccept.setText("Accept");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse termsResponse = response.body();

                    if ("success".equals(termsResponse.getStatus())) {
                        // terms_accepted = 1 in DB → Navigate to Dashboard
                        Toast.makeText(TermsActivity.this, "Terms accepted", Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if ("staff".equals(userRole)) {
                            intent = new Intent(TermsActivity.this, StaffDashboardActivity.class);
                        } else {
                            intent = new Intent(TermsActivity.this, DoctordashboardActivity.class);
                        }

                        // Clear back stack so user can't go back to Terms/OTP screens
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(TermsActivity.this,
                                termsResponse.getMessage() != null ? termsResponse.getMessage() : "Failed to accept terms",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(TermsActivity.this,
                            "Failed to accept terms. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnAccept.setEnabled(true);
                btnAccept.setText("Accept");
                Toast.makeText(TermsActivity.this,
                        "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to OTP screen after verification
        Toast.makeText(this, "Please accept the terms to continue", Toast.LENGTH_SHORT).show();
    }
}