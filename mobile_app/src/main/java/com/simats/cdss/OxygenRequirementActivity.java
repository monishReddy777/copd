package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.PatientDetailResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OxygenRequirementActivity extends AppCompatActivity {

    private int patientId = -1;
    private double spo2Value = 0.0;
    private String hypoxemiaLevel = "";
    private String symptomsLevel = "";
    private String oxygenRequired = "";

    private TextView tvSpo2Value, tvHypoxemiaLevel, tvSymptomsLevel;
    private TextView tvTherapyIndicated, tvTherapyDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oxygen_requirement);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Initialize UI references
        tvSpo2Value = findViewById(R.id.tv_spo2_value);
        tvHypoxemiaLevel = findViewById(R.id.tv_hypoxemia_level);
        tvSymptomsLevel = findViewById(R.id.tv_symptoms_level);
        tvTherapyIndicated = findViewById(R.id.tv_therapy_indicated);
        tvTherapyDesc = findViewById(R.id.tv_therapy_desc);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Fetch SpO2 from patient vitals via existing patient detail API
        fetchSpo2FromVitals();

        findViewById(R.id.btn_proceed).setOnClickListener(v -> {
            if (patientId == -1) {
                Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hypoxemiaLevel.isEmpty()) {
                Toast.makeText(this, "Evaluation not ready yet", Toast.LENGTH_SHORT).show();
                return;
            }
            saveOxygenRequirement();
        });

        setupBottomNav();
    }

    private void fetchSpo2FromVitals() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientDetails(patientId).enqueue(new Callback<PatientDetailResponse>() {
            @Override
            public void onResponse(Call<PatientDetailResponse> call, Response<PatientDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PatientDetailResponse details = response.body();
                    String spo2Str = details.getSpo2();

                    try {
                        spo2Value = Double.parseDouble(spo2Str);
                    } catch (Exception e) {
                        spo2Value = 0.0;
                    }

                    evaluateAndPopulateUI();
                } else {
                    Toast.makeText(OxygenRequirementActivity.this, "Failed to fetch patient data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PatientDetailResponse> call, Throwable t) {
                Toast.makeText(OxygenRequirementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void evaluateAndPopulateUI() {
        // COPD target SpO2: 88-92%
        // Determine hypoxemia level and symptoms based on COPD thresholds
        if (spo2Value < 85) {
            hypoxemiaLevel = "Severe";
            symptomsLevel = "Severe";
            oxygenRequired = "Yes";
        } else if (spo2Value < 88) {
            hypoxemiaLevel = "Moderate";
            symptomsLevel = "Moderate";
            oxygenRequired = "Yes";
        } else if (spo2Value <= 92) {
            // On target for COPD — no additional O2 needed
            hypoxemiaLevel = "None";
            symptomsLevel = "Mild";
            oxygenRequired = "OnTarget";
        } else {
            // >92% — risk of CO2 retention in COPD
            hypoxemiaLevel = "None";
            symptomsLevel = "Mild";
            oxygenRequired = "Reduce";
        }

        // Update UI
        tvSpo2Value.setText((int) spo2Value + " %");
        tvHypoxemiaLevel.setText(hypoxemiaLevel);
        tvSymptomsLevel.setText(symptomsLevel);

        if ("Yes".equals(oxygenRequired)) {
            tvTherapyIndicated.setText("Oxygen Therapy Indicated");
            tvTherapyDesc.setText("Patient meets criteria for supplemental oxygen therapy due to persistent hypoxemia (SpO\u2082 < 88%).");
        } else if ("OnTarget".equals(oxygenRequired)) {
            tvTherapyIndicated.setText("SpO\u2082 Within Target Range");
            tvTherapyDesc.setText("Patient SpO\u2082 is within COPD target (88\u201392%). Continue current oxygen therapy and monitor.");
        } else if ("Reduce".equals(oxygenRequired)) {
            tvTherapyIndicated.setText("Reduce Oxygen Flow");
            tvTherapyDesc.setText("Patient SpO\u2082 is above target (>92%). Risk of CO\u2082 retention. Consider reducing oxygen flow rate.");
        } else {
            tvTherapyIndicated.setText("Assessment Pending");
            tvTherapyDesc.setText("Unable to determine oxygen requirement. Please reassess patient.");
        }
    }

    private void saveOxygenRequirement() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("spo2", spo2Value);
        body.put("hypoxemia_level", hypoxemiaLevel);
        body.put("symptoms_level", symptomsLevel);
        body.put("oxygen_required", oxygenRequired);

        apiService.setOxygenRequirement(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(OxygenRequirementActivity.this, "Oxygen requirement saved successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OxygenRequirementActivity.this, DeviceSelectionActivity.class);
                    intent.putExtra("patient_id", patientId);
                    startActivity(intent);
                } else {
                    Toast.makeText(OxygenRequirementActivity.this, "Failed to save oxygen requirement", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(OxygenRequirementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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