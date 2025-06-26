package com.gsoft.inventory.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.AssetsViewActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.AssetsCategory;
import com.gsoft.inventory.utils.AssetsCategoryHelper;
import com.gsoft.inventory.utils.DictionaryHelper;
import com.gsoft.inventory.utils.FixedHScrollView;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;

public class AssetsRecodeListAdapter extends BaseAdapter {

    private List<Assets> assetsArrayList;

    private LayoutInflater mInflator;
    private Context mContext;

    public AssetsRecodeListAdapter(List<Assets> assetsList, Context context) {
        super();
        assetsArrayList = assetsList;
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return assetsArrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return assetsArrayList.get(i);
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
                view = mInflator.inflate(R.layout.assets_recode_item, null);
                viewContainer = new ViewContainer();
                viewContainer.tv_assets_barcode = view.findViewById(R.id.tv_assets_barcode);
                viewContainer.tv_assets_sortcode = view.findViewById(R.id.tv_assets_sortcode);
                viewContainer.tv_assets_name = view.findViewById(R.id.tv_assets_name);
                viewContainer.tv_assets_writer = view.findViewById(R.id.tv_assets_writer);
                viewContainer.tv_assets_useman = view.findViewById(R.id.tv_assets_useman);
                view.setTag(viewContainer);
            }

        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final Assets assets = assetsArrayList.get(i);
        //Accounts accounts = AccountHelper.getACCTSUITEID(assets.getACCTSUITEID());
        //viewContainer.tvACCTSUITEID.setText(accounts == null ? "" : accounts.getZTMC());
        viewContainer.tv_assets_barcode.setText(assets.getBARCODEID());
        AssetsCategory assetsCategory = AssetsCategoryHelper.getAssetsCategory(assets.getASSETSSORTCODE());
        viewContainer.tv_assets_sortcode.setText(assetsCategory == null ? "" : assetsCategory.getNAME());

        viewContainer.tv_assets_name.setText(assets.getASSETSNAME());
        viewContainer.tv_assets_writer.setText(AccountHelper.getUserName(assets.getASSETSWRITER()));
        viewContainer.tv_assets_useman.setText(assets.getASSETSUSER());

        return view;
    }

    public class ViewContainer {
        /*条码编号*/
        TextView tv_assets_barcode;
        /*资产类别*/
        TextView tv_assets_sortcode;
        /*资产名称*/
        TextView tv_assets_name;
        /*使用人*/
        TextView tv_assets_useman;
        /*录入人*/
        TextView tv_assets_writer;
    }
}
