package com.koubilgi.submenus;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SyllabusSubmenu extends Submenu
{
    SyllabusSubmenu()
    {
        super(R.string.SYLLABUS, R.drawable.icon_mainmenu_dersprogrami);
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

                                    days[columnIndex - 1].addClass(new Class(className, location, teacher, rowTime));
                                }
                            }
                            //days[rowIndex-1].addClass();
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
class Day
{
    // TODO: Find a better way
    final String[] days = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};

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
        layout.setPadding((int) metrics.density * 20, (int) metrics.density * 10, (int) metrics.density * 20,
                (int) metrics.density * 10);

        View divider = inflater.inflate(R.layout.text_divider, null);
        TextView dividerMain = divider.findViewById(R.id.textdivider_maintext);
        TextView dividerText = divider.findViewById(R.id.textdivider_text);
        dividerText.setVisibility(View.GONE);
        dividerMain.setText(getName());

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 20);

        layout.addView(divider, layoutParams);

        layoutParams.setMargins(0, 20, 0, 0);

        for (int i = 0; i < classes.size(); i++)
        {
            View cardView = this.classes.get(i).getView(context);
            layout.addView(cardView, layoutParams);
        }

        return layout;
    }

    String getName()
    {
        return days[dayIndex];
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

class Class
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
        ((TextView) cardView.findViewById(R.id.syllabus_count)).setText(count + " ders");
        ((TextView) cardView.findViewById(R.id.syllabus_totaltime)).setText(count * 40 + " dakika");

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