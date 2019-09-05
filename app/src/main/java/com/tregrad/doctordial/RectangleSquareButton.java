package com.tregrad.doctordial;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageButton;

/**
 * Created by piduck on 30/06/17.
 */
public class RectangleSquareButton extends ImageButton {

    public RectangleSquareButton(Context context) {
        super(context);
    }

    public RectangleSquareButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectangleSquareButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width/2 - (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getDimension(R.dimen.square_button_margin), getResources().getDisplayMetrics()));
    }

}
