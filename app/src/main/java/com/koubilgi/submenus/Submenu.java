package com.koubilgi.submenus;

import android.content.Context;

/**
 * Alt sayfaların (Sınav takvimi, harç bilgileri, genel durum vs.) ana sınıfı
 */
public abstract class Submenu {
    private final int nameResource;
    private final int iconResource;

    Submenu(int nameRes, int iconRes) {
        nameResource = nameRes;
        iconResource = iconRes;
    }

    public abstract void fillContentView(Context context);

    public int getNameResource() {
        return nameResource;
    }

    public int getIconResource() {
        return iconResource;
    }
}
