package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.models.EscalationCriteriaResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EscalationCriteriaActivity extends AppCompatActivity {

    private static final String TAG = "EscalationCriteria";
    private int patientId;
    private View warningBanner;
    private TextView tvWarningDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escalation_criteria);

        patientId = getIntent().getIntExtra("patient_id", -1);
        
        warningBanner = findViewById(R.id.warning_banner);
        tvWarningDetails = findViewById(R.id.tv_warning_details);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_view_niv).setOnClickListener(v -> {
            Intent intent = new Intent(this, NIVRecommendationActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });
        
        if (patientId != -1) {
            setupEscalationData();
        } else {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEscalationData() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getEscalationCriteria(patientId).enqueue(new Callback<EscalationCriteriaResponse>() {
            @Override
            public void onResponse(Call<EscalationCriteriaResponse> call, Response<EscalationCriteriaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EscalationCriteriaResponse data = response.body();
                    if (data.isCriteriaMet()) {
                        warningBanner.setVisibility(View.VISIBLE);
                        int count = data.getEscalationTriggers() != null ? data.getEscalationTriggers().size() : 0;
                        if (count > 0) {
                            tvWarningDetails.setText("Patient meets " + count + " criteria for escalation of care.");
                        } else {
                            tvWarningDetails.setText("Patient meets criteria for escalation of care.");
                        }
                    } else {
                        warningBanner.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Failed to load escalation criteria");
                    Toast.makeText(EscalationCriteriaActivity.this, "Failed to load criteria", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<EscalationCriteriaResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching escalation criteria", t);
                Toast.makeText(EscalationCriteriaActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}