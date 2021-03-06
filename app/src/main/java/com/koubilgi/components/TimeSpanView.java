package com.koubilgi.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.koubilgi.R;
import com.koubilgi.utils.SimpleDate;

import java.util.Calendar;

/**
 * Ders programı sayfasında derslerin yanında dersin ne kadar kısmının tamamlandığını gösteren çubuk componenti.
 */
public class TimeSpanView extends View {
    public SimpleDate startTime;
    public SimpleDate endTime;

    public Paint colorBordersPaint;
    public Paint colorTextPaint;
    public Paint colorPrimaryPaint;

    public TimeSpanView(Context context) {
        super(context);

        init(context);
    }

    private void init(Context context) {
        colorBordersPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorBordersPaint.setColor(context.getResources().getColor(R.color.colorBorders));

        colorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorTextPaint.setColor(context.getResources().getColor(R.color.colorText));

        colorPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorPrimaryPaint.setColor(context.getResources().getColor(R.color.colorPrimary));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect back = new Rect(getWidth() / 2 - getWidth() / 4, 6, getWidth() / 2 + getWidth() / 4, getHeight() - 6);
        Rect top = new Rect(0, 0, getWidth(), 6);
        Rect bottom = new Rect(0, getHeight() - 6, getWidth(), getHeight());

        Calendar calendar = Calendar.getInstance();
        SimpleDate now = new SimpleDate(calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        if (now.getDay() > endTime.getDay() || now.getTime() > endTime.getTime()) {
            canvas.drawRect(back, colorPrimaryPaint);
        } else {
            canvas.drawRect(back, colorBordersPaint);

            if (now.getTime() > startTime.getTime()) {
                Rect progress = new Rect();

                progress.left = back.left;
                progress.top = back.top;
                progress.right = back.right;

                progress.bottom = (int) (((float) (now.getTime() - startTime.getTime()) / (endTime.getTime() - startTime.getTime())) * (getHeight() - 6));

                canvas.drawRect(progress, colorPrimaryPaint);
            }
        }

        canvas.drawRect(top, colorTextPaint);
        canvas.drawRect(bottom, colorTextPaint);
    }

    public void setStartTime(SimpleDate startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(SimpleDate endTime) {
        this.endTime = endTime;
    }
}
