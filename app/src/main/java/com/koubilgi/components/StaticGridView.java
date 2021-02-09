package com.koubilgi.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/*
    Kaynak:
        https://stackoverflow.com/a/28898401/12734824
 */

public class StaticGridView extends GridView
{

	public StaticGridView(Context context)
	{
		super(context);
	}

	public StaticGridView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public StaticGridView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST));
		getLayoutParams().height = getMeasuredHeight();
	}
}