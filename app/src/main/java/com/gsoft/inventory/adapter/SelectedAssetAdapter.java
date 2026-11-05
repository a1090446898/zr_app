package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单页已选资产精简列表：名称 + 条码 + 移除
 *
 * @author 10904
 */
public class SelectedAssetAdapter extends BaseAdapter {

    public interface OnRemoveClickListener {
        void onRemoveClick(Assets assets);
    }

    private final LayoutInflater mInflater;
    private List<Assets> dataList = new ArrayList<>();
    private OnRemoveClickListener removeListener;

    public SelectedAssetAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeListener = listener;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_selected_asset, parent, false);
            holder = new ViewHolder();
            holder.tvAssetsName = convertView.findViewById(R.id.tvAssetsName);
            holder.tvBarcode = convertView.findViewById(R.id.tvBarcode);
            holder.tvRemove = convertView.findViewById(R.id.tvRemove);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Assets assets = dataList.get(position);
        holder.tvAssetsName.setText(StringUtils.isNullOrEmpty(assets.getASSETSNAME())
                ? "未命名资产" : assets.getASSETSNAME());
        holder.tvBarcode.setText("条码：" + (assets.getBARCODEID() == null ? "" : assets.getBARCODEID()));
        holder.tvRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveClick(assets);
            }
        });
        return convertView;
    }

    static class ViewHolder {
        TextView tvAssetsName;
        TextView tvBarcode;
        TextView tvRemove;
    }
}