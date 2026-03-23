package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.TherapyPlanResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TherapyRecommendationActivity extends AppCompatActivity {

    private int patientId = -1;
    private TextView tvDeviceName, tvFio2Setting, tvTargetSpo2, tvNextAbg, tvRationale;

    // Doctor's selected device (passed from review screen)
    private String doctorSelectedDevice = "";
    private String doctorSelectedFlowRange = "";
    private boolean isOverride = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapy_recommendation);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Read doctor's device selection
        String selDev = getIntent().getStringExtra("selected_device");
        String selFlow = getIntent().getStringExtra("selected_flow_range");
        doctorSelectedDevice = selDev != null ? selDev : "";
        doctorSelectedFlowRange = selFlow != null ? selFlow : "";
        isOverride = getIntent().getBooleanExtra("is_override", false);

        tvDeviceName = findViewById(R.id.tv_device_name);
        tvFio2Setting = findViewById(R.id.tv_fio2_setting);
        tvTargetSpo2 = findViewById(R.id.tv_target_spo2);
        tvNextAbg = findViewById(R.id.tv_next_abg);
        tvRationale = findViewById(R.id.tv_rationale);

        // Update card label if doctor overrode
        TextView tvCardLabel = findViewById(R.id.tv_card_label);
        if (tvCardLabel != null && isOverride) {
            tvCardLabel.setText("Doctor Selected Therapy");
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_accept).setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleReassessmentActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        setupBottomNav();
        fetchTherapyPlan();
    }

    private void fetchTherapyPlan() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getTherapyPlan(patientId).enqueue(new Callback<TherapyPlanResponse>() {
            @Override
            public void onResponse(Call<TherapyPlanResponse> call, Response<TherapyPlanResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TherapyPlanResponse data = response.body();
                    if (data.isHasData()) {
                        populateUI(data);
                    } else {
                        tvDeviceName.setText("No recommendation data available");
                        tvFio2Setting.setText("");
                        tvTargetSpo2.setText("--");
                        tvNextAbg.setText("--");
                        tvRationale.setText("No clinical data available for this patient.");
                    }
                } else {
                    Toast.makeText(TherapyRecommendationActivity.this, "Failed to load therapy plan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TherapyPlanResponse> call, Throwable t) {
                Toast.makeText(TherapyRecommendationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(TherapyPlanResponse data) {
        // If doctor overrode, show doctor's selected device; otherwise show backend result
        if (isOverride && !doctorSelectedDevice.isEmpty()) {
            tvDeviceName.setText(doctorSelectedDevice);
            tvFio2Setting.setText(doctorSelectedFlowRange + " FiO\u2082 @ " + data.getFlowRate());
        } else {
            tvDeviceName.setText(data.getDevice());
            tvFio2Setting.setText(data.getFio2() + " FiO\u2082 @ " + data.getFlowRate());
        }
        tvTargetSpo2.setText(data.getTargetSpo2());
        tvNextAbg.setText(data.getNextAbgTime());
        tvRationale.setText(data.getRationale());
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_patients);
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
