package com.gsoft.inventory;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.drawerlayout.widget.DrawerLayout;
import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.adapter.AssetsListAdapter;
import com.gsoft.inventory.pojo.JumPagePojo;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 */
public class AssetsQueryActivity extends BaseCompatActivity {

    RelativeLayout mHead;
    ArrayList<Assets> assetsList;
    AssetsListAdapter assetsAdapter;
    ListView listAssets;

    String[] dwArray = null;

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.querybar)
    QMUITopBar queryBar;
    @BindView(R.id.lyQuery)
    LinearLayout lyQuery;
    @BindView(R.id.lyDrawLayout)
    DrawerLayout lyDrawLayout;
    @BindView(R.id.search_close)
    QMUIButton search_close;
    @BindView(R.id.search_button)
    QMUIButton search_button;
    @BindView(R.id.editASSETSNAME)
    EditText editASSETSNAME;

    // 规格
    @BindView(R.id.editASSETSSTANDARD)
    EditText editASSETSSTANDARD;

    // 型号
    @BindView(R.id.editASSETSMODEL)
    EditText editASSETSMODEL;

    @BindView(R.id.editCUSTODIAN)
    EditText editCUSTODIAN;
    @BindView(R.id.editACCTSUITEID)
    EditText editACCTSUITEID;
    @BindView(R.id.editASSETSORGANIZATION)
    EditText editASSETSORGANIZATION;
    @BindView(R.id.editASSETSTYPE)
    EditText editASSETSTYPE;
    @BindView(R.id.editASSETSDEPT)
    EditText editASSETSDEPT;
    @BindView(R.id.editGROUNDMANAGEDEPT)
    EditText editGROUNDMANAGEDEPT;

    private String groundManageDeptCode;
    @BindView(R.id.editASSETSLAYADD)
    EditText editASSETSLAYADD;
    @BindView(R.id.editASSETSUSER)
    EditText editASSETSUSER;
    @BindView(R.id.editBARCODEID)
    EditText editBARCODEID;
    @BindView(R.id.editREMARKS)
    EditText editREMARKS;

    @BindView(R.id.editASSETSCURRPRICE)
    EditText editASSETSCURRPRICE;
    @BindDrawable(R.drawable.icon_right)
    Drawable icon_right;
    @BindView(R.id.tv_assets_msg)
    TextView tv_assets_msg;
    @BindView(R.id.search_reset)
    QMUIButton button_search_reset;
    @BindView(R.id.switcher)
    Switch switcher;
    String[] ztArray = null;
    /*资产类别*/
    @BindArray(R.array.zclb)
    String[] zclbArray;

    /*资产类别新*/
    @BindArray(R.array.zclbNew)
    String[] zclbNewArray;
    /*编制情况*/
    @BindArray(R.array.bzqk)
    String[] bzqkArray;

    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;


    private String searchPDZT = "";

    /**
     * 用于资产条目点击事件判断处理
     */

    private JumPagePojo jumPagePojo = new JumPagePojo();


    int pageSize = 15;
    boolean hasMoreData = false;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    long maxAssetsID = 0;
    boolean isFirstLoad = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_query);
        ButterKnife.bind(this);
        initializeView();
        receiveNewQueries();
        // 显式初始化
        maxAssetsID = 0;

    }

    private void receiveNewQueries(){
        // 获取传递的数据
        // 资产名称
        String assetsName = getIntent().getStringExtra("ASSETS_NAME");
        // 规格
        String assetsModel = getIntent().getStringExtra("ASSETS_MODEL");
        // 型号
        String assetsStandard = getIntent().getStringExtra("ASSETS_STANDARD");
        // 类别
        String assetsType = getIntent().getStringExtra("ASSETS_TYPE");
        // 管理人
        String assetManager = getIntent().getStringExtra("ASSET_MANAGER");
        // 使用人
        String assetsUser = getIntent().getStringExtra("ASSETS_USER");
        // 管理部门
        String groundManageDept = getIntent().getStringExtra("GROUND_MANAGE_DEPT");
        String deptCode = getIntent().getStringExtra("GROUND_MANAGE_DEPT_CODE");
        // 使用部门
        String assetsDept = getIntent().getStringExtra("ASSETS_DEPT");
        // 存放地点
        String assetsLayAdd = getIntent().getStringExtra("ASSETS_LAYADD");
        // 增加一个资产原值
        String assetsCurPrice = getIntent().getStringExtra("ASSETS_CURRPRICE");

//        searchPDZT = getIntent().getStringExtra("PDZT");

        boolean isJumpPage = getIntent().getBooleanExtra("isJumpPage", false);
        if(isJumpPage){
            searchPDZT = "01";
        }

        if(!TextUtils.isEmpty(assetsName)){
            editASSETSNAME.setText(assetsName);
        }
        if(!TextUtils.isEmpty(assetsModel)){
            editASSETSMODEL.setText(assetsModel);
        }
        if(!TextUtils.isEmpty(assetsStandard)){
            editASSETSSTANDARD.setText(assetsStandard);
        }
        if(!TextUtils.isEmpty(assetsType)){
            editASSETSTYPE.setText(assetsType);
        }

        /*
        if(!TextUtils.isEmpty(assetManager)){
            editCUSTODIAN.setText(assetManager);
        }

        if(!TextUtils.isEmpty(assetsUser)){
            editASSETSUSER.setText(assetsUser);
        }

        if(!TextUtils.isEmpty(assetsLayAdd)){
            editASSETSLAYADD.setText(assetsLayAdd);
        }
        if(!TextUtils.isEmpty(assetsCurPrice)){
            editASSETSCURRPRICE.setText(assetsCurPrice);
        }

        if(!TextUtils.isEmpty(assetsDept)){
            editASSETSDEPT.setText(assetsDept);
        }
        if(!TextUtils.isEmpty(deptCode)){
            groundManageDeptCode = deptCode;
        }
        if(!TextUtils.isEmpty(groundManageDept)){
            editGROUNDMANAGEDEPT.setText(groundManageDept);
        }*/

        jumPagePojo.setIsJumpPage(isJumpPage);

        // 执行搜索逻辑
        new SearchAssetsTask().execute();

    }

    @Override
    public void initializeView() {
        Accounts loginAccount = application.getLoginAccount();
        topBar.setTitle("查看资产信息");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        topBar.addRightTextButton("搜索", R.id.topbar_right_query_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lyDrawLayout.openDrawer(lyQuery);
            }
        });
        search_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lyDrawLayout.closeDrawer(lyQuery);
            }
        });

        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFirstLoad = false;
                lyDrawLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        //refreshLayout.setEnableAutoLoadMore(true);
                        assetsList.clear();
                        maxAssetsID = 0;
                        lyDrawLayout.closeDrawer(lyQuery);
                        new SearchAssetsTask().execute();
                    }
                });
            }
        });
        button_search_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editASSETSTYPE.setText("");
                editACCTSUITEID.setText("");
                editASSETSDEPT.setText("");
                editASSETSLAYADD.setText("");
                editASSETSNAME.setText("");
                editASSETSSTANDARD.setText("");
                editASSETSMODEL.setText("");
                editASSETSUSER.setText("");
                editCUSTODIAN.setText("");
                editGROUNDMANAGEDEPT.setText("");
                editASSETSORGANIZATION.setText("");
                editREMARKS.setText("");
                groundManageDeptCode = "";
            }
        });
        ztArray = new String[SysConfig.getZTList().size()];
        SysConfig.getZTList().toArray(ztArray);

        queryBar.setTitle("查询条件");
        //必须设置图片的大小否则没有作用
        icon_right.setBounds(0, 0, 60, 60);
        editACCTSUITEID.setCompoundDrawables(null, null, icon_right, null);
        editASSETSTYPE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSORGANIZATION.setCompoundDrawables(null, null, icon_right, null);
        editGROUNDMANAGEDEPT.setCompoundDrawables(null, null, icon_right, null);

        // 获取部门名称
        List<String> danweiList = AccountHelper.getDanweiList(loginAccount.getZTBH());
        dwArray = new String[danweiList.size()];
        danweiList.toArray(dwArray);
        editGROUNDMANAGEDEPT.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(dwArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editGROUNDMANAGEDEPT.setText(dwArray[which]);
                        String danweiCode = AccountHelper.getDanweiCode(dwArray[which]);
                        groundManageDeptCode = danweiCode;
                        dialog.dismiss();
                    }
                })
                .create(mCurrentDialogStyle).show());


        /*资产类别选择*/
        editASSETSTYPE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int assetsCategoryNewOrOld = AssetsCategoryHelper.getAssetsCategoryNewOrOld();
                String[] temp = null;
                if (assetsCategoryNewOrOld == 1) {
                    temp = zclbNewArray;
                } else {
                    temp = zclbArray;
                }
                // 高效复制临时变量
                String[] finalTemp = temp;
                new QMUIDialog.CheckableDialogBuilder(mContext)
                        
                        .addItems(finalTemp, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editASSETSTYPE.setText(finalTemp[which]);
                                dialog.dismiss();
                            }
                        })
                        .create(mCurrentDialogStyle).show();
            }
        });
        editASSETSORGANIZATION.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new QMUIDialog.CheckableDialogBuilder(mContext)
                        
                        .addItems(bzqkArray, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editASSETSORGANIZATION.setText(bzqkArray[which]);
                                dialog.dismiss();
                            }
                        })
                        .create(mCurrentDialogStyle).show();
            }
        });
        /*账套选择*/
        editACCTSUITEID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new QMUIDialog.CheckableDialogBuilder(mContext)
                        
                        .addItems(ztArray, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editACCTSUITEID.setText(ztArray[which]);
                                dialog.dismiss();
                            }
                        })
                        .create(mCurrentDialogStyle).show();
            }
        });
        mHead = (RelativeLayout) findViewById(R.id.head);
        mHead.setFocusable(true);
        mHead.setClickable(true);
        mHead.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets = (ListView) findViewById(R.id.listAssets);
        assetsList = new ArrayList<Assets>();
        assetsAdapter = new AssetsListAdapter(assetsList, mContext, mHead, jumPagePojo);
        listAssets.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets.setAdapter(assetsAdapter);
        //new SearchAssetsTask().execute();

        //lyDrawLayout.openDrawer(lyQuery);
        refreshLayout.setEnableRefresh(false);
        //refreshLayout.setEnableAutoLoadMore(false);
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                if (hasMoreData)
                    new SearchAssetsTask().execute();
                else{
//                    refreshLayout.finishLoadMore(100);
                    refreshLayout.finishLoadMoreWithNoMoreData();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tv_assets_msg.setText("当前共有资产：" + Assets.count(Assets.class) + "条");
        maxAssetsID = 0;
        // 重置"是否有更多数据"标志（关键修复点）
        hasMoreData = true;
        if (!isFirstLoad) {
            assetsList.clear();
            lyDrawLayout.closeDrawer(lyQuery);
            new SearchAssetsTask().execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class SearchAssetsTask extends AsyncTask<Void, Void, List<Assets>> {

        StringBuilder queryWhere = new StringBuilder();
        List<String> whereParams = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            queryWhere.append(" Id > " + maxAssetsID);

            if (!TextUtils.isEmpty(editACCTSUITEID.getText().toString()) && !editACCTSUITEID.getText().toString().equals("选择账套")) {
                queryWhere.append(" and ACCTSUITEID=? ");
                whereParams.add(SysConfig.getAccountsByZTMC(editACCTSUITEID.getText().toString()).getZTBH());
            }
            if (!TextUtils.isEmpty(editASSETSDEPT.getText().toString())) {
                queryWhere.append(" and ASSETSDEPT LIKE ? ");
                whereParams.add("%" + editASSETSDEPT.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editBARCODEID.getText().toString())) {
                queryWhere.append(" and BARCODEID LIKE ? ");
                whereParams.add("%" + editBARCODEID.getText().toString() + "%");
            }

            if (!TextUtils.isEmpty(editASSETSLAYADD.getText().toString())) {
                queryWhere.append(" and ASSETSLAYADD LIKE ? ");
                whereParams.add("%" + editASSETSLAYADD.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editASSETSNAME.getText().toString())) {
                queryWhere.append(" and ASSETSNAME LIKE ? ");
                whereParams.add("%" + editASSETSNAME.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editASSETSSTANDARD.getText().toString())) {
                queryWhere.append(" and ASSETSSTANDARD LIKE ? ");
                whereParams.add("%" + editASSETSSTANDARD.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editASSETSMODEL.getText().toString())) {
                queryWhere.append(" and ASSETSMODEL LIKE ? ");
                whereParams.add("%" + editASSETSMODEL.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editASSETSTYPE.getText().toString())) {
                queryWhere.append(" and ASSETSTYPE=? ");
                whereParams.add(AssetsCategoryHelper.getAssetsCategoryCode(editASSETSTYPE.getText().toString()));
            }
            if (!TextUtils.isEmpty(editASSETSUSER.getText().toString())) {
                queryWhere.append(" and ASSETSUSER LIKE ? ");
                whereParams.add("%" + editASSETSUSER.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editCUSTODIAN.getText().toString())) {
                queryWhere.append(" and CUSTODIAN LIKE ? ");
                whereParams.add("%" + editCUSTODIAN.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editGROUNDMANAGEDEPT.getText().toString()) && !TextUtils.isEmpty(groundManageDeptCode)) {
                queryWhere.append(" and GROUNDMANAGEDEPT = ? ");
                whereParams.add(groundManageDeptCode);
//                whereParams.add("%" + editGROUNDMANAGEDEPT.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editASSETSORGANIZATION.getText().toString())) {
                queryWhere.append(" and PROPERTY=? ");
                whereParams.add(editASSETSORGANIZATION.getText().toString());
            }
            if(!StringUtils.isNullOrEmpty(searchPDZT)){
//                queryWhere.append(" and PDZT IN ('01', '03', '04')");
                queryWhere.append(" and PDZT = '01'");
//              whereParams.add(searchPDZT);

            }

            if (!TextUtils.isEmpty(editASSETSCURRPRICE.getText().toString())) {
                queryWhere.append(" and ASSETSCURRPRICE=? ");
                whereParams.add(editASSETSCURRPRICE.getText().toString());
            }
            if (!TextUtils.isEmpty(editREMARKS.getText().toString())) {
                queryWhere.append(" and REMARKS LIKE ? ");
                whereParams.add("%" + editREMARKS.getText().toString() + "%");
            }
            if (switcher.isChecked()) {
                queryWhere.append(" and ISUPLOAD=? ");
                whereParams.add("0");
            }
            showProgressDialog("", "正在检索资产");
        }

        @Override
        protected List<Assets> doInBackground(Void... params) {
            String[] whereArray = new String[whereParams.size()];
            whereParams.toArray(whereArray);

            List<Assets> assetsList = Assets.find(Assets.class, queryWhere.toString(), whereArray, null, "Id ASC", pageSize + "");
            if (!assetsList.isEmpty()) {
                AssetsQueryActivity.this.assetsList.addAll(assetsList);
                int size = assetsList.size();
                maxAssetsID = assetsList.get(size - 1).getId();

                // 获取resultList的ID，并调用接口进行同步操作
                List<String> stringList = new ArrayList<>();
                for(Assets assets : assetsList){
                    String docId = assets.getDOCID();
                    stringList.add(docId);
                }
                String ztbh = application.getLoginAccount().getZTBH();
                StringBuilder sb = new StringBuilder();
                for(Assets assets : assetsList){
                    String docId = assets.getDOCID();
                    sb.append(docId).append(",");
                }
                // 去掉最后的逗号
                int sbLen = sb.length();
                if(sbLen > 0){
                    sb.deleteCharAt( sbLen- 1);
                }
    /*            String dataStr = sb.toString();
                String param = String.format(SysDefine.SERVERHOST_SYNC_ASSETS_BY_ID_GET, ztbh, dataStr);
                String s = OkHttpUtils.getInstance().get(param);
                if (StringUtils.isNullOrEmpty(s) || s.startsWith("ERROR")) {
                    showShortText("同步出错，".concat(s));
                }
                else{
                    StringBuilder errorStr = new StringBuilder();
                    try {
                        List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
                        for (String dataLine : results) {
                            long newID = DataTransmitHelper.updateAssets(dataLine);
                            if (newID == -1) {
                                String[] rowStrArray = dataLine.split("\\|", -1);
                                String bcode = rowStrArray[0].trim();
                                errorStr.append("条码编号：".concat(bcode).concat("更新失败；"));
                            }
                        }
                        if(errorStr.length() > 0){
                            showShortText(errorStr.toString());
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }*/

            }
            else {
                // 若无新数据，重置分页标志
                hasMoreData = false;
            }
            // 移除重复的第二次查询（原有代码中的第二次查询是冗余的，可能导致覆盖）
            // assetsList = Assets.find(Assets.class, queryWhere.toString(), whereArray, null, "Id ASC", pageSize + "");
            return assetsList;
        }

        @Override
        protected void onPostExecute(List<Assets> assetsList) {
            super.onPostExecute(assetsList);
            hideProgressDialog();
            /*if (assetsList.size() < pageSize) {
                hasMoreData = false;
                //refreshLayout.setEnableLoadMore(false);
            } else {
                hasMoreData = true;
                //refreshLayout.setEnableLoadMore(true);
            }

            tv_assets_msg.setText("当前共有资产：" + assetsList.size() + "条");
            assetsAdapter.notifyDataSetChanged();
            refreshLayout.finishLoadMore(500);//传入false表示加载失败*/

            // 根据本次加载的结果判断是否有更多数据
            hasMoreData = assetsList.size() >= pageSize;
            refreshLayout.finishLoadMore(hasMoreData ? 500 : 100);

            // 更新UI显示总数（使用全局列表的总数）
            tv_assets_msg.setText("共有资产：" +  Assets.count(Assets.class) + "条");
            assetsAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<Assets> assetsList) {
            super.onCancelled(assetsList);
            hideProgressDialog();
        }
    }

    class ListViewAndHeadViewTouchLinstener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View arg0, MotionEvent motionEvent) {
            //当在列头 和 listView控件上touch时，将这个touch的事件分发给 ScrollView
            HorizontalScrollView headSrcrollView = (HorizontalScrollView) mHead
                    .findViewById(R.id.scrollView);
            headSrcrollView.onTouchEvent(motionEvent);
            return false;
        }
    }



}
