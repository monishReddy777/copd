package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.models.DecisionSupportResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DecisionSupportActivity extends AppCompatActivity {

    private int patientId;
    private TextView tvRiskLevel, tvConfidence, tvActionLevel, tvRecommendation;
    private TextView tvOverallStatus, tvPaco2, tvPh, tvSpo2;
    private MaterialCardView cardAcidosis, cardHypercapnia, cardRecommendation;
    private LinearLayout layoutEmpty;
    private ScrollView scrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decision_support);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Initialize views
        tvRiskLevel = findViewById(R.id.tv_risk_level);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvActionLevel = findViewById(R.id.tv_action_level);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        tvOverallStatus = findViewById(R.id.tv_overall_status);
        tvPaco2 = findViewById(R.id.tv_paco2);
        tvPh = findViewById(R.id.tv_ph);
        tvSpo2 = findViewById(R.id.tv_spo2);
        cardAcidosis = findViewById(R.id.card_acidosis);
        cardHypercapnia = findViewById(R.id.card_hypercapnia);
        cardRecommendation = findViewById(R.id.card_recommendation);
        layoutEmpty = findViewById(R.id.layout_empty);
        scrollContent = findViewById(R.id.scroll_content);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        setupBottomNav();
        fetchDecisionSupportData();
    }

    private void fetchDecisionSupportData() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientDecisionSupport(patientId).enqueue(new Callback<DecisionSupportResponse>() {
            @Override
            public void onResponse(Call<DecisionSupportResponse> call, Response<DecisionSupportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DecisionSupportResponse data = response.body();
                    if (data.isHasData()) {
                        showContent();
                        populateUI(data);
                    } else {
                        showEmptyState();
                    }
                } else {
                    Toast.makeText(DecisionSupportActivity.this, "Failed to load Decision Support Data", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<DecisionSupportResponse> call, Throwable t) {
                Toast.makeText(DecisionSupportActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutEmpty.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);
    }

    private void populateUI(DecisionSupportResponse data) {
        // ── Risk Level ──
        String risk = data.getRiskLevel();
        tvRiskLevel.setText(risk + " RISK");

        int riskColor;
        if ("HIGH".equalsIgnoreCase(risk)) {
            riskColor = Color.parseColor("#B91C1C");
        } else if ("MODERATE".equalsIgnoreCase(risk)) {
            riskColor = Color.parseColor("#F59E0B");
        } else {
            riskColor = Color.parseColor("#10B981");
        }
        tvRiskLevel.setTextColor(riskColor);
        tvConfidence.setText("Confidence Score: " + data.getConfidenceScore() + "%");

        // ── Recommendation Card ──
        String actionLevel = data.getActionLevel();
        tvActionLevel.setText(actionLevel);
        tvRecommendation.setText(data.getRecommendation());

        if ("CRITICAL".equalsIgnoreCase(actionLevel)) {
            tvActionLevel.setTextColor(Color.parseColor("#B91C1C"));
            cardRecommendation.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
        } else if ("WARNING".equalsIgnoreCase(actionLevel)) {
            tvActionLevel.setTextColor(Color.parseColor("#F59E0B"));
            cardRecommendation.setCardBackgroundColor(Color.parseColor("#FFFBEB"));
        } else {
            tvActionLevel.setTextColor(Color.parseColor("#10B981"));
            cardRecommendation.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
        }

        // ── Trend Summary ──
        String overallStatus = data.getOverallStatus() != null ? data.getOverallStatus() : "Stable";
        tvOverallStatus.setText(overallStatus);
        applyColor(tvOverallStatus, overallStatus);

        String paco2 = data.getPaco2Status() != null ? data.getPaco2Status() : "Normal";
        tvPaco2.setText(paco2);
        applyColor(tvPaco2, paco2);

        String ph = data.getPhStatus() != null ? data.getPhStatus() : "Normal";
        tvPh.setText(ph);
        applyColor(tvPh, ph);

        String spo2 = data.getSpo2Status() != null ? data.getSpo2Status() : "Stable";
        tvSpo2.setText(spo2);
        applyColor(tvSpo2, spo2);

        // ── Key Factors ──
        cardAcidosis.setVisibility(data.getAcidosis() == 1 ? View.VISIBLE : View.GONE);
        cardHypercapnia.setVisibility(data.getHypercapnia() == 1 ? View.VISIBLE : View.GONE);
    }

    private void applyColor(TextView textView, String status) {
        if (status.equalsIgnoreCase("Rising") || status.equalsIgnoreCase("Dropping")
                || status.equalsIgnoreCase("Worsening") || status.equalsIgnoreCase("Critical")) {
            textView.setTextColor(Color.parseColor("#EF4444"));
        } else if (status.equalsIgnoreCase("Unstable")) {
            textView.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            textView.setTextColor(Color.parseColor("#10B981"));
        }
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
