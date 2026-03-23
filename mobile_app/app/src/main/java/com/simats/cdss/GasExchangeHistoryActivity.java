package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class GasExchangeHistoryActivity extends AppCompatActivity {

    private MaterialCardView cardHypoYes, cardHypoNo, cardHypoUnknown;
    private MaterialCardView cardOxyYes, cardOxyNo;
    private MaterialCardView selectedHypo = null;
    private MaterialCardView selectedOxy = null;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gas_exchange_history);

        cardHypoYes = findViewById(R.id.card_hypoxemia_yes);
        cardHypoNo = findViewById(R.id.card_hypoxemia_no);
        cardHypoUnknown = findViewById(R.id.card_hypoxemia_unknown);
        
        cardOxyYes = findViewById(R.id.card_oxygen_yes);
        cardOxyNo = findViewById(R.id.card_oxygen_no);
        
        btnNext = findViewById(R.id.btn_next);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        cardHypoYes.setOnClickListener(v -> handleHypoSelection(cardHypoYes));
        cardHypoNo.setOnClickListener(v -> handleHypoSelection(cardHypoNo));
        cardHypoUnknown.setOnClickListener(v -> handleHypoSelection(cardHypoUnknown));

        cardOxyYes.setOnClickListener(v -> handleOxySelection(cardOxyYes));
        cardOxyNo.setOnClickListener(v -> handleOxySelection(cardOxyNo));

        int patientId = getIntent().getIntExtra("patient_id", -1);

        btnNext.setOnClickListener(v -> {
            if (selectedHypo == null || selectedOxy == null) {
                android.widget.Toast.makeText(this, "Please select options for both questions", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String chronicHypoxemia = "Unknown";
            if (selectedHypo.getId() == R.id.card_hypoxemia_yes) {
                chronicHypoxemia = "Yes";
            } else if (selectedHypo.getId() == R.id.card_hypoxemia_no) {
                chronicHypoxemia = "No";
            } else if (selectedHypo.getId() == R.id.card_hypoxemia_unknown) {
                chronicHypoxemia = "Unknown";
            }

            String homeOxygenUse = "No";
            if (selectedOxy.getId() == R.id.card_oxygen_yes) {
                homeOxygenUse = "Yes";
            } else if (selectedOxy.getId() == R.id.card_oxygen_no) {
                homeOxygenUse = "No";
            }

            btnNext.setEnabled(false);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("patient_id", patientId);
            body.put("chronic_hypoxemia", chronicHypoxemia);
            body.put("home_oxygen_use", homeOxygenUse);

            com.simats.cdss.network.ApiService apiService = com.simats.cdss.network.RetrofitClient.getClient(this).create(com.simats.cdss.network.ApiService.class);
            retrofit2.Call<com.simats.cdss.models.GenericResponse> call = apiService.addGasExchangeHistory(body);

            call.enqueue(new retrofit2.Callback<com.simats.cdss.models.GenericResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, retrofit2.Response<com.simats.cdss.models.GenericResponse> response) {
                    btnNext.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        android.widget.Toast.makeText(GasExchangeHistoryActivity.this, "Gas exchange history saved successfully", android.widget.Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(GasExchangeHistoryActivity.this, CurrentSymptomsActivity.class);
                        intent.putExtra("patient_id", patientId);
                        startActivity(intent);
                    } else {
                        android.widget.Toast.makeText(GasExchangeHistoryActivity.this, "Failed to save gas exchange history", android.widget.Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, Throwable t) {
                    btnNext.setEnabled(true);
                    android.widget.Toast.makeText(GasExchangeHistoryActivity.this, "Network error. Please try again.", android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });

        setupBottomNav();
    }

    private void handleHypoSelection(MaterialCardView card) {
        if (selectedHypo != null) {
            selectedHypo.setStrokeWidth(0);
        }
        selectedHypo = card;
        selectedHypo.setStrokeWidth(4);
        selectedHypo.setStrokeColor(getResources().getColor(R.color.primary_teal));
        checkNextButton();
    }

    private void handleOxySelection(MaterialCardView card) {
        if (selectedOxy != null) {
            selectedOxy.setStrokeWidth(0);
        }
        selectedOxy = card;
        selectedOxy.setStrokeWidth(4);
        selectedOxy.setStrokeColor(getResources().getColor(R.color.primary_teal));
        checkNextButton();
    }

    private void checkNextButton() {
        boolean isReady = selectedHypo != null && selectedOxy != null;
        btnNext.setEnabled(isReady);
        btnNext.setAlpha(isReady ? 1.0f : 0.5f);
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