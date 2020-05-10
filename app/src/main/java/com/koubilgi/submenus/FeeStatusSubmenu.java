package com.koubilgi.submenus;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koubilgi.R;
import com.koubilgi.api.ConnectionListener;
import com.koubilgi.api.Student;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FeeStatusSubmenu extends Submenu
{
    public FeeStatusSubmenu()
    {
        super(R.string.FEE_STATUS, R.drawable.icon_mainmenu_harcdurumu);
    }

    @Override
    public void fillContentView(final Context context)
    {
        Student.getInstance(context).makeGetRequest("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/OgrenciIsleri/HarcBilgi.cfm",
                new ConnectionListener()
                {
                    @Override
                    public void onSuccess(String... args)
                    {
                        String response = args[0];

                        Document doc = Jsoup.parse(response);

                        Elements mainDivs = doc.select("div.col-lg-12:has(div.col-lg-2):gt(0)");
                        Fee[] fees = new Fee[mainDivs.size()];

                        for (int i = 0; i < mainDivs.size(); i++)
                        {
                            Element parent = mainDivs.get(i);
                            Elements els = parent.select("div.col-lg-2");

                            if (els.size() != 6)
                            {
                                // TODO: Site has been changed, go offline for this submenu indefinitely (till updated)
                                return;
                            }

                            Fee fee = new Fee();
                            fee.term = els.first().text();
                            fee.fee = els.get(1).text();
                            fee.paid = els.get(3).text();
                            fee.status = els.last().text();

                            fees[i] = fee;
                        }

                        FeeStatusAdapter adapter = new FeeStatusAdapter(context, fees);
                        LinearLayout layout = ((Activity) context).findViewById(R.id.submenu_linearlayout);
                        for (int i = 0; i < adapter.getCount(); i++)
                        {
                            View toAdd = adapter.getView(i, null, null);
                            layout.addView(toAdd);
                        }
                    }

                    @Override
                    public void onFailure(String reason)
                    {
                        // TODO: Go offline until next start of this submenu
                    }
                });
    }
}

class Fee
{
    String term;
    String status;
    String fee;
    String paid;
}

class FeeStatusAdapter extends BaseAdapter
{
    private final Context context;
    private Fee[] fees;

    public FeeStatusAdapter(Context context, Fee[] fees)
    {
        this.context = context;
        this.fees = fees;
    }

    @Override
    public int getCount()
    {
        return fees.length;
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setClipToPadding(false);
        layout.setPadding((int) metrics.density * 20, (int) metrics.density * 10, (int) metrics.density * 20, (int) metrics.density * 10);

        View divider = inflater.inflate(R.layout.text_divider, null);
        TextView dividerMain = divider.findViewById(R.id.textdivider_maintext);
        TextView dividerText = divider.findViewById(R.id.textdivider_text);
        dividerText.setVisibility(View.GONE);
        dividerMain.setText(fees[position].term);
        ImageView dividerImage = divider.findViewById(R.id.textdivider_image);
        dividerImage.setImageResource(R.drawable.icon_lesson_daymarker_circular);
        layout.addView(divider);

        View cardView = inflater.inflate(R.layout.cardview_feestatus, null);
        TextView fee = cardView.findViewById(R.id.feestatus_fee);
        TextView paid = cardView.findViewById(R.id.feestatus_paid);
        ImageView checkmark = cardView.findViewById(R.id.feestatus_checkmark);

        fee.setText(fees[position].fee);
        paid.setText(fees[position].paid);

        if (fees[position].fee.equals(fees[position].paid))
            checkmark.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
        else
            checkmark.setColorFilter(context.getResources().getColor(R.color.colorBorders));

        layout.addView(cardView);

        return layout;
    }
}