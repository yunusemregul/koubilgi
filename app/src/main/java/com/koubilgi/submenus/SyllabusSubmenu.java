package com.koubilgi.submenus;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koubilgi.R;
import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * TODO: Find a way to get when did school term started
 */

public class SyllabusSubmenu extends Submenu
{
    SyllabusSubmenu()
    {
        super(R.string.submenu_syllabus, R.drawable.icon_mainmenu_dersprogrami);
    }

    @Override
    public void fillContentView(final Context context)
    {
        Student.getInstance(context).makeGetRequest("https://ogr.kocaeli.edu" + ".tr/KOUBS/Ogrenci/DersIslemleri" +
                "/DersProgramiIcerik.cfm", new ConnectionListener()
        {
            @Override
            public void onSuccess(String... args)
            {
                String response = args[0];

                Document doc = Jsoup.parse(response);

                Day[] days = new Day[7];

                for (int i = 0; i < 7; i++)
                {
                    days[i] = new Day(i);
                }

                Elements rows = doc.select("tr");
                for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++)
                {
                    Element row = rows.get(rowIndex);
                    Elements columns = row.select("td");

                    try
                    {
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

                        Date rowTime = format.parse(columns.get(0).select("i").text());

                        for (int columnIndex = 1; columnIndex < columns.size(); columnIndex++)
                        {
                            Element column = columns.get(columnIndex);

                            if (column.text().length() > 0)
                            {
                                for (int i = 0; i < column.select("b").size(); i++)
                                {
                                    String className = column.select("b").get(i).text();
                                    String[] teacherAndLocation = column.select("i").get(i).html().split("<br>");
                                    String teacher = teacherAndLocation[0];
                                    String location = teacherAndLocation[1];
                                    location = location.substring(1, location.length() - 1);

                                    days[columnIndex - 1].addClass(new Class(className, location, teacher, rowTime));
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        // TODO: Site has been changed, go offline for this submenu indefinitely (till updated)
                        e.printStackTrace();
                    }
                }

                LinearLayout layout = ((Activity) context).findViewById(R.id.submenu_linearlayout);
                for (Day day : days)
                {
                    if (day.classes.size() == 0)
                        continue;

                    View toAdd = day.getView(context);
                    layout.addView(toAdd);
                }
            }

            @Override
            public void onFailure(String reason)
            {

            }
        });
    }
}

/**
 * Represents a syllabus day which includes classes
 */
class Day implements Serializable
{
    int dayIndex;
    ArrayList<Class> classes = new ArrayList<>();

    Day(int dayIndex)
    {
        this.dayIndex = dayIndex;
    }

    View getView(Context context)
    {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setClipToPadding(false);
        layout.setPadding(0, (int) metrics.density * 10, (int) metrics.density * 20, (int) metrics.density * 10);

        View divider = inflater.inflate(R.layout.text_divider, null);
        TextView dividerMain = divider.findViewById(R.id.textdivider_maintext);
        TextView dividerText = divider.findViewById(R.id.textdivider_text);
        dividerText.setVisibility(View.GONE);
        dividerMain.setText(getName());

        if ((Calendar.MONDAY + dayIndex) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            dividerText.setVisibility(View.VISIBLE);
            dividerText.setText(R.string.submenu_syllabus_today);
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, (int) (metrics.density * 12));

        layout.addView(divider, layoutParams);

        for (int i = 0; i < classes.size(); i++)
        {
            LinearLayout.LayoutParams cardViewLayout =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            cardViewLayout.setMargins((int) (metrics.density * 36), 0, 0, (int) (metrics.density * 12));
            View cardView = classes.get(i).getView(context);
            layout.addView(cardView, cardViewLayout);

            // TODO: Follow the design, its not complete
            if (i + 1 < classes.size())
            {
                if (classes.get(i).getEndTime() < classes.get(i + 1).startTime.getTime())
                {
                    TextView freeTime = new TextView(context);
                    String text =
                            TimeUnit.MINUTES.convert(classes.get(i + 1).startTime.getTime() - classes.get(i).getEndTime(), TimeUnit.MILLISECONDS) + " dakika ara";
                    freeTime.setText(text);
                    freeTime.setGravity(Gravity.CENTER);
                    freeTime.setTextColor(context.getResources().getColor(R.color.colorText));
                    layout.addView(freeTime, cardViewLayout);
                }
            }
        }

        return layout;
    }

    String getName()
    {
        return DateFormatSymbols.getInstance().getWeekdays()[Calendar.MONDAY + dayIndex];
    }

    void addClass(Class toAdd)
    {
        for (int i = 0; i < this.classes.size(); i++)
        {
            Class cl = this.classes.get(i);

            if (!cl.name.equals(toAdd.name))
                continue;

            if (TimeUnit.MINUTES.convert(toAdd.startTime.getTime() - cl.getEndTime(), TimeUnit.MILLISECONDS) <= 60)
            {
                cl.increaseCount();
                return;
            }
        }

        this.classes.add(toAdd);
    }
}

class Class implements Serializable
{
    String name;
    String location;
    String teacher;
    Date startTime;
    private int count = 1; // how many of this class

    Class(String name, String location, String teacher, Date start)
    {
        this.name = name;
        this.location = location;
        this.teacher = teacher;
        this.startTime = start;
    }

    View getView(Context context)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View cardView = inflater.inflate(R.layout.cardview_syllabus, null);

        ((TextView) cardView.findViewById(R.id.syllabus_classname)).setText(name);
        //((TextView) cardView.findViewById(R.id.syllabus_subject)).setText(subject);
        ((TextView) cardView.findViewById(R.id.syllabus_teacher)).setText(teacher);
        ((TextView) cardView.findViewById(R.id.syllabus_location)).setText(location);

        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        ((TextView) cardView.findViewById(R.id.syllabus_starttime)).setText(format.format(startTime));
        ((TextView) cardView.findViewById(R.id.syllabus_count)).setText(String.format(context.getString(R.string.submenu_syllabus_class_count), count));
        ((TextView) cardView.findViewById(R.id.syllabus_totaltime)).setText(String.format(context.getString(R.string.submenu_syllabus_class_total_time), count * 40));

        return cardView;
    }

    void increaseCount()
    {
        count++;
    }

    int getCount()
    {
        return this.count;
    }

    long getEndTime()
    {
        return (startTime.getTime() + count * 40 * 60 * 1000);
    }
}