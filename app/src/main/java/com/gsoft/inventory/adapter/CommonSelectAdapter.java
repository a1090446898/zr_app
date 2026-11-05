package com.gsoft.inventory.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.data.SelectItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用选择列表适配器：部门、人员共用
 */
public class CommonSelectAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<SelectItem> dataList = new ArrayList<>();
    private String selectedName = "";

    public CommonSelectAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<SelectItem> list) {
        dataList.clear();
        if (list != null) {
            dataList.addAll(list);
        }
        notifyDataSetChanged();
    }

    /**
     * 标记当前已选中的名称，列表中显示"当前"标签
     */
    public void setSelectedName(String name) {
        selectedName = name == null ? "" : name;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public SelectItem getItem(int position) {
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
            convertView = inflater.inflate(R.layout.item_common_select, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SelectItem item = getItem(position);
        holder.tvName.setText(item.getName());
        holder.tvSubTitle.setText(item.getSubTitle());
        holder.tvSubTitle.setVisibility(TextUtils.isEmpty(item.getSubTitle()) ? View.GONE : View.VISIBLE);
        holder.tvCurrent.setVisibility(selectedName.equals(item.getName()) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    static class ViewHolder {
        final TextView tvName;
        final TextView tvSubTitle;
        final TextView tvCurrent;

        ViewHolder(View view) {
            tvName = view.findViewById(R.id.tvName);
            tvSubTitle = view.findViewById(R.id.tvSubTitle);
            tvCurrent = view.findViewById(R.id.tvCurrent);
        }
    }
}
