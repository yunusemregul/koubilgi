package com.koubilgi.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.MainApplication;
import com.koubilgi.R;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.setActiveActivity(this);
    }
}
