package com.gsoft.inventory;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsoft.inventory.const_data.UserRoleConst;
import com.gsoft.inventory.service.import_net_assets.OnDepartButtonCheckedChangeListener;
import com.gsoft.inventory.service.import_net_assets.SyncServerAssetsTask;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.widget.QMUITabSegment;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author 10904
 */
public class ImportNetAssetsActivity extends BaseCompatActivity {
    @BindView(R.id.topbar) QMUITopBar topBar;
    @BindView(R.id.tabSegment)
    QMUITabSegment tabSegment;
    @BindView(R.id.tvAssetCountTip)
    TextView tvAssetCountTip;
    @BindView(R.id.layoutPersonal) View layoutPersonal;
    @BindView(R.id.layoutDepartment) View layoutDepartment;
    @BindView(R.id.layoutAll) View layoutAll;
    @BindView(R.id.groupListViewPersonal) QMUIGroupListView mGroupListViewPersonal;
    @BindView(R.id.groupListViewDepartment) QMUIGroupListView mGroupListViewDepartment;
    @BindView(R.id.groupListView) QMUIGroupListView mGroupListView;
    @BindView(R.id.chkZengLiang) CheckBox chkZengLiang;
    @BindView(R.id.chkAll) CheckBox chkAll;

    QMUIGroupListView.Section groupSection = null;
    List<Accounts> departArray;
    List<Accounts> chooseDeparts;
    List<QMUICommonListItemView> listItemViews;
    /**
     * 当前角色可见的导入模式，下标与Tab下标一致
     */
    private List<Integer> visibleModes;
    /**
     * 当前选中的导入模式
     */
    private int currentMode = UserRoleConst.MODE_ALL;
    private Accounts loginAccount;
    boolean isClearAssets = true;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    /**
     * 登录人姓名、编号：个人资产按管理人(CUSTODIAN)匹配，兼容数据源存姓名或存编号
     */
    private String loginUserName = "";
    private String loginUserNo = "";
    /**
     * 本机个人资产数量，-1表示尚未统计完成
     */
    private long personalAssetCount = -1;
    /**
     * 本机全部资产数量，-1表示尚未统计完成
     */
    private long allAssetCount = -1;
    /**
     * 统计请求序号：用于丢弃过期的异步统计结果
     */
    private int assetCountToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_net_assets);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        loginAccount = application.getLoginAccount();
        if (loginAccount == null) {
            showShortText("当前登录信息异常，请重新登录！");
            finish();
            return;
        }
        loginUserName = StringUtils.trim(loginAccount.getYHXM());
        loginUserNo = StringUtils.trim(loginAccount.getYHBH());
        String ztbh = loginAccount.getZTBH();

        topBar.setTitle("导入资产数据");
        topBar.addRightImageButton(R.drawable.ic_download, R.id.topbar_right_about_button).setOnClickListener(v -> handleImport());

        visibleModes = UserRoleConst.getVisibleModes(loginAccount.getROLE());
        currentMode = visibleModes.get(0);
        initTabSegment();
        initRangeViews();
        initDepartViews(ztbh);
        showMode(currentMode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到本页（含导入完成、资产被修改后返回）重新统计，保证提示数量与本地库一致
        refreshAssetCounts();
    }

    /**
     * 根据角色初始化Tab，仅一个模式时隐藏Tab栏
     */
    private void initTabSegment() {
        if (visibleModes.size() <= 1) {
            tabSegment.setVisibility(View.GONE);
            return;
        }
        tabSegment.setVisibility(View.VISIBLE);
        for (int mode : visibleModes) {
            tabSegment.addTab(new QMUITabSegment.Tab(getModeTitle(mode)));
        }
        tabSegment.notifyDataChanged();
        tabSegment.addOnTabSelectedListener(new QMUITabSegment.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                currentMode = visibleModes.get(index);
                showMode(currentMode);
            }

            @Override
            public void onTabUnselected(int index) {
            }

            @Override
            public void onTabReselected(int index) {
            }

            @Override
            public void onDoubleTap(int index) {
            }
        });
        tabSegment.selectTab(0);
    }

    private String getModeTitle(int mode) {
        switch (mode) {
            case UserRoleConst.MODE_PERSONAL:
                return "个人导入";
            case UserRoleConst.MODE_DEPARTMENT:
                return "部门导入";
            default:
                return "全部部门导入";
        }
    }

    private void showMode(int mode) {
        layoutPersonal.setVisibility(mode == UserRoleConst.MODE_PERSONAL ? View.VISIBLE : View.GONE);
        layoutDepartment.setVisibility(mode == UserRoleConst.MODE_DEPARTMENT ? View.VISIBLE : View.GONE);
        layoutAll.setVisibility(mode == UserRoleConst.MODE_ALL ? View.VISIBLE : View.GONE);
        // 切换Tab时同步刷新数量提示
        updateAssetCountTip();
    }

    /**
     * 统计本机资产数量（后台执行，避免数据量大时阻塞界面）：
     * 个人资产按管理人(CUSTODIAN)匹配，全部资产为本地资产总条数
     */
    private void refreshAssetCounts() {
        if (loginAccount == null) {
            return;
        }
        final String userName = loginUserName;
        final String userNo = loginUserNo;
        final int token = ++assetCountToken;
        AsyncManager.getInstance().submitWithCallback(() -> {
            long personalCount = countPersonalAssets(userName, userNo);
            long totalCount = Assets.count(Assets.class);
            return new long[]{personalCount, totalCount};
        }, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != assetCountToken) {
                return;
            }
            if (result.isSuccess() && result.getData() != null) {
                personalAssetCount = result.getData()[0];
                allAssetCount = result.getData()[1];
            } else {
                personalAssetCount = -1;
                allAssetCount = -1;
            }
            updateAssetCountTip();
        }));
    }

    /**
     * 统计本机个人资产数量：按管理人(CUSTODIAN)匹配
     * 服务器同步报文的管理人字段可能存姓名，也可能存用户编号，两种都纳入匹配
     */
    private long countPersonalAssets(String userName, String userNo) {
        List<String> whereParts = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (!StringUtils.isNullOrEmpty(userName)) {
            whereParts.add("CUSTODIAN = ?");
            args.add(userName);
        }
        if (!StringUtils.isNullOrEmpty(userNo)) {
            whereParts.add("CUSTODIAN = ?");
            args.add(userNo);
        }
        if (whereParts.isEmpty()) {
            return 0;
        }
        return Assets.count(Assets.class, TextUtils.join(" OR ", whereParts), args.toArray(new String[0]));
    }

    /**
     * 按当前导入模式刷新数量提示：
     * 个人导入只显示个人资产；部门导入、全部部门导入同时显示个人资产与全部资产
     */
    private void updateAssetCountTip() {
        if (personalAssetCount < 0 || allAssetCount < 0) {
            tvAssetCountTip.setText("本机资产数量统计中…");
            return;
        }
        String personalValue = StringUtils.isNullOrEmpty(loginUserName) && StringUtils.isNullOrEmpty(loginUserNo)
                ? "-" : String.valueOf(personalAssetCount);
        String personalTip = "个人资产：" + personalValue + " 条";
        if (currentMode == UserRoleConst.MODE_PERSONAL) {
            tvAssetCountTip.setText(personalTip);
        } else {
            tvAssetCountTip.setText(personalTip + "，全部资产：" + allAssetCount + " 条");
        }
    }

    /**
     * 初始化个人导入、部门导入的展示信息
     */
    private void initRangeViews() {
        String userName = loginAccount.getYHXM() == null ? "" : loginAccount.getYHXM();
        String userNo = loginAccount.getYHBH() == null ? "" : loginAccount.getYHBH();
        QMUIGroupListView.newSection(mContext)
                .setTitle("导入范围")
                .addItemView(mGroupListViewPersonal.createItemView("使用人：".concat(userName).concat("（").concat(userNo).concat("）")), null)
                .addTo(mGroupListViewPersonal);

        String deptName = loginAccount.getBMMC() == null ? "" : loginAccount.getBMMC();
        String deptNo = loginAccount.getBMBH() == null ? "" : loginAccount.getBMBH();
        QMUIGroupListView.newSection(mContext)
                .setTitle("导入范围")
                .addItemView(mGroupListViewDepartment.createItemView("部门：".concat(deptName).concat("（").concat(deptNo).concat("）")), null)
                .addTo(mGroupListViewDepartment);
    }

    private void initDepartViews(String ztbh) {
        departArray = AccountHelper.getDepartListWithObjects(ztbh);
        chooseDeparts = new ArrayList<>();
        groupSection = QMUIGroupListView.newSection(mContext).setTitle("选择要导入资产的部门");
        listItemViews = new ArrayList<>();
        initGroupListView();
        groupSection.addTo(mGroupListView);
        chkAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (QMUICommonListItemView view : listItemViews) {
                view.getSwitch().setChecked(isChecked);
            }
        });
        chkZengLiang.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            this.isClearAssets = !isChecked;
        }));
    }

    private void initGroupListView() {
        for (Accounts account : departArray) {
            QMUICommonListItemView itemWithSwitch = mGroupListView.createItemView(account.getBMMC());
            itemWithSwitch.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_SWITCH);
            itemWithSwitch.getSwitch().setOnCheckedChangeListener(new OnDepartButtonCheckedChangeListener(account, chooseDeparts));
            groupSection.addItemView(itemWithSwitch, null);
            listItemViews.add(itemWithSwitch);
        }
    }

    private void handleImport() {
        switch (currentMode) {
            case UserRoleConst.MODE_PERSONAL:
                importPersonal();
                break;
            case UserRoleConst.MODE_DEPARTMENT:
                importDepartment();
                break;
            default:
                importAllDepartments();
                break;
        }
    }

    /**
     * 个人导入：按当前登录用户编号同步
     */
    private void importPersonal() {
        final String yhbh = loginAccount.getYHBH();
        if (StringUtils.isNullOrEmpty(yhbh)) {
            showShortText("未获取到当前用户编号，无法导入");
            return;
        }
        final String ztbh = loginAccount.getZTBH();
        String userName = StringUtils.isNullOrEmpty(loginAccount.getYHXM()) ? "" : loginAccount.getYHXM();
        showConfirmDialog("将要导入个人资产（".concat(userName).concat("），方式为：清空后重新导入"),
                () -> startSync(ztbh, "", yhbh, true));
    }

    /**
     * 部门导入：按当前登录用户所在部门同步
     */
    private void importDepartment() {
        final String bmbh = loginAccount.getBMBH();
        if (StringUtils.isNullOrEmpty(bmbh)) {
            showShortText("未获取到当前部门信息，无法导入");
            return;
        }
        final String ztbh = loginAccount.getZTBH();
        String bmmc = StringUtils.isNullOrEmpty(loginAccount.getBMMC()) ? "" : loginAccount.getBMMC();
        showConfirmDialog("将要导入部门资产（".concat(bmmc).concat("），方式为：清空后重新导入"),
                () -> startSync(ztbh, bmbh, "", true));
    }

    /**
     * 全部部门导入：同步选中部门，方式由增量复选框决定
     */
    private void importAllDepartments() {
        if (chooseDeparts == null || chooseDeparts.isEmpty()) {
            showShortText("请选择要同步的部门");
            return;
        }
        final String ztbh = loginAccount.getZTBH();
        StringBuilder deptIdsBuilder = new StringBuilder();
        StringBuilder deptNamesBuilder = new StringBuilder();
        for (int i = 0; i < chooseDeparts.size(); i++) {
            Accounts account = chooseDeparts.get(i);
            if (i > 0) {
                deptIdsBuilder.append(",");
                deptNamesBuilder.append("、");
            }
            deptIdsBuilder.append(account.getBMBH());
            deptNamesBuilder.append(account.getBMMC());
        }
        final String fetchDeptids = deptIdsBuilder.toString();
        showConfirmDialog("将要同步：".concat(deptNamesBuilder.toString()).concat(" 的数据，方式为：").concat(isClearAssets ? "清空" : "增量更新"),
                () -> startSync(ztbh, fetchDeptids, "", isClearAssets));
    }

    private void showConfirmDialog(String message, Runnable onConfirm) {
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("系统提示")
                .setMessage(message)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    onConfirm.run();
                })
                .create(mCurrentDialogStyle).show();
    }

    private void startSync(String ztbh, String deptIds, String userId, boolean clearAssets) {
        showProgressDialog("", "正在同步");
        SyncServerAssetsTask task = new SyncServerAssetsTask(ztbh, deptIds, userId, clearAssets);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                hideProgressDialog();
                // 导入会增删本地资产（含部分失败的情况），完成后立即刷新数量提示
                refreshAssetCounts();
                if (result.isSuccess()) {
                    String data = result.getData();
                    if (StringUtils.isNullOrEmpty(data)) {
                        showShortText("同步成功!");
                    } else if (SyncServerAssetsTask.RESULT_EMPTY_DATA.equals(data)) {
                        showShortText(data);
                    } else {
                        showShortText("同步出错，".concat(data));
                    }
                } else {
                    showShortText("同步出错，发生异常");
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listItemViews != null && !listItemViews.isEmpty()) {
            for (QMUICommonListItemView view : listItemViews) {
                view.getSwitch().setOnCheckedChangeListener(null);
            }
            listItemViews.clear();
            groupSection.removeFrom(mGroupListView);
            mGroupListView.removeAllViews();
        }
    }
}