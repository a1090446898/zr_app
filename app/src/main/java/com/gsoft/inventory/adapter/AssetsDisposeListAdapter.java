package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetDispose;

import java.util.List;

public class AssetsDisposeListAdapter extends BaseAdapter {

    private List<AssetDispose> disposeList;

    private LayoutInflater mInflator;
    private Context mContext;

    public AssetsDisposeListAdapter(List<AssetDispose> assetDisposes, Context context) {
        super();
        disposeList = assetDisposes;
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return disposeList.size();
    }

    @Override
    public Object getItem(int i) {
        return disposeList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // TODO Auto-generated method stub
        ViewContainer viewContainer;
        // General ListView optimization code.
        if (view == null) {
            synchronized (mContext) {
                view = mInflator.inflate(R.layout.list_assets_dispose_item, null);
                viewContainer = new ViewContainer();
                viewContainer.tv_assets_date = view.findViewById(R.id.tv_date);
                viewContainer.tv_assets_depart = view.findViewById(R.id.tv_depart);
                viewContainer.tv_assets_name = view.findViewById(R.id.tv_name);
                viewContainer.tv_assets_remark = view.findViewById(R.id.tv_remark);
                view.setTag(viewContainer);
            }

        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final AssetDispose dispose = disposeList.get(i);
        viewContainer.tv_assets_date.setText(dispose.getDate());
        viewContainer.tv_assets_depart.setText(dispose.getDepart());
        viewContainer.tv_assets_name.setText(dispose.getName());
        viewContainer.tv_assets_remark.setText(dispose.getRemark());

        return view;
    }

    public class ViewContainer {
        /*条码编号*/
        TextView tv_assets_name;
        /*资产类别*/
        TextView tv_assets_date;
        /*资产名称*/
        TextView tv_assets_depart;
        /*使用人*/
        TextView tv_assets_remark;
    }
}
