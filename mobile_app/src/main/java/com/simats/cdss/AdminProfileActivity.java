package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.AdminProfileResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminProfileActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfileActivity";

    private TextView tvAdminName, tvAdminEmail, tvAdminId, tvAdminRole, tvAdminPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        // Initialize views
        tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminEmail = findViewById(R.id.tv_admin_email);
        tvAdminId = findViewById(R.id.tv_admin_id);
        tvAdminRole = findViewById(R.id.tv_admin_role);
        tvAdminPermission = findViewById(R.id.tv_admin_permission);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Logout button (existing logic unchanged)
        findViewById(R.id.btn_logout_admin).setOnClickListener(v -> showLogoutConfirmation());

        // Fetch admin profile from API
        fetchAdminProfile();
    }

    private void fetchAdminProfile() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);

        apiService.getAdminProfile().enqueue(new Callback<AdminProfileResponse>() {
            @Override
            public void onResponse(Call<AdminProfileResponse> call, Response<AdminProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AdminProfileResponse profile = response.body();

                    tvAdminName.setText(profile.getName());
                    tvAdminEmail.setText(profile.getEmail());
                    tvAdminId.setText("ADM-" + profile.getAdminId());
                    tvAdminRole.setText(profile.getRole());
                    tvAdminPermission.setText(profile.getPermissions());
                } else {
                    Log.e(TAG, "Failed to fetch admin profile: " + response.code());
                    Toast.makeText(AdminProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AdminProfileResponse> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(AdminProfileActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from Admin Panel?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Navigate back to Role selection or Login screen
                    Intent intent = new Intent(AdminProfileActivity.this, RoleActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}