package com.koubilgi.submenus;

import android.content.Context;

import com.koubilgi.R;
import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;

public class SyllabusSubmenu extends Submenu
{
    public SyllabusSubmenu()
    {
        super(R.string.SYLLABUS, R.drawable.icon_mainmenu_dersprogrami);
    }

    @Override
    public void fillContentView(Context context)
    {
        Student.getInstance(context).makeGetRequest("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/DersIslemleri/DersProgrami.cfm", new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {

            }

            @Override
            public void onFailure(String reason)
            {

            }
        });
    }
}
