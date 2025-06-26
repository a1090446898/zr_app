package com.gsoft.inventory.adapter.asset_repair;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetRepair;

import java.util.List;

/**
 * @author 10904
 */
public class AssetRepairAdapter extends RecyclerView.Adapter<AssetRepairAdapter.ViewHolder> {

    private Context mContext;
    private List<AssetRepair> mAssetReceiveList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(AssetRepair item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public AssetRepairAdapter(Context context, List<AssetRepair> assetReceiveList) {
        mContext = context;
        mAssetReceiveList = assetReceiveList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_asset_repair_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetRepair item = mAssetReceiveList.get(position);
        holder.tv_DOCID.setText("编号：" + item.getDOCID());
        holder.tv_SENDUSER.setText("报修人: " + item.getSENDUSER());
        holder.tv_SENDPHONE.setText("联系电话: " + item.getSENDPHONE());
        holder.tv_SENDDATE.setText("报修日期: " + item.getSENDDATE());
        holder.tv_SENDREASON.setText("报修原因: " + item.getSENDREASON());
        holder.tv_DOUSER.setText("经办人: " + item.getDOUSER());
        holder.tv_RESULT.setText("维修情况: " + item.getRESULT());
        holder.tv_DODATE.setText("办结时间: " + item.getDODATE());
        holder.tv_FID.setText("资产编号: " + item.getFID());

        if (mListener != null) {
            holder.itemView.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return mAssetReceiveList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_DOCID, tv_SENDUSER, tv_SENDPHONE, tv_SENDDATE,
                tv_SENDREASON, tv_DOUSER, tv_RESULT, tv_DODATE, tv_FID;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_DOCID = itemView.findViewById(R.id.tv_DOCID);
            tv_SENDUSER = itemView.findViewById(R.id.tv_SENDUSER);
            tv_SENDPHONE = itemView.findViewById(R.id.tv_SENDPHONE);
            tv_SENDDATE = itemView.findViewById(R.id.tv_SENDDATE);
            tv_SENDREASON = itemView.findViewById(R.id.tv_SENDREASON);
            tv_DOUSER = itemView.findViewById(R.id.tv_DOUSER);
            tv_RESULT = itemView.findViewById(R.id.tv_RESULT);
            tv_DODATE = itemView.findViewById(R.id.tv_DODATE);
            tv_FID = itemView.findViewById(R.id.tv_FID);
        }
    }


}