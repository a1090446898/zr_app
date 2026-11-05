package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.AssetsCategoryHelper;
import com.gsoft.inventory.utils.DictionaryHelper;
import com.gsoft.inventory.utils.FixedHScrollView;
import com.gsoft.inventory.utils.SysDefine;
import com.gsoft.inventory.utils.print_batch.BatchPrintConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 资产列表适配器（查询页/资产选择页共用）
 * <p>
 * 勾选模式：用于批量打印，选中态按资产Id记录（SugarORM实体无SELECTED字段，且列表会分页/复用，
 * 不能用position），默认关闭，不影响原调用页。
 *
 * @author 10904
 */
public class AssetsListAdapter extends BaseAdapter {

    /** 勾选变化回调（批量打印模式） */
    public interface OnSelectionChangedListener {
        /** 选中条数变化（用于更新底部统计与表头全选态） */
        void onSelectionChanged(int selectedCount);

        /** 勾选数达到上限（用于提示分批打印） */
        void onSelectLimitReached(int maxCount);
    }

    private ArrayList<Assets> assetsArrayList;
    public List<ViewContainer> mHolderList = new ArrayList<ViewContainer>();
    private LayoutInflater mInflator;
    private Context mContext;

    private JumPagePojo jumPagePojo;
    ViewGroup mHead;

    /** 勾选模式开关（批量打印） */
    private boolean selectMode;
    /** 已勾选资产Id（稳定key，跨分页/复用不丢） */
    private final Set<Long> selectedIds = new HashSet<>();
    /** 单次最多勾选数 */
    private int maxSelectCount = BatchPrintConfig.MAX_BATCH_SIZE;
    private OnSelectionChangedListener selectionListener;

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
                viewContainer.cbSelect = view.findViewById(R.id.allCheck);
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

        bindSelection(viewContainer.cbSelect, assets);

        final CheckBox cbSelect = viewContainer.cbSelect;
        viewContainer.tvASSETSNAME.setOnClickListener(view1 -> {
            // 勾选模式下点名称即切换勾选，不跳转详情
            if (selectMode) {
                cbSelect.toggle();
                return;
            }
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

    // ==================== 批量打印勾选 ====================

    /** 进入/退出勾选模式：退出时清空已选，避免脏状态带到下次 */
    public void setSelectMode(boolean selectMode) {
        if (this.selectMode == selectMode) {
            return;
        }
        this.selectMode = selectMode;
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void setMaxSelectCount(int maxSelectCount) {
        this.maxSelectCount = maxSelectCount;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    /** 当前已加载列表是否全部勾选（用于表头全选框状态） */
    public boolean isAllSelected() {
        if (assetsArrayList.isEmpty()) {
            return false;
        }
        for (Assets assets : assetsArrayList) {
            if (assets.getId() == null || !selectedIds.contains(assets.getId())) {
                return false;
            }
        }
        return true;
    }

    /** 全选当前已加载列表：超出上限只选前N条并提示，避免用户误以为已全选 */
    public void selectAll() {
        boolean limitReached = false;
        for (Assets assets : assetsArrayList) {
            Long id = assets.getId();
            if (id == null || selectedIds.contains(id)) {
                continue;
            }
            if (selectedIds.size() >= maxSelectCount) {
                limitReached = true;
                break;
            }
            selectedIds.add(id);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
        if (limitReached && selectionListener != null) {
            selectionListener.onSelectLimitReached(maxSelectCount);
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /** 已勾选资产（按列表顺序，仅返回当前列表内存在的数据） */
    public List<Assets> getSelectedAssets() {
        List<Assets> result = new ArrayList<>();
        for (Assets assets : assetsArrayList) {
            if (assets.getId() != null && selectedIds.contains(assets.getId())) {
                result.add(assets);
            }
        }
        return result;
    }

    /** 绑定单项勾选态：先解监听再setChecked，避免复用触发出错误的选中事件 */
    private void bindSelection(CheckBox checkBox, final Assets assets) {
        if (checkBox == null) {
            return;
        }
        checkBox.setOnCheckedChangeListener(null);
        if (!selectMode) {
            checkBox.setChecked(false);
            checkBox.setVisibility(android.view.View.GONE);
            return;
        }
        checkBox.setVisibility(android.view.View.VISIBLE);
        checkBox.setChecked(assets.getId() != null && selectedIds.contains(assets.getId()));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Long id = assets.getId();
            if (id == null) {
                buttonView.setChecked(false);
                return;
            }
            if (isChecked) {
                if (selectedIds.size() >= maxSelectCount) {
                    buttonView.setChecked(false);
                    if (selectionListener != null) {
                        selectionListener.onSelectLimitReached(maxSelectCount);
                    }
                    return;
                }
                selectedIds.add(id);
            } else {
                selectedIds.remove(id);
            }
            notifySelectionChanged();
        });
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    //cell
    public class ViewContainer {
        /*批量打印勾选框*/
        CheckBox cbSelect;

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
        /*资产编号*/
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
