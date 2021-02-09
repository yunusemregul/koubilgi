package com.koubilgi.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.MainApplication;
import com.koubilgi.R;
import com.koubilgi.submenus.SubmenuManager;

public class Submenu extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_submenu);

        int nameResource = getIntent().getExtras().getInt("name");
        TextView header = findViewById(R.id.submenu_headertext);
        header.setText(nameResource);

        SubmenuManager.getSubmenuByName(nameResource).fillContentView(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MainApplication.setActiveActivity(this);
    }
}
