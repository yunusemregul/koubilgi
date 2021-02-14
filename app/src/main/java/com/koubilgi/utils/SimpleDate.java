package com.koubilgi.utils;

import androidx.annotation.NonNull;

/**
 * Ders programı sayfasındaki bir günü temsil edecek sınıf.
 */
public class SimpleDate {
    private int day;
    private int hour;
    private int minute;

    public SimpleDate(int time) {
        this.day = (int) (time / (float) (24 * 60));
        this.hour = (int) ((time % (24 * 60)) / 60.f);
        this.minute = ((time % (24 * 60)) % 60);
    }

    public SimpleDate(int day, int hour, int minute) {
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }

    public int getTime() {
        return ((this.day * 24 * 60) + (this.hour * 60 + this.minute));
    }

    public SimpleDate addMinutes(int minutes) {
        return new SimpleDate(getTime() + minutes);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%02d:%02d", this.hour, this.minute);
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
}
