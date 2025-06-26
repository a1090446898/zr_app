package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.AssetsViewActivity;
import com.gsoft.inventory.NewAssetsActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.pojo.JumPagePojo;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Accounts;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.AssetsCategoryHelper;
import com.gsoft.inventory.utils.BluetoothDeviceDetail;
import com.gsoft.inventory.utils.DictionaryHelper;
import com.gsoft.inventory.utils.FixedHScrollView;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;

public class AssetsListAdapter extends BaseAdapter {

    private ArrayList<Assets> assetsArrayList;
    public List<ViewContainer> mHolderList = new ArrayList<ViewContainer>();
    private LayoutInflater mInflator;
    private Context mContext;

    private JumPagePojo jumPagePojo;
    ViewGroup mHead;

    public AssetsListAdapter(ArrayList<Assets> assetsList, Context context, ViewGroup mHead, JumPagePojo jumPagePojo) {
        super();
        assetsArrayList = assetsList;
        mContext = context;
        mInflator = LayoutInflater.from(context);
        this.mHead = mHead;
        this.jumPagePojo = jumPagePojo;
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
        ViewContainer viewContainer;
        if (view == null) {
            synchronized (mContext) {
                view = mInflator.inflate(R.layout.assets_list_item, null);
                viewContainer = new ViewContainer();
                FixedHScrollView scrollView = (FixedHScrollView) view.findViewById(R.id.scrollView);
                viewContainer.scrollView = scrollView;
                //viewContainer.tvACCTSUITEID = view.findViewById(R.id.tvACCTSUITEID);
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
                //viewContainer.tvCUSTODIAN = view.findViewById(R.id.tvCUSTODIAN);
                //viewContainer.tvGETMODE = view.findViewById(R.id.tvGETMODE);
                //viewContainer.tvGROUNDMANAGEDEPT = view.findViewById(R.id.tvGROUNDMANAGEDEPT);
                viewContainer.tvPROPERTY = view.findViewById(R.id.tvPROPERTY);
                viewContainer.tvREMARKS = view.findViewById(R.id.tvREMARKS);
                viewContainer.tvPDZT = view.findViewById(R.id.tvPDZT);
                viewContainer.tvSERIALCODE = view.findViewById(R.id.tvSERIALCODE);
                viewContainer.tvUSESTATE = view.findViewById(R.id.tvUSESTATE);

                FixedHScrollView headSrcrollView = (FixedHScrollView) mHead.findViewById(R.id.scrollView);
                headSrcrollView.AddOnScrollChangedListener(new OnScrollChangedListenerImp(scrollView));

                view.setTag(viewContainer);
                mHolderList.add(viewContainer);
            }

        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final Assets assets = assetsArrayList.get(position);
        //Accounts accounts = AccountHelper.getACCTSUITEID(assets.getACCTSUITEID());
        //viewContainer.tvACCTSUITEID.setText(accounts == null ? "" : accounts.getZTMC());
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
        //viewContainer.tvCUSTODIAN.setText(assets.getCUSTODIAN());
        //viewContainer.tvGETMODE.setText(DictionaryHelper.getDictionaryName(assets.getGETMODE(), R.array.qdfs, mContext));
        //viewContainer.tvGROUNDMANAGEDEPT.setText(AccountHelper.getDanweiName(assets.getGROUNDMANAGEDEPT()));
        viewContainer.tvPROPERTY.setText(assets.getPROPERTY());
        viewContainer.tvREMARKS.setText(assets.getREMARKS());
        String pdztStr = handlePdzt(assets.getPDZT());
        viewContainer.tvPDZT.setText(pdztStr);
        viewContainer.tvSERIALCODE.setText(assets.getSERIALCODE());

        viewContainer.tvUSESTATE.setText(DictionaryHelper.getDictionaryName(assets.getUSESTATE(), R.array.syzk, mContext));

        viewContainer.tvASSETSNAME.setOnClickListener(view1 -> {
            Long assetsId = assets.getId();
            if(jumPagePojo != null){
                if(jumPagePojo.getIsJumpPage()){
                    // 如果是真的就把ID传给父页面NewAssetsActivity，因为是从父页面跳过来，所以要回传ID
                    Intent resultIntent = new Intent(mContext, NewAssetsActivity.class);
                    resultIntent.putExtra(SysDefine.IntentExtra_AssetsID, assetsId.toString());
                    ((Activity) mContext).setResult(Activity.RESULT_OK, resultIntent);
                    ((Activity) mContext).finish();
                    return;
                }
            }
            Intent intentAssetsView = new Intent(mContext, AssetsViewActivity.class);
            intentAssetsView.putExtra(SysDefine.IntentExtra_AssetsID, assetsId);
            mContext.startActivity(intentAssetsView);
        });

        return view;
    }


    private String handlePdzt(String pdzt){

        String result = "";
        if("01".equals(pdzt)){
            result = "待盘点";
        }
        else if("02".equals(pdzt)){
            result = "盘实";
        }
        else if("03".equals(pdzt)){
            result = "盘盈";
        }
        else if("04".equals(pdzt)){
            result = "盘亏";
        }
        return result;
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

        /*盘点状态*/
        TextView tvPDZT;
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
