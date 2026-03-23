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
import com.simats.cdss.adapters.ReassessmentAdapter;
import com.simats.cdss.models.StaffDashboardResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StaffDashboard";

    private TextView tvStaffName, tvStaffDisplayName, tvBadgeCount, tvNoReassessments;
    private RecyclerView rvReassessments;
    private ReassessmentAdapter adapter;
    private List<StaffDashboardResponse.ReassessmentItem> reassessmentItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Bind views
        tvStaffName = findViewById(R.id.tv_staff_name);
        tvStaffDisplayName = findViewById(R.id.tv_staff_display_name);
        tvBadgeCount = findViewById(R.id.tv_badge_count);
        tvNoReassessments = findViewById(R.id.tv_no_reassessments);
        rvReassessments = findViewById(R.id.rv_reassessments);

        // Setup RecyclerView
        rvReassessments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReassessmentAdapter(this, reassessmentItems);
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, ReassessmentChecklistActivity.class);
            intent.putExtra("patient_id", item.getPatientId());
            intent.putExtra("reassessment_id", item.getId());
            intent.putExtra("patient_name", item.getPatientName());
            intent.putExtra("bed_no", item.getBedNumber());
            intent.putExtra("ward_no", item.getWardNo());
            intent.putExtra("reassessment_type", item.getType());
            startActivity(intent);
        });
        rvReassessments.setAdapter(adapter);

        // Notification Bell Navigation
        findViewById(R.id.card_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, StaffAlertsActivity.class));
        });

        // Quick Actions Navigation
        findViewById(R.id.card_add_patient).setOnClickListener(v -> {
            startActivity(new Intent(this, AddNewPatientActivity.class));
        });

        // Navigate to StaffPatientsActivity for Vitals entry
        findViewById(R.id.card_enter_vitals).setOnClickListener(v -> {
            Intent intent = new Intent(this, StaffPatientsActivity.class);
            intent.putExtra("from_action", "vitals");
            startActivity(intent);
        });

        // Navigate to StaffPatientsActivity for Symptoms entry
        findViewById(R.id.card_symptoms).setOnClickListener(v -> {
            Intent intent = new Intent(this, StaffPatientsActivity.class);
            intent.putExtra("from_action", "symptoms");
            startActivity(intent);
        });

        // Navigate to StaffPatientsActivity for ABG entry
        findViewById(R.id.card_enter_abg).setOnClickListener(v -> {
            Intent intent = new Intent(this, StaffPatientsActivity.class);
            intent.putExtra("from_action", "abg_entry");
            startActivity(intent);
        });

        setupBottomNav();

        // Load dashboard data from API
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        SessionManager session = new SessionManager(this);
        String email = session.getEmail();

        if (email == null || email.isEmpty()) {
            Log.w(TAG, "No staff email found in session");
            tvStaffName.setText("CLINICAL STAFF");
            return;
        }

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getStaffDashboard(email).enqueue(new Callback<StaffDashboardResponse>() {
            @Override
            public void onResponse(Call<StaffDashboardResponse> call, Response<StaffDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StaffDashboardResponse data = response.body();

                    // Set staff header info
                    StaffDashboardResponse.StaffInfo staff = data.getStaff();
                    if (staff != null) {
                        tvStaffName.setText("CLINICAL STAFF\n" + staff.getName());
                    }

                    // Set badge count
                    int pendingCount = data.getPendingCount();
                    tvBadgeCount.setText(pendingCount + " Due");

                    // Update reassessment list
                    reassessmentItems.clear();
                    List<StaffDashboardResponse.ReassessmentItem> items = data.getReassessments();
                    if (items != null && !items.isEmpty()) {
                        reassessmentItems.addAll(items);
                        rvReassessments.setVisibility(View.VISIBLE);
                        tvNoReassessments.setVisibility(View.GONE);
                    } else {
                        rvReassessments.setVisibility(View.GONE);
                        tvNoReassessments.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();

                } else {
                    Log.e(TAG, "Dashboard API error: " + response.code());
                    Toast.makeText(StaffDashboardActivity.this,
                            "Failed to load dashboard", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StaffDashboardResponse> call, Throwable t) {
                Log.e(TAG, "Dashboard API failure: " + t.getMessage());
                Toast.makeText(StaffDashboardActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_patients) {
                    startActivity(new Intent(this, StaffPatientsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    startActivity(new Intent(this, StaffAlertsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return itemId == R.id.nav_home;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}