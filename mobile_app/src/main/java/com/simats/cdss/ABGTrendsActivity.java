package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.models.ABGTrendResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import com.simats.cdss.views.SparkLineView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ABGTrendsActivity extends AppCompatActivity {

    private static final String TAG = "ABGTrends";

    private int patientId = -1;
    private String currentFilter = "5h"; // Default filter

    // Patient info
    private TextView tvPatientInitials, tvPatientName, tvDiagnosis, tvStatusBadge;

    // pH
    private TextView tvPhValue;
    private ImageView ivPhIcon, ivPhTrend;
    private SparkLineView chartPh;

    // PaCO2
    private TextView tvPaco2Value;
    private ImageView ivPaco2Icon, ivPaco2Trend;
    private SparkLineView chartPaco2;

    // PaO2
    private TextView tvPao2Value;
    private ImageView ivPao2Icon, ivPao2Trend;
    private SparkLineView chartPao2;

    // HCO3
    private TextView tvHco3Value;
    private ImageView ivHco3Icon, ivHco3Trend;
    private SparkLineView chartHco3;

    // FiO2
    private TextView tvFio2Value;
    private ImageView ivFio2Trend;
    private SparkLineView chartFio2;

    // Trend Analysis
    private TextView tvTrendAnalysis;

    // Filter
    private TextView tvFilterLabel;
    private static final String[] FILTER_LABELS = {"5h", "8h", "24h", "1D", "2D", "3D", "7D", "15D", "1M"};
    private static final String[] FILTER_VALUES = {"5h", "8h", "24h", "1d", "2d", "3d", "7d", "15d", "1m"};

    // Colors
    private static final int COLOR_RED = Color.parseColor("#EF4444");
    private static final int COLOR_AMBER = Color.parseColor("#F59E0B");
    private static final int COLOR_TEAL = Color.parseColor("#139487");
    private static final int COLOR_GREEN = Color.parseColor("#166534");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abg_trends);

        patientId = getIntent().getIntExtra("patient_id", -1);

        // Initialize views
        tvPatientInitials = findViewById(R.id.tv_patient_initials);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvDiagnosis = findViewById(R.id.tv_diagnosis);
        tvStatusBadge = findViewById(R.id.tv_status_badge);

        tvPhValue = findViewById(R.id.tv_ph_value);
        ivPhIcon = findViewById(R.id.iv_ph_icon);
        ivPhTrend = findViewById(R.id.iv_ph_trend);
        chartPh = findViewById(R.id.chart_ph);

        tvPaco2Value = findViewById(R.id.tv_paco2_value);
        ivPaco2Icon = findViewById(R.id.iv_paco2_icon);
        ivPaco2Trend = findViewById(R.id.iv_paco2_trend);
        chartPaco2 = findViewById(R.id.chart_paco2);

        tvPao2Value = findViewById(R.id.tv_pao2_value);
        ivPao2Icon = findViewById(R.id.iv_pao2_icon);
        ivPao2Trend = findViewById(R.id.iv_pao2_trend);
        chartPao2 = findViewById(R.id.chart_pao2);

        tvHco3Value = findViewById(R.id.tv_hco3_value);
        ivHco3Icon = findViewById(R.id.iv_hco3_icon);
        ivHco3Trend = findViewById(R.id.iv_hco3_trend);
        chartHco3 = findViewById(R.id.chart_hco3);

        tvFio2Value = findViewById(R.id.tv_fio2_value);
        ivFio2Trend = findViewById(R.id.iv_fio2_trend);
        chartFio2 = findViewById(R.id.chart_fio2);

        tvTrendAnalysis = findViewById(R.id.tv_trend_analysis);

        // Initialize filter button
        tvFilterLabel = findViewById(R.id.tv_filter_label);
        tvFilterLabel.setText(getFilterDisplayLabel(currentFilter));

        findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterDialog());

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        fetchABGTrends();
        setupBottomNav();
    }

    private void showFilterDialog() {
        // Find current selection index
        int checkedIndex = 0;
        for (int i = 0; i < FILTER_VALUES.length; i++) {
            if (FILTER_VALUES[i].equals(currentFilter)) {
                checkedIndex = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Time Range")
                .setSingleChoiceItems(FILTER_LABELS, checkedIndex, (dialog, which) -> {
                    currentFilter = FILTER_VALUES[which];
                    tvFilterLabel.setText(FILTER_LABELS[which]);
                    fetchABGTrends();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getFilterDisplayLabel(String filterValue) {
        for (int i = 0; i < FILTER_VALUES.length; i++) {
            if (FILTER_VALUES[i].equals(filterValue)) {
                return FILTER_LABELS[i];
            }
        }
        return filterValue;
    }

    private void fetchABGTrends() {
        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getABGTrends(patientId, currentFilter).enqueue(new Callback<ABGTrendResponse>() {
            @Override
            public void onResponse(Call<ABGTrendResponse> call, Response<ABGTrendResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        populateUI(response.body());
                    } else {
                        Toast.makeText(ABGTrendsActivity.this, "Failed to load ABG trends", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing ABG trends: " + e.getMessage(), e);
                    Toast.makeText(ABGTrendsActivity.this, "Error displaying ABG trends", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ABGTrendResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(ABGTrendsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(ABGTrendResponse data) {
        // Patient info
        String name = data.getPatientName();
        if (name != null && !name.isEmpty()) {
            tvPatientName.setText(name);
            // Generate initials
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty() && initials.length() < 2) {
                    initials.append(part.charAt(0));
                }
            }
            tvPatientInitials.setText(initials.toString().toUpperCase());
        }

        tvDiagnosis.setText(data.getDiagnosis() != null ? data.getDiagnosis() : "COPD Exacerbation");

        // Status badge
        String status = data.getStatus();
        if (status != null) {
            tvStatusBadge.setText(status);
            switch (status) {
                case "Critical":
                    tvStatusBadge.setTextColor(COLOR_RED);
                    tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
                    break;
                case "Warning":
                    tvStatusBadge.setTextColor(COLOR_AMBER);
                    tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFBEB")));
                    break;
                default:
                    tvStatusBadge.setTextColor(COLOR_GREEN);
                    tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
                    break;
            }
        }

        // Trend analysis text
        if (data.getTrendAnalysis() != null) {
            tvTrendAnalysis.setText(data.getTrendAnalysis());
        }

        // Process ABG entries
        List<ABGTrendResponse.ABGEntry> entries = data.getAbgTrendData();
        if (entries == null || entries.isEmpty()) {
            // No data for the selected time range — clear all charts and values
            tvTrendAnalysis.setText("No ABG data available for the selected time range (" + getFilterDisplayLabel(currentFilter) + ").");

            // Clear all chart data so they show "No data available"
            List<Float> emptyData = new ArrayList<>();
            List<String> emptyLabels = new ArrayList<>();

            tvPhValue.setText("N/A");
            tvPhValue.setTextColor(Color.GRAY);
            ivPhIcon.setColorFilter(Color.GRAY);
            ivPhTrend.setImageResource(0);
            chartPh.setTargetRange(7.35f, 7.45f);
            chartPh.setDateLabels(emptyLabels);
            chartPh.setData(emptyData, Color.GRAY, emptyLabels);

            tvPaco2Value.setText("N/A");
            tvPaco2Value.setTextColor(Color.GRAY);
            ivPaco2Icon.setColorFilter(Color.GRAY);
            ivPaco2Trend.setImageResource(0);
            chartPaco2.setTargetRange(35f, 45f);
            chartPaco2.setDateLabels(emptyLabels);
            chartPaco2.setData(emptyData, Color.GRAY, emptyLabels);

            tvPao2Value.setText("N/A");
            tvPao2Value.setTextColor(Color.GRAY);
            ivPao2Icon.setColorFilter(Color.GRAY);
            ivPao2Trend.setImageResource(0);
            chartPao2.setTargetRange(60f, 100f);
            chartPao2.setDateLabels(emptyLabels);
            chartPao2.setData(emptyData, Color.GRAY, emptyLabels);

            tvHco3Value.setText("N/A");
            tvHco3Value.setTextColor(Color.GRAY);
            ivHco3Icon.setColorFilter(Color.GRAY);
            ivHco3Trend.setImageResource(0);
            chartHco3.setTargetRange(22f, 26f);
            chartHco3.setDateLabels(emptyLabels);
            chartHco3.setData(emptyData, Color.GRAY, emptyLabels);

            tvFio2Value.setText("N/A");
            tvFio2Value.setTextColor(Color.GRAY);
            ivFio2Trend.setImageResource(0);
            chartFio2.setDateLabels(emptyLabels);
            chartFio2.setData(emptyData, Color.GRAY, emptyLabels);
            return;
        }

        // Extract data arrays, time labels, and date labels
        List<Float> phData = new ArrayList<>();
        List<Float> paco2Data = new ArrayList<>();
        List<Float> pao2Data = new ArrayList<>();
        List<Float> hco3Data = new ArrayList<>();
        List<Float> fio2Data = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        for (ABGTrendResponse.ABGEntry entry : entries) {
            phData.add((float) entry.getPh());
            paco2Data.add((float) entry.getPaco2());
            pao2Data.add((float) entry.getPao2());
            hco3Data.add((float) entry.getHco3());
            fio2Data.add((float) entry.getFio2());

            // Extract time label from the entry
            String label = entry.getTimeLabel();
            if (label == null || label.isEmpty()) {
                // Fallback: parse from created_at (format: "YYYY-MM-DD HH:MM:SS")
                String createdAt = entry.getCreatedAt();
                if (createdAt != null && createdAt.length() >= 16) {
                    label = createdAt.substring(11, 16); // Extract "HH:MM"
                } else {
                    label = "";
                }
            }
            timeLabels.add(label);

            // Extract date label for tooltip display
            String dateLabel = entry.getDateLabel();
            if (dateLabel == null || dateLabel.isEmpty()) {
                // Fallback: parse from created_at
                String createdAt = entry.getCreatedAt();
                if (createdAt != null && createdAt.length() >= 16) {
                    dateLabel = createdAt.substring(8, 10) + "/" + createdAt.substring(5, 7) + " " + createdAt.substring(11, 16);
                } else {
                    dateLabel = "";
                }
            }
            dateLabels.add(dateLabel);
        }

        // Get latest entry for displaying current values
        ABGTrendResponse.ABGEntry latest = entries.get(entries.size() - 1);

        // ── pH ──
        double ph = latest.getPh();
        tvPhValue.setText(String.format("%.2f", ph));
        int phColor;
        if (ph < 7.35 || ph > 7.45) {
            phColor = COLOR_RED;
        } else {
            phColor = COLOR_TEAL;
        }
        tvPhValue.setTextColor(phColor);
        ivPhIcon.setColorFilter(phColor);
        setTrendIcon(ivPhTrend, phData, phColor);
        chartPh.setTargetRange(7.35f, 7.45f);
        chartPh.setDateLabels(dateLabels);
        chartPh.setData(phData, phColor, timeLabels);

        // ── PaCO2 ──
        double paco2 = latest.getPaco2();
        tvPaco2Value.setText(String.valueOf((int) paco2));
        int paco2Color;
        if (paco2 > 45) {
            paco2Color = COLOR_AMBER;
        } else if (paco2 < 35) {
            paco2Color = COLOR_RED;
        } else {
            paco2Color = COLOR_TEAL;
        }
        tvPaco2Value.setTextColor(paco2Color);
        ivPaco2Icon.setColorFilter(paco2Color);
        setTrendIcon(ivPaco2Trend, paco2Data, paco2Color);
        chartPaco2.setTargetRange(35f, 45f);
        chartPaco2.setDateLabels(dateLabels);
        chartPaco2.setData(paco2Data, paco2Color, timeLabels);

        // ── PaO2 ──
        double pao2 = latest.getPao2();
        tvPao2Value.setText(String.valueOf((int) pao2));
        int pao2Color;
        if (pao2 < 60) {
            pao2Color = COLOR_RED;
        } else {
            pao2Color = COLOR_TEAL;
        }
        tvPao2Value.setTextColor(pao2Color);
        ivPao2Icon.setColorFilter(pao2Color);
        setTrendIcon(ivPao2Trend, pao2Data, pao2Color);
        chartPao2.setTargetRange(60f, 100f);
        chartPao2.setDateLabels(dateLabels);
        chartPao2.setData(pao2Data, pao2Color, timeLabels);

        // ── HCO3 ──
        double hco3 = latest.getHco3();
        tvHco3Value.setText(String.valueOf((int) hco3));
        int hco3Color;
        if (hco3 < 22 || hco3 > 26) {
            hco3Color = COLOR_AMBER;
        } else {
            hco3Color = COLOR_TEAL;
        }
        tvHco3Value.setTextColor(hco3Color);
        ivHco3Icon.setColorFilter(hco3Color);
        setTrendIcon(ivHco3Trend, hco3Data, hco3Color);
        chartHco3.setTargetRange(22f, 26f);
        chartHco3.setDateLabels(dateLabels);
        chartHco3.setData(hco3Data, hco3Color, timeLabels);

        // ── FiO2 ──
        double fio2 = latest.getFio2();
        tvFio2Value.setText(String.valueOf((int) fio2));
        tvFio2Value.setTextColor(COLOR_TEAL);
        setTrendIcon(ivFio2Trend, fio2Data, COLOR_TEAL);
        chartFio2.setDateLabels(dateLabels);
        chartFio2.setData(fio2Data, COLOR_TEAL, timeLabels);
    }

    /**
     * Sets the trend direction icon (up/down) based on comparing
     * the last two data points.
     */
    private void setTrendIcon(ImageView iv, List<Float> data, int color) {
        if (data.size() >= 2) {
            float last = data.get(data.size() - 1);
            float prev = data.get(data.size() - 2);
            if (last > prev) {
                iv.setImageResource(R.drawable.ic_trending_up);
            } else if (last < prev) {
                iv.setImageResource(R.drawable.ic_trending_down);
            } else {
                iv.setImageResource(R.drawable.ic_trending_up);
            }
        }
        iv.setColorFilter(color);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;
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
