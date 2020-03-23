package com.koubilgi.submenus;

public abstract class Submenu
{
    private int nameResource;
    private int iconResource;

    Submenu(int nameRes, int iconRes)
    {
        nameResource = nameRes;
        iconResource = iconRes;
    }

    public abstract void fillContentView();

    public int getNameResource()
    {
        return nameResource;
    }

    public int getIconResource()
    {
        return iconResource;
    }
}
