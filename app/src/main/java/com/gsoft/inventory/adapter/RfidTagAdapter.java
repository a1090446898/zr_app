package com.gsoft.inventory.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RFID识别标签卡片列表
 */
public class RfidTagAdapter extends RecyclerView.Adapter<RfidTagAdapter.RfidTagViewHolder> {

    public static class RfidTagItem {
        /** 标签EPC（十六进制） */
        public String epc;
        /** EPC解码出的资产条码 */
        public String barcode;
        /** 信号强度 */
        public String rssi;
        /** 查到的资产，null表示未入库 */
        public Assets assets;
    }

    public interface OnItemClickListener {
        void onItemClick(RfidTagItem item);
    }

    private final List<RfidTagItem> dataList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public List<RfidTagItem> getDataList() {
        return dataList;
    }

    public void addItem(RfidTagItem item) {
        dataList.add(item);
        notifyItemInserted(dataList.size() - 1);
    }

    @NonNull
    @Override
    public RfidTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rfid_tag, parent, false);
        return new RfidTagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RfidTagViewHolder holder, int position) {
        RfidTagItem item = dataList.get(position);
        if (item.assets != null) {
            holder.tvName.setText(item.assets.getASSETSNAME());
            holder.tvName.setTextColor(0xFF333333);
            holder.tvBarcode.setText("条码：" + item.assets.getBARCODEID());
            holder.tvDept.setText("使用部门：" + item.assets.getASSETSDEPT());
        } else {
            holder.tvName.setText("未知标签（未入库）");
            holder.tvName.setTextColor(0xFFC62828);
            holder.tvBarcode.setText("EPC：" + item.epc
                    + (StringUtils.isNullOrEmpty(item.barcode) ? "" : "（" + item.barcode + "）"));
            holder.tvDept.setText("该条码未在资产台账中登记");
        }
        holder.tvRssi.setText(StringUtils.isNullOrEmpty(item.rssi) ? "" : item.rssi + "dBm");
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class RfidTagViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvRssi;
        TextView tvBarcode;
        TextView tvDept;

        RfidTagViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTagName);
            tvRssi = itemView.findViewById(R.id.tvTagRssi);
            tvBarcode = itemView.findViewById(R.id.tvTagBarcode);
            tvDept = itemView.findViewById(R.id.tvTagDept);
        }
    }
}