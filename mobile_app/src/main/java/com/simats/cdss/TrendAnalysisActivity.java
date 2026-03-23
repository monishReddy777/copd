package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.TrendAnalysisResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrendAnalysisActivity extends AppCompatActivity {

    private int patientId;
    private TextView tvStatus, tvStatusSubtitle, tvPaco2, tvPh, tvSpo2;
    private ImageView ivTrendTop;
    private LinearLayout layoutEmpty;
    private ScrollView scrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trend_analysis);

        patientId = getIntent().getIntExtra("patient_id", -1);

        tvStatus = findViewById(R.id.tv_status);
        tvStatusSubtitle = findViewById(R.id.tv_status_subtitle);
        ivTrendTop = findViewById(R.id.iv_trend_top);

        tvPaco2 = findViewById(R.id.tv_paco2);
        tvPh = findViewById(R.id.tv_ph);
        tvSpo2 = findViewById(R.id.tv_spo2);

        layoutEmpty = findViewById(R.id.layout_empty);
        scrollContent = findViewById(R.id.scroll_content);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Navigation to Causes of Hypoxemia (first step of clinical decision support flow)
        findViewById(R.id.btn_proceed).setOnClickListener(v -> {
            Intent intent = new Intent(this, HypoxemiaCauseActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });

        setupBottomNav();
        fetchTrendData();
    }

    private void fetchTrendData() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatientTrendAnalysis(patientId).enqueue(new Callback<TrendAnalysisResponse>() {
            @Override
            public void onResponse(Call<TrendAnalysisResponse> call, Response<TrendAnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showContent();
                    populateUI(response.body());
                } else {
                    Toast.makeText(TrendAnalysisActivity.this, "Failed to load Trend Data", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<TrendAnalysisResponse> call, Throwable t) {
                Toast.makeText(TrendAnalysisActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void populateUI(TrendAnalysisResponse data) {
        // ── Header Section (Overall Status) ──
        String overallStatus = data.getOverallStatus() != null ? data.getOverallStatus() : "Stable";
        tvStatus.setText(overallStatus);

        if ("Worsening".equalsIgnoreCase(overallStatus)) {
            tvStatusSubtitle.setText("Patient condition is deteriorating compared\nto baseline.");
        } else if ("Improving".equalsIgnoreCase(overallStatus)) {
            tvStatusSubtitle.setText("Patient condition is improving compared\nto baseline.");
        } else if ("Insufficient Data".equalsIgnoreCase(overallStatus)) {
            tvStatusSubtitle.setText("Need at least two reassessment entries\nto determine trends.");
        } else {
            tvStatusSubtitle.setText("Patient condition is stable.");
        }

        // ── Trend Indicators ──
        String paco2 = data.getPaco2Status() != null ? data.getPaco2Status() : "Insufficient Data";
        String ph = data.getPhStatus() != null ? data.getPhStatus() : "Insufficient Data";
        String spo2 = data.getSpo2Status() != null ? data.getSpo2Status() : "Insufficient Data";

        tvPaco2.setText(paco2);
        tvPh.setText(ph);
        tvSpo2.setText(spo2);

        // ── Apply Color Logic ──
        applyColor(tvPaco2, paco2);
        applyColor(tvPh, ph);
        applyColor(tvSpo2, spo2);
    }

    /**
     * Color logic:
     * Rising / Dropping / Critical / Declining → RED
     * Unstable → ORANGE
     * Improving / Decreasing → BLUE (positive trend)
     * Normal / Stable → GREEN
     * Insufficient Data → GRAY
     */
    private void applyColor(TextView textView, String status) {
        if (status.equalsIgnoreCase("Rising") || status.equalsIgnoreCase("Dropping") || status.equalsIgnoreCase("Critical") || status.equalsIgnoreCase("Declining")) {
            textView.setTextColor(Color.parseColor("#EF4444")); // RED
        } else if (status.equalsIgnoreCase("Unstable")) {
            textView.setTextColor(Color.parseColor("#F59E0B")); // ORANGE
        } else if (status.equalsIgnoreCase("Improving") || status.equalsIgnoreCase("Decreasing")) {
            textView.setTextColor(Color.parseColor("#3B82F6")); // BLUE — positive trend
        } else if (status.equalsIgnoreCase("Insufficient Data")) {
            textView.setTextColor(Color.parseColor("#9CA3AF")); // GRAY — no data
        } else {
            // Normal / Stable
            textView.setTextColor(Color.parseColor("#10B981")); // GREEN
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