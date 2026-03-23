package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.VitalsRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VitalsActivity extends AppCompatActivity {

    private EditText etSpo2, etRespRate, etHeartRate, etTemperature, etBp;
    private Button btnNext;
    private TextView tvPatientName, tvLastRecorded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals);

        etSpo2 = findViewById(R.id.et_spo2);
        etRespRate = findViewById(R.id.et_resp_rate);
        etHeartRate = findViewById(R.id.et_heart_rate);
        etTemperature = findViewById(R.id.et_temperature);
        etBp = findViewById(R.id.et_bp);
        btnNext = findViewById(R.id.btn_next);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvLastRecorded = findViewById(R.id.tv_last_recorded);

        // Set patient info from intent extras
        String patientName = getIntent().getStringExtra("patient_name");
        String bedNo = getIntent().getStringExtra("bed_no");
        String wardNo = getIntent().getStringExtra("ward_no");
        if (patientName != null && !patientName.isEmpty()) {
            StringBuilder info = new StringBuilder(patientName);
            if (bedNo != null && !bedNo.isEmpty()) {
                info.append(" (Bed ").append(bedNo);
                if (wardNo != null && !wardNo.isEmpty()) {
                    info.append(" • Ward ").append(wardNo);
                }
                info.append(")");
            }
            tvPatientName.setText(info.toString());
        }

        boolean isReassessment = getIntent().getBooleanExtra("is_reassessment", false);
        if (isReassessment) {
            tvLastRecorded.setText("Reassessment entry");
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etSpo2.addTextChangedListener(watcher);
        etRespRate.addTextChangedListener(watcher);
        etHeartRate.addTextChangedListener(watcher);
        etTemperature.addTextChangedListener(watcher);
        etBp.addTextChangedListener(watcher);

        // Initially disable button
        btnNext.setEnabled(false);
        btnNext.setAlpha(0.5f);

        btnNext.setOnClickListener(v -> {
            submitVitals();
        });

        setupBottomNav();
    }

    private void submitVitals() {
        try {
            double spo2 = Double.parseDouble(etSpo2.getText().toString().trim());
            int respRate = Integer.parseInt(etRespRate.getText().toString().trim());
            int heartRate = Integer.parseInt(etHeartRate.getText().toString().trim());
            double temp = Double.parseDouble(etTemperature.getText().toString().trim());
            String bp = etBp.getText().toString().trim();

            int patientId = getIntent().getIntExtra("patient_id", -1);

            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("patient_id", patientId);
            request.put("spo2", spo2);
            request.put("respiratory_rate", respRate);
            request.put("heart_rate", heartRate);
            request.put("temperature", temp);
            request.put("blood_pressure", bp);

            btnNext.setEnabled(false);
            btnNext.setText("Submitting...");

            boolean isUpdate = getIntent().getBooleanExtra("is_update", false);
            Call<GenericResponse> call;
            if (isUpdate) {
                call = api.updateStaffVitals(patientId, request);
            } else {
                call = api.addVitalsData(request);
            }

            call.enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    btnNext.setEnabled(true);
                    btnNext.setText("NEXT");
                    if (response.isSuccessful()) {
                        Toast.makeText(VitalsActivity.this, "Vitals saved. AI processing...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(VitalsActivity.this, ABGEntryActivity.class);
                        intent.putExtra("patient_id", patientId);
                        intent.putExtra("is_update", isUpdate);
                        // Forward reassessment details if coming from alert
                        int reassessmentId = getIntent().getIntExtra("reassessment_id", -1);
                        if (reassessmentId != -1) {
                            intent.putExtra("reassessment_id", reassessmentId);
                            intent.putExtra("is_reassessment", true);
                        }
                        intent.putExtra("patient_name", getIntent().getStringExtra("patient_name"));
                        intent.putExtra("bed_no", getIntent().getStringExtra("bed_no"));
                        intent.putExtra("ward_no", getIntent().getStringExtra("ward_no"));
                        startActivity(intent);
                    } else {
                        Toast.makeText(VitalsActivity.this, "Failed to save vitals: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                    btnNext.setEnabled(true);
                    btnNext.setText("NEXT");
                    Toast.makeText(VitalsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkValidation() {
        boolean isValid = !etSpo2.getText().toString().trim().isEmpty() &&
                         !etRespRate.getText().toString().trim().isEmpty() &&
                         !etHeartRate.getText().toString().trim().isEmpty() &&
                         !etTemperature.getText().toString().trim().isEmpty() &&
                         !etBp.getText().toString().trim().isEmpty();
        
        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                // Return to Staff Dashboard for any bottom navigation click
                Intent intent = new Intent(this, StaffDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            });
        }
    }
}