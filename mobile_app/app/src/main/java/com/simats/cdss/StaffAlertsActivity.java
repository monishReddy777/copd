package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.adapters.StaffAlertAdapter;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.StaffAlertsResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffAlertsActivity extends AppCompatActivity {

    private static final String TAG = "StaffAlerts";
    private static final long REFRESH_INTERVAL_MS = 30_000; // 30 seconds

    private RecyclerView rvCriticalAlerts, rvModerateAlerts;
    private TextView tvCriticalHeader, tvModerateHeader, tvAlertCountBadge;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressLoading;

    private StaffAlertAdapter criticalAdapter, moderateAdapter;
    private List<StaffAlertsResponse.ReassessmentAlert> criticalList = new ArrayList<>();
    private List<StaffAlertsResponse.ReassessmentAlert> moderateList = new ArrayList<>();

    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_alerts);

        // Bind views
        tvCriticalHeader = findViewById(R.id.tv_critical_header);
        tvModerateHeader = findViewById(R.id.tv_moderate_header);
        tvAlertCountBadge = findViewById(R.id.tv_alert_count_badge);
        rvCriticalAlerts = findViewById(R.id.rv_critical_alerts);
        rvModerateAlerts = findViewById(R.id.rv_moderate_alerts);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressLoading = findViewById(R.id.progress_loading);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Setup RecyclerViews
        rvCriticalAlerts.setLayoutManager(new LinearLayoutManager(this));
        criticalAdapter = new StaffAlertAdapter(this, criticalList);
        criticalAdapter.setOnAlertActionListener(createAlertActionListener());
        rvCriticalAlerts.setAdapter(criticalAdapter);

        rvModerateAlerts.setLayoutManager(new LinearLayoutManager(this));
        moderateAdapter = new StaffAlertAdapter(this, moderateList);
        moderateAdapter.setOnAlertActionListener(createAlertActionListener());
        rvModerateAlerts.setAdapter(moderateAdapter);

        setupBottomNav();

        // Auto-refresh every 30 seconds
        refreshRunnable = () -> {
            loadAlerts();
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFirstLoad = true;
        loadAlerts();
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private StaffAlertAdapter.OnAlertActionListener createAlertActionListener() {
        return new StaffAlertAdapter.OnAlertActionListener() {
            @Override
            public void onPerformReassessment(StaffAlertsResponse.ReassessmentAlert alert) {
                // Navigate to ReassessmentChecklist with all alert details
                // The checklist will save to staff_checklist AND auto-complete the schedule
                Intent intent = new Intent(StaffAlertsActivity.this, ReassessmentChecklistActivity.class);
                intent.putExtra("patient_id", alert.getPatientId());
                intent.putExtra("reassessment_id", alert.getId());
                intent.putExtra("patient_name", alert.getPatientName());
                intent.putExtra("bed_no", alert.getBedNo());
                intent.putExtra("ward_no", alert.getWardNo());
                intent.putExtra("reassessment_type", alert.getReassessmentType());
                startActivity(intent);
            }

            @Override
            public void onViewPatient(StaffAlertsResponse.ReassessmentAlert alert) {
                // Navigate to Enter Vitals screen for reassessment workflow
                Intent intent = new Intent(StaffAlertsActivity.this, VitalsActivity.class);
                intent.putExtra("patient_id", alert.getPatientId());
                intent.putExtra("patient_name", alert.getPatientName());
                intent.putExtra("bed_no", alert.getBedNo());
                intent.putExtra("ward_no", alert.getWardNo());
                intent.putExtra("reassessment_id", alert.getId());
                intent.putExtra("is_reassessment", true);
                intent.putExtra("is_update", true);
                startActivity(intent);
            }
        };
    }

    private void loadAlerts() {
        if (isFirstLoad) {
            progressLoading.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getStaffAlerts().enqueue(new Callback<StaffAlertsResponse>() {
            @Override
            public void onResponse(Call<StaffAlertsResponse> call, Response<StaffAlertsResponse> response) {
                progressLoading.setVisibility(View.GONE);
                isFirstLoad = false;

                if (response.isSuccessful() && response.body() != null) {
                    StaffAlertsResponse data = response.body();

                    Log.d(TAG, "Loaded alerts: critical=" + data.getCriticalCount()
                            + " moderate=" + data.getModerateCount());

                    // Update badge
                    int total = data.getTotalCount();
                    if (total > 0) {
                        tvAlertCountBadge.setText(String.valueOf(total));
                        tvAlertCountBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvAlertCountBadge.setVisibility(View.GONE);
                    }

                    // Update critical alerts
                    criticalList.clear();
                    List<StaffAlertsResponse.ReassessmentAlert> criticals = data.getCriticalAlerts();
                    if (criticals != null && !criticals.isEmpty()) {
                        criticalList.addAll(criticals);
                        tvCriticalHeader.setVisibility(View.VISIBLE);
                        rvCriticalAlerts.setVisibility(View.VISIBLE);
                    } else {
                        tvCriticalHeader.setVisibility(View.GONE);
                        rvCriticalAlerts.setVisibility(View.GONE);
                    }
                    criticalAdapter.notifyDataSetChanged();

                    // Update moderate alerts
                    moderateList.clear();
                    List<StaffAlertsResponse.ReassessmentAlert> moderates = data.getModerateAlerts();
                    if (moderates != null && !moderates.isEmpty()) {
                        moderateList.addAll(moderates);
                        tvModerateHeader.setVisibility(View.VISIBLE);
                        rvModerateAlerts.setVisibility(View.VISIBLE);
                    } else {
                        tvModerateHeader.setVisibility(View.GONE);
                        rvModerateAlerts.setVisibility(View.GONE);
                    }
                    moderateAdapter.notifyDataSetChanged();

                    // Empty state
                    if (total == 0) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                    }

                } else {
                    Log.e(TAG, "API error: " + response.code());
                    if (isFirstLoad) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<StaffAlertsResponse> call, Throwable t) {
                progressLoading.setVisibility(View.GONE);
                isFirstLoad = false;
                Log.e(TAG, "Network error: " + t.getMessage());
                Toast.makeText(StaffAlertsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsDone(int reassessmentId, Runnable onSuccess) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("action", "mark_done");
        body.put("reassessment_id", reassessmentId);

        api.postStaffAlertAction(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Reassessment " + reassessmentId + " marked as done");
                    Toast.makeText(StaffAlertsActivity.this,
                            "Reassessment marked as done", Toast.LENGTH_SHORT).show();
                    loadAlerts(); // Refresh list
                    if (onSuccess != null) onSuccess.run();
                } else {
                    Log.e(TAG, "Failed to mark done: " + response.code());
                    Toast.makeText(StaffAlertsActivity.this,
                            "Failed to update", Toast.LENGTH_SHORT).show();
                    // Still navigate even if mark_done fails
                    if (onSuccess != null) onSuccess.run();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "Mark done failed: " + t.getMessage());
                // Still navigate even on network failure
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_alerts);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, StaffDashboardActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_patients) {
                    startActivity(new Intent(this, StaffPatientsActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    finish();
                    return true;
                }
                return itemId == R.id.nav_alerts;
            });
        }
    }
}