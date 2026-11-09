package com.gsoft.inventory.service.rfid_stats;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

/**
 * 两列等高网格LayoutManager：
 * 每次布局完成后把同一行卡片的最小高度拉平为该行最高卡片，
 * 实现"同一行卡片随最长名称自动等高"（RecyclerView本身不会同行等高）
 */
public class EqualRowHeightGridLayoutManager extends GridLayoutManager {

    public EqualRowHeightGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        equalizeRowHeights();
    }

    /** 同行卡片按行内最大高度统一最小高度，保证卡片底部对齐；高度一致后不再触发重排 */
    private void equalizeRowHeights() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }
        // 行号 → 行内最大卡片高度
        Map<Integer, Integer> rowMaxHeights = new HashMap<>();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getPosition(child);
            if (position == RecyclerView.NO_POSITION) {
                continue;
            }
            int row = position / getSpanCount();
            Integer max = rowMaxHeights.get(row);
            if (max == null || child.getHeight() > max) {
                rowMaxHeights.put(row, child.getHeight());
            }
        }
        // 低于行最大高度的卡片拉平（setMinimumHeight值不变时不会再触发布局，不会循环）
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getPosition(child);
            if (position == RecyclerView.NO_POSITION) {
                continue;
            }
            Integer max = rowMaxHeights.get(position / getSpanCount());
            if (max != null && child.getHeight() < max && child.getMinimumHeight() != max) {
                child.setMinimumHeight(max);
            }
        }
    }
}
