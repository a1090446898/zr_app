package com.gsoft.inventory.adapter.asset_receive;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetReceiveDetailItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 */
public class AssetReceiveDetailAdapter extends RecyclerView.Adapter<AssetReceiveDetailAdapter.ViewHolder> {

    private List<AssetReceiveDetailItem> dataList = new ArrayList<>();

    public AssetReceiveDetailAdapter(List<AssetReceiveDetailItem> data) {
        this.dataList.addAll(data);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_asset_receive_detail_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetReceiveDetailItem item = dataList.get(position);

        // 绑定数据到列表项
        holder.cbSelect.setChecked(item.isSelected());
        holder.tvAssetId.setText("资产编号: " + item.getAssetId());
        holder.tvAssetsName.setText("资产名称: " + item.getAssetsName());
        holder.tvBarcodeId.setText("条码编号: " + item.getBarcodeId());
        holder.tvStandard.setText("规格型号: " + item.getAssetsStandard());
        holder.tvUser.setText("使用人: " + item.getAssetsUser());
        holder.tvLocation.setText("存放地点: " + item.getAssetsLayAdd());
        holder.tvPrice.setText("资产原值: " + item.getAssetsCurrPrice());

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
        for (AssetReceiveDetailItem item : dataList) {
            item.setSelected(isSelect);
        }
        notifyDataSetChanged();
    }

    // 删除选中项
    public void removeSelected() {
        List<AssetReceiveDetailItem> toRemove = new ArrayList<>();
        for (AssetReceiveDetailItem item : dataList) {
            if (item.isSelected()) toRemove.add(item);
        }
        dataList.removeAll(toRemove);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvAssetId, tvAssetsName, tvBarcodeId, tvStandard, tvUser, tvLocation, tvPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvAssetId = itemView.findViewById(R.id.tv_asset_id);
            tvAssetsName = itemView.findViewById(R.id.tv_assetsname);
            tvBarcodeId = itemView.findViewById(R.id.tv_barcodeid);
            tvStandard = itemView.findViewById(R.id.tv_assetsstandard);
            tvUser = itemView.findViewById(R.id.tv_assetsuser);
            tvLocation = itemView.findViewById(R.id.tv_assetslayadd);
            tvPrice = itemView.findViewById(R.id.tv_assetscurrprice);
        }
    }
}
