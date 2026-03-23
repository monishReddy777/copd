package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.adapters.DoctorPatientAdapter;
import com.simats.cdss.models.PatientResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorPatientsActivity extends AppCompatActivity {

    private MaterialCardView btnAll, btnCritical, btnWarning, btnStable;
    private TextView tvAll, tvCritical, tvWarning, tvStable;
    
    private RecyclerView rvPatients;
    private DoctorPatientAdapter adapter;
    private List<PatientResponse> allPatients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        // Initialize Filter Buttons
        btnAll = findViewById(R.id.btn_all);
        btnCritical = findViewById(R.id.btn_critical);
        btnWarning = findViewById(R.id.btn_warning);
        btnStable = findViewById(R.id.btn_stable);

        // Initialize Filter Texts
        tvAll = findViewById(R.id.tv_all);
        tvCritical = findViewById(R.id.tv_critical);
        tvWarning = findViewById(R.id.tv_warning);
        tvStable = findViewById(R.id.tv_stable);

        // Initialize RecyclerView
        rvPatients = findViewById(R.id.rv_patients);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorPatientAdapter(this, new ArrayList<>());
        rvPatients.setAdapter(adapter);

        // Initialize Search
        android.widget.EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchList(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        
        setupFilters();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatients();
    }

    private void searchList(String query) {
        List<PatientResponse> filteredList = new ArrayList<>();
        for (PatientResponse p : allPatients) {
            String name = p.getName() != null ? p.getName() : "";
            if (name.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(p);
            }
        }
        adapter.updateList(filteredList);
    }

    private void fetchPatients() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatients().enqueue(new Callback<List<PatientResponse>>() {
            @Override
            public void onResponse(Call<List<PatientResponse>> call, Response<List<PatientResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPatients = response.body();
                    
                    // Check if a filter was passed from the Dashboard after data load
                    String filter = getIntent().getStringExtra("filter");
                    if (filter != null) {
                        filterList(filter);
                    } else {
                        filterList("all"); // Default view
                    }
                }
            }

            @Override
            public void onFailure(Call<List<PatientResponse>> call, Throwable t) {
                // handle failure
            }
        });
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> filterList("all"));
        btnCritical.setOnClickListener(v -> filterList("critical"));
        btnWarning.setOnClickListener(v -> filterList("warning"));
        btnStable.setOnClickListener(v -> filterList("stable"));
    }

    private void filterList(String status) {
        // Reset all buttons to unselected state
        resetButton(btnAll, tvAll);
        resetButton(btnCritical, tvCritical);
        resetButton(btnWarning, tvWarning);
        resetButton(btnStable, tvStable);

        List<PatientResponse> filteredList = new ArrayList<>();

        switch (status.toLowerCase()) {
            case "all":
                selectButton(btnAll, tvAll);
                filteredList.addAll(allPatients);
                break;
            case "critical":
                selectButton(btnCritical, tvCritical);
                for (PatientResponse p : allPatients) {
                    if ("critical".equalsIgnoreCase(p.getStatus())) filteredList.add(p);
                }
                break;
            case "warning":
                selectButton(btnWarning, tvWarning);
                for (PatientResponse p : allPatients) {
                    if ("warning".equalsIgnoreCase(p.getStatus())) filteredList.add(p);
                }
                break;
            case "stable":
                selectButton(btnStable, tvStable);
                for (PatientResponse p : allPatients) {
                    if ("stable".equalsIgnoreCase(p.getStatus())) filteredList.add(p);
                }
                break;
        }
        
        adapter.updateList(filteredList);
    }

    private void selectButton(MaterialCardView card, TextView text) {
        card.setCardBackgroundColor(getResources().getColor(R.color.primary_teal));
        card.setStrokeWidth(0);
        text.setTextColor(Color.WHITE);
    }

    private void resetButton(MaterialCardView card, TextView text) {
        card.setCardBackgroundColor(Color.WHITE);
        card.setStrokeWidth(1);
        card.setStrokeColor(Color.parseColor("#E2E8F0"));
        text.setTextColor(Color.parseColor("#64748B"));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
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
            return itemId == R.id.nav_patients;
        });
    }
}
