package com.koubilgi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;
import com.koubilgi.submenus.SubmenuButtonAdapter;

/*
    TODO:
        Do parallax effect for top dock, when scrolled.
 */

public class MainmenuActivity extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_mainmenu);

        final TextView tStudentName = findViewById(R.id.studentName),
                tStudentNumber = findViewById(R.id.studentNumber),
                tStudentDepartment = findViewById(R.id.studentDepartment);

        final SharedPreferences studentCredentials = getSharedPreferences("credentials", MODE_PRIVATE);
        final SharedPreferences data = getSharedPreferences("data", MODE_PRIVATE);

        final String numb = studentCredentials.getString("number", null),
                pass = studentCredentials.getString("password", null);

        if (numb == null || pass == null)
            return;

        if (data.contains("studentName"))
            tStudentName.setText(data.getString("studentName", "Bilinmeyen Öğrenci"));
        if (data.contains("studentNumber"))
            tStudentNumber.setText(data.getString("studentNumber", "123456789"));
        if (data.contains("studentDepartment"))
            tStudentDepartment.setText(data.getString("studentDepartment", "Bilinmeyen Bölüm"));

        Student.getInstance(this).logIn(numb, pass, new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                tStudentName.setText(args[0]); // name
                tStudentNumber.setText(args[1]); // number

                Student.getInstance(getApplicationContext()).personalInfo(new ConnectionListener()
                {
                    @Override
                    public void onSuccess(String... args)
                    {
                        tStudentDepartment.setText(args[0]);
                    }

                    @Override
                    public void onFailure(String reason)
                    {

                    }
                });
            }

            @Override
            public void onFailure(String reason)
            {
                // TODO: Show error screen 'Can not log in.' if reason equals site
            }
        });

        GridView submenus = findViewById(R.id.submenus);
        submenus.setAdapter(new SubmenuButtonAdapter(this));
    }
}
