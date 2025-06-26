package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetPhoto;

import java.util.List;

public class WaitingUploadPhotoListAdapter extends BaseAdapter {

    private List<AssetPhoto> recordList;

    private LayoutInflater mInflator;
    private Context mContext;

    public WaitingUploadPhotoListAdapter(List<AssetPhoto> records, Context context) {
        super();
        recordList = records;
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return recordList.size();
    }

    @Override
    public Object getItem(int i) {
        return recordList.get(i);
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
                view = mInflator.inflate(R.layout.list_waiting_photo_item, null);
                viewContainer = new ViewContainer();
                viewContainer.tv_bar = view.findViewById(R.id.tv_bar);
                viewContainer.tv_status = view.findViewById(R.id.tv_status);
                viewContainer.tv_name = view.findViewById(R.id.tv_name);
                view.setTag(viewContainer);
            }
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final AssetPhoto dispose = recordList.get(i);
        viewContainer.tv_bar.setText(dispose.getCode());
        viewContainer.tv_status.setText(dispose.getStatus());
        viewContainer.tv_name.setText(dispose.getName());


        return view;
    }

    public class ViewContainer {
        /*条码编号*/
        TextView tv_name;
        /*资产类别*/
        TextView tv_bar;
        /*资产名称*/
        TextView tv_status;
    }
}
