package com.koubilgi.submenus;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.koubilgi.R;
import com.koubilgi.api.Student;
import com.koubilgi.components.TimeSpanView;
import com.koubilgi.utils.ConnectionListener;
import com.koubilgi.utils.SimpleDate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * TODO: İçinde olduğumuz dönemin ne zaman başaldığını bulabilmemiz gerek şu an kaçıncı haftada olduğumuzu bulmak için.
 * Belki akademik takvimi indirip onu ayrıştırarak bulabiliriz. Akademik takvim sayfası da ekleriz uygulamaya.
 */

public class Syllabus extends Submenu
{
    Syllabus()
    {
        super(R.string.submenu_syllabus, R.drawable.icon_mainmenu_dersprogrami);
    }

    @Override
    public void fillContentView(final Context context)
    {
        Student.getInstance(context).getRequestMaker().makeGetRequest("https://ogr.kocaeli.edu" + ".tr/KOUBS/Ogrenci" +
                "/DersIslemleri" + "/DersProgramiIcerik.cfm", new ConnectionListener()
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
                        String hourString = columns.get(0).select("i").text();
                        int hour = Integer.parseInt(hourString.substring(0, 2));
                        int minute = Integer.parseInt(hourString.substring(3, 5));

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

                                    SimpleDate classDate = new SimpleDate(columnIndex - 1, hour, minute);

                                    days[columnIndex - 1].addClass(new Class(className, location, teacher, classDate));
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        // TODO: Üniversite site tasarımını değiştirmiş demektir, bu menüyü uygulamaya güncelleme
                        //  gelene kadar offline moda geçir.
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
    final int dayIndex;
    final ArrayList<Class> classes = new ArrayList<>();

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

        final View divider = inflater.inflate(R.layout.view_submenu_divider, null);
        final TextView dividerMain = divider.findViewById(R.id.textdivider_maintext);
        TextView dividerText = divider.findViewById(R.id.textdivider_text);
        dividerText.setVisibility(View.GONE);
        dividerMain.setText(getName());

        if ((Calendar.MONDAY + dayIndex) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            dividerText.setVisibility(View.VISIBLE);
            dividerText.setText(R.string.submenu_syllabus_today);

            // TODO: Bugün e scroll atılması gerek otomatik

            final ScrollView scrollView = ((Activity) context).findViewById(R.id.submenu_scrollview);
            scrollView.post(new Runnable()
            {
                @Override
                public void run()
                {
                    divider.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    System.out.println(divider.getTop());
                    //scrollView.scrollTo(0, divider.getScrollY());
                }
            });
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, (int) (metrics.density * 5));

        layout.addView(divider, layoutParams);

        for (int i = 0; i < classes.size(); i++)
        {
            LinearLayout.LayoutParams cardViewLayout =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            cardViewLayout.setMargins(0, 0, 0, (int) (metrics.density * 5));
            View cardView = classes.get(i).getView(context);
            layout.addView(cardView, cardViewLayout);

            // TODO: Tasarım hedefleri takip edilmeli, bu kısım tam değil.
            if (i + 1 < classes.size())
            {
                if (classes.get(i).endTime.getTime() < classes.get(i + 1).startTime.getTime())
                {
                    TextView freeTime = new TextView(context);
                    String text = (classes.get(i + 1).startTime.getTime() - classes.get(i).endTime.getTime()) + " " +
                                    "dakika ara";
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

            if ((toAdd.startTime.getTime() - cl.endTime.getTime()) <= 60)
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
    final String name;
    final String location;
    final String teacher;

    final SimpleDate startTime;
    SimpleDate endTime;

    private int count = 1; // bu dersten kaç tane olduğu

    Class(String name, String location, String teacher, SimpleDate start)
    {
        this.name = name;
        this.location = location;
        this.teacher = teacher;
        this.startTime = start;
        this.endTime = start.addMinutes(40 * count);
    }

    View getView(Context context)
    {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(0, (int) (metrics.density * 10), 0, (int) (metrics.density * 10));
        layout.setClipToPadding(false);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View cardView = inflater.inflate(R.layout.cardview_syllabus, null);

        ((TextView) cardView.findViewById(R.id.syllabus_classname)).setText(name);
        //((TextView) cardView.findViewById(R.id.syllabus_subject)).setText(subject);
        ((TextView) cardView.findViewById(R.id.syllabus_teacher)).setText(teacher);
        ((TextView) cardView.findViewById(R.id.syllabus_location)).setText(location);

        ((TextView) cardView.findViewById(R.id.syllabus_starttime)).setText(startTime.toString());
        ((TextView) cardView.findViewById(R.id.syllabus_count)).setText(String.format(context.getString(R.string.submenu_syllabus_class_count), count));
        ((TextView) cardView.findViewById(R.id.syllabus_totaltime)).setText(String.format(context.getString(R.string.submenu_syllabus_class_total_time), count * 40));

        final TimeSpanView spanView = new TimeSpanView(context);
        spanView.setStartTime(startTime);
        spanView.setEndTime(endTime);
        int spanViewWidth = (int) (metrics.density * 13);
        spanView.setLayoutParams(new LinearLayout.LayoutParams(spanViewWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        spanView.setX((int) (metrics.density * 20) - spanViewWidth / 2);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int) (metrics.density * 32), 0, (int) (metrics.density * 20), 0);

        layout.addView(spanView);
        layout.addView(cardView, layoutParams);

        return layout;
    }

    void increaseCount()
    {
        count++;
        endTime = startTime.addMinutes(count * 40);
    }

    int getCount()
    {
        return this.count;
    }
}