package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReassessmentChecklistActivity extends AppCompatActivity {

    private static final String TAG = "ReassessmentChecklist";

    private int patientId = -1;
    private int reassessmentId = -1;
    private String patientName = "";
    private String reassessmentType = "SpO2";

    private CheckBox cbSpo2, cbRespiratoryRate, cbConsciousness, cbDeviceFit, cbRepeatAbg;
    private TextView tvPatientHeader, tvReassessmentType;
    private Button btnComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassessment_checklist);

        // Get data from intent
        patientId = getIntent().getIntExtra("patient_id", -1);
        reassessmentId = getIntent().getIntExtra("reassessment_id", -1);
        patientName = getIntent().getStringExtra("patient_name");
        reassessmentType = getIntent().getStringExtra("reassessment_type");
        String bedNo = getIntent().getStringExtra("bed_no");
        String wardNo = getIntent().getStringExtra("ward_no");

        if (reassessmentType == null || reassessmentType.isEmpty()) {
            reassessmentType = "SpO2";
        }
        if (patientName == null) patientName = "";

        Log.d(TAG, "Opened with patient_id=" + patientId
                + " reassessment_id=" + reassessmentId
                + " type=" + reassessmentType);

        // Bind views
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        cbSpo2 = findViewById(R.id.cb_spo2);
        cbRespiratoryRate = findViewById(R.id.cb_respiratory_rate);
        cbConsciousness = findViewById(R.id.cb_consciousness);
        cbDeviceFit = findViewById(R.id.cb_device_fit);
        cbRepeatAbg = findViewById(R.id.cb_repeat_abg);
        btnComplete = findViewById(R.id.btn_complete);

        // Pre-fill patient info header if available
        tvPatientHeader = findViewById(R.id.tv_patient_header);
        tvReassessmentType = findViewById(R.id.tv_reassessment_type);

        if (tvPatientHeader != null && !patientName.isEmpty()) {
            StringBuilder header = new StringBuilder(patientName);
            if (bedNo != null && !bedNo.isEmpty()) {
                header.append(" • Bed ").append(bedNo);
            }
            if (wardNo != null && !wardNo.isEmpty()) {
                header.append(" • Ward ").append(wardNo);
            }
            tvPatientHeader.setText(header.toString());
            tvPatientHeader.setVisibility(View.VISIBLE);
            // Show the card
            findViewById(R.id.card_patient_info).setVisibility(View.VISIBLE);
        }

        if (tvReassessmentType != null) {
            tvReassessmentType.setText("Reassessment");
            tvReassessmentType.setVisibility(View.VISIBLE);
        }

        btnComplete.setOnClickListener(v -> completeReassessment());

        setupBottomNav();
    }

    private void completeReassessment() {
        // Validate: at least one checkbox should be checked
        boolean spo2Checked = cbSpo2.isChecked();
        boolean rrChecked = cbRespiratoryRate.isChecked();
        boolean consciousnessChecked = cbConsciousness.isChecked();
        boolean deviceFitChecked = cbDeviceFit.isChecked();
        boolean abgChecked = cbRepeatAbg.isChecked();

        if (!spo2Checked && !rrChecked && !consciousnessChecked && !deviceFitChecked && !abgChecked) {
            Toast.makeText(this, "Please complete at least one check", Toast.LENGTH_SHORT).show();
            return;
        }

        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build request body for staff_checklist API
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("check_spo2", spo2Checked);
        body.put("check_respiratory_rate", rrChecked);
        body.put("check_consciousness", consciousnessChecked);
        body.put("check_device_fit", deviceFitChecked);
        body.put("check_repeat_abg", abgChecked);

        // Build remarks from checked items
        StringBuilder remarks = new StringBuilder("Checklist completed: ");
        if (spo2Checked) remarks.append("SpO2, ");
        if (rrChecked) remarks.append("Respiratory Rate, ");
        if (consciousnessChecked) remarks.append("Consciousness/Sensorium, ");
        if (deviceFitChecked) remarks.append("Device Fit & Position, ");
        if (abgChecked) remarks.append("Repeat ABG, ");
        // Remove trailing comma
        String remarksStr = remarks.toString();
        if (remarksStr.endsWith(", ")) {
            remarksStr = remarksStr.substring(0, remarksStr.length() - 2);
        }
        body.put("remarks", remarksStr);

        // If we have a reassessment_id, include it so the backend auto-completes the schedule
        if (reassessmentId != -1) {
            body.put("reassessment_id", reassessmentId);
        }

        Log.d(TAG, "API_REQUEST body: " + body.toString());

        // Prevent duplicate submissions
        btnComplete.setEnabled(false);
        btnComplete.setText("Saving...");

        // Send POST request to staff-checklist endpoint
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.saveStaffChecklist(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                Log.d(TAG, "API_RESPONSE code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API_RESPONSE body: " + response.body().getMessage());
                    Toast.makeText(ReassessmentChecklistActivity.this,
                            "Reassessment completed successfully!", Toast.LENGTH_SHORT).show();
                    showSuccessBottomSheet();
                } else {
                    Log.e(TAG, "API_RESPONSE error: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body");
                    }
                    Toast.makeText(ReassessmentChecklistActivity.this,
                            "Failed to save reassessment", Toast.LENGTH_SHORT).show();
                    // Re-enable button on failure
                    btnComplete.setEnabled(true);
                    btnComplete.setText("Complete Reassessment");
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "API_FAILURE: " + t.getMessage());
                Toast.makeText(ReassessmentChecklistActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Re-enable button on failure
                btnComplete.setEnabled(true);
                btnComplete.setText("Complete Reassessment");
            }
        });
    }

    private void showSuccessBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_reassessment_success, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);

        bottomSheetView.findViewById(R.id.btn_done).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            SessionManager session = new SessionManager(this);
            String role = session.getRole();
            Intent intent;
            if ("staff".equals(role)) {
                intent = new Intent(this, StaffDashboardActivity.class);
            } else {
                intent = new Intent(this, DoctordashboardActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        bottomSheetDialog.show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                SessionManager session = new SessionManager(this);
                String role = session.getRole();

                if (itemId == R.id.nav_home) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctordashboardActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffPatientsActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctorPatientsActivity.class));
                    }
                    finish();
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffAlertsActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctorAlertsActivity.class));
                    }
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
}