package com.koubilgi.components;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.koubilgi.R;
import com.koubilgi.activities.SubmenuActivity;
import com.koubilgi.submenus.SubmenuManager;

public class SubmenuButtonAdapter extends BaseAdapter
{
    private final Context context;

    public SubmenuButtonAdapter(Context ctx)
    {
        context = ctx;
    }

    @Override
    public int getCount()
    {
        return SubmenuManager.submenus.length;
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
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View cardView = inflater.inflate(R.layout.cardview_submenubutton, null);
        ImageView image = cardView.findViewById(R.id.submenu_image);
        TextView text = cardView.findViewById(R.id.submenu_text);

        image.setImageResource(SubmenuManager.submenus[position].getIconResource());
        text.setText(SubmenuManager.submenus[position].getNameResource());

        cardView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(context, SubmenuActivity.class);
                intent.putExtra("name", SubmenuManager.submenus[position].getNameResource());
                context.startActivity(intent);
            }
        });

        return cardView;
    }
}
