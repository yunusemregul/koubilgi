package com.koubilgi;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SubmenuActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_submenu);

        TextView header = findViewById(R.id.submenu_headertext);
        header.setText(getIntent().getExtras().getInt("name"));
    }
}
