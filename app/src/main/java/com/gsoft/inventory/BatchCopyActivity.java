package com.gsoft.inventory;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gsoft.inventory.adapter.BatchCopyCardAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.data.CopyListResult;
import com.gsoft.inventory.service.batch_copy.ClearCopyFlagTask;
import com.gsoft.inventory.service.batch_copy.MarkDeleteCopyAssetsTask;
import com.gsoft.inventory.service.batch_copy.QueryCopyAssetsTask;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SysDefine;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButtonDrawable;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 批量复制列表页
 * <p>
 * 展示当前账套由批量复制生成的数据（IS_COPY_DATA=1），按 Id 倒序，最新复制的排在最前；
 * 复制操作跳转 {@link BatchCopyOptionsActivity} 完成，本页不做任何上传；
 * 删除为软删除：只把 REMARKS 置为"待删除"，由首页同步上传交给服务端处理。
 */
public class BatchCopyActivity extends BaseCompatActivity {

    private static final String TAG = "BatchCopyActivity";

    /** 删除模式下底部主按钮的底色，与原红色删除按钮保持一致 */
    private static final int COLOR_DANGER = 0xFFE54D42;

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.layoutSelectAll)
    View layoutSelectAll;
    @BindView(R.id.cbSelectAll)
    CheckBox cbSelectAll;
    @BindView(R.id.tvSelectedCount)
    TextView tvSelectedCount;
    @BindView(R.id.listCopy)
    ListView listCopy;
    @BindView(R.id.layoutEmpty)
    View layoutEmpty;
    @BindView(R.id.layoutLoading)
    View layoutLoading;
    /** 底部主按钮：普通模式为"复制"，删除模式复用同一个按钮 */
    @BindView(R.id.btnCopy)
    QMUIRoundButton btnCopy;

    /** 标题右侧：清空列表 */
    private TextView tvClear;
    /** 标题右侧：批量删除（删除模式下标为"取消"） */
    private TextView tvDeleteMode;
    /** 列表尾部加载提示 */
    private View footerView;

    private BatchCopyCardAdapter adapter;
    private final List<Assets> dataList = new ArrayList<>();

    private String ztbH;
    private long totalCount;
    private int loadToken;
    private boolean loading;
    private boolean deleteMode;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_copy);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        ztbH = application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH();
        topBar.setTitle("批量复制");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        tvClear = topBar.addRightTextButton("清空列表", R.id.topbar_right_about_button);
        tvClear.setOnClickListener(v -> confirmClear());
        tvDeleteMode = topBar.addRightTextButton("批量删除", R.id.topbar_right_query_button);
        tvDeleteMode.setOnClickListener(v -> toggleDeleteMode());

        tvSelectedCount.setText("");
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });

        footerView = new TextView(mContext);
        footerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                QMUIDisplayHelper.dp2px(mContext, 40)));
        ((TextView) footerView).setGravity(Gravity.CENTER);
        ((TextView) footerView).setTextSize(13);
        ((TextView) footerView).setTextColor(0xFF999999);
        listCopy.addFooterView(footerView);

        adapter = new BatchCopyCardAdapter(mContext);
        adapter.setAccountZtbh(ztbH);
        adapter.setData(dataList);
        adapter.setOnItemClickListener(this::openAssetsDetail);
        adapter.setOnSelectionChangedListener(new BatchCopyCardAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectedCount();
            }

            @Override
            public void onSelectLimitReached(int maxCount) {
                showShortText("单次最多删除" + maxCount + "条，请分批操作");
            }
        });
        listCopy.setAdapter(adapter);

        listCopy.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    tryLoadNextPage();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount >= adapter.getCount()) {
                    tryLoadNextPage();
                }
            }
        });

        btnCopy.setOnClickListener(v -> {
            if (deleteMode) {
                confirmDelete();
            } else {
                openCopyOptions();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 删除模式下不重载：重载会清空列表与已勾选状态
        if (deleteMode) {
            return;
        }
        reload();
    }

    // ==================== 列表加载 ====================

    private void reload() {
        final int token = ++loadToken;
        loading = true;
        layoutLoading.setVisibility(View.VISIBLE);
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(
                new QueryCopyAssetsTask(ztbH, 0, Const.BATCH_COPY_PAGE_SIZE),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || token != loadToken) {
                        return;
                    }
                    layoutLoading.setVisibility(View.GONE);
                    loading = false;
                    if (result.isSuccess() && result.getData() != null) {
                        CopyListResult page = result.getData();
                        totalCount = page.getTotalCount();
                        dataList.clear();
                        dataList.addAll(page.getAssets());
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "加载批量复制列表失败：ztbH=" + ztbH, result.getError());
                        showShortText("加载失败，请重试");
                    }
                    updateEmpty();
                    updateTopActions();
                    updateFooter();
                }));
    }

    private void tryLoadNextPage() {
        if (deleteMode || loading || dataList.size() >= totalCount) {
            return;
        }
        final int token = loadToken;
        final int offset = dataList.size();
        loading = true;
        updateFooter();
        AsyncManager.getInstance().submitWithCallback(
                new QueryCopyAssetsTask(ztbH, offset, Const.BATCH_COPY_PAGE_SIZE),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || token != loadToken) {
                        return;
                    }
                    loading = false;
                    if (result.isSuccess() && result.getData() != null) {
                        dataList.addAll(result.getData().getAssets());
                        adapter.notifyDataSetChanged();
                    } else if (!result.isSuccess()) {
                        Log.e(TAG, "加载下一页失败：offset=" + offset, result.getError());
                    }
                    updateFooter();
                }));
    }

    private void updateFooter() {
        if (footerView == null) {
            return;
        }
        TextView footer = (TextView) footerView;
        if (loading) {
            footer.setText("正在加载…");
        } else if (dataList.isEmpty()) {
            footer.setText("");
        } else if (dataList.size() >= totalCount) {
            footer.setText("已加载全部 " + totalCount + " 条");
        } else {
            footer.setText("上拉加载更多");
        }
    }

    private void updateEmpty() {
        boolean empty = dataList.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listCopy.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateTopActions() {
        int visibility = totalCount > 0 ? View.VISIBLE : View.GONE;
        tvClear.setVisibility(visibility);
        tvDeleteMode.setVisibility(visibility);
    }

    private void updateSelectedCount() {
        int count = adapter == null ? 0 : adapter.getSelectedCount();
        tvSelectedCount.setText(count > 0 ? "已选 " + count + " 条" : "");
        // 底部按钮文案跟随模式：普通模式"复制"，删除模式"删除(N)"
        if (deleteMode) {
            btnCopy.setText(count > 0 ? "删除(" + count + ")" : "删除");
        } else {
            btnCopy.setText("复制");
        }
        if (cbSelectAll.isChecked() != (adapter != null && adapter.isAllSelected())) {
            cbSelectAll.setOnCheckedChangeListener(null);
            cbSelectAll.setChecked(adapter != null && adapter.isAllSelected());
            cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    adapter.selectAll();
                } else {
                    adapter.clearSelection();
                }
            });
        }
    }

    // ==================== 页面跳转 ====================

    private void openAssetsDetail(Assets assets) {
        if (assets == null || assets.getId() == null) {
            return;
        }
        Intent intent = new Intent(mContext, AssetsViewActivity.class);
        intent.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId().longValue());
        startActivity(intent);
    }

    private void openCopyOptions() {
        startActivity(new Intent(mContext, BatchCopyOptionsActivity.class));
    }

    // ==================== 清空列表 ====================

    private void confirmClear() {
        if (totalCount <= 0) {
            showShortText("列表已为空");
            return;
        }
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("清空列表")
                .setMessage("将清空当前账套中 " + totalCount + " 条复制记录的标识。\n\n"
                        + "注意：这只是从批量复制列表中移除，不会删除资产数据，"
                        + "这些资产在资产查询中依然可以查到；待删除的数据仍会继续参与同步。")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("清空", (dialog, index) -> {
                    dialog.dismiss();
                    doClearCopyFlag();
                })
                .show();
    }

    private void doClearCopyFlag() {
        layoutLoading.setVisibility(View.VISIBLE);
        AsyncManager.getInstance().submitWithCallback(new ClearCopyFlagTask(ztbH),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    layoutLoading.setVisibility(View.GONE);
                    if (result.isSuccess() && result.getData() != null) {
                        showShortText("已清空 " + result.getData() + " 条复制标识");
                        reload();
                    } else {
                        Log.e(TAG, "清空复制标识失败", result.getError());
                        showShortText("清空失败，请重试");
                    }
                }));
    }

    // ==================== 批量删除 ====================

    private void toggleDeleteMode() {
        deleteMode = !deleteMode;
        adapter.setSelectMode(deleteMode);
        layoutSelectAll.setVisibility(deleteMode ? View.VISIBLE : View.GONE);
        tvDeleteMode.setText(deleteMode ? "取消" : "批量删除");
        tvClear.setVisibility(deleteMode ? View.GONE : View.VISIBLE);
        updateMainActionButton();
        cbSelectAll.setChecked(false);
        updateSelectedCount();
        if (!deleteMode) {
            reload();
        }
    }

    /**
     * 底部主按钮样式：普通模式蓝色"复制"，删除模式红色"删除"。
     * <p>
     * 两种模式复用同一个按钮：原先复制/删除是叠在 FrameLayout 底部同一位置的两个按钮，
     * 删除模式下很容易看成两个按钮重叠。
     */
    private void updateMainActionButton() {
        Drawable background = btnCopy.getBackground();
        if (background instanceof QMUIRoundButtonDrawable) {
            int color = deleteMode
                    ? COLOR_DANGER
                    : ContextCompat.getColor(mContext, R.color.primary_color);
            ((QMUIRoundButtonDrawable) background).setBgData(ColorStateList.valueOf(color));
        }
    }

    private void confirmDelete() {
        final List<Assets> selected = adapter.getSelectedAssets();
        if (selected.isEmpty()) {
            showShortText("请先勾选要删除的数据");
            return;
        }
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("批量删除")
                .setMessage("已选择 " + selected.size() + " 条数据。\n\n"
                        + "删除请求需同步上传后由服务端处理，本地不会立即删除；"
                        + "请删除后到首页执行【同步上传】。")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("提交", (dialog, index) -> {
                    dialog.dismiss();
                    doMarkDelete(selected);
                })
                .show();
    }

    private void doMarkDelete(List<Assets> selected) {
        List<Long> ids = new ArrayList<>();
        for (Assets assets : selected) {
            if (assets.getId() != null) {
                ids.add(assets.getId());
            }
        }
        layoutLoading.setVisibility(View.VISIBLE);
        AsyncManager.getInstance().submitWithCallback(new MarkDeleteCopyAssetsTask(ids),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    layoutLoading.setVisibility(View.GONE);
                    if (result.isSuccess() && result.getData() != null) {
                        showShortText("已提交 " + result.getData() + " 条删除请求，请到首页同步上传");
                        toggleDeleteMode();
                    } else {
                        Log.e(TAG, "提交删除请求失败：count=" + ids.size(), result.getError());
                        showShortText("提交失败，请重试");
                    }
                }));
    }
}