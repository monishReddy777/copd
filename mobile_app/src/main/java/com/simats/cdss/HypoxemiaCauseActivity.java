package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HypoxemiaCauseActivity extends AppCompatActivity {

    private MaterialCardView selectedVerticalCard = null;
    private Button btnNext;
    private MaterialCardView cardVQ, cardHypo, cardDiff, cardShunt, cardUnknown;
    private ImageView ivCheckVQ, ivCheckHypo, ivCheckDiff, ivCheckShunt, ivCheckUnknown;

    private int patientId = -1;
    private String selectedCause = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hypoxemia_cause);

        btnNext = findViewById(R.id.btn_next);
        
        // Initialize Cards
        cardVQ = findViewById(R.id.card_vq_mismatch);
        cardHypo = findViewById(R.id.card_hypoventilation);
        cardDiff = findViewById(R.id.card_diffusion);
        cardShunt = findViewById(R.id.card_shunt);
        cardUnknown = findViewById(R.id.card_unknown);

        // Initialize Checkmarks
        ivCheckVQ = findViewById(R.id.iv_check_vq);
        ivCheckHypo = findViewById(R.id.iv_check_hypo);
        ivCheckDiff = findViewById(R.id.iv_check_diff);
        ivCheckShunt = findViewById(R.id.iv_check_shunt);
        ivCheckUnknown = findViewById(R.id.iv_check_unknown);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        patientId = getIntent().getIntExtra("patient_id", -1);

        setupSelectionListeners();
        
        btnNext.setOnClickListener(v -> {
            if (patientId == -1 || selectedCause.isEmpty()) {
                Toast.makeText(this, "Please select a cause", Toast.LENGTH_SHORT).show();
                return;
            }
            saveCauseToBackend(patientId, selectedCause);
        });

        setupBottomNav();
    }

    private void setupSelectionListeners() {
        View.OnClickListener listener = v -> {
            MaterialCardView clickedCard = (MaterialCardView) v;
            handleSelection(clickedCard);
        };

        cardVQ.setOnClickListener(listener);
        cardHypo.setOnClickListener(listener);
        cardDiff.setOnClickListener(listener);
        cardShunt.setOnClickListener(listener);
        cardUnknown.setOnClickListener(listener);
    }

    private void handleSelection(MaterialCardView card) {
        // Reset previous selection
        if (selectedVerticalCard != null) {
            resetCardStyle(selectedVerticalCard);
        }

        // Apply new selection
        selectedVerticalCard = card;
        applySelectedStyle(selectedVerticalCard);
        
        // Enable Next Button
        btnNext.setEnabled(true);
    }

    private void applySelectedStyle(MaterialCardView card) {
        card.setStrokeWidth(4);
        card.setStrokeColor(getResources().getColor(R.color.primary_teal));
        
        // Show respective checkmark
        if (card.getId() == R.id.card_vq_mismatch) {
            ivCheckVQ.setVisibility(View.VISIBLE);
            selectedCause = "V/Q Mismatch";
        } else if (card.getId() == R.id.card_hypoventilation) {
            ivCheckHypo.setVisibility(View.VISIBLE);
            selectedCause = "Alveolar Hypoventilation";
        } else if (card.getId() == R.id.card_diffusion) {
            ivCheckDiff.setVisibility(View.VISIBLE);
            selectedCause = "Diffusion Impairment";
        } else if (card.getId() == R.id.card_shunt) {
            ivCheckShunt.setVisibility(View.VISIBLE);
            selectedCause = "Intrapulmonary Shunt";
        } else if (card.getId() == R.id.card_unknown) {
            ivCheckUnknown.setVisibility(View.VISIBLE);
            selectedCause = "Unknown";
        }
    }

    private void resetCardStyle(MaterialCardView card) {
        card.setStrokeWidth(0);
        
        // Hide respective checkmark
        if (card.getId() == R.id.card_vq_mismatch) ivCheckVQ.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_hypoventilation) ivCheckHypo.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_diffusion) ivCheckDiff.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_shunt) ivCheckShunt.setVisibility(View.GONE);
        else if (card.getId() == R.id.card_unknown) ivCheckUnknown.setVisibility(View.GONE);
    }

    private void saveCauseToBackend(int patientId, String cause) {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("cause", cause);

        apiService.setHypoxemiaCause(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(HypoxemiaCauseActivity.this, "Cause saved successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(HypoxemiaCauseActivity.this, OxygenRequirementActivity.class);
                    intent.putExtra("patient_id", patientId);
                    startActivity(intent);
                } else {
                    Toast.makeText(HypoxemiaCauseActivity.this, "Failed to save cause", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(HypoxemiaCauseActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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