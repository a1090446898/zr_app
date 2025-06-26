package com.gsoft.inventory.adapter.asset_purchase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetPurchase;

import java.util.List;

/**
 * @author 10904
 */
public class AssetPurchaseAdapter extends RecyclerView.Adapter<AssetPurchaseAdapter.ViewHolder> {

    private Context mContext;
    private List<AssetPurchase> mAssetReceiveList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(AssetPurchase item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public AssetPurchaseAdapter(Context context, List<AssetPurchase> assetReceiveList) {
        mContext = context;
        mAssetReceiveList = assetReceiveList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_asset_purchase_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetPurchase item = mAssetReceiveList.get(position);
        holder.tv_DOCID.setText("编号：" + item.getDOCID());
        holder.tv_APPLICANT.setText("借用人: " + item.getNAME());
        holder.tv_USEDEPT.setText("采购部门: " + item.getUNITNAME());
        holder.tv_APPLY_EXPLAIN.setText("采购说明: " + item.getAPPLY_EXPLAIN());
        holder.tv_STATE.setText("审核状态: " + item.getSTATE());
        holder.tv_APPLICANTDATE.setText("采购日期: " + item.getAPPLICANTDATE());

        if (mListener != null) {
            holder.itemView.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return mAssetReceiveList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_DOCID;
        TextView tv_APPLICANT;
        TextView tv_USEDEPT;
        TextView tv_APPLY_EXPLAIN;
        TextView tv_STATE;
        TextView tv_APPLICANTDATE;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_DOCID = itemView.findViewById(R.id.tv_DOCID);
            tv_APPLICANT = itemView.findViewById(R.id.tv_APPLICANT);
            tv_USEDEPT = itemView.findViewById(R.id.tv_USEDEPT);
            tv_APPLY_EXPLAIN = itemView.findViewById(R.id.tv_APPLY_EXPLAIN);
            tv_STATE = itemView.findViewById(R.id.tv_STATE);
            tv_APPLICANTDATE = itemView.findViewById(R.id.tv_APPLICANTDATE);
        }
    }


}