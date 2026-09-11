package com.gsoft.inventory.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.CountValueHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * RFID盘点分组统计适配器（RecyclerView两列网格）：分组名称 + 已盘(绿)/未盘(红)/全部(黑)
 * <p>
 * 名称不限行数完整折行显示；同一行卡片高度由统计页LayoutManager在布局后拉平为最高卡片
 */
public class RfidDeptStatsAdapter extends RecyclerView.Adapter<RfidDeptStatsAdapter.ViewHolder> {

    /**
     * 卡片点击监听：部门层点击跳存放地点层，存放地点层点击跳RFID扫描页
     */
    public interface OnItemClickListener {
        void onItemClick(DeptStats stats);
    }

    /** 分组数据源（部门层为部门列表，存放地点层为地点列表） */
    private List<DeptStats> dataList = new ArrayList<>();
    /** 卡片点击回调 */
    private OnItemClickListener onItemClickListener;

    /** 注册卡片点击监听 */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setData(List<DeptStats> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rfid_dept_stats_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeptStats stats = dataList.get(position);
        // 重置最小高度：卡片复用时不残留上一行拉平的高度，先按真实内容测量再由LayoutManager统一拉平
        holder.itemView.setMinimumHeight(0);
        holder.tvDeptName.setText(stats.deptName);
        // 千分位显示，三个数值按最大位数统一字号
        int uncounted = stats.total - stats.counted;
        int maxDigits = CountValueHelper.maxDigitsOf(stats.counted, uncounted, stats.total);
        CountValueHelper.showGroupCardCount(holder.tvCounted, stats.counted, maxDigits);
        CountValueHelper.showGroupCardCount(holder.tvUncounted, uncounted, maxDigits);
        CountValueHelper.showGroupCardCount(holder.tvTotal, stats.total, maxDigits);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(stats);
            }
        });
    }

    /** 分组卡片ViewHolder：分组名称 + 已盘/未盘/全部三列 */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** 部门/地点名称（不限行数完整折行显示） */
        TextView tvDeptName;
        /** 已盘数量（绿） */
        TextView tvCounted;
        /** 未盘数量（红） */
        TextView tvUncounted;
        /** 全部数量（黑） */
        TextView tvTotal;

        ViewHolder(View itemView) {
            super(itemView);
            tvDeptName = itemView.findViewById(R.id.tvDeptName);
            tvCounted = itemView.findViewById(R.id.tvCounted);
            tvUncounted = itemView.findViewById(R.id.tvUncounted);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }

    /**
     * 部门盘点统计数据：部门名称 / 已盘数量 / 全部数量（未盘=全部-已盘，展示时计算）
     */
    public static class DeptStats {
        public String deptName;
        public int counted;
        public int total;
    }
}