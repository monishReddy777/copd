package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.simats.cdss.adapters.NeedsAttentionAdapter;
import com.simats.cdss.models.DoctorDashboardResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctordashboardActivity extends AppCompatActivity {

    private static final String TAG = "DoctorDashboard";

    private TextView tvDoctorLabel, tvDoctorName;
    private TextView tvTotalCount, tvCriticalCount, tvWarningCount;
    private TextView tvNoAttention;
    private RecyclerView rvNeedsAttention;
    private NeedsAttentionAdapter adapter;
    private List<DoctorDashboardResponse.PatientItem> patientList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        // Bind header views
        tvDoctorLabel = findViewById(R.id.tv_doctor_label);
        tvDoctorName = findViewById(R.id.tv_doctor_name);

        // Bind summary card views
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvCriticalCount = findViewById(R.id.tv_critical_count);
        tvWarningCount = findViewById(R.id.tv_warning_count);

        // Bind needs attention views
        tvNoAttention = findViewById(R.id.tv_no_attention);
        rvNeedsAttention = findViewById(R.id.rv_needs_attention);

        // Setup RecyclerView
        rvNeedsAttention.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NeedsAttentionAdapter(this, patientList);
        rvNeedsAttention.setAdapter(adapter);

        // Navigation to Patient List (All)
        findViewById(R.id.card_total_patients).setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorPatientsActivity.class));
        });

        findViewById(R.id.tv_view_all).setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorPatientsActivity.class));
        });

        // Navigation to Critical Patients
        findViewById(R.id.card_critical_patients).setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorPatientsActivity.class);
            intent.putExtra("filter", "critical");
            startActivity(intent);
        });

        // Navigation to Warning Patients
        findViewById(R.id.card_warning_patients).setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorPatientsActivity.class);
            intent.putExtra("filter", "warning");
            startActivity(intent);
        });

        // Notification bell
        findViewById(R.id.card_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorAlertsActivity.class));
        });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_patients) {
                    startActivity(new Intent(this, DoctorPatientsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    startActivity(new Intent(this, DoctorAlertsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return itemId == R.id.nav_home;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Fetch Dashboard Data
        fetchDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDashboardData();
    }

    private void fetchDashboardData() {
        SessionManager session = new SessionManager(this);
        String email = session.getEmail();

        if (email == null || email.isEmpty()) {
            Log.w(TAG, "No doctor email found in session");
            tvDoctorLabel.setText("DOCTOR");
            tvDoctorName.setText("");
            return;
        }

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getDoctorDashboard(email).enqueue(new Callback<DoctorDashboardResponse>() {
            @Override
            public void onResponse(Call<DoctorDashboardResponse> call, Response<DoctorDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DoctorDashboardResponse data = response.body();

                    // Set doctor header: DOCTOR + Dr.<name>
                    DoctorDashboardResponse.DoctorInfo doc = data.getDoctor();
                    if (doc != null) {
                        tvDoctorLabel.setText("DOCTOR");
                        tvDoctorName.setText("Dr." + doc.getName());
                    }

                    // Set summary card counts
                    DoctorDashboardResponse.Summary summary = data.getSummary();
                    if (summary != null) {
                        tvTotalCount.setText(String.valueOf(summary.getTotalPatients()));
                        tvCriticalCount.setText(String.valueOf(summary.getCritical()));
                        tvWarningCount.setText(String.valueOf(summary.getWarning()));
                    }

                    // Update Needs Attention list
                    patientList.clear();
                    List<DoctorDashboardResponse.PatientItem> items = data.getPatients();
                    if (items != null && !items.isEmpty()) {
                        patientList.addAll(items);
                        rvNeedsAttention.setVisibility(View.VISIBLE);
                        tvNoAttention.setVisibility(View.GONE);
                    } else {
                        rvNeedsAttention.setVisibility(View.GONE);
                        tvNoAttention.setVisibility(View.VISIBLE);
                    }
                    adapter.updateList(patientList);

                } else {
                    Log.e(TAG, "Dashboard API error: " + response.code());
                    Toast.makeText(DoctordashboardActivity.this,
                            "Failed to load dashboard", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DoctorDashboardResponse> call, Throwable t) {
                Log.e(TAG, "Dashboard API failure: " + t.getMessage());
                Toast.makeText(DoctordashboardActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}