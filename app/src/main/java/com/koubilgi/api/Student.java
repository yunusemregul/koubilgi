package com.koubilgi.api;

import android.content.Context;
import android.content.SharedPreferences;

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

import static android.content.Context.MODE_PRIVATE;

public class Student
{
    private static String name;
    private static String number;
    private static String password;
    private static String department;

    private static boolean loggedIn;
    private static String cookieString;

    private static Context context;
    private static Student instance;
    private static CookieManager cookieManager;
    private static RequestQueue queue;

    private Student(Context ctx)
    {
        context = ctx;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
        loggedIn = false;
    }

    private void saveData()
    {
        if (!loggedIn)
            return;

        final SharedPreferences data = context.getSharedPreferences("data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = data.edit();

        editor.putString("studentName", name);
        editor.putString("studentNumber", number);
        editor.putString("studentDepartment", department);
        editor.putString("cookieString", cookieString);
        editor.apply();
    }

    private void saveCredentials()
    {
        if (!loggedIn)
            return;

        SharedPreferences studentCredentials = context.getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences.Editor editor = studentCredentials.edit();
        editor.putString("number", number);
        editor.putString("password", password);
        editor.apply();
    }

    public void logIn(final String num, final String pass, final LoginListener listener)
    {
        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        StringRequest postReq = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        if (response.contains("alert") && response.contains("hata"))
                        {
                            listener.onFailure("relogin");
                            return;
                        }

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

                            // Save cookies
                            CookieStore store = cookieManager.getCookieStore();
                            List<HttpCookie> cookies = store.getCookies();

                            String[] infoTxt = info.text().split(" ", 2);
                                /*
                                    index 1 = student name
                                    index 0 = student number
                                 */
                            name = infoTxt[1];
                            number = infoTxt[0];
                            password = pass;
                            cookieString = StringUtil.join(cookies, "; ");

                            // Save data and credentials for later
                            saveData();
                            saveCredentials();

                            listener.onSuccess(name, number);
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

    /*
        TODO:
            Return saved data if there is, if not, make the request.
     */
    public void personalInfo(final LoginListener listener)
    {
        if (!loggedIn)
            return;

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm";
        StringRequest postReq = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        if (response.contains("alert") && response.contains("hata"))
                        {
                            listener.onFailure("relogin");
                            return;
                        }

                        Document doc = Jsoup.parse(response);
                        Element form = doc.select("#OgrKisiselBilgiler").first();
                        Element boldDiv = form.select("b:contains(Bölüm)").first().parent();
                        Element departmentDiv = boldDiv.parent().select("div.col-sm-8").first();

                        /*
                            We are trying to extract department info from whole personal information page
                            First we find the div that has a child as <b>Bölüm</b> then
                            then get its parent and select the last element we need which is students
                            department.

                            Heres what these variables mean with an example:
                                boldDiv =
                                    <div class="col-sm-4"><b>Bölüm</b></div>

                                boldDiv.parent() =
                                    <div class="col-sm-6">
										<div class="col-sm-4"><b>Bölüm</b></div>
										<div class="col-sm-8">Bilgisayar Mühendisliği (İÖ) Bölümü</div>
									</div>

                                departmentDiv =
                                    <div class="col-sm-8">Bilgisayar Mühendisliği (İÖ) Bölümü</div>
                         */
                        String depart = departmentDiv.html();
                        depart = depart.replace("i", "İ");
                        depart = depart.replace(" Bölümü", "");
                        department = depart;
                        saveData();

                        listener.onSuccess(depart);
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
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", cookieString);

                return headers;
            }

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Anketid", "0");
                params.put("Baglanti", "Giris");
                params.put("Veri", "-1;-1");

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
