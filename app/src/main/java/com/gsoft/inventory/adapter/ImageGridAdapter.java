package com.gsoft.inventory.adapter;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseViewHolder;
import com.gsoft.inventory.utils.IGridItemDeleteListener;
import com.gsoft.inventory.utils.ImageLoaderHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;

import java.util.List;

/**
 * Created by gsc on 2017/4/25.
 */
public class ImageGridAdapter extends BaseAdapter {
    private Context mContext;
    private ImageLoaderHelper imageHelper;
    private List<String> ImageArray;
    private IGridItemDeleteListener ImageDeleteListener;
    private String defImagePath = null;

    public void setImageDeleteListener(IGridItemDeleteListener listener) {
        this.ImageDeleteListener = listener;
    }

    public ImageGridAdapter(Context mContext, List<String> arrStringList, String defImage) {
        super();
        this.mContext = mContext;
        this.ImageArray = arrStringList;
        imageHelper = new ImageLoaderHelper(mContext);
        this.defImagePath = defImage;
    }

    public ImageGridAdapter(Context mContext, List<String> arrImagePath) {
        super();
        this.mContext = mContext;
        this.ImageArray = arrImagePath;
        imageHelper = new ImageLoaderHelper(mContext);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return ImageArray.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return this.ImageArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.grid_item, parent, false);
        }
        TextView tv = BaseViewHolder.get(convertView, R.id.tv_item);
        tv.setVisibility(View.GONE);
        ImageView iv = BaseViewHolder.get(convertView, R.id.iv_item);
        if (!StringUtils.isNullOrEmpty(this.ImageArray.get(position))) {
            final String imgPath = defImagePath == null ? this.ImageArray.get(position) : defImagePath;
            imageHelper.loadImageView(imgPath, iv);
        }
        /*ImageView iv_del = BaseViewHolder.get(convertView, R.id.iv_del);
        iv_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ImageDeleteListener != null) {
                    ImageDeleteListener.onGridItemDelete(imgPath);
                }
            }
        });*/
        return convertView;
    }
}
