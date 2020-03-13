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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

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
        if (studentCredentials.getString("studentId", null)!=null && studentCredentials.getString("password", null)!=null)
        {
            Intent intent = new Intent(this, MainMenuActivity.class);
            finish();
            startActivity(intent);
            overridePendingTransition(0, 0); // Avoid sliding animation
            return;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Get relative DP size
        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        final MaterialButton button = findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // entry StudentID, entry Pass
                TextInputEditText eStud = findViewById(R.id.entry_studentid);
                TextInputEditText ePass = findViewById(R.id.entry_pass);

                GradientDrawable gStudBackground = (GradientDrawable) eStud.getBackground();
                GradientDrawable gPassBackground = (GradientDrawable) ePass.getBackground();

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
                    try
                    {
                        CookieManager cm = new CookieManager();
                        CookieHandler.setDefault(cm);

                        URL url = new URL("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm");

                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        con.setRequestMethod("POST");
                        con.setDoOutput(true);
                        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                        String studId = eStud.getText().toString(), pass = ePass.getText().toString();
                        String params = String.format("LoggingOn=1&OgrNo=%s&Sifre=%s", studId, pass);
                        con.setFixedLengthStreamingMode(params.getBytes().length);
                        con.getOutputStream().write(params.getBytes());
                        con.connect();

                        Scanner scnr = new Scanner(con.getInputStream()).useDelimiter("\\A");
                        String body = scnr.hasNext() ? scnr.next() : "";

                        boolean success = true;
                        if (body.contains("<div class=\"alert alert-danger\" id=\"OgrNoUyari\"></div>"))
                        {
                            gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                            gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                            success = false;
                        }

                        // If login was successful, save cookies, save credentials
                        if (success)
                        {
                            editor.putString("studentId",studId);
                            editor.putString("password",pass);
                            editor.apply();

                            List<HttpCookie> cookies = cm.getCookieStore().getCookies();

                            for (HttpCookie cookie : cookies)
                            {
                                System.out.println(cookie);
                            }

                            Intent intent = new Intent(getBaseContext(), MainMenuActivity.class);
                            finish();
                            startActivity(intent);
                            overridePendingTransition(0, 0); // Avoid sliding animation
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
