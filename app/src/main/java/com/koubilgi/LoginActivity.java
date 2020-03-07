package com.koubilgi;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        final MaterialButton button = findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextInputEditText entry_studentid = findViewById(R.id.entry_studentid);
                TextInputEditText entry_pass = findViewById(R.id.entry_pass);

                GradientDrawable studentid_background = (GradientDrawable) entry_studentid.getBackground();
                GradientDrawable pass_background = (GradientDrawable) entry_pass.getBackground();

                // Set red stroke of 2dp when there's no text on entries
                if (entry_studentid.getText().length() == 0)
                    studentid_background.setStroke((int) metrics.density * 2, Color.RED);
                else
                     studentid_background.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (entry_pass.getText().length() == 0)
                    pass_background.setStroke((int) metrics.density * 2, Color.RED);
                else
                    pass_background.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (entry_studentid.getText().length() > 0 && entry_pass.getText().length() > 0)
                {
                    // Login to the site
                    try
                    {
                        CookieManager cm = new CookieManager();
                        CookieHandler.setDefault(cm);

                        // https://stackoverflow.com/a/35319026

                        URL url = new URL("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm");

                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        con.setRequestMethod("POST");
                        con.setDoOutput(true);
                        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                        String params = String.format("LoggingOn=1&OgrNo=%s&Sifre=%s", entry_studentid.getText(), entry_pass.getText());
                        con.setFixedLengthStreamingMode(params.getBytes().length);
                        PrintWriter out = new PrintWriter(con.getOutputStream());
                        out.print(params);
                        out.close();

                        con.connect();
                        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                        String strCurrentLine;
                        while ((strCurrentLine = br.readLine()) != null)
                        {
                            System.out.println(strCurrentLine);
                        }

                        List<HttpCookie> cookies = cm.getCookieStore().getCookies();

                        for (HttpCookie cookie : cookies)
                        {
                            System.out.println(cookie);
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
