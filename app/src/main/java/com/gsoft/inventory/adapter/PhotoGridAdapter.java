package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseViewHolder;
import com.gsoft.inventory.utils.IGridItemDeleteListener;
import com.gsoft.inventory.utils.ImageLoaderHelper;
import com.gsoft.inventory.utils.StringUtils;

import java.util.List;

/**
 * Created by gsc on 2017/4/25.
 */
public class PhotoGridAdapter extends BaseAdapter {
    private Context mContext;
    private ImageLoaderHelper imageHelper;
    private List<String> ImageArray;
    private IGridItemDeleteListener ImageDeleteListener;
    private String defImagePath = null;

    public void setImageDeleteListener(IGridItemDeleteListener listener) {
        this.ImageDeleteListener = listener;
    }

    public PhotoGridAdapter(Context mContext, List<String> arrStringList, String defImage) {
        super();
        this.mContext = mContext;
        this.ImageArray = arrStringList;
        imageHelper = new ImageLoaderHelper(mContext);
        this.defImagePath = defImage;
    }

    public PhotoGridAdapter(Context mContext, List<String> arrImagePath) {
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
                    R.layout.photo_item, parent, false);
        }
        /*TextView tv = BaseViewHolder.get(convertView, R.id.tv_item);
        tv.setVisibility(View.GONE);*/
        ImageView iv = BaseViewHolder.get(convertView, R.id.iv_item);
        ImageView iv_del = BaseViewHolder.get(convertView, R.id.iv_del);
        String fileUrl = this.ImageArray.get(position);
        if (!StringUtils.isNullOrEmpty(fileUrl)) {
            Glide.with(mContext)
                    .load(fileUrl)
                    .fitCenter()
                    .placeholder(R.mipmap.ic_xct)
                    .error(R.mipmap.ic_xct)
                    //.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    //.skipMemoryCache(true)
                    .into(iv);
            /*final String imgPath = defImagePath == null ? this.ImageArray.get(position) : defImagePath;
            imageHelper.loadImageView(imgPath.startsWith("file://") ? imgPath : ("file://" + imgPath), iv);*/
            iv_del.setVisibility(View.VISIBLE);
            iv_del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ImageDeleteListener != null) {
                        ImageDeleteListener.onGridItemDelete(fileUrl);
                    }
                }
            });
        } else {
            iv_del.setVisibility(View.GONE);
        }
        return convertView;
    }
}
