package com.koubilgi;

import android.content.Context;
import android.content.SharedPreferences;

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

import static android.content.Context.MODE_PRIVATE;

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

public class Student
{
    private static String name;
    private static String number;
    private static String department;

    private static boolean loggedIn;
    private static String cookieString;

    private static Context context;
    private static Student instance;
    private static CookieManager cookieManager;

    private Student(Context ctx)
    {
        context = ctx;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        loggedIn = false;
    }

    public void saveDataToFile()
    {
        if (!loggedIn)
            return;

        final SharedPreferences data = context.getSharedPreferences("data", MODE_PRIVATE);
        final SharedPreferences.Editor dataEditor = data.edit();

        dataEditor.putString("studentName", name);
        dataEditor.putString("studentNumber", number);
        dataEditor.putString("studentDepartment", department);
        dataEditor.putString("cookieString", cookieString);
        dataEditor.apply();
    }

    public void logIn(final String num, final String pass, final LoginListener listener)
    {
        final RequestQueue queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        StringRequest postReq = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        boolean success = true;
                        if (response.contains("<div class=\"alert alert-danger\" id=\"OgrNoUyari\"></div>"))
                            success = false;

                        Document doc = Jsoup.parse(response);

                        // Extract student name and number
                        Element info = doc.select("h4").first();

                        if (info == null)
                            success = false;

                        // If login was successful or not inform so
                        if (success)
                        {
                            loggedIn = true;
                            CookieStore store = cookieManager.getCookieStore();
                            List<HttpCookie> cookies = store.getCookies();

                            String[] infoTxt = info.text().split(" ", 2);
                                /*
                                    index 1 = student name
                                    index 0 = student number
                                 */
                            name = infoTxt[1];
                            number = infoTxt[0];
                            cookieString = StringUtil.join(cookies, "; ");

                            listener.onSuccess(name, number);

                            // Save data for later
                            saveDataToFile();
                        } else
                        {
                            // We assume that we failed because of the login credentials given
                            listener.onFailure("credentials");
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // We assume that we failed because site is not reachable
                        listener.onFailure("site");
                    }
                }
        )
        {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("LoggingOn", "1");
                params.put("OgrNo", num);
                params.put("Sifre", pass);

                return params;
            }
        };
        queue.add(postReq);
    }

    public static synchronized Student getInstance(Context ctx)
    {
        if (instance == null)
            instance = new Student(ctx);

        return instance;
    }

    public static String getName()
    {
        return name;
    }

    public static String getNumber()
    {
        return number;
    }

    public static String getDepartment()
    {
        return department;
    }

    public static boolean isLoggedIn()
    {
        return loggedIn;
    }
}
