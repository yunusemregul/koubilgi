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

public class RequestMaker
{
    public CookieManager cookieManager;
    private Student student;
    private RequestQueue queue;

    public RequestMaker(Context context, Student student)
    {
        this.student = student;
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        queue = SingletonRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
    }

    /**
     * Verilen URL adresine verilen parametreler ile POST isteği gerçekleştirir ve sonuçlarını listener ile döndürür.
     * POST isteğini yaparken öğrencinin session cookie lerini kullanır.
     *
     * @param url      istek yapılacak URL adresi
     * @param params   POST parametreleri
     * @param listener isteğin sonucunu bekleyen listener
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
                            listener.onFailure(reason);
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
     * Belirtilen URL adresine GET isteği yapar. GET isteğini yaparken öğrencinin session cookie lerini kullanır.
     *
     * @param url      GET isteği yapılacak URL adresi
     * @param listener isteğin sonucunu bekleyen listener
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
                            listener.onFailure(reason);
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

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(student.getContext());

        final WebView webView = new WebView(student.getContext());
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

        // TODO: Kodda çok çirkin gözüküyor ve okunmuyor bir şekilde daha iyi bi çözüm bulunabilir belki dosyadan
        //  okuyarak bunu
        String data =
                "<html>\n" + "<head>\n" + "\t<script src=\"https://www.google.com/recaptcha/api" + ".js?onload" +
                        "=onloadCallback\"></script>\n" + "</head>\n" + "<body>\n" + "\t<div id=\"captcha\" " +
                        "style" + "=\"display:flex;justify-content:center;align-items:center;overflow:hidden;" +
                        "padding:20px;\"> " + "</div>\n" + "</body>\n" + "\t<script type=\"text/javascript\">\n" +
                        "\t\tfunction onloadCallback()" + "\n" + "\t\t{\n" + "\t\t\tgrecaptcha.render(\"captcha\", " + "{\n" + "\t\t\t\t\"sitekey\" : " + "\"6Le02eMUAAAAAF8BB2Ur7AuEErb6hvvtlUUwcf2a\",\n" + "\t\t" + "\t\t\"callback\" : function(response) {\n" + "\t\t\t\t\tconsole.log" + "(\"koubilgicaptchatoken:\"+response)\n" + "\t\t\t\t}\n" + "\t\t\t})\n" + "\t\t}\n" + "\t" + "</script>\n" + "</html>";
        webView.loadDataWithBaseURL(url, data, "text/html", "UTF-8", null);
    }


    void makeLogInRequest(final String num, final String pass, final ConnectionListener listener)
    {
        final String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm";
        getRecaptchaToken(new ConnectionListener()
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

                makePostRequest(url, params, new ConnectionListener()
                {
                    @Override
                    public void onSuccess(String... args)
                    {
                        String response = args[0];

                        if (response.contains("alert") && response.contains("hata"))
                        {
                            if (listener != null)
                                listener.onFailure("relogin");
                            // TODO: Offline moda geç
                            return;
                        }

                        boolean success = true;
                        if (response.contains("<div class=\"alert alert-danger\" id=\"OgrNoUyari\"></div>"))
                            success = false;

                        Document doc = Jsoup.parse(response);

                        // Öğrencinin adını ve numarasını ayrıştırır

                        Element info = doc.select("h4").first();

                        if (info == null)
                            success = false;

                        if (success)
                        {
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

                            if (listener != null)
                                listener.onSuccess(name, number, cookieString);
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

    public void makePersonalInfoRequest(final ConnectionListener listener)
    {
        if (!student.isLoggedIn())
            return;

        if (student.getDepartment() != null)
        {
            listener.onSuccess(student.getDepartment());
            return;
        }

        String url = "https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/KisiselBilgiler/KisiselBilgiGoruntuleme.cfm";

        Map<String, String> params = new HashMap<>();
        params.put("Anketid", "0");
        params.put("Baglanti", "Giris");
        params.put("Veri", "-1;-1");

        makePostRequest(url, params, new ConnectionListener()
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
                    Öğrencinin bölüm bilgisini tüm kişisel bilgiler sayfası ndan ayrıştırmaya çalışıyoruz.
                    Önce tüm sayfadan <b>Bölüm</b> elementini içeren ana element i buluyoruz daha sonra o ana element
                    içindeki öğrencinin bölümü kısmını alıyoruz.

                    Buradaki değişkenleri şöyle açıklayabilirim sayfada şu anlama geliyorlar:
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
}
