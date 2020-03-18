package com.koubilgi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    TODO:
        Make a separated singleton-pattern Student class.
 */

public class MainMenuActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_mainmenu);

        final TextView tStudentName = findViewById(R.id.studentName),
                tStudentNumber = findViewById(R.id.studentNumber);

        final RequestQueue queue = SingletonRequestQueue.getInstance(this).getRequestQueue();
        final SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences data = getSharedPreferences("data", MODE_PRIVATE);
        final SharedPreferences.Editor dataEditor = data.edit();

        final String studId = studentCredentials.getString("studentNumber", null),
                pass = studentCredentials.getString("password", null);

        if (studId == null || pass == null)
            return;

        if (data.contains("studentName"))
            tStudentName.setText(data.getString("studentName", "Bilinmeyen Öğrenci"));
        if (data.contains("studentNumber"))
            tStudentNumber.setText(data.getString("studentNumber","123456789"));

        final CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

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

                            tStudentName.setText(infoTxt[1]);
                            tStudentNumber.setText(infoTxt[0]);

                            dataEditor.putString("studentName",infoTxt[1]);
                            dataEditor.putString("studentNumber",infoTxt[0]);

                            CookieStore store = manager.getCookieStore();
                            List<HttpCookie> cookies = store.getCookies();

                            String cookieString = StringUtil.join(cookies, "; ");
                            dataEditor.putString("cookieString", cookieString);
                            dataEditor.apply();

                            /*
                                TODO:
                                 Get department info from
                                    'https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm'
                                 with POST parameters
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
                        // TODO: Show error screen 'Can not connect' and go offline mode maybe?
                    }
                }
        )
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String> headers = new HashMap<String, String>();

                if (data.contains("cookieString"))
                    headers.put("Cookie", data.getString("cookieString", ""));

                return headers;
            }

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
