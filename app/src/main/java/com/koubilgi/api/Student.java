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

/*
    TODO:
        If university changes any of the pages,
        we should make the app go offline mode until been updated.
 */

/**
 * Student class that is single instanced over the whole app. It does the functionalities like logging
 * in, getting student's personal info and other student-related things.
 * <p>
 * Made referring to the Singleton design pattern.
 */
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

    private static SharedPreferences credentials;
    private static SharedPreferences data;

    /**
     * Constructs the singleton Student within given context.
     *
     * @param ctx the context we will be working in
     */
    private Student(Context ctx)
    {
        context = ctx;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
        data = context.getSharedPreferences("data", MODE_PRIVATE);
        credentials = context.getSharedPreferences("credentials", MODE_PRIVATE);
        loggedIn = false;
    }

    /**
     * Returns the only instance of Student, creates one if it does not exist.
     *
     * @param ctx the context we will be working in
     * @return singleton Student
     */
    public static synchronized Student getInstance(Context ctx)
    {
        if (instance == null)
            instance = new Student(ctx);

        return instance;
    }

    /**
     * Saves the student data using SharedPreferences for giving the user faster startup in later
     * uses.
     */
    private void saveData()
    {
        if (!loggedIn)
            return;

        SharedPreferences.Editor editor = data.edit();
        editor.putString("studentName", name);
        editor.putString("studentNumber", number);
        editor.putString("studentDepartment", department);
        editor.putString("cookieString", cookieString);
        editor.apply();
    }

    /**
     * Saves student credentials using SharedPreferences to use when logging in every time.
     */
    private void saveCredentials()
    {
        if (!loggedIn)
            return;

        SharedPreferences.Editor editor = credentials.edit();
        editor.putString("number", number);
        editor.putString("password", password);
        editor.apply();
    }

    /**
     * Logs in the student with given credentials. If the connection is successful, calls
     * listener.onSuccess with student's name and number, if the connection is not successful
     * calls listener.onFailure with the reason.
     *
     * @param num number of the logging in student
     * @param pass password of the logging in student
     * @param listener the listener that waits for the methods response
     */
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
                            listener.onFailure("credentials");
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
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

    /**
     * If this method has been called before, calls method listener.onSuccess with the parameter
     * of active student's department name.
     * If the method has never called before, connects to the school site and parses the
     * student department from students personal info, saves it for later uses, calls
     * listener.onSuccess with student's department name.
     *
     * Calls listener.onFailure with the reason if there's any errors.
     *
     * @param listener the listener that waits for the methods response
     */
    public void personalInfo(final LoginListener listener)
    {
        if (!loggedIn)
            return;

        if (data.getString("studentDepartment", null) != null)
        {
            listener.onSuccess(data.getString("studentDepartment", "Bilinmiyor"));
            return;
        }

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
