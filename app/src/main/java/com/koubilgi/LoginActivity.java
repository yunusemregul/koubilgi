package com.koubilgi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Saving student credentials could be encrypted but not on open-source
        SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences.Editor editor = studentCredentials.edit();

        // Start the main menu activity if user has already logged in
        if (studentCredentials.contains("studentNumber") && studentCredentials.contains("password"))
        {
            finish();
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0); // Avoid sliding animation
            return;
        }

        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_login);

        final RequestQueue queue = SingletonRequestQueue.getInstance(this).getRequestQueue();

        // Get relative DP size
        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        final MaterialButton button = findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // entry StudentNumber, entry Pass
                TextInputEditText eStud = findViewById(R.id.entry_studentnumber),
                        ePass = findViewById(R.id.entry_pass);

                final GradientDrawable gStudBackground = (GradientDrawable) eStud.getBackground(),
                        gPassBackground = (GradientDrawable) ePass.getBackground();

                // Set red stroke of 2dp when there's no text on entries
                // Should find some better way to not repeat the code
                if (eStud.getText().length() == 0)
                    gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                else
                    gStudBackground.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (ePass.getText().length() == 0)
                    gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                else
                    gPassBackground.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (eStud.getText().length() > 0 && ePass.getText().length() > 0)
                {
                    // Login to the site with student credentials
                    final String studId = eStud.getText().toString(), pass = ePass.getText().toString();

                    Student.getInstance(getApplicationContext()).logIn(studId,
                            pass,
                            new LoginListener()
                            {
                                @Override
                                public void onSuccess(String name, String number)
                                {
                                    editor.putString("studentNumber", studId);
                                    editor.putString("password", pass);
                                    editor.apply();

                                    Intent intent = new Intent(getBaseContext(), MainMenuActivity.class);
                                    finish();
                                    startActivity(intent);
                                    overridePendingTransition(0, 0); // Avoid sliding animation
                                }

                                @Override
                                public void onFailure(String reason)
                                {
                                    if (reason.equals("credentials"))
                                    {
                                        gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                                        gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                                    }
                                }
                            });
                }
            }
        });
    }
}
