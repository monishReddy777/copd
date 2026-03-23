package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.DashboardStatsResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalDoctors, tvTotalStaff, tvPendingRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add dashboard protection check
        SessionManager session = new SessionManager(this);
        if (session.getAccessToken() == null || !"admin".equals(session.getRole())) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);

        // Profile Click
        findViewById(R.id.iv_admin_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfileActivity.class));
        });

        // Navigation Buttons
        findViewById(R.id.btn_manage_doctors).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminManageDoctorsActivity.class));
        });

        findViewById(R.id.btn_manage_staff).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminManageStaffActivity.class));
        });

        findViewById(R.id.btn_approval_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, ApprovalRequestsActivity.class));
        });

        // Summary Cards
        findViewById(R.id.card_total_doctors).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminManageDoctorsActivity.class));
        });

        findViewById(R.id.card_total_staff).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminManageStaffActivity.class));
        });

        findViewById(R.id.card_pending_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, ApprovalRequestsActivity.class));
        });

        tvTotalDoctors = findViewById(R.id.tv_total_doctors);
        tvTotalStaff = findViewById(R.id.tv_total_staff);
        tvPendingRequests = findViewById(R.id.tv_pending_requests);

        fetchDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDashboardStats(); // Refresh data whenever returning to this screen
    }

    private void fetchDashboardStats() {
        Log.d("AdminDashboard", "Fetching stats...");
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getAdminDashboardStats().enqueue(new Callback<DashboardStatsResponse>() {
            @Override
            public void onResponse(Call<DashboardStatsResponse> call, Response<DashboardStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DashboardStatsResponse stats = response.body();
                    Log.d("AdminDashboard", "Stats received: Docs=" + stats.getTotalDoctors() + ", Staff=" + stats.getTotalStaff());
                    
                    tvTotalDoctors.setText(String.valueOf(stats.getTotalDoctors()));
                    tvTotalStaff.setText(String.valueOf(stats.getTotalStaff()));

                    int pending = stats.getPendingRequests();
                    tvPendingRequests.setText(String.format("%02d Action Required", pending));
                } else {
                    Log.e("AdminDashboard", "Error response: " + response.code() + " " + response.message());
                    Toast.makeText(AdminDashboardActivity.this, "Failed to load stats: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DashboardStatsResponse> call, Throwable t) {
                Log.e("AdminDashboard", "Network failure", t);
                Toast.makeText(AdminDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}