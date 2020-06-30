package com.koubilgi.api;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
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
import com.koubilgi.utils.ConnectionListener;
import com.koubilgi.utils.SingletonRequestQueue;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.HashMap;
import java.util.Map;

public class RequestMaker
{
    private static Context context;
    public CookieManager cookieManager;
    private Student student;
    private RequestQueue queue;

    public RequestMaker(Context context, Student student)
    {
        this.student = student;
        this.context = context;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
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
                if (response.contains("alert") && response.contains("hata") && student.isLoggedIn())
                {
                    listener.onFailure("relogin");
                    student.markForRelog(new ConnectionListener()
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
                headers.put("Cookie", student.getCookies());

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
                if (response.contains("alert") && response.contains("hata") && student.isLoggedIn())
                {
                    listener.onFailure("relogin");
                    student.markForRelog(new ConnectionListener()
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
                headers.put("Cookie", student.getCookies());

                return headers;
            }
        };
        queue.add(getReq);
    }

    void getRecaptchaToken(final ConnectionListener listener)
    {
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        final WebView webView = new WebView(context);
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        webView.setVerticalScrollBarEnabled(false);

        dialogBuilder.setView(webView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle("reCAPTCHA");

        final AlertDialog recaptchaDialog = dialogBuilder.show();
        recaptchaDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

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
}
