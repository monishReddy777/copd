package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

public class BaselineDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BaselineDetails";

    private MaterialCardView cardYes, cardNo;
    private MaterialCardView cvIconYes, cvPlaceholderYes;
    private MaterialCardView cvIconNo, cvPlaceholderNo;
    private TextView tvYes, tvNo;
    private Button btnNext;
    private MaterialCardView selectedCard = null;
    private String selectedCopdHistory = "";
    private int patientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baseline_details);

        // Get patient_id from previous screen (AddNewPatientActivity)
        patientId = getIntent().getIntExtra("patient_id", -1);

        // Initialize Views
        cardYes = findViewById(R.id.card_yes);
        cardNo = findViewById(R.id.card_no);
        cvIconYes = findViewById(R.id.cv_icon_yes);
        cvPlaceholderYes = findViewById(R.id.cv_placeholder_yes);
        cvIconNo = findViewById(R.id.cv_icon_no);
        cvPlaceholderNo = findViewById(R.id.cv_placeholder_no);
        tvYes = findViewById(R.id.tv_yes);
        tvNo = findViewById(R.id.tv_no);
        btnNext = findViewById(R.id.btn_next);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        cardYes.setOnClickListener(v -> handleSelection(cardYes));
        cardNo.setOnClickListener(v -> handleSelection(cardNo));

        btnNext.setOnClickListener(v -> {
            if (selectedCopdHistory.isEmpty()) {
                Toast.makeText(this, "Please select COPD history", Toast.LENGTH_SHORT).show();
                return;
            }
            saveBaselineDetails();
        });

        setupBottomNav();
    }

    private void handleSelection(MaterialCardView card) {
        // Reset all states
        resetStyles();

        selectedCard = card;
        btnNext.setEnabled(true);

        if (card.getId() == R.id.card_yes) {
            selectedCopdHistory = "Yes";
            cardYes.setStrokeWidth(4);
            cardYes.setStrokeColor(getResources().getColor(R.color.primary_teal));
            cvIconYes.setVisibility(View.VISIBLE);
            cvPlaceholderYes.setVisibility(View.GONE);
            tvYes.setTextColor(getResources().getColor(R.color.primary_teal));
        } else {
            selectedCopdHistory = "No/Unknown";
            cardNo.setStrokeWidth(4);
            cardNo.setStrokeColor(getResources().getColor(R.color.primary_teal));
            cvIconNo.setVisibility(View.VISIBLE);
            cvPlaceholderNo.setVisibility(View.GONE);
            tvNo.setTextColor(getResources().getColor(R.color.primary_teal));
        }
    }

    private void resetStyles() {
        // Reset Yes Card
        cardYes.setStrokeWidth(0);
        cvIconYes.setVisibility(View.INVISIBLE);
        cvPlaceholderYes.setVisibility(View.VISIBLE);
        tvYes.setTextColor(getResources().getColor(R.color.text_secondary));

        // Reset No Card
        cardNo.setStrokeWidth(0);
        cvIconNo.setVisibility(View.INVISIBLE);
        cvPlaceholderNo.setVisibility(View.VISIBLE);
        tvNo.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    /**
     * Sends POST request to /api/baseline-details/add/
     * Body: { "patient_id": <id>, "copd_history": "Yes" or "No/Unknown" }
     */
    private void saveBaselineDetails() {
        btnNext.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("copd_history", selectedCopdHistory);

        Log.d(TAG, "Sending baseline details: patient_id=" + patientId + ", copd_history=" + selectedCopdHistory);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Call<GenericResponse> call = apiService.addBaselineDetails(body);

        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                btnNext.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(BaselineDetailsActivity.this,
                            "Baseline details saved successfully", Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Baseline saved. Navigating to GOLD Classification.");

                    // Navigate to GOLD Classification screen
                    Intent intent = new Intent(BaselineDetailsActivity.this, GoldClassificationActivity.class);
                    intent.putExtra("patient_id", patientId);
                    startActivity(intent);
                } else {
                    String errorMsg = "Failed to save baseline details";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "API Error " + response.code() + ": " + errorMsg);
                    Toast.makeText(BaselineDetailsActivity.this,
                            "Failed to save baseline details", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                btnNext.setEnabled(true);
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(BaselineDetailsActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
            }
        });
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
