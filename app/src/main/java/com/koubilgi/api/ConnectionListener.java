package com.koubilgi.api;

public interface ConnectionListener
{
    void onSuccess(String... args);

    void onFailure(String reason);
}
