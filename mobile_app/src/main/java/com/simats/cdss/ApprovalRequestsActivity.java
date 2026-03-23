package com.simats.cdss;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.simats.cdss.adapters.ApprovalRequestAdapter;
import com.simats.cdss.models.ApprovalRequest;
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

public class ApprovalRequestsActivity extends AppCompatActivity implements ApprovalRequestAdapter.OnActionClickListener {

    private static final String TAG = "ApprovalRequestsActivity";

    private RecyclerView rvApprovalRequests;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearch;
    private ApprovalRequestAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approvals);

        // Initialize views
        rvApprovalRequests = findViewById(R.id.rv_approval_requests);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        etSearch = findViewById(R.id.et_search);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Setup RecyclerView
        rvApprovalRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovalRequestAdapter(new ArrayList<>(), this);
        rvApprovalRequests.setAdapter(adapter);

        // Setup search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize Retrofit
        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        // Fetch data
        fetchApprovalRequests();
    }

    private void fetchApprovalRequests() {
        progressBar.setVisibility(View.VISIBLE);
        rvApprovalRequests.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        apiService.getApprovalRequests().enqueue(new Callback<List<ApprovalRequest>>() {
            @Override
            public void onResponse(Call<List<ApprovalRequest>> call, Response<List<ApprovalRequest>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<ApprovalRequest> requests = response.body();

                    if (requests.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvApprovalRequests.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvApprovalRequests.setVisibility(View.VISIBLE);
                        adapter.updateList(requests);
                    }
                } else {
                    Log.e(TAG, "Failed to fetch approval requests: " + response.code());
                    Toast.makeText(ApprovalRequestsActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                    emptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<ApprovalRequest>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(ApprovalRequestsActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onApproveClick(ApprovalRequest request, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Approve Request")
            .setMessage("Are you sure you want to approve " + request.getName() + "?")
            .setPositiveButton("Approve", (dialog, which) -> {
                approveUser(request, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onRejectClick(ApprovalRequest request, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Reject Request")
            .setMessage("Are you sure you want to reject " + request.getName() + "?")
            .setPositiveButton("Reject", (dialog, which) -> {
                rejectUser(request, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void approveUser(ApprovalRequest request, int position) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", request.getId());
        body.put("user_type", request.getUserType());

        apiService.approveUser(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ApprovalRequestsActivity.this, "User Approved", Toast.LENGTH_SHORT).show();
                    adapter.removeItem(position);
                    
                    if (adapter.getItemCount() == 0) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvApprovalRequests.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Approval Failed: " + response.code());
                    Toast.makeText(ApprovalRequestsActivity.this, "Failed to approve", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "Approval Network Error: " + t.getMessage());
                Toast.makeText(ApprovalRequestsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectUser(ApprovalRequest request, int position) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", request.getId());
        body.put("user_type", request.getUserType());

        apiService.rejectUser(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ApprovalRequestsActivity.this, "User Rejected", Toast.LENGTH_SHORT).show();
                    adapter.removeItem(position);

                    if (adapter.getItemCount() == 0) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvApprovalRequests.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Rejection Failed: " + response.code());
                    Toast.makeText(ApprovalRequestsActivity.this, "Failed to reject", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "Rejection Network Error: " + t.getMessage());
                Toast.makeText(ApprovalRequestsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
