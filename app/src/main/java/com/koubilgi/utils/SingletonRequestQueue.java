package com.koubilgi.utils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.koubilgi.MainApplication;

public class SingletonRequestQueue {
    private static SingletonRequestQueue instance;
    private RequestQueue requestQueue;

    private SingletonRequestQueue() {
        requestQueue = getRequestQueue();
    }

    public static synchronized SingletonRequestQueue getInstance() {
        if (instance == null) instance = new SingletonRequestQueue();

        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(MainApplication.getAppContext());

        return requestQueue;
    }

    public <T> void add(Request<T> req) {
        getRequestQueue().add(req);
    }
}
