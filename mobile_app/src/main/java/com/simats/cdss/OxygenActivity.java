package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.PatientDetailResponse;
import com.simats.cdss.models.StaffReassessmentValuesResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OxygenActivity extends AppCompatActivity {

    private static final String TAG = "OxygenStatus";
    private int patientId = -1;

    private TextView tvSpo2Value, tvTargetRange, tvDeviceName, tvDeviceFlow, tvObservation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oxygen);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Bind views
        tvSpo2Value = findViewById(R.id.tv_spo2_value);
        tvTargetRange = findViewById(R.id.tv_target_range);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceFlow = findViewById(R.id.tv_device_flow);
        tvObservation = findViewById(R.id.tv_observation);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Fetch patient data from DB
        fetchPatientData();

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatientData();
    }

    private void fetchPatientData() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientDetails(patientId).enqueue(new Callback<PatientDetailResponse>() {
            @Override
            public void onResponse(Call<PatientDetailResponse> call, Response<PatientDetailResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        PatientDetailResponse data = response.body();
                        populateUI(data);
                        // After loading patient details, overlay staff reassessment values
                        fetchAndApplyStaffValues(data);
                    } else {
                        Toast.makeText(OxygenActivity.this, "Failed to load oxygen status", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing patient data: " + e.getMessage(), e);
                }
            }

            @Override
            public void onFailure(Call<PatientDetailResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(OxygenActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(PatientDetailResponse data) {
        // SpO2 value
        String spo2 = data.getSpo2() != null ? data.getSpo2() : "--";
        if (!spo2.equals("--")) {
            tvSpo2Value.setText(spo2 + " %");
        } else {
            tvSpo2Value.setText("-- %");
        }

        // Target range
        String targetSpo2 = data.getTargetSpo2() != null ? data.getTargetSpo2() : "88-92";
        tvTargetRange.setText(targetSpo2 + "%");

        // Device
        String device = data.getDevice() != null && !data.getDevice().equals("--") ? data.getDevice() : "Not Set";
        tvDeviceName.setText(device);

        // Flow
        String flow = data.getFlow() != null && !data.getFlow().equals("--") ? String.valueOf(data.getFlow()) : "--";
        tvDeviceFlow.setText(flow);

        // Observation based on SpO2
        generateObservation(spo2, targetSpo2);
    }

    private void fetchAndApplyStaffValues(PatientDetailResponse initialData) {
        if (patientId == -1) return;

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getStaffReassessmentValues(patientId).enqueue(new Callback<StaffReassessmentValuesResponse>() {
            @Override
            public void onResponse(Call<StaffReassessmentValuesResponse> call, Response<StaffReassessmentValuesResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        StaffReassessmentValuesResponse data = response.body();
                        List<StaffReassessmentValuesResponse.StaffEntry> entries = data.getData();

                        if (entries != null && !entries.isEmpty()) {
                            StaffReassessmentValuesResponse.StaffEntry latest = entries.get(0);

                            double spo2 = latest.getSpo2();
                            if (spo2 > 0) {
                                tvSpo2Value.setText((int) spo2 + " %");
                                String targetSpo2 = initialData.getTargetSpo2() != null ? initialData.getTargetSpo2() : "88-92";
                                generateObservation(String.valueOf((int) spo2), targetSpo2);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing staff reassessment values: " + e.getMessage(), e);
                }
            }

            @Override
            public void onFailure(Call<StaffReassessmentValuesResponse> call, Throwable t) {
                Log.e(TAG, "Staff reassessment API failure: " + t.getMessage());
            }
        });
    }

    private void generateObservation(String spo2Str, String targetStr) {
        if (spo2Str.equals("--")) {
            tvObservation.setText("No SpO2 data available. Please check monitoring device.");
            return;
        }

        try {
            int spo2 = Integer.parseInt(spo2Str);

            // Parse target range
            int targetMin = 88, targetMax = 92;
            if (targetStr != null && targetStr.contains("-")) {
                String[] parts = targetStr.replace("%", "").split("-");
                targetMin = Integer.parseInt(parts[0].trim());
                targetMax = Integer.parseInt(parts[1].trim());
            }

            if (spo2 < targetMin) {
                tvObservation.setText("Patient is currently below target range. Consider increasing FiO2 or checking device fit.");
            } else if (spo2 > targetMax) {
                tvObservation.setText("Patient SpO2 is above target range. Consider decreasing flow to avoid oxygen toxicity in COPD.");
            } else {
                tvObservation.setText("Patient SpO2 is within target range. Continue current oxygen therapy settings.");
            }
        } catch (NumberFormatException e) {
            tvObservation.setText("Unable to determine observation. Check SpO2 reading.");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_patients);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DoctordashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_patients) {
                startActivity(new Intent(this, DoctorPatientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(this, DoctorAlertsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}