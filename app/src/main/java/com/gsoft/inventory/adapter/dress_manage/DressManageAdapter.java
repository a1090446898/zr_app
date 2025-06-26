package com.gsoft.inventory.adapter.dress_manage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.DressManage;

import java.util.List;

/**
 * @author 10904
 */
public class DressManageAdapter extends RecyclerView.Adapter<DressManageAdapter.ViewHolder> {

    private Context mContext;
    private List<DressManage> list;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(DressManage item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public DressManageAdapter(Context context, List<DressManage> list) {
        this.mContext = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_dress_manage_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DressManage item = list.get(position);
        holder.tv_docId.setText("编号：" + item.getDocId());
        holder.tv_receiveDepart.setText("领用部门: " + item.getReceiveDepart());
        holder.tv_receiveUser.setText("领用人: " + item.getReceiveUser());
        holder.tv_receiveTime.setText("领用时间: " + item.getReceiveTime());
        holder.tv_receivePlace.setText("领用地点: " + item.getReceivePlace());
        holder.tv_giveUser.setText("发放人: " + item.getGiveUser());
        holder.tv_giveTime.setText("发放时间: " + item.getGiveTime());

        if (mListener != null) {
            holder.itemView.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_docId;
        TextView tv_receiveDepart;
        TextView tv_receiveUser;
        TextView tv_receiveTime;
        TextView tv_receivePlace;
        TextView tv_giveUser;
        TextView tv_giveTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_docId = itemView.findViewById(R.id.tv_docId);
            tv_receiveDepart = itemView.findViewById(R.id.tv_receiveDepart);
            tv_receiveUser = itemView.findViewById(R.id.tv_receiveUser);
            tv_receiveTime = itemView.findViewById(R.id.tv_receiveTime);
            tv_receivePlace = itemView.findViewById(R.id.tv_receivePlace);
            tv_giveUser = itemView.findViewById(R.id.tv_giveUser);
            tv_giveTime = itemView.findViewById(R.id.tv_giveTime);
        }
    }


}