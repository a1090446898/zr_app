package com.gsoft.inventory.view.common;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.adapter.CommonSelectAdapter;
import com.gsoft.inventory.data.SelectItem;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Accounts;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 公共选择页：使用部门、管理部门、人员三种模式共用一个页面。
 * <p>
 * 调用方只需静态方法 + 请求码，结果通过 {@link #readResult(Intent)} 统一解析：
 * <pre>
 *   CommonSelectActivity.pickAssetsDept(this, REQ_ASSETS_DEPT, 当前值);
 *   SelectItem item = CommonSelectActivity.readResult(data);
 * </pre>
 * 数据源统一为当前账套的 {@link Accounts}：按名称去重、按拼音排序，名称/编号均可搜索。
 */
public class CommonSelectActivity extends BaseCompatActivity {

    /**
     * 使用部门：返回 BMMC + BMBH，写入 Assets.ASSETSDEPT 的是部门名称
     */
    public static final int TYPE_ASSETS_DEPT = 1;
    /**
     * 管理部门：返回 BMMC + BMBH，写入 Assets.GROUNDMANAGEDEPT 的是部门编号 BMBH
     * <p>
     * 注意：历史数据里该字段出现过中文名称、BMBH、DWBH 三种取值，
     * 业务正解为 BMBH，展示层已做三重兼容
     */
    public static final int TYPE_MANAGE_DEPT = 2;
    /**
     * 人员：返回 YHXM + YHBH，写入 Assets.CUSTODIAN/ASSETSUSER 的是姓名
     */
    public static final int TYPE_USER = 3;

    private static final String EXTRA_TYPE = "extra_type";
    private static final String EXTRA_SELECTED_NAME = "extra_selected_name";

    private static final String RESULT_NAME = "result_name";
    private static final String RESULT_CODE = "result_code";
    private static final String RESULT_SUB_TITLE = "result_sub_title";

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.editKeyword)
    EditText editKeyword;
    @BindView(R.id.listItems)
    ListView listItems;
    @BindView(R.id.layoutEmpty)
    View layoutEmpty;
    @BindView(R.id.layoutLoading)
    View layoutLoading;
    @BindView(R.id.tvCount)
    TextView tvCount;

    private CommonSelectAdapter adapter;
    private final List<SelectItem> allItems = new ArrayList<>();
    private int type = TYPE_ASSETS_DEPT;
    private String selectedName = "";

    // ==================== 对外调用入口 ====================

    /**
     * 选择使用部门（可手动输入，也可从这里选）
     */
    public static void pickAssetsDept(Activity activity, int requestCode, String currentName) {
        start(activity, TYPE_ASSETS_DEPT, currentName, requestCode);
    }

    /**
     * 选择管理部门（字典数据，只能选不能改，回传部门编号 BMBH）
     */
    public static void pickManageDept(Activity activity, int requestCode, String currentName) {
        start(activity, TYPE_MANAGE_DEPT, currentName, requestCode);
    }

    /**
     * 选择人员（管理人、使用人共用）
     */
    public static void pickUser(Activity activity, int requestCode, String currentName) {
        start(activity, TYPE_USER, currentName, requestCode);
    }

    /**
     * 统一解析回传结果，调用方无需关心 extra key
     */
    public static SelectItem readResult(Intent data) {
        if (data == null) {
            return null;
        }
        String name = data.getStringExtra(RESULT_NAME);
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }
        return new SelectItem(name, data.getStringExtra(RESULT_CODE), data.getStringExtra(RESULT_SUB_TITLE));
    }

    private static void start(Activity activity, int type, String currentName, int requestCode) {
        Intent intent = new Intent(activity, CommonSelectActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_SELECTED_NAME, currentName == null ? "" : currentName);
        activity.startActivityForResult(intent, requestCode);
    }

    // ==================== 页面初始化 ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_select);
        ButterKnife.bind(this);

        type = getIntent().getIntExtra(EXTRA_TYPE, TYPE_ASSETS_DEPT);
        selectedName = getIntent().getStringExtra(EXTRA_SELECTED_NAME);
        if (selectedName == null) {
            selectedName = "";
        }

        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle(titleOfType());
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        adapter = new CommonSelectAdapter(mContext);
        listItems.setAdapter(adapter);
        listItems.setOnItemClickListener((parent, view, position, id) -> deliverResult(adapter.getItem(position)));

        editKeyword.setHint(hintOfType());
        editKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s == null ? "" : s.toString());
            }
        });

        loadData();
    }

    private String titleOfType() {
        switch (type) {
            case TYPE_MANAGE_DEPT:
                return "选择管理部门";
            case TYPE_USER:
                return "选择人员";
            default:
                return "选择使用部门";
        }
    }

    private String hintOfType() {
        switch (type) {
            case TYPE_USER:
                return "搜索姓名、工号或部门";
            default:
                return "搜索部门名称或编号";
        }
    }

    // ==================== 数据加载 ====================

    private void loadData() {
        layoutLoading.setVisibility(View.VISIBLE);
        AsyncManager.getInstance().submitWithCallback(this::buildItems, result -> mainHandler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            layoutLoading.setVisibility(View.GONE);
            allItems.clear();
            if (result.isSuccess() && result.getData() != null) {
                allItems.addAll(result.getData());
            } else {
                showShortText("加载数据失败，请重试");
            }
            adapter.setSelectedName(resolveCurrentName());
            applyFilter(editKeyword.getText() == null ? "" : editKeyword.getText().toString());
        }));
    }

    /**
     * 按当前账套构建去重后的选择项，子线程执行
     */
    private List<SelectItem> buildItems() {
        List<SelectItem> items = new ArrayList<>();
        String ztbH = ztbH();
        if (StringUtils.isNullOrEmpty(ztbH)) {
            return items;
        }
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH = ?", ztbH);
        if (accountsList == null || accountsList.isEmpty()) {
            return items;
        }

        if (type == TYPE_USER) {
            Set<String> seen = new HashSet<>();
            for (Accounts accounts : accountsList) {
                String name = safe(accounts.getYHXM());
                if (StringUtils.isNullOrEmpty(name) || !seen.add(name)) {
                    continue;
                }
                items.add(new SelectItem(name, safe(accounts.getYHBH()), buildUserSubTitle(accounts)));
            }
        } else {
            // 使用部门与管理部门同源：部门字典 Accounts.BMMC + BMBH
            // 使用部门取名称写入 ASSETSDEPT，管理部门取编号写入 GROUNDMANAGEDEPT
            Map<String, String> codeByName = new LinkedHashMap<>();
            for (Accounts accounts : accountsList) {
                String name = safe(accounts.getBMMC());
                if (StringUtils.isNullOrEmpty(name) || codeByName.containsKey(name)) {
                    continue;
                }
                codeByName.put(name, safe(accounts.getBMBH()));
            }
            for (Map.Entry<String, String> entry : codeByName.entrySet()) {
                items.add(new SelectItem(entry.getKey(), entry.getValue(), "部门编号：" + entry.getValue()));
            }
        }

        sortByName(items);
        return items;
    }

    /**
     * 人员副标题：部门 · 单位 · 编号，便于区分同名人员
     */
    private String buildUserSubTitle(Accounts accounts) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, safe(accounts.getBMMC()));
        appendPart(builder, safe(accounts.getDWMC()));
        String userCode = safe(accounts.getYHBH());
        if (!StringUtils.isNullOrEmpty(userCode)) {
            appendPart(builder, "编号 " + userCode);
        }
        return builder.toString();
    }

    private void appendPart(StringBuilder builder, String part) {
        if (StringUtils.isNullOrEmpty(part)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(part);
    }

    /**
     * 中文按拼音排序，避免字典数据顺序随机
     */
    private void sortByName(List<SelectItem> items) {
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(items, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem left, SelectItem right) {
                return collator.compare(left.getName(), right.getName());
            }
        });
    }

    /**
     * 当前值可能是部门名称，也可能是部门编号 BMBH（管理部门入库值），统一解析成名称用于高亮
     */
    private String resolveCurrentName() {
        if (StringUtils.isNullOrEmpty(selectedName)) {
            return "";
        }
        String trimmed = selectedName.trim();
        for (SelectItem item : allItems) {
            if (trimmed.equals(item.getName()) || trimmed.equals(item.getCode())) {
                return item.getName();
            }
        }
        // 历史编码兼容：按部门编号反查部门名，尽量命中同名部门
        String deptName = AccountHelper.getDepartName(trimmed);
        for (SelectItem item : allItems) {
            if (!StringUtils.isNullOrEmpty(deptName) && deptName.equals(item.getName())) {
                return item.getName();
            }
        }
        return trimmed;
    }

    private void applyFilter(String keyword) {
        List<SelectItem> filtered = new ArrayList<>();
        if (StringUtils.isNullOrEmpty(keyword)) {
            filtered.addAll(allItems);
        } else {
            for (SelectItem item : allItems) {
                if (item.matches(keyword.trim())) {
                    filtered.add(item);
                }
            }
        }
        adapter.setData(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        tvCount.setText("共 " + filtered.size() + " 项");
    }

    private void deliverResult(SelectItem item) {
        Intent data = new Intent();
        data.putExtra(RESULT_NAME, item.getName());
        data.putExtra(RESULT_CODE, item.getCode());
        data.putExtra(RESULT_SUB_TITLE, item.getSubTitle());
        setResult(RESULT_OK, data);
        finish();
    }

    private String ztbH() {
        if (application.getLoginAccount() == null) {
            return "";
        }
        return application.getLoginAccount().getZTBH();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
