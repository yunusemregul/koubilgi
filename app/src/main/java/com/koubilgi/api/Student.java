package com.koubilgi.api;

import android.app.AlertDialog;
import android.content.Context;

import com.koubilgi.R;
import com.koubilgi.utils.ConnectionListener;

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
    private static RequestMaker requestMaker;


    /**
     * Constructs the singleton Student within given context.
     *
     * @param ctx the context we will be working in
     */
    private Student(Context ctx)
    {
        loggedIn = false;
        context = ctx;
        requestMaker = new RequestMaker(context, this);
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
            requestMaker.makeGetRequest(harcurl, new ConnectionListener()
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

    /**
     * Marks active student for re-log. Means that there was an error making some request to the school site and we
     * should log in again.
     */
    void markForRelog(ConnectionListener listener)
    {
        // if already marked, do not try to mark again
        if (!loggedIn)
            return;

        loggedIn = false;
        // Log in again
        makeLogInRequest(number, password, listener);
    }

    private void makeLogInRequest(final String num, final String pass, final ConnectionListener listener)
    {
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        requestMaker.getRecaptchaToken(new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                final String token = args[0];

                Map<String, String> params = new HashMap<>();
                params.put("LoggingOn", "1");
                params.put("OgrNo", num);
                params.put("Sifre", pass);
                params.put("g-recaptcha-response", token);

                requestMaker.makePostRequest(url, params, new ConnectionListener()
                {
                    @Override
                    public void onSuccess(String... args)
                    {
                        String response = args[0];

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
                            CookieStore store = requestMaker.cookieManager.getCookieStore();
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

                    @Override
                    public void onFailure(String reason)
                    {
                        if (listener != null)
                            listener.onFailure(reason);
                    }
                });
            }

            @Override
            public void onFailure(String reason)
            {
                if (listener != null)
                    listener.onFailure(reason);
            }
        });
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

        Map<String, String> params = new HashMap<>();
        params.put("Anketid", "0");
        params.put("Baglanti", "Giris");
        params.put("Veri", "-1;-1");

        requestMaker.makePostRequest(url, params, new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                String response = args[0];

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

            @Override
            public void onFailure(String reason)
            {
                if (listener != null)
                    listener.onFailure(reason);
            }
        });
    }

    public String getCookies()
    {
        return cookieString;
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

    public RequestMaker getRequestMaker()
    {
        return requestMaker;
    }
}
