package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class GoldClassificationActivity extends AppCompatActivity {

    private MaterialCardView selectedCard = null;
    private Button btnNext;
    private MaterialCardView cardGold1, cardGold2, cardGold3, cardGold4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gold_classification);

        btnNext = findViewById(R.id.btn_next);
        btnNext.setEnabled(false);

        cardGold1 = findViewById(R.id.card_gold_1);
        cardGold2 = findViewById(R.id.card_gold_2);
        cardGold3 = findViewById(R.id.card_gold_3);
        cardGold4 = findViewById(R.id.card_gold_4);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        View.OnClickListener listener = v -> handleSelection((MaterialCardView) v);

        cardGold1.setOnClickListener(listener);
        cardGold2.setOnClickListener(listener);
        cardGold3.setOnClickListener(listener);
        cardGold4.setOnClickListener(listener);

        int patientId = getIntent().getIntExtra("patient_id", -1);

        btnNext.setOnClickListener(v -> {
            if (selectedCard == null) {
                android.widget.Toast.makeText(this, "Please select a GOLD stage", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String goldStage;
            if (selectedCard.getId() == R.id.card_gold_1) {
                goldStage = "GOLD 1";
            } else if (selectedCard.getId() == R.id.card_gold_2) {
                goldStage = "GOLD 2";
            } else if (selectedCard.getId() == R.id.card_gold_3) {
                goldStage = "GOLD 3";
            } else {
                goldStage = "GOLD 4";
            }

            btnNext.setEnabled(false);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("patient_id", patientId);
            body.put("gold_stage", goldStage);

            com.simats.cdss.network.ApiService apiService = com.simats.cdss.network.RetrofitClient.getClient(this).create(com.simats.cdss.network.ApiService.class);
            retrofit2.Call<com.simats.cdss.models.GenericResponse> call = apiService.addGoldClassification(body);

            call.enqueue(new retrofit2.Callback<com.simats.cdss.models.GenericResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, retrofit2.Response<com.simats.cdss.models.GenericResponse> response) {
                    btnNext.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        android.widget.Toast.makeText(GoldClassificationActivity.this, "GOLD classification saved successfully", android.widget.Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(GoldClassificationActivity.this, SpirometryDataActivity.class);
                        intent.putExtra("patient_id", patientId);
                        startActivity(intent);
                    } else {
                        android.widget.Toast.makeText(GoldClassificationActivity.this, "Failed to save GOLD classification", android.widget.Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.simats.cdss.models.GenericResponse> call, Throwable t) {
                    btnNext.setEnabled(true);
                    android.widget.Toast.makeText(GoldClassificationActivity.this, "Network error. Please try again.", android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });

        setupBottomNav();
    }

    private void handleSelection(MaterialCardView card) {
        if (selectedCard != null) {
            selectedCard.setStrokeWidth(0);
        }

        selectedCard = card;
        selectedCard.setStrokeWidth(4);
        selectedCard.setStrokeColor(getResources().getColor(R.color.primary_teal));

        btnNext.setEnabled(true);
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