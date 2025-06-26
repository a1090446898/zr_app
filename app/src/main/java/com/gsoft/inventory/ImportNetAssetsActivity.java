package com.gsoft.inventory;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;

import com.gsoft.inventory.service.import_net_assets.OnDepartButtonCheckedChangeListener;
import com.gsoft.inventory.service.import_net_assets.SyncServerAssetsTask;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
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
    @BindView(R.id.groupListView) QMUIGroupListView mGroupListView;
    @BindView(R.id.chkZengLiang) CheckBox chkZengLiang;
    @BindView(R.id.chkAll) CheckBox chkAll;
    QMUIGroupListView.Section groupSection = null;
    List<String> departArray;
    List<String> chooseDeparts;
    List<QMUICommonListItemView> listItemViews;
    boolean isClearAssets = true;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_net_assets);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        String ztbh = application.getLoginAccount().getZTBH();

        topBar.setTitle("导入资产数据");
        topBar.addRightImageButton(R.drawable.ic_download, R.id.topbar_right_about_button).setOnClickListener(v -> {
            showShortText(chooseDeparts.size() + "");
            if (chooseDeparts.isEmpty()) {
                showShortText("请选择要同步的部门");
                return;
            }
            String deptids = "";
            String deptNames = "";
            for (int i = 0; i < chooseDeparts.size(); i++) {
                deptNames = deptNames.concat(chooseDeparts.get(i)).concat(",");
                String bmbh = AccountHelper.getDepartCode(ztbh, chooseDeparts.get(i));
                deptids = deptids.concat(bmbh).concat(",");
            }
            final String fetchDeptids = deptids.substring(0, deptids.length() - 1);
            final QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(mContext);
            builder.setTitle("系统提示")
                    .setMessage("将要".concat("同步：").concat(deptNames).concat("数据，方式为：").concat(isClearAssets ? "清空" : "增量更新"))
                    .addAction("取消", (dialog, index) -> dialog.dismiss())
                    .addAction("确定", (dialog, index) -> {
                        dialog.dismiss();
                        if (!StringUtils.isNullOrEmpty(fetchDeptids)) {
                            showProgressDialog("", "正在同步");
                            SyncServerAssetsTask task = new SyncServerAssetsTask(ztbh, fetchDeptids, isClearAssets);
                            AsyncManager.getInstance().submitWithCallback(task, result -> {
                                mainHandler.post(() -> {
                                    hideProgressDialog();
                                    if (result.isSuccess()) {
                                        String data = result.getData();
                                        if (StringUtils.isNullOrEmpty(data)) {
                                            showShortText("同步成功!");
                                        } else {
                                            showShortText("同步出错，".concat(data));
                                        }
                                    } else {
                                        showShortText("同步出错，发生异常");
                                    }
                                });
                            });
                        }
                    })
                    .create(mCurrentDialogStyle).show();

        });
        departArray = AccountHelper.getDepartList(ztbh);
        chooseDeparts = new ArrayList<>();
        int size = QMUIDisplayHelper.dp2px(mContext, 40);
        groupSection = QMUIGroupListView.newSection(mContext)
                .setTitle("选择要导入资产的部门");
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
        for (String name : departArray) {
            QMUICommonListItemView itemWithSwitch = mGroupListView.createItemView(name);
            itemWithSwitch.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_SWITCH);
            itemWithSwitch.getSwitch().setOnCheckedChangeListener(new OnDepartButtonCheckedChangeListener(name, chooseDeparts));
            groupSection.addItemView(itemWithSwitch, null);
            listItemViews.add(itemWithSwitch);
        }
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