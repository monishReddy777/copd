package com.simats.cdss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        findViewById(R.id.card_doctor).setOnClickListener(v -> {
            startActivity(new Intent(RoleActivity.this, DoctorLoginActivity.class));
        });

        findViewById(R.id.card_staff).setOnClickListener(v -> {
            startActivity(new Intent(RoleActivity.this, StaffLoginActivity.class));
        });

        findViewById(R.id.card_admin).setOnClickListener(v -> {
            startActivity(new Intent(RoleActivity.this, AdminLoginActivity.class));
        });
    }
}