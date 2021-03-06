package com.koubilgi.api;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import com.koubilgi.MainApplication;
import com.koubilgi.utils.AssetReader;
import com.koubilgi.utils.ConnectionListener;
import com.koubilgi.utils.SingletonRequestQueue;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    TODO:
         ReCaptcha düzgün çalışıp çalışmadığı test edilmeli.
 */

/**
 * Öğrenciyle alakalı HTTP requestleri yapan sınıf.
 */

public class RequestMaker {
    private final RequestQueue queue;
    public CookieManager cookieManager;
    private String recaptchaHtml;
    private AlertDialog recaptchaDialog;

    public RequestMaker() {
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance().getRequestQueue();

        try {
            recaptchaHtml = AssetReader.readFileAsString("recaptcha.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean responseHasErrors(String response) {
        return response.toLowerCase().contains("giriş yapınız");
    }

    /**
     * Verilen URL adresine verilen parametreler ile POST isteği gerçekleştirir ve sonuçlarını listener ile döndürür.
     * POST isteğini yaparken öğrencinin session cookie lerini kullanır.
     *
     * @param url      istek yapılacak URL adresi
     * @param params   POST parametreleri
     * @param listener isteğin sonucunu bekleyen listener
     */
    public void makePostRequest(final String url, final Map<String, String> params, final ConnectionListener listener) {
        StringRequest postReq = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (responseHasErrors(response)) {
                    listener.onFailure("relogin");
                    Student.getInstance().markForRelog(new ConnectionListener() {
                        @Override
                        public void onSuccess(String... args) {
                            makePostRequest(url, params, listener);
                        }

                        @Override
                        public void onFailure(String reason) {
                            listener.onFailure(reason);
                        }
                    });
                    return;
                }

                listener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("makeGetRequest", "Can not connect to " + url);
                listener.onFailure("site");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", Student.getInstance().getCookies());

                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        queue.add(postReq);
    }

    /**
     * Belirtilen URL adresine GET isteği yapar. GET isteğini yaparken öğrencinin session cookie lerini kullanır.
     *
     * @param url      GET isteği yapılacak URL adresi
     * @param listener isteğin sonucunu bekleyen listener
     */
    public void makeGetRequest(final String url, final ConnectionListener listener) {
        Log.d("makeGetRequest", "Making get request to " + url);
        StringRequest getReq = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("makeGetRequest", "Got response for " + url);
                if (responseHasErrors(response)) {
                    Log.d("makeGetRequest", "Relog is needed to make a get request for " + url);
                    listener.onFailure("relogin");
                    Student.getInstance().markForRelog(new ConnectionListener() {
                        @Override
                        public void onSuccess(String... args) {
                            makeGetRequest(url, listener);
                        }

                        @Override
                        public void onFailure(String reason) {
                            listener.onFailure(reason);
                        }
                    });
                    return;
                }

                listener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("makeGetRequest", "Can not connect to " + url);
                listener.onFailure("site");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", Student.getInstance().getCookies());

                return headers;
            }
        };
        queue.add(getReq);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void getRecaptchaToken(final ConnectionListener listener) {
        if (recaptchaDialog != null)
            return;

        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainApplication.getActiveActivity());

        final WebView webView = new WebView(MainApplication.getActiveActivity());
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        webView.setVerticalScrollBarEnabled(false);

        dialogBuilder.setView(webView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle("reCAPTCHA");

        recaptchaDialog = dialogBuilder.show();
        recaptchaDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                if (message.startsWith("koubilgicaptchatoken:")) {
                    final String token = message.substring(21);

                    recaptchaDialog.dismiss();
                    recaptchaDialog = null;
                    listener.onSuccess(token);
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });

        webView.loadDataWithBaseURL(url, recaptchaHtml, "text/html", "UTF-8", null);
    }

    public void makeLogInRequest(final String num, final String pass, final ConnectionListener listener) {
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        getRecaptchaToken(new ConnectionListener() {
            @Override
            public void onSuccess(String... args) {
                final String token = args[0];

                Map<String, String> params = new HashMap<>();
                params.put("LoggingOn", "1");
                params.put("OgrNo", num);
                params.put("Sifre", pass);
                params.put("g-recaptcha-response", token);

                makePostRequest(url, params, new ConnectionListener() {
                    @Override
                    public void onSuccess(String... args) {
                        String response = args[0];

                        if (response.contains("alert") && response.contains("hata")) {
                            if (listener != null) listener.onFailure("relogin");
                            // TODO: Offline moda geç
                            return;
                        }

                        boolean success = true;
                        if (response.contains("<div class=\"alert alert-danger\" id=\"OgrNoUyari\"></div>"))
                            success = false;

                        Document doc = Jsoup.parse(response);

                        // Öğrencinin adını ve numarasını ayrıştırır

                        Element info = doc.select("h4").first();

                        if (info == null) success = false;

                        if (success) {
                            // Session cookie lerini kaydet
                            CookieStore store = cookieManager.getCookieStore();
                            List<HttpCookie> cookies = store.getCookies();

                            String[] infoTxt = info.text().split(" ", 2);
                                /*
                                    index 1 = öğrenci adı
                                    index 0 = öğrenci numarası
                                 */
                            String name = infoTxt[1];
                            String number = infoTxt[0];
                            String cookieString = StringUtil.join(cookies, "; ");

                            if (listener != null) listener.onSuccess(name, number, cookieString);
                        } else {
                            if (listener != null) listener.onFailure("credentials");
                        }
                    }

                    @Override
                    public void onFailure(String reason) {
                        if (listener != null) listener.onFailure(reason);
                    }
                });
            }

            @Override
            public void onFailure(String reason) {
                if (listener != null) listener.onFailure(reason);
            }
        });
    }

    /**
     * Kişisel bilgiler sayfasındaki bilgileri anahtar: değer şeklinde döndürür
     *
     * @param listener
     */
    public void makePersonalInfoRequest(final ConnectionListener listener) {
        if (!Student.getInstance().isLoggedIn()) return;

        if (Student.getInstance().getDepartment() != null) {
            listener.onSuccess(Student.getInstance().getDepartment());
            return;
        }

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm";

        Map<String, String> params = new HashMap<>();
        params.put("Anketid", "0");
        params.put("Baglanti", "Giris");
        params.put("Veri", "-1;-1");

        makePostRequest(url, params, new ConnectionListener() {
            @Override
            public void onSuccess(String... args) {
                String response = args[0];

                Document doc = Jsoup.parse(response);
                Element form = doc.select("#OgrKisiselBilgiler").first();
                Elements colsm6s = form.select(".col-sm-6");

                StringBuilder infoStr = new StringBuilder();

                for (Element colsm6 : colsm6s) {
                    Element key = colsm6.selectFirst(".col-sm-4 > b");
                    Element value = colsm6.selectFirst(".col-sm-8");

                    if (key != null && value != null) {
                        infoStr.append(String.format("%s=%s;", key.text().trim(), value.text().trim()));
                    }
                }

                listener.onSuccess(infoStr.toString());
            }

            @Override
            public void onFailure(String reason) {
                if (listener != null) listener.onFailure(reason);
            }
        });
    }
}
