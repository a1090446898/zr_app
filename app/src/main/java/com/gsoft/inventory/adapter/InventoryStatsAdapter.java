package com.gsoft.inventory.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 盘点统计列表：资产名称 / 条码编号 / 盘点情况(PDZT) / 盘点状态(ZT)
 */
public class InventoryStatsAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private List<Assets> dataList = new ArrayList<>();

    public InventoryStatsAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Assets getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.inventory_stats_item, parent, false);
            holder = new ViewHolder();
            holder.tvASSETSNAME = convertView.findViewById(R.id.tvASSETSNAME);
            holder.tvBARCODEID = convertView.findViewById(R.id.tvBARCODEID);
            holder.tvPDZT = convertView.findViewById(R.id.tvPDZT);
            holder.tvZT = convertView.findViewById(R.id.tvZT);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Assets assets = dataList.get(position);
        holder.tvASSETSNAME.setText(assets.getASSETSNAME() == null ? "" : assets.getASSETSNAME());
        holder.tvBARCODEID.setText(assets.getBARCODEID() == null ? "" : assets.getBARCODEID());
        holder.tvPDZT.setText(pdztName(assets.getPDZT()));
        holder.tvZT.setText(ztName(assets.getZT()));
        return convertView;
    }

    /**
     * 盘点情况：01待盘点 02盘实 03盘盈 04待核实
     */
    private static String pdztName(String pdzt) {
        if ("01".equals(pdzt)) {
            return "待盘点";
        }
        if ("02".equals(pdzt)) {
            return "盘实";
        }
        if ("03".equals(pdzt)) {
            return "盘盈";
        }
        if ("04".equals(pdzt)) {
            return "待核实";
        }
        return "";
    }

    /**
     * 盘点状态：01未盘 02已盘，空值也是未盘
     */
    private static String ztName(String zt) {
        if ("02".equals(zt)) {
            return "已盘";
        }
        return "未盘";
    }

    static class ViewHolder {
        TextView tvASSETSNAME;
        TextView tvBARCODEID;
        TextView tvPDZT;
        TextView tvZT;
    }
}

