package com.simats.cdss;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClinicalGuidelinesActivity extends AppCompatActivity {

    // GOLD 2024 Report PDF URL
    private static final String GOLD_2024_PDF_URL =
            "https://goldcopd.org/2024-gold-report/";

    // BTS Oxygen Guidelines PDF URL
    private static final String BTS_OXYGEN_PDF_URL =
            "https://www.brit-thoracic.org.uk/quality-improvement/guidelines/emergency-oxygen/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clinical_guidelines);

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());

        // Handle click on "View PDF" for GOLD 2024 Report
        findViewById(R.id.btn_view_gold).setOnClickListener(v -> {
            openPdfUrl(GOLD_2024_PDF_URL, "GOLD 2024 Report");
        });

        // Handle click on "View PDF" for BTS Oxygen Guidelines
        findViewById(R.id.btn_view_bts).setOnClickListener(v -> {
            openPdfUrl(BTS_OXYGEN_PDF_URL, "BTS Oxygen Guidelines");
        });

        setupBottomNav();
    }

    private void openPdfUrl(String url, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser available to open " + title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open " + title + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DoctordashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_patients) {
                startActivity(new Intent(this, PatientListActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(this, DoctorAlertsActivity.class));
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