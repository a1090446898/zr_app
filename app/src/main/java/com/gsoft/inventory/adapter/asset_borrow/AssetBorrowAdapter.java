package com.gsoft.inventory.adapter.asset_borrow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetBorrow;

import java.util.List;

/**
 * @author 10904
 */
public class AssetBorrowAdapter extends RecyclerView.Adapter<AssetBorrowAdapter.ViewHolder> {

    private Context mContext;
    private List<AssetBorrow> mAssetReceiveList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(AssetBorrow item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public AssetBorrowAdapter(Context context, List<AssetBorrow> assetReceiveList) {
        mContext = context;
        mAssetReceiveList = assetReceiveList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_asset_borrow_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetBorrow item = mAssetReceiveList.get(position);
        holder.tv_DOCID.setText("编号：" + item.getDOCID());
        holder.tv_APPLICANT.setText("借用人: " + item.getNAME());
        holder.tv_USEDEPT.setText("借用部门: " + item.getUNITNAME());
        holder.tv_APPLY_EXPLAIN.setText("借用说明: " + item.getAPPLY_EXPLAIN());
        holder.tv_STATE.setText("审核状态: " + item.getSTATE());
        holder.tv_APPLICANTDATE.setText("借用日期: " + item.getAPPLICANTDATE());

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