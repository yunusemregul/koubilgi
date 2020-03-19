package com.koubilgi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;

/*
    TODO:
        Make a separated singleton-pattern Student class.
 */

public class MainMenuActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_mainmenu);

        final TextView tStudentName = findViewById(R.id.studentName),
                tStudentNumber = findViewById(R.id.studentNumber);

        final RequestQueue queue = SingletonRequestQueue.getInstance(this).getRequestQueue();
        final SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences data = getSharedPreferences("data", MODE_PRIVATE);
        final SharedPreferences.Editor dataEditor = data.edit();

        final String studId = studentCredentials.getString("studentNumber", null),
                pass = studentCredentials.getString("password", null);

        if (studId == null || pass == null)
            return;

        if (data.contains("studentName"))
            tStudentName.setText(data.getString("studentName", "Bilinmeyen Öğrenci"));
        if (data.contains("studentNumber"))
            tStudentNumber.setText(data.getString("studentNumber","123456789"));

        Student.getInstance(this).logIn(studId, pass, new LoginListener()
        {
            @Override
            public void onSuccess(String name, String number)
            {
                tStudentName.setText(name);
                tStudentNumber.setText(number);
            }

            @Override
            public void onFailure(String reason)
            {

            }
        });
    }
}
