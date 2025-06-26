package com.gsoft.inventory.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.gsoft.inventory.R;

/**
 * Created by gsc on 2017/4/25.
 */

public class ImageLoaderHelper {

    private Context context;
    protected ImageLoaderHelper imageLoader = null;

    public ImageLoaderHelper getInstance(Context context) {
        if (imageLoader == null) {
            imageLoader = new ImageLoaderHelper(context);
        }
        return imageLoader;
    }

    public ImageLoaderHelper(Context _context) {
        // TODO Auto-generated constructor stub
        this.context = _context;
    }

    public ImageView getImageView(String imageUrl, boolean bSmallImage) {
        ImageView iv = new ImageView(context);

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.mipmap.logo)
                .error(R.mipmap.logo)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(iv);
        return iv;
    }

    public ImageView getImageView(String imageUrl) {
        return getImageView(imageUrl, true);
    }

    public void loadImageView(String imageUrl, ImageView iv) {
        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.mipmap.logo)
                .error(R.mipmap.logo)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(iv);
    }
}