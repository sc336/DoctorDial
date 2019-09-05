package com.tregrad.doctordial;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

/**
 * Created by piduck on 30/06/17.
 */
public class SquareButton extends ImageButton {

    public SquareButton(Context context) {
        super(context);
    }

    public SquareButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

}
