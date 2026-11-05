package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.gsoft.inventory.adapter.BatchCopySourceAdapter;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.data.SelectItem;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 选择资产页：按条件查询当前账套未盘资产，多选后回传表单页
 * <p>
 * 选中态按资产Id记录，跨搜索/分页不丢失；单次最多加载 {@link Const#BATCH_COPY_QUERY_LIMIT} 条。
 */
public class BatchCopySelectActivity extends BaseCompatActivity {

    private static final String TAG = "BatchCopySelectActivity";

    private static final int PAGE_SIZE = 50;
    /** 管理部门筛选：跳公共选择页，回传部门编号 BMBH */
    private static final int REQ_MANAGE_DEPT_FILTER = 2001;

    @BindView(R.id.lyDrawLayout)
    DrawerLayout lyDrawLayout;
    /** 右侧查询条件面板 */
    @BindView(R.id.lyQuery)
    View lyQuery;
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.querybar)
    QMUITopBar queryBar;
    @BindView(R.id.editASSETSNAME)
    EditText editASSETSNAME;
    @BindView(R.id.editBARCODEID)
    EditText editBARCODEID;
    @BindView(R.id.editASSETSDEPT)
    EditText editASSETSDEPT;
    @BindView(R.id.editGROUNDMANAGEDEPT)
    EditText editGROUNDMANAGEDEPT;
    @BindView(R.id.btnPickManageDept)
    TextView btnPickManageDept;
    @BindView(R.id.editASSETSUSER)
    EditText editASSETSUSER;
    @BindView(R.id.editASSETSLAYADD)
    EditText editASSETSLAYADD;
    @BindView(R.id.btnReset)
    QMUIRoundButton btnReset;
    @BindView(R.id.btnSearch)
    QMUIRoundButton btnSearch;
    @BindView(R.id.cbSelectAll)
    CheckBox cbSelectAll;
    @BindView(R.id.tvSelectedCount)
    TextView tvSelectedCount;
    @BindView(R.id.listSource)
    ListView listSource;
    @BindView(R.id.layoutEmpty)
    View layoutEmpty;
    @BindView(R.id.layoutLoading)
    View layoutLoading;

    private TextView tvConfirm;
    private BatchCopySourceAdapter adapter;
    private final List<Assets> dataList = new ArrayList<>();
    private View footerView;

    private String ztbH;
    /** 管理部门筛选条件：部门编号 BMBH */
    private String groundManageDeptCode = "";
    /** 管理部门筛选条件：部门名称，用于兼容历史存名称的数据 */
    private String groundManageDeptName = "";

    private String whereClause = " Id > 0 ";
    private String[] whereArgs = new String[0];
    private long totalCount;
    private int loadToken;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_copy_select);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        ztbH = application.getLoginAccount() == null ? "" : application.getLoginAccount().getZTBH();
        topBar.setTitle("选择资产");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        TextView tvSearch = topBar.addRightTextButton("搜索", R.id.topbar_right_about_button);
        tvSearch.setOnClickListener(v -> openSearchPanel());
        tvConfirm = topBar.addRightTextButton("确定", R.id.topbar_right_query_button);
        tvConfirm.setOnClickListener(v -> confirmSelection());

        // 查询条件面板：从右侧滑出，左侧返回键/取消仅收起面板，不退出页面
        queryBar.setTitle("查询条件");
        queryBar.addLeftBackImageButton().setOnClickListener(v -> closeSearchPanel());

        footerView = new TextView(mContext);
        footerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                QMUIDisplayHelper.dp2px(mContext, 40)));
        ((TextView) footerView).setGravity(Gravity.CENTER);
        ((TextView) footerView).setTextSize(13);
        ((TextView) footerView).setTextColor(0xFF999999);
        listSource.addFooterView(footerView);

        adapter = new BatchCopySourceAdapter(mContext);
        adapter.setData(dataList);
        adapter.setMaxSelectCount(Const.BATCH_COPY_QUERY_LIMIT);
        adapter.setSelectionListener(new BatchCopySourceAdapter.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectedCount();
            }

            @Override
            public void onSelectLimitReached(int maxCount) {
                showShortText("单次最多选择 " + maxCount + " 条，请分批复制");
            }
        });
        listSource.setAdapter(adapter);

        listSource.setOnScrollListener(new AbsListView.OnScrollListener() {
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

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });

        btnSearch.setOnClickListener(v -> {
            closeSearchPanel();
            reload();
        });
        btnReset.setOnClickListener(v -> {
            clearSearchFields();
            closeSearchPanel();
            reload();
        });
        initGroundManageDeptPicker();
        initSelectedFromIntent();
        buildQuery();
        reload();
    }

    /**
     * 管理部门筛选：跳公共选择页（部门字典），回传部门编号 BMBH
     */
    private void initGroundManageDeptPicker() {
        editGROUNDMANAGEDEPT.setFocusable(false);
        editGROUNDMANAGEDEPT.setFocusableInTouchMode(false);
        editGROUNDMANAGEDEPT.setClickable(true);
        View.OnClickListener picker = v -> CommonSelectActivity.pickManageDept(
                this, REQ_MANAGE_DEPT_FILTER, editGROUNDMANAGEDEPT.getText().toString().trim());
        editGROUNDMANAGEDEPT.setOnClickListener(picker);
        btnPickManageDept.setOnClickListener(picker);
    }

    @Override
    public void onBackPressed() {
        // 面板打开时返回键先收起面板，避免误退出整个选择页
        if (lyDrawLayout.isDrawerOpen(lyQuery)) {
            closeSearchPanel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQ_MANAGE_DEPT_FILTER) {
            return;
        }
        SelectItem item = CommonSelectActivity.readResult(data);
        if (item == null) {
            return;
        }
        editGROUNDMANAGEDEPT.setText(item.getName());
        groundManageDeptCode = item.getCode();
        groundManageDeptName = item.getName();
        // 只回填条件，不立即查询：面板保持打开，用户可继续设置其它条件后点"搜索"
    }

    private void initSelectedFromIntent() {
        long[] ids = getIntent().getLongArrayExtra(BatchCopyOptionsActivity.EXTRA_SELECTED_IDS);
        List<Long> idList = new ArrayList<>();
        if (ids != null) {
            for (long id : ids) {
                if (id > 0) {
                    idList.add(id);
                }
            }
        }
        adapter.setSelectedIds(idList);
        updateSelectedCount();
    }

    /** 从右侧滑出查询条件面板 */
    private void openSearchPanel() {
        lyDrawLayout.openDrawer(lyQuery);
    }

    /** 收起查询条件面板 */
    private void closeSearchPanel() {
        if (lyDrawLayout.isDrawerOpen(lyQuery)) {
            lyDrawLayout.closeDrawer(lyQuery);
        }
    }

    /**
     * 清空查询条件。管理部门除输入框外还要清掉编码，
     * 否则空名称配上旧编码仍会参与 groundManageDeptCode 的匹配。
     */
    private void clearSearchFields() {
        editASSETSNAME.setText("");
        editBARCODEID.setText("");
        editASSETSDEPT.setText("");
        editGROUNDMANAGEDEPT.setText("");
        groundManageDeptCode = "";
        groundManageDeptName = "";
        editASSETSUSER.setText("");
        editASSETSLAYADD.setText("");
    }

    // ==================== 查询 ====================

    private void buildQuery() {
        StringBuilder where = new StringBuilder(" Id > 0 ");
        List<String> params = new ArrayList<>();
        where.append(" and ACCTSUITEID=? ");
        params.add(ztbH);
        where.append(" and IFNULL(PDZT,'') <> '01' ");

        if (!isEmpty(editASSETSNAME)) {
            where.append(" and ASSETSNAME LIKE ? ");
            params.add("%" + editASSETSNAME.getText().toString().trim() + "%");
        }
        if (!isEmpty(editBARCODEID)) {
            where.append(" and BARCODEID LIKE ? ");
            params.add("%" + editBARCODEID.getText().toString().trim() + "%");
        }
        if (!isEmpty(editASSETSDEPT)) {
            where.append(" and ASSETSDEPT LIKE ? ");
            params.add("%" + editASSETSDEPT.getText().toString().trim() + "%");
        }
        if (!isEmpty(editGROUNDMANAGEDEPT) && !StringUtils.isNullOrEmpty(groundManageDeptCode)) {
            // 管理部门历史数据存在三种取值：部门编号 BMBH、单位编号 DWBH、中文名称，三者一并匹配
            String danweiCode = AccountHelper.getDanweiCode(groundManageDeptName);
            where.append(" and (GROUNDMANAGEDEPT = ? or GROUNDMANAGEDEPT = ? or GROUNDMANAGEDEPT = ?) ");
            params.add(groundManageDeptCode);
            params.add(groundManageDeptName);
            params.add(danweiCode);
        }
        if (!isEmpty(editASSETSUSER)) {
            where.append(" and ASSETSUSER LIKE ? ");
            params.add("%" + editASSETSUSER.getText().toString().trim() + "%");
        }
        if (!isEmpty(editASSETSLAYADD)) {
            where.append(" and ASSETSLAYADD LIKE ? ");
            params.add("%" + editASSETSLAYADD.getText().toString().trim() + "%");
        }

        whereClause = where.toString();
        whereArgs = params.toArray(new String[0]);
    }

    private static boolean isEmpty(EditText editText) {
        return StringUtils.isNullOrEmpty(editText.getText().toString().trim());
    }

    private void reload() {
        buildQuery();
        final int token = ++loadToken;
        loading = true;
        layoutLoading.setVisibility(View.VISIBLE);
        updateFooter();
        final String where = whereClause;
        final String[] args = whereArgs;
        AsyncManager.getInstance().submitWithCallback(() -> {
            PageData page = new PageData();
            page.total = Assets.count(Assets.class, where, args, null, "Id DESC", "");
            page.list = Assets.find(Assets.class, where, args, null, "Id DESC", "0, " + PAGE_SIZE);
            return page;
        }, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            layoutLoading.setVisibility(View.GONE);
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                PageData page = result.getData();
                totalCount = Math.min(page.total, Const.BATCH_COPY_QUERY_LIMIT);
                dataList.clear();
                if (page.list != null) {
                    dataList.addAll(page.list);
                }
                adapter.notifyDataSetChanged();
                updateEmpty();
                updateSelectAllState();
            } else {
                Log.e(TAG, "查询资产失败：where=" + whereClause, result.getError());
                showShortText("查询失败，请重试");
            }
            updateFooter();
        }));
    }

    private void tryLoadNextPage() {
        if (loading || dataList.size() >= totalCount) {
            return;
        }
        final int token = loadToken;
        final int offset = dataList.size();
        loading = true;
        updateFooter();
        final String where = whereClause;
        final String[] args = whereArgs;
        AsyncManager.getInstance().submitWithCallback(() -> {
            return Assets.find(Assets.class, where, args, null, "Id DESC", offset + ", " + PAGE_SIZE);
        }, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || token != loadToken) {
                return;
            }
            loading = false;
            if (result.isSuccess() && result.getData() != null) {
                dataList.addAll(result.getData());
                adapter.notifyDataSetChanged();
                updateSelectAllState();
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
        listSource.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateSelectedCount() {
        int count = adapter.getSelectedCount();
        tvSelectedCount.setText(count > 0 ? "已选 " + count + " 条" : "");
        tvConfirm.setText(count > 0 ? "确定(" + count + ")" : "确定");
        updateSelectAllState();
    }

    private void updateSelectAllState() {
        boolean allSelected = adapter.isAllSelected();
        if (cbSelectAll.isChecked() == allSelected) {
            return;
        }
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(allSelected);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
    }

    private void confirmSelection() {
        List<Long> ids = adapter.getSelectedIds();
        long[] result = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        Intent data = new Intent();
        data.putExtra(BatchCopyOptionsActivity.EXTRA_SELECTED_IDS, result);
        setResult(RESULT_OK, data);
        finish();
    }

    /** 分页查询结果 */
    private static class PageData {
        long total;
        List<Assets> list;
    }
}