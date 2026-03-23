package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.simats.cdss.models.UrgentActionResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UrgentActionActivity extends AppCompatActivity {

    private static final String TAG = "UrgentAction";
    private int patientId;
    
    private TextView tvIcuTitle;
    private View cardIcuTriggers;
    private LinearLayout layoutTriggersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urgent_action);

        patientId = getIntent().getIntExtra("patient_id", -1);

        tvIcuTitle = findViewById(R.id.tv_icu_title);
        cardIcuTriggers = findViewById(R.id.card_icu_triggers);
        layoutTriggersContainer = findViewById(R.id.layout_triggers_container);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.tv_return_dashboard).setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctordashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Dialpad functionality — opens empty dialpad
        findViewById(R.id.btn_call_icu).setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            startActivity(callIntent);
        });

        if (patientId != -1) {
            setupUrgentActionData();
        } else {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUrgentActionData() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getUrgentAction(patientId).enqueue(new Callback<UrgentActionResponse>() {
            @Override
            public void onResponse(Call<UrgentActionResponse> call, Response<UrgentActionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UrgentActionResponse data = response.body();
                    
                    if (data.isIcuRequired()) {
                        tvIcuTitle.setText("ICU Review Required");
                        cardIcuTriggers.setVisibility(View.VISIBLE);
                        
                        layoutTriggersContainer.removeAllViews();
                        if (data.getTriggers() != null) {
                            for (String trigger : data.getTriggers()) {
                                addTriggerView(trigger);
                            }
                        }
                    } else {
                        tvIcuTitle.setText("Patient Stable");
                        cardIcuTriggers.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Failed to load urgent action info");
                }
            }

            @Override
            public void onFailure(Call<UrgentActionResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching urgent action data", t);
                Toast.makeText(UrgentActionActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTriggerView(String triggerText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 0); // approx 8dp
        row.setLayoutParams(params);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                dpToPx(8), dpToPx(8)
        );
        dot.setBackgroundResource(R.drawable.chip_red_rounded);
        dot.setLayoutParams(dotParams);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvParams.setMargins(dpToPx(12), 0, 0, 0);
        tv.setLayoutParams(tvParams);
        tv.setText(triggerText);
        tv.setTextColor(Color.parseColor("#1A1C1E"));
        tv.setTextSize(14f);

        row.addView(dot);
        row.addView(tv);

        layoutTriggersContainer.addView(row);
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}