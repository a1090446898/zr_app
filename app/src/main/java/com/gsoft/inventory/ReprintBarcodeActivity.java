package com.gsoft.inventory;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.gsoft.inventory.adapter.ReprintBarcodeAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.print_batch.BatchPrintConfig;
import com.gsoft.inventory.utils.print_batch.BatchPrintReport;
import com.gsoft.inventory.utils.print_batch.BatchPrintSession;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 补打条码页：展示需要打印条码标签(ISPRINTBARCODE=01)的资产，支持逐条补打与批量补打
 * <p>
 * 1. 卡片列表分页加载（每页100条，滚动到底部自动加载下一页），每张卡片显示资产名称、
 *    条码、管理部门（编号自动转名称）、使用人、存放地点，右侧"打印"按钮；
 * 2. 点击"打印"先弹确认框，确认后走打印公共流程（打印机未连接时自动跳转蓝牙设备页，
 *    连接成功返回后自动续打）；
 * 3. 打印发起后更新资产 ISPRINTBARCODE=02（已打印）、ISUPLOAD=02（标记待上传），
 *    上传字段数组（assets_fields）已包含ISPRINTBARCODE，联网同步时回写服务器
 *    （需服务端接口配合确认）；本次会话中该条按钮置灰"已打印"保留显示，重新加载后不再出现；
 * 4. 底部统计条实时展示待补打总数；无数据时显示空状态提示；
 * 5. 批量补打：顶部"批量打印"进入勾选模式（全选条+卡片勾选框+悬浮打印按钮），
 *    队列/连接检查/品牌分发复用PrintCompatActivity，本页只负责勾选UI与失败决策。
 */
public class ReprintBarcodeActivity extends PrintCompatActivity {

    /** 分页每页条数 */
    private static final int PAGE_SIZE = 100;
    /** 待补打条码查询条件：ISPRINTBARCODE=01（01需要打印条码，02已经打印过条码） */
    private static final String WHERE_REPRINT = "TRIM(ISPRINTBARCODE) = ?";
    private static final String[] WHERE_REPRINT_ARGS = new String[]{Const.NOT_PRINT};

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 待补打条码卡片列表 */
    @BindView(R.id.listReprint)
    ListView listReprint;
    /** 底部统计条：待补打总数 */
    @BindView(R.id.tvTotalCount)
    TextView tvTotalCount;
    /** 空状态提示 */
    @BindView(R.id.layoutEmpty)
    View layoutEmpty;
    /** 首屏/刷新加载中转圈提示 */
    @BindView(R.id.layoutLoading)
    View layoutLoading;

