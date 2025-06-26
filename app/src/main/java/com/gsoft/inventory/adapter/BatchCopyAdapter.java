package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.gsoft.inventory.AssetsViewActivity;
import com.gsoft.inventory.NewAssetsActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.pojo.JumPagePojo;
import com.gsoft.inventory.utils.*;

import java.util.ArrayList;
import java.util.List;


/**
 * @author 10904
 */
public class BatchCopyAdapter extends BaseAdapter {
    private List<Assets> assetsArrayList;
    public List<BatchCopyAdapter.ViewContainer> mHolderList = new ArrayList<BatchCopyAdapter.ViewContainer>();
    private LayoutInflater mInflator;
    private Context mContext;

    private JumPagePojo jumPagePojo;


    public BatchCopyAdapter(Context context,  List<Assets> assetsList) {
        super();
        this.assetsArrayList = assetsList != null ? assetsList : new ArrayList<>();
        this.mContext = context;
        this.mInflator = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return assetsArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return assetsArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        BatchCopyAdapter.ViewContainer viewContainer;

        if (view == null) {
            view = mInflator.inflate(R.layout.assets_list_item_new, null);
            viewContainer = new BatchCopyAdapter.ViewContainer();
            viewContainer.tvASSETSDEPT = view.findViewById(R.id.tvASSETSDEPT);
            viewContainer.tvASSETSMODEL = view.findViewById(R.id.tvASSETSMODEL);
            viewContainer.tvASSETSLAYADD = view.findViewById(R.id.tvASSETSLAYADD);
            viewContainer.tvASSETSMEASURE = view.findViewById(R.id.tvASSETSMEASURE);
            viewContainer.tvASSETSNAME = view.findViewById(R.id.tvASSETSNAME);
            viewContainer.tvASSETSSTANDARD = view.findViewById(R.id.tvASSETSSTANDARD);
            viewContainer.tvASSETSTYPE = view.findViewById(R.id.tvASSETSTYPE);
            viewContainer.tvASSETSUSER = view.findViewById(R.id.tvASSETSUSER);
            viewContainer.tvASSETSWRITER = view.findViewById(R.id.tvASSETSWRITER);
            viewContainer.tvASSETSWRITERDATE = view.findViewById(R.id.tvASSETSWRITERDATE);
            viewContainer.tvASSETSNUM = view.findViewById(R.id.tvASSETSNUM);
            viewContainer.tvBARCODEID = view.findViewById(R.id.tvBARCODEID);

            viewContainer.tvPROPERTY = view.findViewById(R.id.tvPROPERTY);
            viewContainer.tvREMARKS = view.findViewById(R.id.tvREMARKS);
            viewContainer.tvSERIALCODE = view.findViewById(R.id.tvSERIALCODE);
            viewContainer.tvUSESTATE = view.findViewById(R.id.tvUSESTATE);

            view.setTag(viewContainer);
        } else {
            viewContainer = (BatchCopyAdapter.ViewContainer) view.getTag();
        }

        final Assets assets = assetsArrayList.get(position);

        viewContainer.tvASSETSDEPT.setText(assets.getASSETSDEPT());
        viewContainer.tvASSETSMODEL.setText(assets.getASSETSMODEL());
        viewContainer.tvASSETSLAYADD.setText(assets.getASSETSLAYADD());
        viewContainer.tvASSETSMEASURE.setText(assets.getASSETSMEASURE());
        viewContainer.tvASSETSNAME.setText(assets.getASSETSNAME());
        viewContainer.tvASSETSSTANDARD.setText(assets.getASSETSSTANDARD());
        viewContainer.tvASSETSTYPE.setText(AssetsCategoryHelper.getAssetsCategoryTitle(assets.getASSETSTYPE()));
        viewContainer.tvASSETSUSER.setText(assets.getASSETSUSER());
        viewContainer.tvASSETSWRITER.setText(AccountHelper.getUserName(assets.getASSETSWRITER()));
        viewContainer.tvASSETSWRITERDATE.setText(assets.getASSETSWRITERDATE());
        viewContainer.tvASSETSNUM.setText(assets.getASSETSNUM() + "");
        viewContainer.tvBARCODEID.setText(assets.getBARCODEID());

        viewContainer.tvPROPERTY.setText(assets.getPROPERTY());
        viewContainer.tvREMARKS.setText(assets.getREMARKS());
        viewContainer.tvSERIALCODE.setText(assets.getSERIALCODE());

        viewContainer.tvUSESTATE.setText(DictionaryHelper.getDictionaryName(assets.getUSESTATE(), R.array.syzk, mContext));


        return view;
    }

    //cell
    public class ViewContainer {
        /*条码编号*/
        TextView tvBARCODEID;

        /*规格*/
        TextView tvASSETSMODEL;
        /*CardID*/
        TextView tvCARDID;
        /*取得方式*/
        TextView tvGETMODE;
        /*备注*/
        TextView tvREMARKS;
        /*产权单位*/
        TextView tvPROPERTY;
        /*使用(保管)人*/
        TextView tvCUSTODIAN;
        /*账套编号*/
        TextView tvACCTSUITEID;
        /*卡片序号*/
        TextView tvSERIALNUM;
        /*资产类别*/
        TextView tvASSETSTYPE;
        /*计量单位*/
        TextView tvASSETSMEASURE;
        /*卡片编号*/
        TextView tvSERIALCODE;
        /*资产名称*/
        TextView tvASSETSNAME;
        /*规格型号*/
        TextView tvASSETSSTANDARD;
        /*使用状况*/
        TextView tvUSESTATE;
        /*存放地点*/
        TextView tvASSETSLAYADD;
        /*使用人*/
        TextView tvASSETSUSER;
        /*使用部门*/
        TextView tvASSETSDEPT;
        /*管理部门*/
        TextView tvGROUNDMANAGEDEPT;
        /*分类用途*/
        TextView tvVEHPURPOSE;
        /*编制情况*/
        TextView tvASSETSORGANIZATION;
        /*数量*/
        TextView tvASSETSNUM;
        /*录入人*/
        TextView tvASSETSWRITER;
        /*录入日期*/
        TextView tvASSETSWRITERDATE;
        /**
         * 滚动条
         */
        FixedHScrollView scrollView;
    }


}
