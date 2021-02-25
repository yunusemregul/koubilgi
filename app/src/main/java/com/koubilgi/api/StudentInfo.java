package com.koubilgi.api;

import android.content.Context;
import android.util.Log;

import com.koubilgi.MainApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class StudentInfo implements Serializable {
    public String name;
    public String number;
    public String password;
    public String cookieString;

    /**
     * ÖBS > Kişisel Bilgiler sayfasındaki bilgilerin Bölüm=Bilgisayar Mühendisliği;Sınıf=3;... şeklinde birleştirilmiş hali
     */
    public String personalInfo;

    /**
     * Kaydedilmiş öğrenci bilgilerini okur, okuduğu bilgilerle bir öğrenci oluşturup döndürür.
     *
     * @return okunan bilgilerle oluşturulan öğrenci
     * @throws Exception dosya okunurken IO exceptionu oluşursa
     */
    public static StudentInfo loadFromFile() throws IOException {
        File file = MainApplication.getAppContext().getFileStreamPath("StudentInfo");

        if (file == null || !file.exists()) return new StudentInfo();

        FileInputStream inputStream = MainApplication.getAppContext().openFileInput("StudentInfo");
        ObjectInputStream objectStream = new ObjectInputStream(inputStream);

        StudentInfo loaded = new StudentInfo();
        try {
            loaded = (StudentInfo) objectStream.readObject();
        } catch (ClassNotFoundException | IOException e) {
            Log.d("loadFromFile", "Destroying save file because of an exception.");
            objectStream.close();
            inputStream.close();
            file.delete();
            return loaded;
        }

        objectStream.close();
        inputStream.close();

        return loaded;
    }

    /**
     * Öğrenci bilgilerini tekrar kullanmak üzere kaydeder.
     *
     * @throws Exception
     */
    public void save() throws Exception {
        FileOutputStream outputStream = MainApplication.getAppContext().openFileOutput("StudentInfo", Context.MODE_PRIVATE);
        ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
        objectStream.writeObject(this);
        objectStream.close();
        outputStream.close();
    }
}
