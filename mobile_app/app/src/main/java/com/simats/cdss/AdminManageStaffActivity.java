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
import com.simats.cdss.adapters.StaffAdapter;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.models.Staff;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageStaffActivity extends AppCompatActivity implements StaffAdapter.OnStaffActionListener {

    private RecyclerView rvStaff;
    private StaffAdapter adapter;
    private ProgressBar progressBar;
    private ApiService apiService;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_staff);

        // Initialize views
        rvStaff = findViewById(R.id.rv_staff);
        progressBar = findViewById(R.id.progress_bar);
        etSearch = findViewById(R.id.et_search_staff);
        
        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Setup RecyclerView
        rvStaff.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StaffAdapter(new ArrayList<>(), this);
        rvStaff.setAdapter(adapter);

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

        // Load staff
        fetchStaff();
    }

    private void fetchStaff() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getStaff().enqueue(new Callback<List<Staff>>() {
            @Override
            public void onResponse(Call<List<Staff>> call, Response<List<Staff>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateList(response.body());
                } else {
                    Toast.makeText(AdminManageStaffActivity.this, "Failed to load staff", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Staff>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminManageStaffActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onToggleStatus(Staff staff, int position, boolean isActive) {
        Map<String, Object> body = new HashMap<>();
        body.put("staff_id", staff.getId());
        body.put("is_active", isActive);

        apiService.toggleStaffStatus(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    String status = isActive ? "active" : "disabled";
                    adapter.updateItemStatus(position, status);
                    Toast.makeText(AdminManageStaffActivity.this, "Staff " + status, Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(AdminManageStaffActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                adapter.notifyItemChanged(position);
                Toast.makeText(AdminManageStaffActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRemoveStaff(Staff staff, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Staff")
                .setMessage("Are you sure you want to permanently remove " + staff.getName() + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    apiService.deleteStaff(staff.getId()).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful()) {
                                adapter.removeItem(position);
                                Toast.makeText(AdminManageStaffActivity.this, "Staff removed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(AdminManageStaffActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewDetails(Staff staff) {
        Intent intent = new Intent(this, AdminStaffDetailsActivity.class);
        intent.putExtra("staff_id", staff.getId());
        startActivity(intent);
    }
}