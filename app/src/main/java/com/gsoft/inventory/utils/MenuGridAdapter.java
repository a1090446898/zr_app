package com.gsoft.inventory.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.gsoft.inventory.R;
import com.gsoft.inventory.data.GridMenuItem;

import java.util.List;

/**
 * @author http://blog.csdn.net/finddreams
 * @Description:gridview的Adapter
 */
public class MenuGridAdapter extends BaseAdapter {
    private Context mContext;

    private List<GridMenuItem> MenuArray;

    public MenuGridAdapter(Context mContext, List<GridMenuItem> arrmenu) {
        super();
        this.mContext = mContext;
        this.MenuArray = arrmenu;
    }

    @Override
    public int getCount() {
        return MenuArray.size();
    }

    @Override
    public Object getItem(int position) {
        return this.MenuArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.grid_menu_item, parent, false);
        }
        TextView tv = BaseViewHolder.get(convertView, R.id.tv_item);
        ImageView iv = BaseViewHolder.get(convertView, R.id.iv_item);
        int resDrawbleID = this.MenuArray.get(position).getIconRes();
        if (resDrawbleID == -1) {
            iv.setVisibility(View.INVISIBLE);
        } else {
            iv.setVisibility(View.VISIBLE);
            iv.setBackgroundResource(resDrawbleID);
        }
        tv.setText(this.MenuArray.get(position).getTitle());
        return convertView;
    }

}
