package com.koubilgi.api;

import android.app.AlertDialog;
import android.util.Log;

import com.koubilgi.MainApplication;
import com.koubilgi.R;
import com.koubilgi.utils.ConnectionListener;

import java.io.IOException;

/*
    TODO:
        Üniversite submenu sayfalarından herhangi birini değiştirirse o sayfayı uygulama güncellenene kadar
        offline olarak kullanıcılara sunabilmeliyiz.
 */

/**
 * Uygulamayı kullanan öğrenciyi temsil eden sınıf. Öğrenciyle ilgili, giriş yapmak, bilgilerini almak (isim, bölüm)
 * gibi temel işlemleri yerine getirir.
 * <p>
 * Uygulamada oturumu açık sadece 1 öğrenci olacağı için bu sınıfın tüm uygulama genelinde tek olmasını daha doğru
 * buldum. Bu yüzden Singleton pattern kullandım.
 */
public class Student {
    private static Student instance;
    private final RequestMaker requestMaker;
    public StudentInfo studentInfo;
    private boolean loggedIn;

    /**
     * Singleton öğrencinin constructor metodu.
     */
    private Student() {
        setLoggedIn(false);
        requestMaker = new RequestMaker();
        try {
            this.studentInfo = StudentInfo.loadFromFile();
        } catch (IOException e) {
            this.studentInfo = new StudentInfo();
        }
    }

    /**
     * Öğrencinin tek instance ini döndürür, instance yoksa oluşturup döndürür.
     *
     * @return singleton Student
     */
    public static synchronized Student getInstance() {
        if (instance == null) {
            instance = new Student();
        }

        return instance;
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
    public void logIn(final String num, final String pass, final ConnectionListener listener) {
        // Eğer zaten önceden giriş yaptıysak tekrar girmeyi deneme
        if (loggedIn) {
            if (listener != null) listener.onSuccess(studentInfo.name, studentInfo.number);
            return;
        }

        // Giriş yapılıyor... popup
        AlertDialog.Builder logginginPopup = new AlertDialog.Builder(MainApplication.getActiveActivity());
        logginginPopup.setMessage(R.string.logging_in);

        logginginPopup.setCancelable(false);
        final AlertDialog loggingin = logginginPopup.show();

        final ConnectionListener logginginListener = new ConnectionListener() {
            @Override
            public void onSuccess(String... args) {
                loggingin.dismiss();

                studentInfo.name = args[0];
                studentInfo.number = args[1];
                studentInfo.password = pass;
                studentInfo.cookieString = args[2];

                setLoggedIn(true);

                // Öğrenci bilgilerini ilerde otomatik giriş yapmak üzere kaydet
                try {
                    studentInfo.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                listener.onSuccess(args);
            }

            @Override
            public void onFailure(String reason) {
                loggingin.dismiss();
                listener.onFailure(reason);
                // TODO: Offline moda geç
                Log.d("HATA", "Giriş yapılamadı! (Sebep: " + reason + ")");
            }
        };

        // Login isteğini gerçekleştiriyoruz
        requestMaker.makeLogInRequest(num, pass, logginginListener);
    }

    /**
     * Daha önceden giriş yapmış öğrenciyi giriş yapmadı olarak işaretleyerek tekrar giriş yaptırır.
     */
    public void markForRelog(ConnectionListener listener) {
        Log.d("markForRelog", "Relogging the student..");

        setLoggedIn(false);
        // Log in again
        logIn(studentInfo.number, studentInfo.password, listener);
    }

    public String getCookies() {
        return studentInfo.cookieString;
    }

    public String getName() {
        return studentInfo.name;
    }

    public String getNumber() {
        return studentInfo.number;
    }

    /**
     * Öğrencinin kişisel bilgiler sayfasından daha önce kaydedilmiş bilgilerden isteneni döndürmeye yarar.
     *
     * @param infoKey istenen bilginin adı örnek: Bölüm
     * @return bilginin içeriği
     */
    private String getInfoFromPersonalInfo(String infoKey) {
        if (studentInfo.personalInfo == null)
            return null;

        infoKey = infoKey.toLowerCase();
        for (String keyValue : studentInfo.personalInfo.split(";")) {
            String[] splitted = keyValue.split("=");
            String key = splitted[0].toLowerCase();
            String value = splitted[1];

            if (key.contains(infoKey)) {
                return value;
            }
        }

        return null;
    }

    /**
     * Öğrencinin kişisel bilgiler sayfasından istenen bilgiyi döndürür.
     *
     * @param infoKey  istenen bilginin adı örnek: Bölüm
     * @param listener bilginin gelmesini bekleyen listener
     */
    public void getPersonalInfo(final String infoKey, final ConnectionListener listener) {
        String info = getInfoFromPersonalInfo(infoKey);

        if (info != null)
            listener.onSuccess(info);
        else {
            requestMaker.makePersonalInfoRequest(new ConnectionListener() {
                @Override
                public void onSuccess(String... args) {
                    studentInfo.personalInfo = args[0];
                    try {
                        studentInfo.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    listener.onSuccess(getInfoFromPersonalInfo(infoKey));
                }

                @Override
                public void onFailure(String reason) {
                    listener.onFailure(reason);
                }
            });
        }
    }

    public String getDepartment() {
        return getInfoFromPersonalInfo("Bölüm");
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean bool) {
        loggedIn = bool;

        if (bool) {
            Log.d("setLoggedIn", bool + "");
        }
    }

    public boolean hasCredentials() {
        return (studentInfo.number != null && studentInfo.password != null);
    }

    public RequestMaker getRequestMaker() {
        return requestMaker;
    }
}