    /** 列表适配器 */
    private ReprintBarcodeAdapter reprintAdapter;
    /** 已加载数据（分页累加，与适配器共享同一列表实例） */
    private final List<Assets> dataList = new ArrayList<>();
    /** 待补打真实总数（count查询） */
    private long totalCount = 0;
    /** 是否正在加载（防止滚动重复触发分页） */
    private boolean loading = false;
    /** 加载代数：重新加载时自增，过期回调直接丢弃，避免分页与刷新相互覆盖 */
    private int loadToken = 0;
    /**
     * 本次会话已打印条数：打印后数据库已置02不再匹配查询，而本地列表保留置灰项，
     * 分页offset需减去该数量补偿，避免翻页跳过数据
     */
    private int printedCount = 0;
    /** 列表尾部加载状态提示 */
    private TextView footerView;
    /** 顶部右侧"批量打印/取消打印"按钮 */
    private TextView tvBatchPrintMode;
    /** 批量补打全选条 */
    @BindView(R.id.layoutSelectAll)
    View layoutSelectAll;
    /** 批量补打全选框 */
    @BindView(R.id.cbSelectAll)
    CheckBox cbSelectAll;
    /** 批量补打悬浮打印按钮 */
    @BindView(R.id.btnBatchPrint)
    QMUIRoundButton btnBatchPrint;
    /** 是否处于批量补打模式 */
    private boolean batchPrintMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reprint_barcode);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("补打条码");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        tvBatchPrintMode = topBar.addRightTextButton("批量打印", R.id.topbar_right_about_button);
        tvBatchPrintMode.setOnClickListener(v -> {
            if (batchPrintMode) {
                confirmExitBatchPrintMode();
            } else {
                enterBatchPrintMode();
            }
        });
        btnBatchPrint.setOnClickListener(v -> printSelectedAssets());
        cbSelectAll.setOnCheckedChangeListener(selectAllListener);

        // 列表尾部加载状态提示（ListView的footer不参与数据项，需在setAdapter之前添加）
        footerView = new TextView(mContext);
        footerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                QMUIDisplayHelper.dp2px(mContext, 40)));
        footerView.setGravity(Gravity.CENTER);
        footerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        footerView.setTextColor(0xFF999999);
        listReprint.addFooterView(footerView);

        reprintAdapter = new ReprintBarcodeAdapter(mContext);
        Accounts loginAccount = application.getLoginAccount();
        // 账套编号：管理部门字段存单位编号(DWBH)时，卡片展示按账套转换为单位名称(DWMC)
        reprintAdapter.setAccountZtbh(loginAccount == null ? "" : loginAccount.getZTBH());
        reprintAdapter.setData(dataList);
        reprintAdapter.setOnPrintClickListener(this::showPrintConfirm);
        reprintAdapter.setOnSelectionChangedListener(new ReprintBarcodeAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectionSummary();
            }

            @Override
            public void onSelectLimitReached(int maxCount) {
                showShortText("单次最多打印" + maxCount + "条，请分批勾选");
            }
        });
        listReprint.setAdapter(reprintAdapter);

        // 上拉分页：滚动到底部自动加载下一页（加载中/已全部加载时由tryLoadNextPage内部拦截）
        listReprint.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    tryLoadNextPage();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount >= reprintAdapter.getCount()) {
                    tryLoadNextPage();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 批量模式下不重载：重载会清空列表与已勾选状态
        if (batchPrintMode) {
            return;
        }
        // 首次进入/从蓝牙连接页返回：回到第一页重新加载，保证数据最新
        reload();
    }

    /**
     * 重新加载：回到第一页，查询待补打真实总数与第一页数据
     * <p>
     * 数据库查询在后台线程执行（AsyncManager），主线程刷新列表；
     * loadToken自增使在途的翻页/刷新回调作废，避免数据相互覆盖
     */
    private void reload() {
        final int token = ++loadToken;
        loading = true;
        // 整表重载后不再有本会话置灰项，offset补偿归零
        printedCount = 0;
        layoutLoading.setVisibility(View.VISIBLE);
        btnBatchPrint.setEnabled(false);
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(() -> {
            PageData page = new PageData();
            page.totalCount = countTotal();
            page.assets = queryPage(0);
            return page;
        }, result -> mainHandler.post(() -> {
            // 过期回调（已被新一轮加载取代）直接丢弃，避免误隐藏新加载的转圈提示
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            layoutLoading.setVisibility(View.GONE);
            btnBatchPrint.setEnabled(true);
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                PageData page = result.getData();
                totalCount = page.totalCount;
                dataList.clear();
                dataList.addAll(page.assets);
                reprintAdapter.notifyDataSetChanged();
                updateTotalText();
                updateEmpty();
            }
            updateFooter();
        }));
    }

    /**
     * 上拉加载下一页：offset按printedCount补偿
     * （本会话已打印项在数据库中已被过滤，本地列表仍保留置灰显示，翻页偏移需扣除）
     */
    private void tryLoadNextPage() {
        if (loading || dataList.size() >= totalCount) {
            return;
        }
        final int token = loadToken;
        final int offset = dataList.size() - printedCount;
        loading = true;
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(() -> queryPage(offset), result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                dataList.addAll(result.getData());
                reprintAdapter.notifyDataSetChanged();
            }
            updateFooter();
        }));
    }

    /** 查询指定偏移的一页待补打数据（后台线程调用），Id升序与统计页查询口径一致 */
    private List<Assets> queryPage(int offset) {
        return Assets.find(Assets.class, WHERE_REPRINT, WHERE_REPRINT_ARGS, null, "Id ASC", offset + ", " + PAGE_SIZE);
    }

    /** 查询待补打真实总数（后台线程调用），底部统计条与加载完成判断共用 */
    private long countTotal() {
        return Assets.count(Assets.class, WHERE_REPRINT, WHERE_REPRINT_ARGS, null, "Id ASC", "");
    }

    /** 点击"打印"先弹确认框防误触，确认后执行打印 */
    private void showPrintConfirm(Assets assets) {
        String name = StringUtils.isNullOrEmpty(assets.getASSETSNAME()) ? "未命名资产" : assets.getASSETSNAME();
        String barcode = assets.getBARCODEID() == null ? "" : assets.getBARCODEID();
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("补打条码")
                .setMessage("是否打印该资产的条码标签？\n资产：" + name + "\n条码：" + barcode)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("打印", (dialog, index) -> {
                    dialog.dismiss();
                    doPrint(assets);
                })
                .show();
    }

    /**
     * 执行打印：走打印公共流程（打印机未连接时自动跳蓝牙设备页，连接成功返回后自动续打），
     * 打印发起后更新打印状态
     */
    private void doPrint(Assets assets) {
        requestPrintAssets(assets, () -> onPrintSubmitted(assets));
    }

    /**
     * 打印发起后处理：
     * 1. 调用公共markBarcodePrinted标记已打印：ISPRINTBARCODE置02（已打印）、ISUPLOAD置02（标记待上传，联网后随资产同步回服务器）；
     * 2. 本地列表该条按钮置灰"已打印"保留显示（不打断当前浏览位置），底部统计数减一
     */
    private void onPrintSubmitted(Assets assets) {
        // 打印后标记已打印（ISPRINTBARCODE置02、ISUPLOAD置02标记待上传）
        markBarcodePrinted(assets);
        totalCount--;
        if (totalCount < 0) {
            totalCount = 0;
        }
        printedCount++;
        reprintAdapter.notifyDataSetChanged();
        updateTotalText();
        updateFooter();
        showShortText("已发起打印");
    }

    /** 底部统计条：批量模式显示已选条数，否则显示待补打总数 */
    private void updateTotalText() {
        if (batchPrintMode) {
            int selectedCount = reprintAdapter == null ? 0 : reprintAdapter.getSelectedCount();
            tvTotalCount.setText(selectedCount == 0 ? "请勾选要补打的条码" : "已勾选 " + selectedCount + " 条");
            return;
        }
        tvTotalCount.setText("共 " + totalCount + " 条待补打");
    }

    // ==================== 批量补打 ====================

    /** 全选：勾选当前已加载的未打印数据（上限内），取消则清空 */
    private final CompoundButton.OnCheckedChangeListener selectAllListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        reprintAdapter.selectAll();
                    } else {
                        reprintAdapter.clearSelection();
                    }
                }
            };

    /** 进入批量补打模式：显示全选条、卡片勾选框与悬浮打印按钮 */
    private void enterBatchPrintMode() {
        batchPrintMode = true;
        tvBatchPrintMode.setText("取消打印");
        layoutSelectAll.setVisibility(View.VISIBLE);
        btnBatchPrint.setVisibility(View.VISIBLE);
        reprintAdapter.setSelectMode(true);
        updateSelectionSummary();
    }

    /** 退出批量补打模式：隐藏勾选相关控件，恢复底部待补打总数 */
    private void exitBatchPrintMode() {
        batchPrintMode = false;
        tvBatchPrintMode.setText("批量打印");
        layoutSelectAll.setVisibility(View.GONE);
        btnBatchPrint.setVisibility(View.GONE);
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(false);
        cbSelectAll.setOnCheckedChangeListener(selectAllListener);
        reprintAdapter.setSelectMode(false);
        updateTotalText();
    }

    /** 退出前确认：打印进行中需二次确认，避免误终止造成只打了一部分 */
    private void confirmExitBatchPrintMode() {
        if (!isBatchPrintRunning()) {
            exitBatchPrintMode();
            return;
        }
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("批量打印进行中")
                .setMessage("当前仍有打印任务未完成，确定终止并退出批量打印吗？")
                .addAction("继续打印", (dialog, index) -> dialog.dismiss())
                .addAction("终止并退出", (dialog, index) -> {
                    dialog.dismiss();
                    cancelBatchPrint();
                    hideProgressDialog();
                    exitBatchPrintMode();
                })
                .show();
    }

    /** 悬浮按钮：批量补打已勾选资产（数量上限、连接检查、品牌分发由基类统一处理） */
    private void printSelectedAssets() {
        if (isBatchPrintRunning()) {
            showShortText("批量打印进行中，请稍候");
            return;
        }
        List<Assets> selectedAssets = reprintAdapter.getSelectedAssets();
        if (selectedAssets.isEmpty()) {
            showShortText("未勾选打印数据");
            return;
        }
        if (selectedAssets.size() > BatchPrintConfig.MAX_BATCH_SIZE) {
            showShortText("单次最多打印" + BatchPrintConfig.MAX_BATCH_SIZE + "条，请分批勾选");
            return;
        }
        Accounts loginAccount = application.getLoginAccount();
        requestBatchPrintAssets(loginAccount == null ? "" : loginAccount.getZTMC(),
                selectedAssets, batchPrintListener);
    }

    /** 同步底部统计与全选框状态：setChecked前先解监听，避免与全选形成回调环 */
    private void updateSelectionSummary() {
        updateTotalText();
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(reprintAdapter.isAllSelected());
        cbSelectAll.setOnCheckedChangeListener(selectAllListener);
    }

    /** 批量补打回调：进度框、失败决策弹框、结束汇总 */
    private final BatchPrintSession.Listener batchPrintListener = new BatchPrintSession.Listener() {
        @Override
        public void onProgress(int finishedCount, int total, Assets current) {
            showProgressDialog("", "正在打印 " + (finishedCount + 1) + "/" + total);
        }

        @Override
        public void onItemFailed(Assets assets, int position, int total, String message) {
            hideProgressDialog();
            showItemFailedDialog(assets, position, total, message);
        }

        @Override
        public void onFinished(BatchPrintReport report) {
            hideProgressDialog();
            int printedSize = report.getPrintedIds().size();
            // 已打印数据在库中不再匹配补打条件，分页offset需补偿，否则继续下滑会跳过数据
            printedCount += printedSize;
            totalCount = Math.max(0, totalCount - printedSize);
            updateTotalText();
            updateFooter();
            updateSelectionSummary();
            // 标记已打印在后台线程写库，延时刷新卡片"已打印"置灰态
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    reprintAdapter.notifyDataSetChanged();
                }
            }, 300);
            showShortText(report.summary());
        }
    };

    /** 单条失败弹框：交由用户选择立即终止或跳过继续 */
    private void showItemFailedDialog(Assets assets, int position, int total, String message) {
        StringBuilder content = new StringBuilder();
        if (assets != null && !StringUtils.isNullOrEmpty(assets.getBARCODEID())) {
            content.append("条码：").append(assets.getBARCODEID()).append("\n");
        }
        content.append(StringUtils.isNullOrEmpty(message) ? "打印失败" : message);

        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("第 " + position + "/" + total + " 条打印失败")
                .setMessage(content.toString())
                .addAction("立即终止", (dialog, index) -> {
                    dialog.dismiss();
                    cancelBatchPrint();
                    updateSelectionSummary();
                })
                .addAction("继续打印", (dialog, index) -> {
                    dialog.dismiss();
                    resumeBatchPrint();
                })
                .show();
    }

    /** 空状态提示：无待补打数据时显示 */
    private void updateEmpty() {
        layoutEmpty.setVisibility(dataList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 刷新列表尾部加载状态提示 */
    private void updateFooter() {
        if (footerView == null) {
            return;
        }
        if (loading) {
            footerView.setText("正在加载…");
        } else if (dataList.isEmpty()) {
            footerView.setText("");
        } else if (dataList.size() >= totalCount) {
            footerView.setText("已全部加载");
        } else {
            footerView.setText("上拉加载更多");
        }
    }

    /** 一次加载的数据载体：真实总数 + 当前页资产 */
    private static class PageData {
        long totalCount;
        List<Assets> assets;
    }
}