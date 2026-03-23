package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import com.simats.cdss.adapters.StaffPatientAdapter;
import com.simats.cdss.models.PatientResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffPatientsActivity extends AppCompatActivity {

    private MaterialCardView btnAll, btnCritical, btnWarning, btnStable;
    private TextView tvAll, tvCritical, tvWarning, tvStable;
    
    private RecyclerView rvPatients;
    private StaffPatientAdapter patientAdapter;
    private List<PatientResponse> allPatients = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_patient_list);

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

        // Set up RecyclerView
        rvPatients = findViewById(R.id.rv_patients);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        String fromAction = getIntent().getStringExtra("from_action");
        patientAdapter = new StaffPatientAdapter(this, new ArrayList<>(), fromAction);
        rvPatients.setAdapter(patientAdapter);

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

    private void searchList(String query) {
        List<PatientResponse> filteredList = new ArrayList<>();
        for (PatientResponse p : allPatients) {
            String name = p.getName() != null ? p.getName() : "";
            if (name.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(p);
            }
        }
        patientAdapter.updateList(filteredList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatients();
    }

    private void fetchPatients() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getPatients().enqueue(new Callback<List<PatientResponse>>() {
            @Override
            public void onResponse(Call<List<PatientResponse>> call, Response<List<PatientResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPatients = response.body();
                    filterList("all"); // Refresh views
                } else {
                    Toast.makeText(StaffPatientsActivity.this, "Failed to load patients", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PatientResponse>> call, Throwable t) {
                Toast.makeText(StaffPatientsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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
        resetButton(btnAll, tvAll);
        resetButton(btnCritical, tvCritical);
        resetButton(btnWarning, tvWarning);
        resetButton(btnStable, tvStable);

        List<PatientResponse> filteredList = new ArrayList<>();

        switch (status.toLowerCase()) {
            case "all":
                selectButton(btnAll, tvAll);
                filteredList = new ArrayList<>(allPatients);
                break;
            case "critical":
                selectButton(btnCritical, tvCritical);
                for (PatientResponse p : allPatients) {
                    if ("critical".equalsIgnoreCase(p.getStatus())) {
                        filteredList.add(p);
                    }
                }
                break;
            case "warning":
                selectButton(btnWarning, tvWarning);
                for (PatientResponse p : allPatients) {
                    if ("warning".equalsIgnoreCase(p.getStatus())) {
                        filteredList.add(p);
                    }
                }
                break;
            case "stable":
                selectButton(btnStable, tvStable);
                for (PatientResponse p : allPatients) {
                    if ("stable".equalsIgnoreCase(p.getStatus())) {
                        filteredList.add(p);
                    }
                }
                break;
        }
        
        patientAdapter.updateList(filteredList);
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
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_patients);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, StaffDashboardActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_alerts) {
                    startActivity(new Intent(this, StaffAlertsActivity.class));
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
}
