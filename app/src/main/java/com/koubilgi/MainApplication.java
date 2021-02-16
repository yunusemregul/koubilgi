package com.koubilgi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

public class MainApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @SuppressLint("StaticFieldLeak")
    private static Activity activeActivity;

    public static Activity getActiveActivity() {
        return MainApplication.activeActivity;
    }

    public static void setActiveActivity(Activity activity) {
        MainApplication.activeActivity = activity;
    }

    public static Context getAppContext() {
        return MainApplication.context;
    }

    public void onCreate() {
        super.onCreate();
        MainApplication.context = getApplicationContext();
    }
}
