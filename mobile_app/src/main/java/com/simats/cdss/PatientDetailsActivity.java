package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.PatientDetailResponse;
import com.simats.cdss.models.StaffReassessmentValuesResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PatientDetails";

    private int patientId;

    private TextView tvPatientName, tvPatientInfo, tvPatientStatus;
    private TextView tvSpo2Value, tvDeviceValue, tvFlowValue;
    private TextView tvHeartRateValue, tvRespRateValue;
    private TextView tvDiagnosis, tvTargetSpo2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Bind views
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvPatientInfo = findViewById(R.id.tv_patient_info);
        tvPatientStatus = findViewById(R.id.tv_patient_status);
        tvSpo2Value = findViewById(R.id.tv_spo2_value);
        tvDeviceValue = findViewById(R.id.tv_device_value);
        tvFlowValue = findViewById(R.id.tv_flow_value);
        tvHeartRateValue = findViewById(R.id.tv_heart_rate_value);
        tvRespRateValue = findViewById(R.id.tv_resp_rate_value);
        tvDiagnosis = findViewById(R.id.tv_diagnosis);
        tvTargetSpo2 = findViewById(R.id.tv_target_spo2);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Three-dots menu with Remove Patient option
        findViewById(R.id.iv_more).setOnClickListener(v -> showPopupMenu(v));

        // Handle click on SpO2 gauge card to move to OxygenActivity
        findViewById(R.id.card_gauge).setOnClickListener(v -> {
            Intent intent = new Intent(this, OxygenActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        // Handle click on "Run AI Risk Analysis" button
        findViewById(R.id.btn_run_ai).setOnClickListener(v -> {
            Intent intent = new Intent(this, AIAnalysisActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        // Handle click on "View ABG Trends" button
        findViewById(R.id.btn_view_trends).setOnClickListener(v -> {
            Intent intent = new Intent(this, ABGTrendsActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        // Handle click on "Escalation Criteria" button
        findViewById(R.id.btn_escalation).setOnClickListener(v -> {
            Intent intent = new Intent(this, EscalationCriteriaActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        // Fetch patient details from API
        fetchPatientDetails();

        setupBottomNav();
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Remove Patient");
        popup.setOnMenuItemClickListener(item -> {
            if ("Remove Patient".equals(item.getTitle())) {
                confirmRemovePatient();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmRemovePatient() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Patient")
                .setMessage("Are you sure you want to remove this patient? This action cannot be undone and all patient data will be permanently deleted.")
                .setPositiveButton("Remove", (dialog, which) -> deletePatient())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePatient() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.deletePatient(patientId).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PatientDetailsActivity.this, "Patient removed successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to patient list
                } else {
                    Toast.makeText(PatientDetailsActivity.this, "Failed to remove patient", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "Error deleting patient", t);
                Toast.makeText(PatientDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh patient details every time doctor returns to this screen
        fetchPatientDetails();
    }

    private void fetchPatientDetails() {
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
                        populateUI(response.body());
                        // After loading patient details, overlay staff reassessment values
                        fetchAndApplyStaffValues();
                    } else {
                        Toast.makeText(PatientDetailsActivity.this, "Failed to load patient details", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing patient details: " + e.getMessage(), e);
                    Toast.makeText(PatientDetailsActivity.this, "Error displaying patient details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PatientDetailResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(PatientDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches the latest staff-entered reassessment values and updates
     * the main SpO2, Heart Rate, and Resp Rate cards directly.
     * This replaces the old "Reassessment values entered by staff" section.
     */
    private void fetchAndApplyStaffValues() {
        if (patientId == -1) return;

        Log.d(TAG, "Fetching staff reassessment values for patient_id=" + patientId);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getStaffReassessmentValues(patientId).enqueue(new Callback<StaffReassessmentValuesResponse>() {
            @Override
            public void onResponse(Call<StaffReassessmentValuesResponse> call, Response<StaffReassessmentValuesResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        StaffReassessmentValuesResponse data = response.body();
                        List<StaffReassessmentValuesResponse.StaffEntry> entries = data.getData();

                        if (entries != null && !entries.isEmpty()) {
                            // Get the latest entry (first one, ordered by -created_at)
                            StaffReassessmentValuesResponse.StaffEntry latest = entries.get(0);

                            Log.d(TAG, "Updating vitals with staff reassessment: spo2="
                                    + latest.getSpo2() + " rr=" + latest.getRespiratoryRate()
                                    + " hr=" + latest.getHeartRate());

                            // Update SpO2 card
                            double spo2 = latest.getSpo2();
                            if (spo2 > 0) {
                                tvSpo2Value.setText((int) spo2 + " %");
                            }

                            // Update Respiratory Rate card
                            double rr = latest.getRespiratoryRate();
                            if (rr > 0) {
                                tvRespRateValue.setText((int) rr + " /min");
                            }

                            // Update Heart Rate card (nullable Double — must null-check)
                            Double hr = latest.getHeartRate();
                            if (hr != null && hr > 0) {
                                tvHeartRateValue.setText(hr.intValue() + " bpm");
                            }

                            // Re-evaluate status based on updated SpO2
                            if (spo2 > 0) {
                                updateStatusChip(spo2);
                            }
                        }
                    } else {
                        Log.e(TAG, "Staff reassessment API error: " + response.code());
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

    private void updateStatusChip(double spo2) {
        // < 85 = Critical, 85-87 = Warning, >= 88 = Stable
        String newStatus;
        if (spo2 < 85) {
            newStatus = "CRITICAL";
        } else if (spo2 < 88) {
            newStatus = "WARNING";
        } else {
            newStatus = "STABLE";
        }
        tvPatientStatus.setText(newStatus);
        if (newStatus.equals("CRITICAL")) {
            tvPatientStatus.setTextColor(Color.parseColor("#EF4444"));
            tvPatientStatus.setBackgroundResource(R.drawable.chip_red_rounded);
            tvPatientStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
        } else if (newStatus.equals("WARNING")) {
            tvPatientStatus.setTextColor(Color.parseColor("#854D0E"));
            tvPatientStatus.setBackgroundResource(R.drawable.chip_orange_rounded);
            tvPatientStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
        } else {
            tvPatientStatus.setTextColor(Color.parseColor("#166534"));
            tvPatientStatus.setBackgroundResource(R.drawable.chip_green_rounded);
            tvPatientStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
        }
    }

    private void populateUI(PatientDetailResponse data) {
        // Patient Name
        tvPatientName.setText(data.getName() != null ? data.getName() : "Unknown");

        // Patient Info (Age, Gender, Room)
        String age = data.getAge() != null ? String.valueOf(data.getAge()) : "--";
        String gender = data.getGender() != null ? data.getGender() : "--";
        String room = data.getRoomNo() != null ? data.getRoomNo() : "--";
        tvPatientInfo.setText(age + " yrs • " + gender + " • Room " + room);

        // Diagnosis
        String diagnosisStr = data.getDiagnosis() != null ? data.getDiagnosis() : "Unknown";
        tvDiagnosis.setText("Diagnosis: " + diagnosisStr);

        // Status chip
        String status = data.getStatus();
        if (status != null) {
            tvPatientStatus.setText(status.toUpperCase());
            if (status.equalsIgnoreCase("critical")) {
                tvPatientStatus.setTextColor(Color.parseColor("#EF4444"));
                tvPatientStatus.setBackgroundResource(R.drawable.chip_red_rounded);
                tvPatientStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
            } else if (status.equalsIgnoreCase("warning")) {
                tvPatientStatus.setTextColor(Color.parseColor("#854D0E"));
                tvPatientStatus.setBackgroundResource(R.drawable.chip_orange_rounded);
                tvPatientStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
            } else {
                tvPatientStatus.setTextColor(Color.parseColor("#166534"));
                tvPatientStatus.setBackgroundResource(R.drawable.chip_green_rounded);
                tvPatientStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
            }
        }

        // SpO2 gauge
        String spo2 = data.getSpo2() != null ? data.getSpo2() : "--";
        if (!spo2.equals("--")) {
            tvSpo2Value.setText(spo2 + " %");
        } else {
            tvSpo2Value.setText("-- %");
        }

        // Target SpO2
        String targetSpo2 = data.getTargetSpo2() != null ? data.getTargetSpo2() : "88-92";
        tvTargetSpo2.setText("Target: " + targetSpo2 + "%");

        // Device & Flow
        tvDeviceValue.setText(data.getDevice() != null ? data.getDevice() : "--");
        tvFlowValue.setText(data.getFlow() != null ? data.getFlow() : "--");

        // Heart Rate
        String hr = data.getHeartRate() != null ? data.getHeartRate() : "--";
        if (!hr.equals("--")) {
            tvHeartRateValue.setText(hr + " bpm");
        } else {
            tvHeartRateValue.setText("-- bpm");
        }

        // Respiratory Rate
        String rr = data.getRespiratoryRate() != null ? data.getRespiratoryRate() : "--";
        if (!rr.equals("--")) {
            tvRespRateValue.setText(rr + " /min");
        } else {
            tvRespRateValue.setText("-- /min");
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
