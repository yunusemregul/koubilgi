package com.koubilgi.submenus;

import android.content.Context;

import com.koubilgi.R;
import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;

public class FeeStatusSubmenu extends Submenu
{
    public FeeStatusSubmenu()
    {
        super(R.string.FEE_STATUS, R.drawable.icon_mainmenu_harcdurumu);
    }

    @Override
    public void fillContentView(Context context)
    {
        Student.getInstance(context).makeGetRequest("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/OgrenciIsleri/HarcBilgi.cfm",
                new ConnectionListener()
                {
                    @Override
                    public void onSuccess(String... args)
                    {
                        String response = args[0];


                    }

                    @Override
                    public void onFailure(String reason)
                    {

                    }
                });
    }
}
