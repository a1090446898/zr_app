package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.FileBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileListAdapter extends BaseAdapter {
    private List<FileBean> fileBeans;

    private LayoutInflater mInflator;
    private Context mContext;

    public FileListAdapter(List<FileBean> fileList, Context context) {
        super();
        fileBeans = fileList;
        mContext = context;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return fileBeans.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return fileBeans.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewContainer viewContainer;
        // General ListView optimization code.
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.file_item, null);
            viewContainer = new ViewContainer();
            viewContainer.tvDate = view.findViewById(R.id.tv_file_date);
            viewContainer.tvName = view.findViewById(R.id.tv_file_name);
            //viewContainer.tvSize = view.findViewById(R.id.tv_file_size);
            view.setTag(viewContainer);
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        FileBean fileBean = fileBeans.get(position);
        viewContainer.tvDate.setText(fileBean.getDateStr());
        viewContainer.tvName.setText(fileBean.getName());
        //viewContainer.tvSize.setText(fileBean.getSize() + "");
        return view;
    }

    //cell
    public class ViewContainer {
        /*条码编号*/
        TextView tvName;
        /*CardID*/
        TextView tvDate;
        /*取得方式*/
        //TextView tvSize;
    }

}
