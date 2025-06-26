package com.gsoft.inventory;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.adapter.BatchCopyAdapter;
import com.gsoft.inventory.adapter.BatchCopyCheckBoxAdapter;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchCopyActivity extends BaseCompatActivity {

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    @BindView(R.id.batchCopyLayout)
    DrawerLayout batchCopyLayout;

    @BindView(R.id.lyQuery)
    LinearLayout lyQuery;

    @BindView(R.id.search_close)
    QMUIButton search_close;

    @BindView(R.id.search_reset)
    QMUIButton search_reset;

    @BindView(R.id.search_button)
    QMUIButton searchButton;

    // 搜索框的值
    // 条码编号
    @BindView(R.id.editBARCODEID)
    EditText editBARCODEID;

    // 资产名称
    @BindView(R.id.editASSETSNAME)
    EditText editASSETSNAME;

    // 规格
    @BindView(R.id.editASSETSMODEL)
    EditText editASSETSMODEL;

    // 型号
    @BindView(R.id.editASSETSSTANDARD)
    EditText editASSETSSTANDARD;

    // 管理人
    @BindView(R.id.editCUSTODIAN)
    EditText editCUSTODIAN;

    // 使用人
    @BindView(R.id.editASSETSUSER)
    EditText editASSETSUSER;

    // 使用部门
    @BindView(R.id.editASSETSDEPT)
    EditText editASSETSDEPT;

    // 管理部门
    @BindView(R.id.editGROUNDMANAGEDEPT)
    EditText editGROUNDMANAGEDEPT;

    // 存放地点
    @BindView(R.id.editASSETSLAYADD)
    EditText editASSETSLAYADD;

    private String groundManageDeptCode;

    @BindDrawable(R.drawable.icon_right)
    Drawable icon_right;


    @BindView(R.id.listAssets)
    ListView listAssets;

    StringBuilder queryWhere = null;
    List<String> whereParams = new ArrayList<>();

    String[] dwArray = null;

    List<AssetsCopy> assetsCopyList = new ArrayList<>();

    private List<Assets> queryResult;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    private BatchCopyCheckBoxAdapter copyCheckBoxAdapter;

    RelativeLayout mHead;

    private QMUIDialog deleteDialog;

    private RadioGroup radioGroup;
    private RadioButton radioButton1, radioButton2;

    private String copyImg = "否";

    private Gson gson = new Gson();

    private TextView tvTotal;

    private View footerView;

    private CheckBox cbSelectAll;

    // 在Activity中定义监听器变量
    private CompoundButton.OnCheckedChangeListener checkAllListener = (buttonView, isChecked) -> {
        for (AssetsCopy item : assetsCopyList) {
            item.setSELECTED(isChecked);
        }
        copyCheckBoxAdapter.notifyDataSetChanged();
        updateTotalCount(assetsCopyList.size());
    };


    /**
     * 复制次数
     */
    private int copyTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_copy);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("批量复制");
        // 设置让check可见
        TextView allCheckTextView = findViewById(R.id.allCheck);
        allCheckTextView.setVisibility(View.VISIBLE);

        editGROUNDMANAGEDEPT.setCompoundDrawables(null, null, icon_right, null);
        // 获取部门名称
        List<String> danweiList = AccountHelper.getDanweiList(application.getLoginAccount().getZTBH());
        dwArray = new String[danweiList.size()];
        danweiList.toArray(dwArray);
        editGROUNDMANAGEDEPT.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                
                .addItems(dwArray, (dialog, which) -> {
                    editGROUNDMANAGEDEPT.setText(dwArray[which]);
                    String danweiCode = AccountHelper.getDanweiCode(dwArray[which]);
                    groundManageDeptCode = danweiCode;
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        this.handleTopBarClick();
        this.handleSearchButtonClick();


    }

    @Override
    protected void onResume() {
        super.onResume();
        // 清空旧数据
        assetsCopyList.clear();
        // 重新从数据库加载数据
        initData();
    }

    private void initData() {
        HorizontalScrollView hScroll = findViewById(R.id.main_scroll);
        NestedScrollView vScroll = findViewById(R.id.nested_scroll);
        listAssets = findViewById(R.id.listAssets);
        // 在Activity中
        cbSelectAll = findViewById(R.id.cbSelectAll);

        // 反序列化
        String assetsCopyListJson = SharedPreferencesUtils.getString(BatchCopyActivity.this, "COPY_ASSETS_JSON", "");
        if (!TextUtils.isEmpty(assetsCopyListJson)) {
            assetsCopyList = gson.fromJson(assetsCopyListJson, new TypeToken<List<AssetsCopy>>() {
            }.getType());
        } else {
            // 确保不为null
            assetsCopyList = new ArrayList<>();
        }

        copyCheckBoxAdapter = new BatchCopyCheckBoxAdapter(BatchCopyActivity.this, assetsCopyList, mHead);


        // 初始化时绑定
        cbSelectAll.setOnCheckedChangeListener(checkAllListener);

        copyCheckBoxAdapter.notifyDataSetChanged();
        // 添加状态监听
        copyCheckBoxAdapter.setOnCheckAllListener(isAllChecked -> {
            // 临时解除监听防止循环调用
            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(isAllChecked);
            // 重新绑定正确的监听器
            cbSelectAll.setOnCheckedChangeListener(checkAllListener);
        });

        // 横向滚动同步
        hScroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollX = hScroll.getScrollX();
            // 仅同步横向位置到垂直容器
            vScroll.scrollTo(scrollX, vScroll.getScrollY());
        });



        // 底部加载数量显示
        tvTotal = findViewById(R.id.tvTotalCount);
        updateTotalCount(assetsCopyList.size());


        // 向list插入条目
        listAssets.setItemsCanFocus(true);
        listAssets.setNestedScrollingEnabled(true);
        listAssets.setScrollContainer(false);
        listAssets.setHorizontalScrollBarEnabled(false);
        listAssets.setVerticalScrollBarEnabled(false);
        listAssets.setAdapter(copyCheckBoxAdapter);
        listAssets.invalidateViews();
    }


    // 更新总数的方法
    private void updateTotalCount(int count) {
        tvTotal.setText("共 " + count + " 条数据");
    }


    private void handleTopBarClick() {
        topBar.addRightTextButton("搜索", R.id.topbar_right_about_button).setOnClickListener(view -> batchCopyLayout.openDrawer(lyQuery));
        topBar.addRightTextButton("删除", R.id.topbar_right_query_button).setOnClickListener(view -> {
            deleteDialog = new QMUIDialog.MessageDialogBuilder(BatchCopyActivity.this)
                    .setTitle("删除")
                    .setMessage("确定删除选中的资产吗？")
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        new deleteAssetsCopyTask().execute();
                    })
                    .create(mCurrentDialogStyle);
            deleteDialog.show();
        });
        search_close.setOnClickListener(view -> batchCopyLayout.closeDrawer(lyQuery));
        topBar.addLeftTextButton("同步资产", R.id.qmuidemo).setOnClickListener(view -> {
            deleteDialog = new QMUIDialog.MessageDialogBuilder(BatchCopyActivity.this)
                    .setTitle("同步到资产表")
                    .setMessage("确定同步选中的资产吗？")
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        new SyncAssetsTask().execute();
                    })
                    .create(mCurrentDialogStyle);
            deleteDialog.show();
        });
    }


    private void handleSearchButtonClick() {
        searchButton.setOnClickListener(view -> {
            // 获取搜索框的值，然后去搜索
            showBatchCopyDialog();
        });

        search_reset.setOnClickListener(view -> {
            editASSETSDEPT.setText("");
            editASSETSLAYADD.setText("");
            editASSETSNAME.setText("");
            editBARCODEID.setText("");
            editASSETSSTANDARD.setText("");
            editASSETSMODEL.setText("");
            editASSETSUSER.setText("");
            editCUSTODIAN.setText("");
            editGROUNDMANAGEDEPT.setText("");
            groundManageDeptCode = "";
        });
    }


    private List<Assets> handleSearchAssets() {
        String barcodeId = editBARCODEID.getText().toString();
        String assetsName = editASSETSNAME.getText().toString();
        String assetsModel = editASSETSMODEL.getText().toString();
        String assetsStandard = editASSETSSTANDARD.getText().toString();
        String custodian = editCUSTODIAN.getText().toString();
        String assetsUser = editASSETSUSER.getText().toString();
        String assetsDept = editASSETSDEPT.getText().toString();
        String groundManageDept = editGROUNDMANAGEDEPT.getText().toString();

        // 重置StringBuilder
        queryWhere = new StringBuilder();
        // 清空参数列表
        whereParams.clear();
        queryWhere.append(" Id > " + 0);

        if (!TextUtils.isEmpty(barcodeId)) {
            queryWhere.append(" and BARCODEID LIKE ? ");
            whereParams.add("%" + barcodeId + "%");
        }

        if (!TextUtils.isEmpty(assetsDept)) {
            queryWhere.append(" and ASSETSDEPT LIKE ? ");
            whereParams.add("%" + assetsDept + "%");
        }

        if (!TextUtils.isEmpty(editASSETSLAYADD.getText().toString())) {
            queryWhere.append(" and ASSETSLAYADD LIKE ? ");
            whereParams.add("%" + editASSETSLAYADD.getText().toString() + "%");
        }
        if (!TextUtils.isEmpty(assetsName)) {
            queryWhere.append(" and ASSETSNAME LIKE ? ");
            whereParams.add("%" + assetsName + "%");
        }
        if (!TextUtils.isEmpty(assetsStandard)) {
            queryWhere.append(" and ASSETSSTANDARD LIKE ? ");
            whereParams.add("%" + assetsStandard + "%");
        }
        if (!TextUtils.isEmpty(assetsModel)) {
            queryWhere.append(" and ASSETSMODEL LIKE ? ");
            whereParams.add("%" + assetsModel + "%");
        }

        if (!TextUtils.isEmpty(assetsUser)) {
            queryWhere.append(" and ASSETSUSER LIKE ? ");
            whereParams.add("%" + assetsUser + "%");
        }
        if (!TextUtils.isEmpty(custodian)) {
            queryWhere.append(" and CUSTODIAN LIKE ? ");
            whereParams.add("%" + custodian + "%");
        }
        if (!TextUtils.isEmpty(groundManageDept) && !TextUtils.isEmpty(groundManageDeptCode)) {
            queryWhere.append(" and GROUNDMANAGEDEPT = ? ");
            whereParams.add(groundManageDeptCode);
        }
        queryWhere.append(" and PDZT != '01' ");

        String[] whereArray = new String[whereParams.size()];
        whereParams.toArray(whereArray);
        return Assets.find(
                Assets.class,
                queryWhere.toString(),
                whereArray,
                null,
                "Id ASC",
                "500");
    }


    // 弹窗显示方法
    private void showBatchCopyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_refresh_list, null);
        // 修改后（正确代码）
        HorizontalScrollView hScroll = dialogView.findViewById(R.id.main_scroll); // 类型匹配
        NestedScrollView vScroll = dialogView.findViewById(R.id.nested_scroll);

        // 添加空指针检查
        if(hScroll != null && vScroll != null){
            // 横向滚动同步
            hScroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
                int scrollX = hScroll.getScrollX();
                // 仅同步横向位置到垂直容器
                vScroll.scrollTo(scrollX, vScroll.getScrollY());
            });
        }


        // 绑定弹窗内控件
        ListView dialogList = dialogView.findViewById(R.id.listAssets);
        EditText etNumber = dialogView.findViewById(R.id.et_number);
        etNumber.setText("");

        // 向list插入条目
        dialogList.setItemsCanFocus(true);
        dialogList.setNestedScrollingEnabled(true);
        dialogList.setScrollContainer(false);
        dialogList.setHorizontalScrollBarEnabled(false);
        dialogList.setVerticalScrollBarEnabled(false);

        radioGroup = dialogView.findViewById(R.id.rg_copy_img);
        radioButton1 = dialogView.findViewById(R.id.rb_copy_img_yes);
        radioButton2 = dialogView.findViewById(R.id.rb_copy_img_no);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_copy_img_yes:
                        // 处理选项1的逻辑
                        copyImg = "是";
                        break;
                    case R.id.rb_copy_img_no:
                        // 处理选项2的逻辑
                        copyImg = "否";
                        break;
                }
            }
        });

        // 配置列表适配器（需提前准备item_checkable.xml）
        queryResult = handleSearchAssets();
        BatchCopyAdapter adapter = new BatchCopyAdapter(BatchCopyActivity.this, queryResult);
        dialogList.setAdapter(adapter);

        // 构建弹窗
        builder.setView(dialogView)
                .setTitle("复制确定")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 进行复制操作
                    String etNumberStr = etNumber.getText().toString();
                    if (StringUtils.isNullOrEmpty(etNumberStr)) {
                        copyTimes = 1;
                    } else {
                        copyTimes = Integer.parseInt(etNumberStr);
                    }
                    new CopyAssetsTask().execute(); // 每次创建新实例
                })
                .setNegativeButton("取消", null)
                .create()
                .show();


    }


    private class CopyAssetsTask extends AsyncTask<Void, Void, List<Assets>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在复制资产……");
        }

        @Override
        protected List<Assets> doInBackground(Void... voids) {
            // 将queryResult中的数据保存到AssetsCopy中
            int num = 0;
            for (Assets assets : queryResult) {
                for (int i = 0; i < copyTimes; i++) {
                    AssetsCopy assetsCopy = new AssetsCopy();
                    assetsCopy.setIDNUM(num+1);
                    // 拷贝值
                    assetsCopy.deepCopyAssets(assets);
                    // 重新设置id
                    String docId = AssetsUtils.generalDocId(application.getDeviceNo());
                    assetsCopy.setDOCID(docId);
                    // 设置旧id
                    assetsCopy.setCOPY_DOC_ID(assets.getDOCID());
                    // 设置新的条码编号
                    String barCode = SysConfig.getNewAssetsBarcodeByDevice(application.getDeviceNo());
                    // 卡片编号和条码编号一致
                    assetsCopy.setBARCODEID(barCode);
                    assetsCopy.setSERIALCODE(barCode);
                    // 并设置为盘盈
                    assetsCopy.setPDZT("03");
                    assetsCopy.setREMARKS("盘盈");
                    assetsCopy.setCOPY_IMG(copyImg);

                    assetsCopyList.add(assetsCopy);
                    num++;
                }
            }

            String assetsCopyListJson = gson.toJson(assetsCopyList);
            SharedPreferencesUtils.putString(BatchCopyActivity.this, "COPY_ASSETS_JSON", assetsCopyListJson);


            return null;
        }

        /**
         * 该函数在主线程运行
         *
         * @param assets
         */
        @Override
        protected void onPostExecute(List<Assets> assets) {
            super.onPostExecute(assets);
            // 通知适配器更新，页面要重新渲染数据，以免页面不更新
            copyCheckBoxAdapter.notifyDataSetChanged();
            hideProgressDialog();
            batchCopyLayout.closeDrawer(lyQuery);

            copyCheckBoxAdapter.notifyDataSetChanged();
            updateTotalCount(assetsCopyList.size());

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<Assets> s) {
            super.onCancelled(s);
            hideProgressDialog();
        }


    }

    /**
     * 删除资产
     */
    private class deleteAssetsCopyTask extends AsyncTask<Void, Void, List<AssetsCopy>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在删除资产……");
        }

        @Override
        protected List<AssetsCopy> doInBackground(Void... voids) {
            int num = 0;
            // 查看assetsCopyList中选中的数据
            Iterator<AssetsCopy> iterator = assetsCopyList.iterator();
            while (iterator.hasNext()) {
                AssetsCopy item = iterator.next();
                if (item.getSELECTED()) {
                    iterator.remove();
                    num++;
                }
            }
            if(num>0){
                String assetsCopyListJson = gson.toJson(assetsCopyList);
                SharedPreferencesUtils.putString(BatchCopyActivity.this, "COPY_ASSETS_JSON", assetsCopyListJson);
            }
            return null;
        }

        /**
         * 该函数在主线程运行
         *
         * @param assets
         */
        @Override
        protected void onPostExecute(List<AssetsCopy> assets) {
            super.onPostExecute(assets);

            // 刷新数据
            listAssets.invalidateViews();
            hideProgressDialog();
            deleteDialog.dismiss();

            copyCheckBoxAdapter.notifyDataSetChanged();
            updateTotalCount(assetsCopyList.size());

            Toast.makeText(BatchCopyActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
        }
    }


    private class SyncAssetsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在同步资产……");
        }

        @Override
        protected String doInBackground(Void... voids) {

            List<Assets> syncAssets = new ArrayList<>();
            int num = 0;
            // 获取选择的数据，将选择的数据放入资产表中，并设置状态为未同步，同时删除
            Iterator<AssetsCopy> iterator = assetsCopyList.iterator();
            while (iterator.hasNext()) {
                AssetsCopy item = iterator.next();
                if (item.getSELECTED()) {
                    Assets assets = new Assets();
                    item.toDeepCopyAssets(assets);
                    assets.setISUPLOAD("0");
                    syncAssets.add(assets);
                    iterator.remove();
                    num++;
                }
            }

            // 将资产新增
            for (Assets assets : syncAssets) {
                assets.save();
            }

            if(num>0){
                String assetsCopyListJson = gson.toJson(assetsCopyList);
                SharedPreferencesUtils.putString(BatchCopyActivity.this, "COPY_ASSETS_JSON", assetsCopyListJson);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // 刷新数据
            listAssets.invalidateViews();
            hideProgressDialog();
            deleteDialog.dismiss();

            copyCheckBoxAdapter.notifyDataSetChanged();
            updateTotalCount(assetsCopyList.size());

            Toast.makeText(BatchCopyActivity.this, "同步成功", Toast.LENGTH_SHORT).show();
        }
    }

}
