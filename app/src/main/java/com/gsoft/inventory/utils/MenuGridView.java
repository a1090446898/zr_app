package com.gsoft.inventory.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;


/**
 * Created by gsc on 2017/2/22.
 */
public class MenuGridView extends GridView {
    public MenuGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MenuGridView(Context context) {
        super(context);
    }

    public MenuGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
