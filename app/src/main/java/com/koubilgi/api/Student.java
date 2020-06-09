package com.koubilgi.api;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.koubilgi.R;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    TODO:
        If the university changes any of the pages, since we might not be able to support
        the changes, we should make the app go offline mode until been updated.
 */

/**
 * Student class that is single instanced over the whole app. It is responsible of logging in to the school site, making
 * get-post requests to the school site with student credentials and other student related things..
 *
 * Made referring to the Singleton design pattern.
 */
public class Student implements Serializable
{
    private static boolean loggedIn;
    private static Context context;
    private String name;
    private String number;
    private String password;

    private static Student instance;
    private String department;
    private String cookieString;
    private static CookieManager cookieManager;
    private static RequestQueue queue;

    private static AlertDialog recaptchaDialog;

    /**
     * Constructs the singleton Student within given context.
     *
     * @param ctx the context we will be working in
     */
    private Student(Context ctx)
    {
        loggedIn = false;
        context = ctx;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
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
        {
            instance = new Student(ctx);
            try
            {
                instance = instance.loadFromFile();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // To avoid window leaks
        context = ctx;

        return instance;
    }

    private void saveToFile() throws Exception
    {
        FileOutputStream outputStream = context.openFileOutput("student", Context.MODE_PRIVATE);
        ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
        objectStream.writeObject(this);
        objectStream.close();
        outputStream.close();
    }

    private Student loadFromFile() throws Exception
    {
        File file = context.getFileStreamPath("student");

        if (file == null || !file.exists())
            return this;

        FileInputStream inputStream = context.openFileInput("student");
        ObjectInputStream objectStream = new ObjectInputStream(inputStream);
        Student loaded = (Student) objectStream.readObject();
        objectStream.close();
        inputStream.close();

        loggedIn = true;

        return loaded;
    }

    /**
     * Logs in the student with given credentials. If the connection is successful, calls listener.onSuccess with
     * student's name and number, if the connection is not successful calls listener.onFailure with the reason.
     *
     * @param num      number of the logging in student
     * @param pass     password of the logging in student
     * @param listener the listener that waits for the methods response
     */
    public void logIn(final String num, final String pass, final ConnectionListener listener)
    {
        // Do not try to login again if we are logged in already
        if (loggedIn)
        {
            if (listener != null)
                listener.onSuccess(name, number);
            return;
        }

        // Logging in... screen
        AlertDialog.Builder logginginPopup = new AlertDialog.Builder(context);
        logginginPopup.setMessage(R.string.logging_in);

        logginginPopup.setCancelable(false);
        final AlertDialog loggingin = logginginPopup.show();

        final ConnectionListener logginginListener = new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                loggingin.dismiss();
                listener.onSuccess(args);
            }

            @Override
            public void onFailure(String reason)
            {
                loggingin.dismiss();
                listener.onFailure(reason);
                // TODO: Go offline mode.
            }
        };

        /*
            Making login requests to the school site every time the app starts is not a good practice I think. So I
            thought it would be better to check if we already logged in. We can check this by using our latest
            session cookies. If they are valid then do not try to log in again, if they are not valid then make a log
             in request.

            Checking session cookies is done by making a get request to any student page. I choose HarcBilgi page
            because it generally has the lowest Content-Length and thus we will waste the least amount of internet
            data I think. Which is good for users with limited mobile data.
         */

        final String harcurl = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/OgrenciIsleri/HarcBilgi.cfm";

        if (listener != null && name != null && number != null)
        {
            makeGetRequest(harcurl, new ConnectionListener()
            {
                @Override
                public void onSuccess(String... args)
                {
                    loggingin.dismiss();
                    listener.onSuccess(name, number);
                }

                @Override
                public void onFailure(String reason)
                {
                    makeLogInRequest(num, pass, logginginListener);
                }
            });
        }
        else
        {
            makeLogInRequest(num, pass, logginginListener);
        }
    }

    private void makeLogInRequest(final String num, final String pass, final ConnectionListener listener)
    {
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        getRecaptchaToken(new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                final String token = args[0];

                StringRequest postReq = new StringRequest(Request.Method.POST, url, new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        if (response.contains("alert") && response.contains("hata"))
                        {
                            if (listener != null)
                                listener.onFailure("relogin");
                            // TODO: Go offline mode
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
                            try
                            {
                                saveToFile();
                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            if (listener != null)
                                listener.onSuccess(name, number);
                        }
                        else
                        {
                            if (listener != null)
                                listener.onFailure("credentials");
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        if (listener != null)
                            listener.onFailure("site");
                    }
                })
                {
                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String> params = new HashMap<>();
                        params.put("LoggingOn", "1");
                        params.put("OgrNo", num);
                        params.put("Sifre", pass);
                        params.put("g-recaptcha-response", token);

                        return params;
                    }
                };
                queue.add(postReq);
            }

