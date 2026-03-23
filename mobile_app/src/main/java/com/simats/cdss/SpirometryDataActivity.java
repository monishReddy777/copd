package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SpirometryDataActivity extends AppCompatActivity {

    private EditText etFev1, etFev1Fvc;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spirometry_data);

        etFev1 = findViewById(R.id.et_fev1);
        etFev1Fvc = findViewById(R.id.et_fev1_fvc);
        btnNext = findViewById(R.id.btn_next);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Real-time validation to enable the "Next" button
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

        etFev1.addTextChangedListener(watcher);
        etFev1Fvc.addTextChangedListener(watcher);

        int patientId = getIntent().getIntExtra("patient_id", -1);

        // EXPLICIT Navigation to Gas Exchange History Screen via API call
        btnNext.setOnClickListener(v -> {
            String fev1PercentStr = etFev1.getText().toString().trim();
            String fev1FvcRatioStr = etFev1Fvc.getText().toString().trim();

            if (fev1PercentStr.isEmpty() || fev1FvcRatioStr.isEmpty()) {
                android.widget.Toast.makeText(this, "Please fill in all spirometry values", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                float fev1Percent = Float.parseFloat(fev1PercentStr);
                float fev1FvcRatio = Float.parseFloat(fev1FvcRatioStr);

                btnNext.setEnabled(false);

                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("patient_id", patientId);
                body.put("fev1_percent", fev1Percent);
                body.put("fev1_fvc_ratio", fev1FvcRatio);

                com.simats.cdss.network.ApiService apiService = com.simats.cdss.network.RetrofitClient.getClient(this).create(com.simats.cdss.network.ApiService.class);
                retrofit2.Call<com.simats.cdss.models.GenericResponse> call = apiService.addSpirometryData(body);

                call.enqueue(new retrofit2.Callback<com.simats.cdss.models.GenericResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, retrofit2.Response<com.simats.cdss.models.GenericResponse> response) {
                        btnNext.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            android.widget.Toast.makeText(SpirometryDataActivity.this, "Spirometry data saved successfully", android.widget.Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SpirometryDataActivity.this, GasExchangeHistoryActivity.class);
                            intent.putExtra("patient_id", patientId);
                            startActivity(intent);
                        } else {
                            android.widget.Toast.makeText(SpirometryDataActivity.this, "Failed to save spirometry data", android.widget.Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, Throwable t) {
                        btnNext.setEnabled(true);
                        android.widget.Toast.makeText(SpirometryDataActivity.this, "Network error. Please try again.", android.widget.Toast.LENGTH_LONG).show();
                    }
                });

            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(this, "Please enter valid numeric values", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNav();
    }

    private void checkValidation() {
        boolean isValid = !etFev1.getText().toString().trim().isEmpty() &&
                         !etFev1Fvc.getText().toString().trim().isEmpty();
        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, StaffDashboardActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    startActivity(new Intent(this, PatientListActivity.class));
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
}