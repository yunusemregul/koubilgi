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
        Üniversite submenu sayfalarından herhangi birini değiştirirse o sayfayı uygulama güncellenene kadar
        offline olarak kullanıcılara sunabilmeliyiz.
 */

/**
 * Uygulamayı kullanan öğrenciyi temsil eden sınıf. Öğrenciyle ilgili, giriş yapmak, bilgilerini almak (isim, bölüm)
 * gibi temel işlemleri yerine getirir.
 *
 * Uygulamada oturumu açık sadece 1 öğrenci olacağı için bu sınıfın tüm uygulama genelinde tek olmasını daha doğru
 * buldum. Bu yüzden Singleton pattern kullandım.
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
     * Singleton öğrencinin constructor metodu.
     */
    private Student()
    {
        loggedIn = false;
        requestMaker = new RequestMaker(context, this);
    }

    /**
     * Öğrencinin tek instance ini döndürür, instance yoksa oluşturup döndürür.
     *
     * @param ctx içinde olduğumuz context
     * @return singleton Student
     */
    public static synchronized Student getInstance(Context ctx)
    {
        if (instance == null)
        {
            context = ctx;
            instance = new Student();
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

    /**
     * Öğrenci bilgilerini tekrar kullanmak üzere kaydeder.
     *
     * @throws Exception
     */
    private void saveToFile() throws Exception
    {
        FileOutputStream outputStream = context.openFileOutput("student", Context.MODE_PRIVATE);
        ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
        objectStream.writeObject(this);
        objectStream.close();
        outputStream.close();
    }

    /**
     * Kaydedilmiş öğrenci bilgilerini okur, okuduğu bilgilerle bir öğrenci oluşturup döndürür.
     *
     * @return okunan bilgilerle oluşturulan öğrenci
     * @throws Exception dosya okunurken IO exceptionu oluşursa
     */
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
     * Verilen credential bilgileri ile öğrenci girişi yapmaya çalışır. Eğer giriş başarılı olursa listener.onSuccess
     * metoduna öğrencinin adını numarasını ve departmanını verir. Eğer giriş başarılı değilse listener.onFailure
     * metodunu ilgili sebep ile çağırır.
     *
     * @param num      giriş yapacak öğrencinin numarası
     * @param pass     giriş yapacak öğrencinin şifresi
     * @param listener giriş sonucunu bekleyen listener
     */
    public void logIn(final String num, final String pass, final ConnectionListener listener)
    {
        // Eğer zaten önceden giriş yaptıysak tekrar girmeyi deneme
        if (loggedIn)
        {
            if (listener != null)
                listener.onSuccess(name, number);
            return;
        }

        // Giriş yapılıyor... popup
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
                // TODO: Offline moda geç
            }
        };

        /*
            Öğrencinin şu anda zaten giriş yapıp yapmadığını anlamak için HarcBilgi sayfasına GET isteği yapıyoruz.
            Eğer sayfa giriş yapılmadığı yönünde hata verirse tekrar giriş yapıyoruz. HarcBilgi sayfasını seçtim çünkü
            boyutu en az olan sayfa genelde o oluyor bence. Boyutu az olması kullanıcının internetini boşuna yemememiz
            açısından önemli.
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
     * Daha önceden giriş yapmış öğrenciyi giriş yapmadı olarak işaretleyerek tekrar giriş yaptırır.
     */
    void markForRelog(ConnectionListener listener)
    {
        // Sadece giriş yapmış öğrenciler giriş yapmadı olarak işaretlenebilir
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
                            loggedIn = true;

                            // Session cookie lerini kaydet
                            CookieStore store = requestMaker.cookieManager.getCookieStore();
                            List<HttpCookie> cookies = store.getCookies();

                            String[] infoTxt = info.text().split(" ", 2);
                                /*
                                    index 1 = öğrenci adı
                                    index 0 = öğrenci numarası
                                 */
                            name = infoTxt[1];
                            number = infoTxt[0];
                            password = pass;
                            cookieString = StringUtil.join(cookies, "; ");

                            // Öğrenci bilgilerini ilerde otomatik giriş yapmak üzere kaydet
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
