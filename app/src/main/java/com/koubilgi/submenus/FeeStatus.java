package com.koubilgi.submenus;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.koubilgi.R;
import com.koubilgi.api.Student;
import com.koubilgi.utils.ConnectionListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FeeStatus extends Submenu {
    FeeStatus() {
        super(R.string.submenu_fee_status, R.drawable.icon_mainmenu_harcdurumu);
    }

    @Override
    public void fillContentView(final Context context) {
        Student.getInstance().getRequestMaker().makeGetRequest("https://ogr.kocaeli.edu.tr/KOUBS/Ogrenci/OgrenciIsleri/HarcBilgi.cfm", new ConnectionListener() {
            @Override
            public void onSuccess(String... args) {
                String response = args[0];

                Document doc = Jsoup.parse(response);

                Elements mainDivs = doc.select("div.panel-body > div.col-lg-12:not(div.bg-info):has(div.col-lg-2)");

                if (mainDivs.size() <= 0) {
                    // TODO: Üniversite site tasarımını değiştirmiş demektir, bu menüyü uygulamaya güncelleme gelene
                    //  kadar offline moda geçir.
                    return;
                }

                Fee[] fees = new Fee[mainDivs.size()];

                for (int i = 0; i < mainDivs.size(); i++) {
                    Element parent = mainDivs.get(i);
                    Elements els = parent.select("div.col-lg-2");

                    Fee fee = new Fee();
                    fee.term = els.first().text();
                    fee.fee = els.get(1).text();
                    fee.paid = els.get(3).text();
                    fee.status = els.last().text();

                    fees[i] = fee;
                }

                LinearLayout layout = ((Activity) context).findViewById(R.id.submenu_linearlayout);
                for (Fee fee : fees) {
                    View toAdd = fee.getView(context);
                    layout.addView(toAdd);
                }
            }

            @Override
            public void onFailure(String reason) {
                // TODO: Bu menü yeniden açılana kadar offline olarak göster?
            }
        });
    }
}

class Fee {
    public String term;
    public String status;
    public String fee;
    public String paid;

    public View getView(Context context) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setClipToPadding(false);
        layout.setPadding((int) metrics.density * 20, (int) metrics.density * 10, (int) metrics.density * 20, (int) metrics.density * 10);

        View divider = inflater.inflate(R.layout.view_submenu_divider, null);
        TextView dividerMain = divider.findViewById(R.id.textdivider_maintext);
        TextView dividerText = divider.findViewById(R.id.textdivider_text);
        dividerText.setVisibility(View.GONE);
        dividerMain.setText(term);
        ImageView dividerImage = divider.findViewById(R.id.textdivider_image);
        dividerImage.setImageResource(R.drawable.icon_lesson_daymarker_circular);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, (int) (metrics.density * 4));

        layout.addView(divider, layoutParams);

        View cardView = inflater.inflate(R.layout.cardview_feestatus, null);
        TextView feeView = cardView.findViewById(R.id.feestatus_fee);
        TextView paidView = cardView.findViewById(R.id.feestatus_paid);
        ImageView checkmark = cardView.findViewById(R.id.feestatus_checkmark);

        feeView.setText(fee);
        paidView.setText(paid);

        if (fee.equals(paid))
            checkmark.setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark));
        else checkmark.setColorFilter(context.getResources().getColor(R.color.colorBorders));

        layout.addView(cardView);

        return layout;
    }
}