package com.koubilgi;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

@SuppressLint("Registered")
public class MainMenuActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences.Editor editor = studentCredentials.edit();

        try
        {
            CookieManager cm = new CookieManager();
            CookieHandler.setDefault(cm);

            URL url = new URL("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/index.cfm");

            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String studId = studentCredentials.getString("studentId",null), pass = studentCredentials.getString("password",null);
            if(studId==null || pass==null)
                return;

            String params = String.format("LoggingOn=1&OgrNo=%s&Sifre=%s", studId, pass);
            con.setFixedLengthStreamingMode(params.getBytes().length);
            con.getOutputStream().write(params.getBytes());
            con.connect();

            Scanner scnr = new Scanner(con.getInputStream()).useDelimiter("\\A");
            String body = scnr.hasNext() ? scnr.next() : "";
            Document doc = Jsoup.parse(body);

            // Extract student name and number
            Element info = doc.select("h4").first();
            if(info!=null)
            {
                String[] infoTxt = info.text().split(" ",2);
                TextView tStudentName = findViewById(R.id.studentName);
                TextView tStudentNumber = findViewById(R.id.studentId);
                tStudentName.setText(infoTxt[1]);
                tStudentNumber.setText(infoTxt[0]);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
