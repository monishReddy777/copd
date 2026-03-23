package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.adapters.KeyFactorsAdapter;
import com.simats.cdss.models.AIRiskResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIAnalysisActivity extends AppCompatActivity {

    private int patientId;
    private TextView tvRiskLevel, tvConfidenceScore, tvKeyFactorsTitle;
    private ProgressBar progressConfidence;
    private RecyclerView rvKeyFactors;
    private KeyFactorsAdapter keyFactorsAdapter;
    private LinearLayout layoutEmpty;
    private ScrollView scrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_analysis);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Initialize views
        tvRiskLevel = findViewById(R.id.tv_risk_level);
        tvConfidenceScore = findViewById(R.id.tv_confidence_score);
        tvKeyFactorsTitle = findViewById(R.id.tv_key_factors_title);
        progressConfidence = findViewById(R.id.progress_confidence);
        rvKeyFactors = findViewById(R.id.rv_key_factors);
        layoutEmpty = findViewById(R.id.layout_empty);
        scrollContent = findViewById(R.id.scroll_content);

        // Setup key factors RecyclerView
        keyFactorsAdapter = new KeyFactorsAdapter(new ArrayList<>());
        rvKeyFactors.setLayoutManager(new LinearLayoutManager(this));
        rvKeyFactors.setAdapter(keyFactorsAdapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Navigation to Trend Analysis screen
        findViewById(R.id.btn_view_trends).setOnClickListener(v -> {
            Intent intent = new Intent(this, TrendAnalysisActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        setupBottomNav();
        fetchAIRiskData();
    }

    private void fetchAIRiskData() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientAIRisk(patientId).enqueue(new Callback<AIRiskResponse>() {
            @Override
            public void onResponse(Call<AIRiskResponse> call, Response<AIRiskResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AIRiskResponse data = response.body();
                    // Check if there's a message indicating no data
                    if (data.getConfidenceScore() == 0 && data.getMessage() != null) {
                        showEmptyState();
                    } else {
                        showContent();
                        populateUI(data);
                    }
                } else {
                    Toast.makeText(AIAnalysisActivity.this, "Failed to load AI Risk Data", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<AIRiskResponse> call, Throwable t) {
                Toast.makeText(AIAnalysisActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void populateUI(AIRiskResponse data) {
        // ── Risk Level Display ──
        String risk = data.getRiskLevel();
        tvRiskLevel.setText(risk + " RISK");

        int riskColor;
        if ("HIGH".equalsIgnoreCase(risk)) {
            riskColor = Color.parseColor("#B91C1C"); // RED
        } else if ("MODERATE".equalsIgnoreCase(risk)) {
            riskColor = Color.parseColor("#F59E0B"); // ORANGE
        } else {
            // LOW or default
            riskColor = Color.parseColor("#10B981"); // GREEN
        }
        tvRiskLevel.setTextColor(riskColor);

        // ── Confidence Score ──
        int confidence = data.getConfidenceScore();
        progressConfidence.setProgress(confidence);
        progressConfidence.getProgressDrawable().setColorFilter(riskColor, PorterDuff.Mode.SRC_IN);
        tvConfidenceScore.setText("Confidence Score: " + confidence + "%");

        // ── Dynamic Key Factors ──
        if (data.getKeyFactors() != null && !data.getKeyFactors().isEmpty()) {
            tvKeyFactorsTitle.setVisibility(View.VISIBLE);
            rvKeyFactors.setVisibility(View.VISIBLE);
            keyFactorsAdapter.setFactors(data.getKeyFactors());
        } else {
            tvKeyFactorsTitle.setVisibility(View.GONE);
            rvKeyFactors.setVisibility(View.GONE);
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
