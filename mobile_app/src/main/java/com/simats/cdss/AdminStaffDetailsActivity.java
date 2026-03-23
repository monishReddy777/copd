package com.simats.cdss;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.StaffDetailResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStaffDetailsActivity extends AppCompatActivity {

    private int staffId;
    private ApiService apiService;
    private TextView tvName, tvStatus, tvEmail, tvDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_staff_details);

        staffId = getIntent().getIntExtra("staff_id", -1);
        if (staffId == -1) {
            Toast.makeText(this, "Invalid Staff ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        apiService = RetrofitClient.getClient(this).create(ApiService.class);
        fetchStaffDetails();

        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name);
        tvStatus = findViewById(R.id.tv_status);
        tvEmail = findViewById(R.id.tv_email);
        tvDepartment = findViewById(R.id.tv_department);
    }

    private void fetchStaffDetails() {
        apiService.getStaffDetails(staffId).enqueue(new Callback<StaffDetailResponse>() {
            @Override
            public void onResponse(Call<StaffDetailResponse> call, Response<StaffDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayDetails(response.body());
                } else {
                    Toast.makeText(AdminStaffDetailsActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StaffDetailResponse> call, Throwable t) {
                Toast.makeText(AdminStaffDetailsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDetails(StaffDetailResponse staff) {
        tvName.setText(staff.getName());
        tvEmail.setText(staff.getEmail());
        
        String department = staff.getDepartment();
        tvDepartment.setText(department != null && !department.isEmpty() ? department : "Staff");

        String status = staff.getStatus();
        if (status != null) {
            tvStatus.setText(status.toUpperCase());
            if ("active".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_danger);
            }
        }
    }
}
