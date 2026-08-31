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
import com.gsoft.inventory.pojo.QueryResutlPojo;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.rscja.team.qcom.deviceapi.Q;
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
    String[] pdztString = {"盘实", "盘盈", "盘亏", "未盘"};
    String[] pdztCodeString = {"02", "03", "04", "01"};

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

    @BindView(R.id.editSERIALCODE)
    EditText editSERIALCODE;

    // 规格
    @BindView(R.id.editASSETSSTANDARD)
    EditText editASSETSSTANDARD;

    // 型号
    @BindView(R.id.editASSETSMODEL)
    EditText editASSETSMODEL;

    @BindView(R.id.editCUSTODIAN)
    EditText editCUSTODIAN;
//    @BindView(R.id.editACCTSUITEID)
//    EditText editACCTSUITEID;
    @BindView(R.id.editASSETSORGANIZATION)
    EditText editASSETSORGANIZATION;
    @BindView(R.id.editASSETSTYPE)
    EditText editASSETSTYPE;
    @BindView(R.id.editASSETSDEPT)
    EditText editASSETSDEPT;
    @BindView(R.id.editGROUNDMANAGEDEPT)
    EditText editGROUNDMANAGEDEPT;

    @BindView(R.id.editPDZT)
    EditText editPDZT;

    private String groundManageDeptCode;
    private String pdztCode;
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


    int pageNum = 0;
    int pageSize = 20;
    boolean hasMoreData = false;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    boolean isFirstLoad = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_query);
        ButterKnife.bind(this);
        initializeView();
        receiveNewQueries();

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
            editPDZT.setVisibility(View.GONE);
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


        jumPagePojo.setIsJumpPage(isJumpPage);

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
                        pageNum=0;
                        assetsList.clear();
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
//                editACCTSUITEID.setText("");
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
//        editACCTSUITEID.setCompoundDrawables(null, null, icon_right, null);
        editASSETSTYPE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSORGANIZATION.setCompoundDrawables(null, null, icon_right, null);
        editGROUNDMANAGEDEPT.setCompoundDrawables(null, null, icon_right, null);
        editPDZT.setCompoundDrawables(null, null, icon_right, null);

        // 获取部门名称
        List<String> danweiList = AccountHelper.getDanweiList(loginAccount.getZTBH());
        dwArray = new String[danweiList.size()];
        danweiList.toArray(dwArray);
        editGROUNDMANAGEDEPT.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(dwArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editGROUNDMANAGEDEPT.setText(dwArray[which]);
                        groundManageDeptCode = AccountHelper.getDanweiCode(dwArray[which]);
                        dialog.dismiss();
                    }
                })
                .create(mCurrentDialogStyle).show());

        editPDZT.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(pdztString, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editPDZT.setText(pdztString[which]);
                        pdztCode = pdztCodeString[which];
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
      /*  editACCTSUITEID.setOnClickListener(new View.OnClickListener() {
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
        });*/
        mHead = (RelativeLayout) findViewById(R.id.head);
        mHead.setFocusable(true);
        mHead.setClickable(true);
        mHead.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets = (ListView) findViewById(R.id.listAssets);
        assetsList = new ArrayList<Assets>();
        assetsAdapter = new AssetsListAdapter(assetsList, mContext, mHead, jumPagePojo);
        listAssets.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets.setAdapter(assetsAdapter);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                if (!hasMoreData){
                    new SearchAssetsTask().execute();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.pageNum = 0;
        this.assetsList.clear();
        new SearchAssetsTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class SearchAssetsTask extends AsyncTask<Void, Void, QueryResutlPojo> {

        StringBuilder queryWhere = new StringBuilder();
        List<String> whereParams = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在检索资产");

            queryWhere.append(" Id > 0");
          /*  if (!TextUtils.isEmpty(editACCTSUITEID.getText().toString()) && !editACCTSUITEID.getText().toString().equals("选择账套")) {
                queryWhere.append(" and ACCTSUITEID=? ");
                whereParams.add(SysConfig.getAccountsByZTMC(editACCTSUITEID.getText().toString()).getZTBH());
            }*/
            if (!TextUtils.isEmpty(editASSETSDEPT.getText().toString())) {
                queryWhere.append(" and ASSETSDEPT LIKE ? ");
                whereParams.add("%" + editASSETSDEPT.getText().toString() + "%");
            }
            if (!TextUtils.isEmpty(editBARCODEID.getText().toString())) {
                queryWhere.append(" and BARCODEID LIKE ? ");
                whereParams.add("%" + editBARCODEID.getText().toString() + "%");
            }

            if (!TextUtils.isEmpty(editSERIALCODE.getText().toString())) {
                queryWhere.append(" and SERIALCODE LIKE ? ");
                whereParams.add("%" + editSERIALCODE.getText().toString() + "%");
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
            }

            if (!TextUtils.isEmpty(editASSETSORGANIZATION.getText().toString())) {
                queryWhere.append(" and PROPERTY=? ");
                whereParams.add(editASSETSORGANIZATION.getText().toString());
            }
            if(!StringUtils.isNullOrEmpty(searchPDZT)){
                queryWhere.append(" and PDZT = '01'");
            }
            if (StringUtils.isNullOrEmpty(searchPDZT) && !TextUtils.isEmpty(editPDZT.getText().toString()) && !TextUtils.isEmpty(pdztCode)) {
                queryWhere.append(" and PDZT = ? ");
                whereParams.add(pdztCode);
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

        }

        @Override
        protected QueryResutlPojo doInBackground(Void... params) {

            String[] whereArray = new String[whereParams.size()];
            whereParams.toArray(whereArray);

            List<Assets> assetsList = Assets.find(Assets.class, queryWhere.toString(), whereArray, null, "ID DESC", pageNum + ", "+pageSize);
            long total = Assets.count(Assets.class, queryWhere.toString(), whereArray, null, "ID DESC", "");

            /* if (!assetsList.isEmpty()) {
               int size = assetsList.size();

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
            }*/

            QueryResutlPojo queryResutlPojo = new QueryResutlPojo();
            queryResutlPojo.setAssetsList(assetsList);
            queryResutlPojo.setTotal(total);
            return queryResutlPojo;
        }

        @Override
        protected void onPostExecute(QueryResutlPojo pojo) {
            super.onPostExecute(pojo);

            List<Assets> resultList = pojo.getAssetsList();
            hideProgressDialog();

            long total = pojo.getTotal();
            // 根据本次加载的结果判断是否有更多数据
            if (!resultList.isEmpty()) {
                assetsList.addAll(resultList);
                pageNum += pageSize;
            }
            hasMoreData = resultList.isEmpty();
            // 重置加载状态：清除"没有更多数据"标记，允许再次加载
            refreshLayout.setNoMoreData(hasMoreData);
            refreshLayout.finishLoadMore( 200);
            // 更新UI显示总数（使用全局列表的总数）
            tv_assets_msg.setText("共有资产：" +  total + "条");
            assetsAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(QueryResutlPojo pojo) {
            super.onCancelled(pojo);
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
