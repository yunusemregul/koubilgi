package com.koubilgi;

import android.os.Bundle;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;
import com.koubilgi.components.SubmenuButtonAdapter;

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

        Student student = Student.getInstance(this);

        final TextView tStudentName = findViewById(R.id.studentName), tStudentNumber =
                findViewById(R.id.studentNumber), tStudentDepartment = findViewById(R.id.studentDepartment);

        if (!student.hasCredentials())
            return;

        if (student.getName() != null)
            tStudentName.setText(student.getName());
        if (student.getNumber() != null)
            tStudentNumber.setText(student.getNumber());
        if (student.getDepartment() != null)
            tStudentDepartment.setText(student.getDepartment());

        Student.getInstance(this).logIn(student.getNumber(), student.getPassword(), new ConnectionListener()
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
