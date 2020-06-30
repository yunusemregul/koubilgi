package com.koubilgi.utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class SingletonRequestQueue
{
    private static SingletonRequestQueue instance;
    private static Context context;
    private RequestQueue requestQueue;

    private SingletonRequestQueue(Context ctx)
    {
        context = ctx;
        requestQueue = getRequestQueue();
    }

    public static synchronized SingletonRequestQueue getInstance(Context ctx)
    {
        if (instance == null)
            instance = new SingletonRequestQueue(ctx);

        return instance;
    }

    public RequestQueue getRequestQueue()
    {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        return requestQueue;
    }

    public <T> void add(Request<T> req)
    {
        getRequestQueue().add(req);
    }
}
