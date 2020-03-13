package com.koubilgi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Saving student credentials could be encrypted but not on open-source
        SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences.Editor editor = studentCredentials.edit();

        // Start the main menu activity if user has already logged in
        if (studentCredentials.getString("studentNumber", null) != null && studentCredentials.getString("password", null) != null)
        {
            Intent intent = new Intent(this, MainMenuActivity.class);
            finish();
            startActivity(intent);
            overridePendingTransition(0, 0); // Avoid sliding animation
            return;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final RequestQueue queue = SingletonRequestQueue.getInstance(getApplicationContext()).getRequestQueue();

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
                    String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
                    StringRequest postReq = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>()
                            {
                                @Override
                                public void onResponse(String response)
                                {
                                    boolean success = true;
                                    if (response.contains("<div class=\"alert alert-danger\" id=\"OgrNoUyari\"></div>"))
                                    {
                                        gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                                        gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                                        success = false;
                                    }

                                    // If login was successful, save cookies, save credentials
                                    if (success)
                                    {
                                        editor.putString("studentNumber", studId);
                                        editor.putString("password", pass);
                                        editor.apply();

                                        Intent intent = new Intent(getBaseContext(), MainMenuActivity.class);
                                        finish();
                                        startActivity(intent);
                                        overridePendingTransition(0, 0); // Avoid sliding animation
                                    }
                                }
                            },
                            new Response.ErrorListener()
                            {
                                @Override
                                public void onErrorResponse(VolleyError error)
                                {
                                    // TODO: Show error screen 'Can not log in.'
                                }
                            }
                    )
                    {
                        @Override
                        protected Map<String, String> getParams()
                        {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("LoggingOn", "1");
                            params.put("OgrNo", studId);
                            params.put("Sifre", pass);

                            return params;
                        }
                    };
                    queue.add(postReq);
                }
            }
        });
    }
}
