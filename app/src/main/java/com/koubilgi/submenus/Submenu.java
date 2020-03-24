package com.koubilgi.submenus;

import android.content.Context;

public abstract class Submenu
{
    private int nameResource;
    private int iconResource;

    Submenu(int nameRes, int iconRes)
    {
        nameResource = nameRes;
        iconResource = iconRes;
    }

    public abstract void fillContentView(Context context);

    public int getNameResource()
    {
        return nameResource;
    }

    public int getIconResource()
    {
        return iconResource;
    }
}
