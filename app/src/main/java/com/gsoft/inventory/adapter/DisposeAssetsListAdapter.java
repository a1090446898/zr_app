package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsoft.inventory.AssetsViewActivity;
import com.gsoft.inventory.DisposeAssetsActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.EventPosition;
import com.gsoft.inventory.utils.FixedHScrollView;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;

public class DisposeAssetsListAdapter extends BaseAdapter {

    private ArrayList<DisposeAsset> assetsArrayList;
    public List<ViewContainer> mHolderList = new ArrayList<ViewContainer>();
    private LayoutInflater mInflator;
    private DisposeAssetsActivity mContext;
    ViewGroup mHead;

    public DisposeAssetsListAdapter(ArrayList<DisposeAsset> assetsList, DisposeAssetsActivity context, ViewGroup mHead) {
        super();
        assetsArrayList = assetsList;
        mContext = context;
        mInflator = LayoutInflater.from(context);
        this.mHead = mHead;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return assetsArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return assetsArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewContainer viewContainer;
        // General ListView optimization code.
        if (view == null) {
            synchronized (mContext) {
                view = mInflator.inflate(R.layout.dispose_assets_list_item, null);
                viewContainer = new ViewContainer();
                FixedHScrollView scrollView = (FixedHScrollView) view.findViewById(R.id.scrollView);
                viewContainer.scrollView = scrollView;
                //viewContainer.tvACCTSUITEID = view.findViewById(R.id.tvACCTSUITEID);
                viewContainer.tvASSETSLAYADD = view.findViewById(R.id.tvASSETSLAYADD);
                viewContainer.tvASSETSNAME = view.findViewById(R.id.tvASSETSNAME);
                viewContainer.tvASSETSSTANDARD = view.findViewById(R.id.tvASSETSSTANDARD);
                viewContainer.tvASSETSUSER = view.findViewById(R.id.tvASSETSUSER);
                viewContainer.tvBARCODEID = view.findViewById(R.id.tvBARCODEID);
                viewContainer.tvVALUE = view.findViewById(R.id.tvVALUE);
                viewContainer.chkASSET = view.findViewById(R.id.chkASSET);
                FixedHScrollView headSrcrollView = (FixedHScrollView) mHead.findViewById(R.id.scrollView);
                headSrcrollView.AddOnScrollChangedListener(new OnScrollChangedListenerImp(scrollView));

                view.setTag(viewContainer);
                mHolderList.add(viewContainer);
            }

        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final DisposeAsset asset = assetsArrayList.get(position);
        viewContainer.tvASSETSLAYADD.setText(asset.getPlace());
        viewContainer.tvASSETSNAME.setText(asset.getName());
        viewContainer.tvASSETSSTANDARD.setText(asset.getSpecification());
        viewContainer.tvASSETSUSER.setText(asset.getOwner());
        viewContainer.tvBARCODEID.setText(asset.getSerial());
        viewContainer.tvVALUE.setText(asset.getPrice());
        viewContainer.chkASSET.setChecked(false);
        viewContainer.chkASSET.setOnCheckedChangeListener((buttonView, isChecked) -> asset.setChecked(isChecked));
        /*viewContainer.tvASSETSNAME.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentAssetsView = new Intent(mContext, AssetsViewActivity.class);
                intentAssetsView.putExtra(SysDefine.IntentExtra_AssetsID, asset.getCode());
                mContext.startActivity(intentAssetsView);
            }
        });*/

        /*viewContainer.tvASSETSNAME.setOnLongClickListener(v -> {
            mContext.removeAsset(position);
            return false;
        });*/
        /*viewContainer.tvASSETSNAME.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //EventPosition eventPosition = new EventPosition((int) event.getX(), (int) event.getY())
                    v.setTag((int) event.getX());
                    Log.v("OnTouchListener", "Down");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    int xUp = (int) event.getX();
                    int xDown = (int) v.getTag();
                    if ((xUp - xDown) > 20) {
                        Log.e("PULL", "PULL-RIGHT");
                    } else if ((xUp - xDown) < -20) {
                        Log.e("PULL", "PULL-LEFT");
                    }
                }
                return false;
            }
        });*/
        return view;
    }

    //cell
    public class ViewContainer {
        /*条码编号*/
        TextView tvBARCODEID;
        /*资产名称*/
        TextView tvASSETSNAME;
        /*规格型号*/
        TextView tvASSETSSTANDARD;
        /*存放地点*/
        TextView tvASSETSLAYADD;
        /*使用人*/
        TextView tvASSETSUSER;
        /*资产原值*/
        TextView tvVALUE;
        CheckBox chkASSET;
        /**
         * 滚动条
         */
        FixedHScrollView scrollView;
    }

    class OnScrollChangedListenerImp implements FixedHScrollView.OnScrollChangedListener {
        FixedHScrollView mScrollViewArg;

        public OnScrollChangedListenerImp(FixedHScrollView scrollViewar) {
            mScrollViewArg = scrollViewar;
        }

        @Override
        public void onScrollChanged(int l, int t, int oldl, int oldt) {
            mScrollViewArg.smoothScrollTo(l, t);
        }
    }
}
