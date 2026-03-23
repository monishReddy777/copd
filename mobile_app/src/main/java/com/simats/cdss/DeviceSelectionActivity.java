package com.simats.cdss;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.simats.cdss.models.DeviceRecommendationResponse;
import com.simats.cdss.models.GenericResponse;
import com.simats.cdss.network.ApiService;
import com.simats.cdss.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceSelectionActivity extends AppCompatActivity {

    private static final String TAG = "DeviceSelection";

    // Cache for ML device recommendations (patient_id -> device_name)
    private static final LruCache<Integer, String> recommendationCache = new LruCache<>(20);

    // Green highlight colors
    private static final int COLOR_RECOMMENDED_BORDER = Color.parseColor("#1FA37A");
    private static final int COLOR_RECOMMENDED_BG = Color.parseColor("#F0FDF4");
    private static final int COLOR_SELECTED_BORDER = Color.parseColor("#139487");
    private static final int COLOR_DEFAULT_BG = Color.parseColor("#FFFFFF");

    private int patientId = -1;
    private MaterialCardView selectedCard = null;
    private String selectedDevice = "";
    private String selectedFlowRange = "";
    private String recommendedDeviceName = "";
    private MaterialCardView recommendedCard = null;

    private Button btnConfirm;
    private ProgressBar progressLoading;
    private MaterialCardView cardVenturi, cardNasal, cardHighFlow, cardNonRebreather;

    // Individual badges for each card
    private TextView badgeVenturi, badgeNasal, badgeHighFlow, badgeNonRebreather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selection);

        patientId = getIntent().getIntExtra("patient_id", -1);

        btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setEnabled(false);
        progressLoading = findViewById(R.id.progress_loading);

        // Initialize Cards
        cardVenturi = findViewById(R.id.card_venturi);
        cardNasal = findViewById(R.id.card_nasal);
        cardHighFlow = findViewById(R.id.card_high_flow);
        cardNonRebreather = findViewById(R.id.card_non_rebreather);

        // Initialize Badges
        badgeVenturi = findViewById(R.id.badge_venturi);
        badgeNasal = findViewById(R.id.badge_nasal);
        badgeHighFlow = findViewById(R.id.badge_high_flow);
        badgeNonRebreather = findViewById(R.id.badge_non_rebreather);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        setupSelectionListeners();

        // Try cache first for instant display, then fetch fresh data
        loadRecommendationFromCache();
        fetchDeviceRecommendation();

        btnConfirm.setOnClickListener(v -> {
            if (patientId == -1) {
                Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDevice.isEmpty()) {
                Toast.makeText(this, "Please select a device", Toast.LENGTH_SHORT).show();
                return;
            }
            saveDeviceSelection();
        });

        setupBottomNav();
    }

    private void loadRecommendationFromCache() {
        if (patientId == -1) return;
        String cached = recommendationCache.get(patientId);
        if (cached != null && !cached.isEmpty()) {
            Log.d(TAG, "Using cached recommendation: " + cached);
            recommendedDeviceName = cached;
            highlightRecommendedDevice(cached);
        }
    }

    private void fetchDeviceRecommendation() {
        fetchDeviceRecommendationWithRetry(0);
    }

    private void fetchDeviceRecommendationWithRetry(int attempt) {
        if (patientId == -1) return;

        // Show loading indicator while fetching
        if (progressLoading != null && attempt == 0) progressLoading.setVisibility(View.VISIBLE);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getDeviceRecommendation(patientId).enqueue(new Callback<DeviceRecommendationResponse>() {
            @Override
            public void onResponse(Call<DeviceRecommendationResponse> call, Response<DeviceRecommendationResponse> response) {
                if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    DeviceRecommendationResponse rec = response.body();
                    String rawName = rec.getRecommendedDevice();
                    Log.d(TAG, "AI Recommended: " + rawName +
                            " (confidence: " + rec.getConfidenceScore() + ")");

                    // Normalize device name for matching
                    recommendedDeviceName = normalizeDeviceName(rawName);

                    // Cache the result for instant display next time
                    recommendationCache.put(patientId, recommendedDeviceName);

                    highlightRecommendedDevice(recommendedDeviceName);
                } else {
                    Log.e(TAG, "Recommendation API failed: " + response.code());
                    // Retry once on failure
                    if (attempt < 1) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            () -> fetchDeviceRecommendationWithRetry(attempt + 1), 1000);
                    }
                }
            }

            @Override
            public void onFailure(Call<DeviceRecommendationResponse> call, Throwable t) {
                if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Recommendation Network Error: " + t.getMessage());
                // Retry once on network failure
                if (attempt < 1) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        () -> fetchDeviceRecommendationWithRetry(attempt + 1), 1500);
                }
            }
        });
    }

    /**
     * Normalize device names from the backend to match the UI's switch-case labels.
     */
    private String normalizeDeviceName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "";
        String lower = rawName.toLowerCase();
        if (lower.contains("venturi")) return "Venturi Mask";
        if (lower.contains("high") && lower.contains("flow")) return "High Flow Nasal Cannula";
        if (lower.contains("non") && lower.contains("rebreather")) return "Non-Rebreather Mask";
        if (lower.contains("nasal") && lower.contains("cannula")) return "Nasal Cannula";
        return rawName; // fallback
    }

    private void highlightRecommendedDevice(String device) {
        // First hide all badges
        hideAllBadges();

        MaterialCardView targetCard = null;
        TextView targetBadge = null;

        switch (device) {
            case "Venturi Mask":
                targetCard = cardVenturi;
                targetBadge = badgeVenturi;
                break;
            case "Nasal Cannula":
                targetCard = cardNasal;
                targetBadge = badgeNasal;
                break;
            case "High Flow Nasal Cannula":
                targetCard = cardHighFlow;
                targetBadge = badgeHighFlow;
                break;
            case "Non-Rebreather Mask":
                targetCard = cardNonRebreather;
                targetBadge = badgeNonRebreather;
                break;
        }

        if (targetCard != null && targetBadge != null) {
            // Store reference to recommended card
            recommendedCard = targetCard;

            // Apply green recommended styling
            targetCard.setStrokeWidth(4);
            targetCard.setStrokeColor(COLOR_RECOMMENDED_BORDER);
            targetCard.setCardBackgroundColor(COLOR_RECOMMENDED_BG);

            // Show RECOMMENDED badge
            targetBadge.setVisibility(View.VISIBLE);

            // Auto-select the recommended device
            selectedCard = targetCard;
            setSelectedDeviceFromCard(targetCard);
            btnConfirm.setEnabled(true);
        }
    }

    private void hideAllBadges() {
        badgeVenturi.setVisibility(View.GONE);
        badgeNasal.setVisibility(View.GONE);
        badgeHighFlow.setVisibility(View.GONE);
        badgeNonRebreather.setVisibility(View.GONE);
    }

    private void setupSelectionListeners() {
        View.OnClickListener listener = v -> {
            if (v instanceof MaterialCardView) {
                handleSelection((MaterialCardView) v);
            }
        };

        cardVenturi.setOnClickListener(listener);
        cardNasal.setOnClickListener(listener);
        cardHighFlow.setOnClickListener(listener);
        cardNonRebreather.setOnClickListener(listener);
    }

    private void handleSelection(MaterialCardView card) {
        // Reset all cards to default
        resetAllCards();

        // Apply selection styling
        selectedCard = card;
        card.setStrokeWidth(4);
        card.setStrokeColor(COLOR_SELECTED_BORDER);

        // If this card is also the AI-recommended card, apply green background + badge
        if (card == recommendedCard) {
            card.setCardBackgroundColor(COLOR_RECOMMENDED_BG);
            showBadgeForCard(card);
        }

        // Set device name and flow range
        setSelectedDeviceFromCard(card);

        // Enable Confirm Button
        btnConfirm.setEnabled(true);
    }

    private void resetAllCards() {
        // Reset all cards to default white background and no stroke
        cardVenturi.setStrokeWidth(0);
        cardVenturi.setCardBackgroundColor(COLOR_DEFAULT_BG);

        cardNasal.setStrokeWidth(0);
        cardNasal.setCardBackgroundColor(COLOR_DEFAULT_BG);

        cardHighFlow.setStrokeWidth(0);
        cardHighFlow.setCardBackgroundColor(COLOR_DEFAULT_BG);

        cardNonRebreather.setStrokeWidth(0);
        cardNonRebreather.setCardBackgroundColor(COLOR_DEFAULT_BG);

        // Hide all badges
        hideAllBadges();
    }

    private void showBadgeForCard(MaterialCardView card) {
        if (card == cardVenturi) badgeVenturi.setVisibility(View.VISIBLE);
        else if (card == cardNasal) badgeNasal.setVisibility(View.VISIBLE);
        else if (card == cardHighFlow) badgeHighFlow.setVisibility(View.VISIBLE);
        else if (card == cardNonRebreather) badgeNonRebreather.setVisibility(View.VISIBLE);
    }

    private void setSelectedDeviceFromCard(MaterialCardView card) {
        if (card == cardVenturi) {
            selectedDevice = "Venturi Mask";
            selectedFlowRange = "24% - 60%";
        } else if (card == cardNasal) {
            selectedDevice = "Nasal Cannula";
            selectedFlowRange = "1 - 4 L/min";
        } else if (card == cardHighFlow) {
            selectedDevice = "High Flow Nasal Cannula";
            selectedFlowRange = "30 - 60 L/min";
        } else if (card == cardNonRebreather) {
            selectedDevice = "Non-Rebreather Mask";
            selectedFlowRange = "60% - 90%";
        }
    }

    private void saveDeviceSelection() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("selected_device", selectedDevice);
        body.put("flow_range", selectedFlowRange);

        apiService.saveDeviceSelection(body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(DeviceSelectionActivity.this, "Device selection saved", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DeviceSelectionActivity.this, ReviewRecommendationActivity.class);
                    intent.putExtra("patient_id", patientId);
                    intent.putExtra("selected_device", selectedDevice);
                    intent.putExtra("selected_flow_range", selectedFlowRange);
                    startActivity(intent);
                } else {
                    Toast.makeText(DeviceSelectionActivity.this, "Failed to save device selection", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(DeviceSelectionActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
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
                } else if (itemId == R.id.nav_patients) {
                    if ("staff".equals(role)) {
                        startActivity(new Intent(this, StaffPatientsActivity.class));
                    } else {
                        startActivity(new Intent(this, DoctorPatientsActivity.class));
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
                return false;
            });
        }
    }
}