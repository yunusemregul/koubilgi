package com.koubilgi.api;

public interface LoginListener
{
    void onSuccess(String... args);

    void onFailure(String reason);
}
