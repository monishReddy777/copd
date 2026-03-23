package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.simats.cdss.adapters.DoctorAlertAdapter;
import com.simats.cdss.models.DoctorAlertsResponse;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorAlertsActivity extends AppCompatActivity {

    private static final String TAG = "DoctorAlerts";

    private TextView tvUnreadBadge;
    private TextView tvCriticalHeader, tvWarningHeader, tvInfoHeader;
    private RecyclerView rvCritical, rvWarning, rvInfo;
    private LinearLayout layoutEmpty;

    private DoctorAlertAdapter criticalAdapter, warningAdapter, infoAdapter;
    private List<DoctorAlertsResponse.AlertItem> criticalItems = new ArrayList<>();
    private List<DoctorAlertsResponse.AlertItem> warningItems = new ArrayList<>();
    private List<DoctorAlertsResponse.AlertItem> infoItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_alerts);

        // Bind views
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        tvUnreadBadge = findViewById(R.id.tv_unread_badge);
        tvCriticalHeader = findViewById(R.id.tv_critical_header);
        tvWarningHeader = findViewById(R.id.tv_warning_header);
        tvInfoHeader = findViewById(R.id.tv_info_header);
        rvCritical = findViewById(R.id.rv_critical_alerts);
        rvWarning = findViewById(R.id.rv_warning_alerts);
        rvInfo = findViewById(R.id.rv_info_alerts);
        layoutEmpty = findViewById(R.id.layout_empty);

        // Setup RecyclerViews
        setupRecyclerView(rvCritical, criticalItems, "critical");
        setupRecyclerView(rvWarning, warningItems, "warning");
        setupRecyclerView(rvInfo, infoItems, "info");

        setupBottomNav();
        loadAlerts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlerts();
    }

    private void setupRecyclerView(RecyclerView rv, List<DoctorAlertsResponse.AlertItem> items, String section) {
        rv.setLayoutManager(new LinearLayoutManager(this));
        DoctorAlertAdapter adapter = new DoctorAlertAdapter(this, items, section);

        adapter.setOnAlertActionListener(new DoctorAlertAdapter.OnAlertActionListener() {
            @Override
            public void onViewPatient(DoctorAlertsResponse.AlertItem item) {
                Intent intent = new Intent(DoctorAlertsActivity.this, PatientDetailsActivity.class);
                intent.putExtra("patient_id", item.getPatientId());
                startActivity(intent);
            }

            @Override
            public void onAcknowledge(DoctorAlertsResponse.AlertItem item) {
                acknowledgeAlert(item.getId());
            }

            @Override
            public void onMarkRead(DoctorAlertsResponse.AlertItem item) {
                markAlertRead(item.getId());
            }
        });

        rv.setAdapter(adapter);

        // Save adapter reference
        if ("critical".equals(section)) criticalAdapter = adapter;
        else if ("warning".equals(section)) warningAdapter = adapter;
        else infoAdapter = adapter;
    }

    private void loadAlerts() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getDoctorAlerts().enqueue(new Callback<DoctorAlertsResponse>() {
            @Override
            public void onResponse(Call<DoctorAlertsResponse> call, Response<DoctorAlertsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DoctorAlertsResponse data = response.body();

                    // Update unread badge
                    int unread = data.getUnreadCount();
                    if (unread > 0) {
                        tvUnreadBadge.setText(String.valueOf(unread));
                        tvUnreadBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvUnreadBadge.setVisibility(View.GONE);
                    }

                    // Critical alerts
                    criticalItems.clear();
                    List<DoctorAlertsResponse.AlertItem> critical = data.getCriticalAlerts();
                    if (critical != null && !critical.isEmpty()) {
                        criticalItems.addAll(critical);
                        tvCriticalHeader.setVisibility(View.VISIBLE);
                        rvCritical.setVisibility(View.VISIBLE);
                    } else {
                        tvCriticalHeader.setVisibility(View.GONE);
                        rvCritical.setVisibility(View.GONE);
                    }
                    criticalAdapter.notifyDataSetChanged();

                    // Warning alerts
                    warningItems.clear();
                    List<DoctorAlertsResponse.AlertItem> warning = data.getWarningAlerts();
                    if (warning != null && !warning.isEmpty()) {
                        warningItems.addAll(warning);
                        tvWarningHeader.setVisibility(View.VISIBLE);
                        rvWarning.setVisibility(View.VISIBLE);
                    } else {
                        tvWarningHeader.setVisibility(View.GONE);
                        rvWarning.setVisibility(View.GONE);
                    }
                    warningAdapter.notifyDataSetChanged();

                    // Info alerts
                    infoItems.clear();
                    List<DoctorAlertsResponse.AlertItem> info = data.getInfoAlerts();
                    if (info != null && !info.isEmpty()) {
                        infoItems.addAll(info);
                        tvInfoHeader.setVisibility(View.VISIBLE);
                        rvInfo.setVisibility(View.VISIBLE);
                    } else {
                        tvInfoHeader.setVisibility(View.GONE);
                        rvInfo.setVisibility(View.GONE);
                    }
                    infoAdapter.notifyDataSetChanged();

                    // Show empty state if no alerts at all
                    boolean hasAlerts = !criticalItems.isEmpty() || !warningItems.isEmpty() || !infoItems.isEmpty();
                    layoutEmpty.setVisibility(hasAlerts ? View.GONE : View.VISIBLE);

                } else {
                    Log.e(TAG, "API error: " + response.code());
                    Toast.makeText(DoctorAlertsActivity.this,
                            "Failed to load alerts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DoctorAlertsResponse> call, Throwable t) {
                Log.e(TAG, "API failure: " + t.getMessage());
                Toast.makeText(DoctorAlertsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acknowledgeAlert(int alertId) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("action", "acknowledge");
        body.put("alert_id", alertId);

        api.postDoctorAlertAction(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DoctorAlertsActivity.this,
                            "Alert acknowledged", Toast.LENGTH_SHORT).show();
                    loadAlerts(); // Refresh
                } else {
                    Toast.makeText(DoctorAlertsActivity.this,
                            "Failed to acknowledge", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(DoctorAlertsActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAlertRead(int alertId) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("action", "mark_read");
        body.put("alert_id", alertId);

        api.postDoctorAlertAction(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DoctorAlertsActivity.this,
                            "Marked as read", Toast.LENGTH_SHORT).show();
                    loadAlerts(); // Refresh
                } else {
                    Toast.makeText(DoctorAlertsActivity.this,
                            "Failed to mark as read", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(DoctorAlertsActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_alerts);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DoctordashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_patients) {
                startActivity(new Intent(this, DoctorPatientsActivity.class));
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