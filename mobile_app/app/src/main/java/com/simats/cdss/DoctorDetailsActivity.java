package com.simats.cdss;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.simats.cdss.models.DoctorDetailResponse;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorDetailsActivity extends AppCompatActivity {

    private static final String TAG = "DoctorDetails";
    private int doctorId;
    private ApiService apiService;
    private ProgressBar progressBar;
    private TextView tvFullName, tvSpecialization, tvEmail, tvPhone, tvLicense, tvStatusLabel;
    private CollapsingToolbarLayout collapsingToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details);

        doctorId = getIntent().getIntExtra("doctor_id", -1);
        if (doctorId == -1) {
            Toast.makeText(this, "Error: Doctor ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        progressBar = findViewById(R.id.progress_bar);
        tvFullName = findViewById(R.id.tv_full_name);
        tvSpecialization = findViewById(R.id.tv_detail_specialization);
        tvEmail = findViewById(R.id.tv_detail_email);
        tvPhone = findViewById(R.id.tv_detail_phone);
        tvLicense = findViewById(R.id.tv_detail_license);
        tvStatusLabel = findViewById(R.id.tv_account_status_label);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        findViewById(R.id.btn_revoke_detail).setOnClickListener(v -> confirmAndRemove());

        loadDoctorDetails();
    }

    private void loadDoctorDetails() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDoctorDetails(doctorId).enqueue(new Callback<DoctorDetailResponse>() {
            @Override
            public void onResponse(Call<DoctorDetailResponse> call, Response<DoctorDetailResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayDetails(response.body());
                } else {
                    Toast.makeText(DoctorDetailsActivity.this, "Failed to load doctor details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DoctorDetailResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching doctor details", t);
                Toast.makeText(DoctorDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDetails(DoctorDetailResponse doctor) {
        tvFullName.setText(doctor.getName());
        tvSpecialization.setText(doctor.getSpecialization());
        tvEmail.setText(doctor.getEmail());
        tvPhone.setText(doctor.getPhone() != null ? doctor.getPhone() : "Not provided");
        tvLicense.setText("License: " + doctor.getLicenseNumber());
        collapsingToolbar.setTitle(doctor.getName());

        String status = doctor.getStatus();
        if ("active".equalsIgnoreCase(status)) {
            tvStatusLabel.setText("Current Account Status: ACTIVE");
            tvStatusLabel.setTextColor(getResources().getColor(R.color.admin_success));
        } else {
            tvStatusLabel.setText("Current Account Status: DISABLED");
            tvStatusLabel.setTextColor(getResources().getColor(R.color.admin_danger));
        }
    }

    private void confirmAndRemove() {
        new AlertDialog.Builder(this)
                .setTitle("Permanent Removal")
                .setMessage("This will completely remove the doctor from the database and revoke all login access. This action is irreversible. Proceed?")
                .setPositiveButton("Remove Permanently", (dialog, which) -> deleteDoctor())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteDoctor() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.deleteDoctor(doctorId).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(DoctorDetailsActivity.this, "Doctor removed successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // To notify parent to refresh list
                    finish();
                } else {
                    Toast.makeText(DoctorDetailsActivity.this, "Failed to remove doctor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DoctorDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
