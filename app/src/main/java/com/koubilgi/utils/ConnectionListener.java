package com.koubilgi.utils;

/**
 * Volley bağlantılarından gelecek cevapları dinlemeye yarayan sınıf.
 */
public interface ConnectionListener
{
    void onSuccess(String... args);

    void onFailure(String reason);
}
