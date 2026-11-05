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
import com.bumptech.glide.signature.ObjectKey;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseViewHolder;
import com.gsoft.inventory.utils.IGridItemDeleteListener;
import com.gsoft.inventory.utils.ImageLoaderHelper;
import com.gsoft.inventory.utils.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;

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
        TextView tvSource = convertView.findViewById(R.id.tv_photo_source);
        String fileUrl = this.ImageArray.get(position);
        if (!StringUtils.isNullOrEmpty(fileUrl)) {
            if (fileUrl.toLowerCase(Locale.ROOT).startsWith("http")) {
                Glide.with(mContext)
                        .load(fileUrl)
                        .fitCenter()
                        .placeholder(R.mipmap.ic_xct)
                        .error(R.mipmap.ic_xct)
                        .into(iv);
                tvSource.setText("网络");
                tvSource.setBackgroundResource(R.drawable.bg_photo_source_tag_net);
            } else {
                // 本地照片额外用“路径+修改时间+文件大小”作为缓存签名：
                // 同一路径被新照片覆盖时缓存key会变化，列表不会命中旧缩略图
                File photoFile = new File(fileUrl);
                Glide.with(mContext)
                        .load(photoFile)
                        .fitCenter()
                        .placeholder(R.mipmap.ic_xct)
                        .error(R.mipmap.ic_xct)
                        .signature(new ObjectKey(fileUrl + "#" + photoFile.lastModified() + "#" + photoFile.length()))
                        .into(iv);
                tvSource.setText("本地");
                tvSource.setBackgroundResource(R.drawable.bg_photo_source_tag);
            }
            // 来源标签：网格复用时必须每次重设文字、背景和显隐，避免上一项的标签残留
            tvSource.setVisibility(View.VISIBLE);
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
            tvSource.setVisibility(View.GONE);
        }
        return convertView;
    }
}
