package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.simats.cdss.adapters.DoctorAdapter;
import com.simats.cdss.models.Doctor;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageDoctorsActivity extends AppCompatActivity implements DoctorAdapter.OnDoctorActionListener {

    private RecyclerView rvDoctors;
    private DoctorAdapter adapter;
    private ProgressBar progressBar;
    private ApiService apiService;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_doctors);

        // Initialize views
        rvDoctors = findViewById(R.id.rv_doctors);
        progressBar = findViewById(R.id.progress_bar);
        etSearch = findViewById(R.id.et_search_doctors);
        
        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Setup RecyclerView
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorAdapter(new ArrayList<>(), this);
        rvDoctors.setAdapter(adapter);

        // Initialize API Service
        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        // Setup search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load doctors
        fetchDoctors();
    }

    private void fetchDoctors() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDoctors().enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    adapter.updateList(response.body());
                } else {
                    showMockData();
                }
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showMockData();
            }
        });
    }

    private void showMockData() {
        List<Doctor> mockList = new ArrayList<>();
        
        // Creating mock data to match the "sandhiya" design requested
        mockList.add(createMockDoctor(1, "sandhiya", "Doctor", "null", "disabled"));
        mockList.add(createMockDoctor(2, "sandhiya", "Doctor", "null", "active"));
        mockList.add(createMockDoctor(3, "sandhiya", "Doctor", "null", "active"));
        
        adapter.updateList(mockList);
        Toast.makeText(this, "Displaying sample data", Toast.LENGTH_SHORT).show();
    }

    private Doctor createMockDoctor(int id, String name, String spec, String license, String status) {
        // We use JSON to populate the private fields of the Doctor model safely
        String json = "{\"id\":" + id + ", \"name\":\"" + name + "\", \"specialization\":\"" + spec + "\", \"license_number\":\"" + license + "\", \"status\":\"" + status + "\"}";
        return new Gson().fromJson(json, Doctor.class);
    }

    @Override
    public void onToggleStatus(Doctor doctor, int position, boolean isActive) {
        Map<String, Object> body = new HashMap<>();
        body.put("doctor_id", doctor.getId());
        body.put("is_active", isActive);

        apiService.toggleDoctorStatus(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    String status = isActive ? "active" : "disabled";
                    adapter.updateItemStatus(position, status);
                    Toast.makeText(AdminManageDoctorsActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                adapter.notifyItemChanged(position);
                Toast.makeText(AdminManageDoctorsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRemoveDoctor(Doctor doctor, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Doctor")
                .setMessage("Are you sure you want to remove " + doctor.getName() + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    apiService.deleteDoctor(doctor.getId()).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful()) {
                                adapter.removeItem(position);
                                Toast.makeText(AdminManageDoctorsActivity.this, "Doctor removed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(AdminManageDoctorsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewDetails(Doctor doctor) {
        Intent intent = new Intent(this, AdminDoctorDetailsActivity.class);
        intent.putExtra("doctor_id", doctor.getId());
        startActivity(intent);
    }
}