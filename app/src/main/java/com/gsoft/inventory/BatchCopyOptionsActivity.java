package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.gsoft.inventory.adapter.SelectedAssetAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.data.BatchCopyOptions;
import com.gsoft.inventory.data.CopyResult;
import com.gsoft.inventory.data.SelectItem;
import com.gsoft.inventory.service.batch_copy.BatchCopyCallback;
import com.gsoft.inventory.service.batch_copy.GenerateCopyAssetsTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataConvert;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 批量复制表单页：选择资产、复制数量、是否复制图片、批量修改复制信息
 * <p>
 * 批量设置只覆盖非空字段：每个字段哪个有值就改哪个，"留空"的保留源资产原值，不要求全部填写；
 * 点击"开始复制"直接写入 Assets 表并打上本地标识，不做任何上传，
 * 成功后提示用户前往首页同步上传。
 */
public class BatchCopyOptionsActivity extends BaseCompatActivity {

    public static final String EXTRA_SELECTED_IDS = "extra_selected_ids";

    private static final int REQ_SELECT_ASSETS = 1001;
    private static final int REQ_ASSETS_DEPT = 1002;
    private static final int REQ_MANAGE_DEPT = 1003;
    private static final int REQ_CUSTODIAN = 1004;
    private static final int REQ_ASSETS_USER = 1005;

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.tvSelectedSummary)
    TextView tvSelectedSummary;
    @BindView(R.id.btnSelectAssets)
    View btnSelectAssets;
    @BindView(R.id.tvNoSelection)
    View tvNoSelection;
    @BindView(R.id.listSelectedAssets)
    ListView listSelectedAssets;
    @BindView(R.id.btnTimesMinus)
    View btnTimesMinus;
    @BindView(R.id.btnTimesPlus)
    View btnTimesPlus;
    @BindView(R.id.editCopyTimes)
    EditText editCopyTimes;
    @BindView(R.id.switchCopyImg)
    Switch switchCopyImg;
    @BindView(R.id.tvTotalTip)
    TextView tvTotalTip;
    @BindView(R.id.editAssetsLayAdd)
    EditText editAssetsLayAdd;
    @BindView(R.id.editAssetsDept)
    EditText editAssetsDept;
    @BindView(R.id.btnPickAssetsDept)
    View btnPickAssetsDept;
    @BindView(R.id.editGroundManageDept)
    EditText editGroundManageDept;
    @BindView(R.id.btnPickManageDept)
    View btnPickManageDept;
    @BindView(R.id.editCustodian)
    EditText editCustodian;
    @BindView(R.id.btnPickCustodian)
    View btnPickCustodian;
    @BindView(R.id.editAssetsUser)
    EditText editAssetsUser;
    @BindView(R.id.btnPickAssetsUser)
    View btnPickAssetsUser;
    @BindView(R.id.btnStartCopy)
    View btnStartCopy;

    private SelectedAssetAdapter selectedAdapter;
    private final List<Assets> selectedAssets = new ArrayList<>();

    /**
     * 管理部门编码（Accounts.BMBH），选中部门时同步记录，复制时写入 GROUNDMANAGEDEPT
     */
    private String groundManageDeptCode = "";
    private boolean copying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_copy_options);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("批量复制");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        selectedAdapter = new SelectedAssetAdapter(mContext);
        selectedAdapter.setData(selectedAssets);
        selectedAdapter.setOnRemoveClickListener(this::removeSelectedAsset);
        listSelectedAssets.setAdapter(selectedAdapter);

        btnSelectAssets.setOnClickListener(v -> openSelectPage());
        btnTimesMinus.setOnClickListener(v -> changeTimes(-1));
        btnTimesPlus.setOnClickListener(v -> changeTimes(1));
        editCopyTimes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateTotalTip();
            }
        });
        switchCopyImg.setOnCheckedChangeListener((buttonView, isChecked) -> updateTotalTip());
        btnStartCopy.setOnClickListener(v -> startCopy());

        setupBatchFieldPickers();
        updateSummary();
    }

    /**
     * 四个批量字段的选择入口：
     * 使用部门/管理人/使用人支持手动输入 + 列表选择，
     * 管理部门为字典数据，只能选择，选中后同步记录部门编号 BMBH
     */
    private void setupBatchFieldPickers() {
        btnPickAssetsDept.setOnClickListener(v ->
                CommonSelectActivity.pickAssetsDept(this, REQ_ASSETS_DEPT, textOf(editAssetsDept)));

        editGroundManageDept.setOnClickListener(v -> pickManageDept());
        btnPickManageDept.setOnClickListener(v -> pickManageDept());

        btnPickCustodian.setOnClickListener(v ->
                CommonSelectActivity.pickUser(this, REQ_CUSTODIAN, textOf(editCustodian)));

        btnPickAssetsUser.setOnClickListener(v ->
                CommonSelectActivity.pickUser(this, REQ_ASSETS_USER, textOf(editAssetsUser)));
    }

    private void pickManageDept() {
        CommonSelectActivity.pickManageDept(this, REQ_MANAGE_DEPT, textOf(editGroundManageDept));
    }

    // ==================== 选择资产 ====================

    private void openSelectPage() {
        Intent intent = new Intent(mContext, BatchCopySelectActivity.class);
        long[] ids = new long[selectedAssets.size()];
        for (int i = 0; i < selectedAssets.size(); i++) {
            ids[i] = selectedAssets.get(i).getId() == null ? 0 : selectedAssets.get(i).getId();
        }
        intent.putExtra(EXTRA_SELECTED_IDS, ids);
        startActivityForResult(intent, REQ_SELECT_ASSETS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQ_SELECT_ASSETS) {
            loadSelectedAssets(data == null ? null : data.getLongArrayExtra(EXTRA_SELECTED_IDS));
            return;
        }
        SelectItem item = CommonSelectActivity.readResult(data);
        if (item == null) {
            return;
        }
        switch (requestCode) {
            case REQ_ASSETS_DEPT:
                editAssetsDept.setText(item.getName());
                break;
            case REQ_MANAGE_DEPT:
                editGroundManageDept.setText(item.getName());
                groundManageDeptCode = item.getCode();
                break;
            case REQ_CUSTODIAN:
                editCustodian.setText(item.getName());
                break;
            case REQ_ASSETS_USER:
                editAssetsUser.setText(item.getName());
                break;
            default:
                break;
        }
    }

    private void loadSelectedAssets(long[] ids) {
        if (ids == null || ids.length == 0) {
            selectedAssets.clear();
            selectedAdapter.notifyDataSetChanged();
            updateSummary();
            return;
        }
        final List<Long> idList = new ArrayList<>();
        for (long id : ids) {
            if (id > 0) {
                idList.add(id);
            }
        }
        AsyncManager.getInstance().submitWithCallback(() -> {
            StringBuilder where = new StringBuilder("Id IN (");
            String[] args = new String[idList.size()];
            for (int i = 0; i < idList.size(); i++) {
                where.append(i == 0 ? "?" : ",?");
                args[i] = String.valueOf(idList.get(i));
            }
            where.append(")");
            List<Assets> queried = Assets.find(Assets.class, where.toString(), args);
            // 按传入顺序返回
            List<Assets> ordered = new ArrayList<>();
            if (queried != null) {
                for (Long id : idList) {
                    for (Assets assets : queried) {
                        if (id.equals(assets.getId())) {
                            ordered.add(assets);
                            break;
                        }
                    }
                }
            }
            return ordered;
        }, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (result.isSuccess() && result.getData() != null) {
                selectedAssets.clear();
                selectedAssets.addAll(result.getData());
                selectedAdapter.notifyDataSetChanged();
                updateSummary();
            } else {
                showShortText("加载已选资产失败，请重试");
            }
        }));
    }

    private void removeSelectedAsset(Assets assets) {
        selectedAssets.remove(assets);
        selectedAdapter.notifyDataSetChanged();
        updateSummary();
    }

    // ==================== 数量与汇总 ====================

    private void changeTimes(int delta) {
        int current = parseTimes();
        int next = Math.max(1, Math.min(Const.BATCH_COPY_TIMES_MAX, current + delta));
        editCopyTimes.setText(String.valueOf(next));
        editCopyTimes.setSelection(editCopyTimes.getText().length());
        updateTotalTip();
    }

    private int parseTimes() {
        try {
            int value = Integer.parseInt(editCopyTimes.getText().toString().trim());
            return Math.max(1, Math.min(Const.BATCH_COPY_TIMES_MAX, value));
        } catch (Exception e) {
            return 1;
        }
    }

    private void updateSummary() {
        int count = selectedAssets.size();
        tvSelectedSummary.setText(count + " 条");
        boolean empty = count == 0;
        tvNoSelection.setVisibility(empty ? View.VISIBLE : View.GONE);
        listSelectedAssets.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateSelectedListHeight();
        updateTotalTip();
    }

    /**
     * 已选列表高度随条数变化，最多 3 行
     */
    private void updateSelectedListHeight() {
        int rows = Math.min(selectedAssets.size(), 3);
        int height = rows <= 0 ? 0 : QMUIDisplayHelper.dp2px(mContext, 56) * rows;
        listSelectedAssets.getLayoutParams().height = height;
        listSelectedAssets.requestLayout();
    }

    private void updateTotalTip() {
        int count = selectedAssets.size();
        int times = parseTimes();
        tvTotalTip.setText("已选 " + count + " 条 × 复制 " + times + " 次 = 共 " + (count * times) + " 条");
    }

    // ==================== 开始复制 ====================

    private void startCopy() {
        if (copying) {
            return;
        }
        if (selectedAssets.isEmpty()) {
            showShortText("请先选择要复制的资产");
            return;
        }
        int times = parseTimes();
        if (!editCopyTimes.getText().toString().trim().equals(String.valueOf(times))) {
            editCopyTimes.setText(String.valueOf(times));
        }
        int total = selectedAssets.size() * times;
        if (total > Const.BATCH_COPY_TOTAL_MAX) {
            showShortText("单次最多生成 " + Const.BATCH_COPY_TOTAL_MAX + " 条，请减少数量或分批复制");
            return;
        }
        String missingField = findMissingField();
        if (missingField != null) {
            showShortText("所选资产中有缺少【" + missingField + "】的数据，无法复制，请先补全资产信息");
            return;
        }
        doStartCopy(times);
    }

    /**
     * 复制前的基础数据校验。
     * <p>
     * 批量修改复制信息不再要求全部填写：哪个有值就覆盖哪个，留空的保持源资产原值，
     * 因此这里只校验那些无法在批量设置中补充、且复制后必须存在的基础字段。
     */
    private String findMissingField() {
        for (Assets assets : selectedAssets) {
            if (StringUtils.isNullOrEmpty(assets.getACCTSUITEID())) {
                return "账套";
            }
            if (StringUtils.isNullOrEmpty(assets.getASSETSTYPE())) {
                return "资产类别";
            }
            if (StringUtils.isNullOrEmpty(assets.getASSETSSORTCODE())) {
                return "资产分类";
            }
            if (StringUtils.isNullOrEmpty(assets.getASSETSNAME())) {
                return "资产名称";
            }
        }
        return null;
    }

    private void doStartCopy(int times) {
        BatchCopyOptions options = new BatchCopyOptions();
        options.setCopyTimes(times);
        options.setCopyImg(switchCopyImg.isChecked());
        options.setAssetsLayAdd(textOf(editAssetsLayAdd));
        options.setAssetsDept(textOf(editAssetsDept));
        // 管理部门写入的是部门编号 BMBH，不是显示名称
        options.setGroundManageDept(groundManageDeptCode);
        options.setCustodian(textOf(editCustodian));
        options.setAssetsUser(textOf(editAssetsUser));

        List<Long> sourceIds = new ArrayList<>();
        for (Assets assets : selectedAssets) {
            if (assets.getId() != null) {
                sourceIds.add(assets.getId());
            }
        }

        copying = true;
        btnStartCopy.setEnabled(false);
        showProgressDialog("", "正在生成复制数据");
        int total = sourceIds.size() * times;
        tvTotalTip.setText("正在生成 0/" + total);

        GenerateCopyAssetsTask task = new GenerateCopyAssetsTask(sourceIds, options,
                application.getDeviceNo(), application.getZcqcyCode(),
                DataConvert.FormatDate2HMS(),
                new BatchCopyCallback<CopyResult>() {
                    @Override
                    public void onProgress(int current, int total) {
                        mainHandler.post(() -> tvTotalTip.setText("正在生成 " + current + "/" + total));
                    }

                    @Override
                    public void onSuccess(CopyResult result) {
                    }

                    @Override
                    public void onError(String message) {
                    }
                });

        AsyncManager.getInstance().submitWithCallback(task, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            copying = false;
            btnStartCopy.setEnabled(true);
            hideProgressDialog();
            if (result.isSuccess() && result.getData() != null) {
                showSuccessDialog(result.getData());
            } else {
                tvTotalTip.setText("已选 " + selectedAssets.size() + " 条 × 复制 " + times + " 次");
                showShortText("生成失败，请重试");
            }
        }));
    }

    private void showSuccessDialog(CopyResult result) {
        String message = "已生成 " + result.getSuccessCount() + " 条待上传数据。\n\n"
                + "这些数据不会自动上传，请到首页执行【同步上传】提交。";
        if (result.getFailCount() > 0) {
            message = "成功 " + result.getSuccessCount() + " 条，失败 " + result.getFailCount() + " 条。\n\n"
                    + "已生成的数据不会自动上传，请到首页执行【同步上传】提交。";
        }
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("复制完成")
                .setMessage(message)
                .addAction("确定", (dialog, index) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private static String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
