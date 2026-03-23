package com.simats.cdss;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.DoctorDetailResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDoctorDetailsActivity extends AppCompatActivity {

    private int doctorId;
    private ApiService apiService;
    private TextView tvName, tvStatus, tvEmail, tvDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doctor_details);

        doctorId = getIntent().getIntExtra("doctor_id", -1);
        if (doctorId == -1) {
            Toast.makeText(this, "Invalid Doctor ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        apiService = RetrofitClient.getClient(this).create(ApiService.class);
        fetchDoctorDetails();

        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name);
        tvStatus = findViewById(R.id.tv_status);
        tvEmail = findViewById(R.id.tv_email);
        tvDepartment = findViewById(R.id.tv_department);
    }

    private void fetchDoctorDetails() {
        apiService.getDoctorDetails(doctorId).enqueue(new Callback<DoctorDetailResponse>() {
            @Override
            public void onResponse(Call<DoctorDetailResponse> call, Response<DoctorDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayDetails(response.body());
                } else {
                    Toast.makeText(AdminDoctorDetailsActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DoctorDetailResponse> call, Throwable t) {
                Toast.makeText(AdminDoctorDetailsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDetails(DoctorDetailResponse doctor) {
        tvName.setText("Dr. " + (doctor.getName() != null ? doctor.getName() : "-"));
        tvEmail.setText(doctor.getEmail() != null ? doctor.getEmail() : "-");
        
        // Explicitly setting department to "Doctor" as requested
        tvDepartment.setText("Doctor");

        String status = doctor.getStatus();
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
