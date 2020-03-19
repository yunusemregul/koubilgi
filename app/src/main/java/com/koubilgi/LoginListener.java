package com.koubilgi;

public interface LoginListener
{
    public void onSuccess(String name, String number);
    public void onFailure(String reason);
}
