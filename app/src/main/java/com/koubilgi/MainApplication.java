package com.koubilgi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

public class MainApplication extends Application {
    private static Context context;
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
