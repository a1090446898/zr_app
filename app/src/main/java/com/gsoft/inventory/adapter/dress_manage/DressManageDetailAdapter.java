package com.gsoft.inventory.adapter.dress_manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.DressManageDetailItem;
import com.gsoft.inventory.entities.DressManageDetailItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 */
public class DressManageDetailAdapter extends RecyclerView.Adapter<DressManageDetailAdapter.ViewHolder> {

    private List<DressManageDetailItem> dataList = new ArrayList<>();

    public DressManageDetailAdapter(List<DressManageDetailItem> data) {
        this.dataList.addAll(data);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_dress_manage_detail_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DressManageDetailItem item = dataList.get(position);

        // 绑定数据到列表项
        holder.cbSelect.setChecked(item.isSelected());
        holder.tv_docId.setText("服饰编号: " + item.getDocId());
        holder.tv_dressName.setText("服饰名称: " + item.getDressName());
        holder.tv_receiveNum.setText("领用数量: " + item.getReceiveNum());
        holder.tv_inventoryQuantity.setText("库存数量: " + item.getInventoryQuantity());
        holder.tv_receiveUser.setText("领用人: " + item.getReceiveUser());
        holder.tv_receiveDepart.setText("领用部门: " + item.getReceiveDepart());

        // 处理多选框点击
        holder.cbSelect.setOnClickListener(v -> {
            item.setSelected(holder.cbSelect.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // 全选/反选
    public void selectAll(boolean isSelect) {
        for (DressManageDetailItem item : dataList) {
            item.setSelected(isSelect);
        }
        notifyDataSetChanged();
    }

    // 删除选中项
    public void removeSelected() {
        List<DressManageDetailItem> toRemove = new ArrayList<>();
        for (DressManageDetailItem item : dataList) {
            if (item.isSelected()) toRemove.add(item);
        }
        dataList.removeAll(toRemove);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tv_docId, tv_dressName,
                tv_receiveNum, tv_inventoryQuantity,
                tv_receiveUser, tv_receiveDepart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tv_docId = itemView.findViewById(R.id.tv_docId);
            tv_dressName = itemView.findViewById(R.id.tv_dressName);
            tv_receiveNum = itemView.findViewById(R.id.tv_receiveNum);
            tv_inventoryQuantity = itemView.findViewById(R.id.tv_inventoryQuantity);
            tv_receiveUser = itemView.findViewById(R.id.tv_receiveUser);
            tv_receiveDepart = itemView.findViewById(R.id.tv_receiveDepart);
        }
    }
}
