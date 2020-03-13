package com.koubilgi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;

public class MainMenuActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);

        final String studId = studentCredentials.getString("studentNumber", null),
                pass = studentCredentials.getString("password", null);

        if (studId == null || pass == null)
            return;

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        StringRequest postReq = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        // TODO: Save data to use in offline mode.

                        Document doc = Jsoup.parse(response);

                        // Extract student name and number
                        Element info = doc.select("h4").first();
                        if (info != null)
                        {
                            String[] infoTxt = info.text().split(" ", 2);
                            TextView tStudentName = findViewById(R.id.studentName),
                                    tStudentNumber = findViewById(R.id.studentNumber);

                            tStudentName.setText(infoTxt[1]);
                            tStudentNumber.setText(infoTxt[0]);

                            /*
                                TODO:
                                 Get department info from
                                    'https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm'
                                 with parameters
                                     'Anketid:0
                                      Baglanti:Giris
                                      Veri:-1;-1'
                                 and set R.id.studentDepartment
                             */
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO: Show error screen and go offline mode maybe?
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
        SingletonRequestQueue.getInstance(this).add(postReq);
    }
}