            @Override
            public void onFailure(String reason)
            {

            }
        });
    }

    private void getRecaptchaToken(final ConnectionListener listener)
    {
        Log.d("RECAPTCHA", "Called");
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        final WebView webView = new WebView(context);
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        webView.setVerticalScrollBarEnabled(false);

        dialogBuilder.setView(webView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle("reCAPTCHA");

        recaptchaDialog = dialogBuilder.show();
        recaptchaDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        webView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage)
            {
                String message = consoleMessage.message();
                if (message.startsWith("koubilgicaptchatoken:"))
                {
                    final String token = message.substring(21);

                    recaptchaDialog.dismiss();
                    listener.onSuccess(token);
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });

        // TODO: This looks bad and is not readable
        String data =
                "<html>\n" + "<head>\n" + "\t<script src=\"https://www.google.com/recaptcha/api" + ".js?onload" +
                        "=onloadCallback\"></script>\n" + "</head>\n" + "<body>\n" + "\t<div id=\"captcha\" " +
                        "style" + "=\"display:flex;justify-content:center;align-items:center;overflow:hidden;" +
                        "padding:20px;\"> " + "</div>\n" + "</body>\n" + "\t<script type=\"text/javascript\">\n" +
                        "\t\tfunction onloadCallback()" + "\n" + "\t\t{\n" + "\t\t\tgrecaptcha.render(\"captcha\", " + "{\n" + "\t\t\t\t\"sitekey\" : " + "\"6Le02eMUAAAAAF8BB2Ur7AuEErb6hvvtlUUwcf2a\",\n" + "\t\t" + "\t\t\"callback\" : function(response) {\n" + "\t\t\t\t\tconsole.log" + "(\"koubilgicaptchatoken:\"+response)\n" + "\t\t\t\t}\n" + "\t\t\t})\n" + "\t\t}\n" + "\t" + "</script>\n" + "</html>";
        webView.loadDataWithBaseURL(url, data, "text/html", "UTF-8", null);
    }

    /**
     * Marks active student for re-log. Means that there was an error making some request to the school site and we
     * should log in again.
     */
    private void markForRelog(ConnectionListener listener)
    {
        // if already marked, do not try to mark again
        if (!loggedIn)
            return;

        loggedIn = false;
        // Log in again
        makeLogInRequest(number, password, listener);
    }

    /**
     * TODO: Replace this with makePostRequest method
     */
    public void makePersonalInfoRequest(final ConnectionListener listener)
    {
        if (!loggedIn)
            return;

        if (department != null)
        {
            listener.onSuccess(department);
            return;
        }

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm";
        StringRequest postReq = new StringRequest(Request.Method.POST, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                if (response.contains("alert") && response.contains("hata"))
                {
                    listener.onFailure("relogin");
                    markForRelog(new ConnectionListener()
                    {
                        @Override
                        public void onSuccess(String... args)
                        {
                            makePersonalInfoRequest(listener);
                        }

                        @Override
                        public void onFailure(String reason)
                        {

                        }
                    });
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
                try
                {
                    saveToFile();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                listener.onSuccess(depart);
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                // We assume that we failed because site is not reachable
                listener.onFailure("site");
            }
        })
        {
            @Override
            public Map<String, String> getHeaders()
            {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", cookieString);

                return headers;
            }

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<>();
                params.put("Anketid", "0");
                params.put("Baglanti", "Giris");
                params.put("Veri", "-1;-1");

                return params;
            }
        };
        queue.add(postReq);
    }

    /**
     * Makes post request to the specified url with params and calls back the result on listener. Uses student session
     * cookies while making the request.
     *
     * @param url      to make the request
     * @param params   to post
     * @param listener that waits for the callback
     */
    public void makePostRequest(final String url, final Map<String, String> params, final ConnectionListener listener)
    {
        StringRequest postReq = new StringRequest(Request.Method.POST, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                if (response.contains("alert") && response.contains("hata"))
                {
                    listener.onFailure("relogin");
                    markForRelog(new ConnectionListener()
                    {
                        @Override
                        public void onSuccess(String... args)
                        {
                            makePostRequest(url, params, listener);
                        }

                        @Override
                        public void onFailure(String reason)
                        {

                        }
                    });
                    return;
                }

                listener.onSuccess(response);
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                // We assume that we failed because site is not reachable
                listener.onFailure("site");
            }
        })
        {
            @Override
            public Map<String, String> getHeaders()
            {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", cookieString);

                return headers;
            }

            @Override
            protected Map<String, String> getParams()
            {
                return params;
            }
        };
        queue.add(postReq);
    }

    /**
     * Makes get request to specified url with student cookies.
     *
     * @param url      to make get request
     * @param listener that listens for response
     */
    public void makeGetRequest(final String url, final ConnectionListener listener)
    {
        StringRequest getReq = new StringRequest(Request.Method.GET, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response)
            {
                if (response.contains("alert") && response.contains("hata"))
                {
                    listener.onFailure("relogin");
                    markForRelog(new ConnectionListener()
                    {
                        @Override
                        public void onSuccess(String... args)
                        {
                            makeGetRequest(url, listener);
                        }

                        @Override
                        public void onFailure(String reason)
                        {

                        }
                    });
                    return;
                }

                listener.onSuccess(response);
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                listener.onFailure("site");
            }
        })
        {
            @Override
            public Map<String, String> getHeaders()
            {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", cookieString);

                return headers;
            }
        };
        queue.add(getReq);
    }

    public String getName()
    {
        return name;
    }

    public String getNumber()
    {
        return number;
    }

    public String getDepartment()
    {
        return department;
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public boolean hasCredentials()
    {
        return (number != null && password != null);
    }

    public String getPassword()
    {
        return password;
    }
}
