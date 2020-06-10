package com.koubilgi.api;

import androidx.annotation.NonNull;

public class SimpleDate
{
    public int hour;
    public int minute;

    public SimpleDate(int hour, int minute)
    {
        this.hour = hour;
        this.minute = minute;
    }

    public SimpleDate(String format)
    {
        this.hour = Integer.parseInt(format.substring(0, 2));
        this.minute = Integer.parseInt(format.substring(3, 5));
    }

    public int getTime()
    {
        return (this.hour * 60 + this.minute);
    }

    @NonNull
    @Override
    public String toString()
    {
        return String.format("%02d:%02d", this.hour, this.minute);
    }
}
