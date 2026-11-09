package com.gsoft.inventory.service.rfid_stats;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 网格间距装饰：列间距左右各半保证两列卡片间距均匀，行间距加在非首行卡片顶部（首行与标题自然衔接），
 * 左右/底部边距由RecyclerView的padding提供（与原GridView参数一致：边距12dp、间距10dp）
 */
public class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {

    /** 列数（与LayoutManager的spanCount一致） */
    private final int spanCount;
    /** 列间距(px) */
    private final int columnSpace;
    /** 行间距(px) */
    private final int rowSpace;

    public GridSpaceItemDecoration(int spanCount, int columnSpace, int rowSpace) {
        this.spanCount = spanCount;
        this.columnSpace = columnSpace;
        this.rowSpace = rowSpace;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        int column = position % spanCount;
        int row = position / spanCount;
        outRect.left = column == 0 ? 0 : columnSpace / 2;
        outRect.right = column == 0 ? columnSpace / 2 : 0;
        outRect.top = row == 0 ? 0 : rowSpace;
    }
}
