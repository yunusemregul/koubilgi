package com.koubilgi.utils;

public interface ConnectionListener
{
    void onSuccess(String... args);

    void onFailure(String reason);
}
