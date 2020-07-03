package com.koubilgi.api;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.koubilgi.R;
import com.koubilgi.utils.ConnectionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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
        context = ctx;

        if (instance == null)
        {
            try
            {
                instance = Student.loadFromFile();

                return instance;
            } catch (Exception e)
            {
                e.printStackTrace();

                instance = new Student();
            }
        }

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
    public static Student loadFromFile() throws Exception
    {
        File file = context.getFileStreamPath("student");

        if (file == null || !file.exists())
            return new Student();

        FileInputStream inputStream = context.openFileInput("student");
        ObjectInputStream objectStream = new ObjectInputStream(inputStream);
        Student loaded = (Student) objectStream.readObject();
        objectStream.close();
        inputStream.close();

        return loaded;
    }

    public static Context getContext()
    {
        return context;
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

                name = args[0];
                number = args[1];
                password = pass;
                cookieString = args[2];

                loggedIn = true;

                // Öğrenci bilgilerini ilerde otomatik giriş yapmak üzere kaydet
                try
                {
                    saveToFile();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                listener.onSuccess(args);
            }

            @Override
            public void onFailure(String reason)
            {
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
    void markForRelog(ConnectionListener listener)
    {
        // Sadece giriş yapmış öğrenciler giriş yapmadı olarak işaretlenebilir
        if (!loggedIn)
            return;

        loggedIn = false;
        // Log in again
        logIn(number, password, listener);
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

    public void getPersonalInfo(final ConnectionListener listener)
    {
        if (department != null)
            listener.onSuccess(department);
        else
        {
            requestMaker.makePersonalInfoRequest(new ConnectionListener()
            {
                @Override
                public void onSuccess(String... args)
                {
                    department = args[0];
                    try
                    {
                        saveToFile();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    listener.onSuccess(args);
                }

                @Override
                public void onFailure(String reason)
                {
                    listener.onFailure(reason);
                }
            });
        }
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
        if (requestMaker == null)
            requestMaker = new RequestMaker(context, this);

        return requestMaker;
    }
}
