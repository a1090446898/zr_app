package com.gsoft.inventory.view.rfidfind;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.data.SelectItem;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.view.common.AssetsPickerActivity;
import com.gsoft.inventory.view.common.AssetsPickerOptions;
import com.gsoft.inventory.view.common.CommonSelectActivity;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 找资产设置页。
 * <p>
 * 仅RFID扫描页使用：配置扫描目标过滤条件，结果通过 {@link #readResult(Intent)} 解析后回传扫描页。
 * <ul>
 *     <li>找个人资产：目标为当前登录人，匹配字段可选管理人/使用人/全部；</li>
 *     <li>找他人资产：手动输入或从人员列表选择他人姓名（仅按姓名匹配）；</li>
 *     <li>找特定资产：复用公共资产选择页选一个或多个资产。</li>
 * </ul>
 * 页面不涉及扫描业务，只负责条件录入与校验；过滤逻辑由 {@link FindAssetsCriteria} 与扫描页完成。
 */
public class FindAssetsSettingActivity extends BaseCompatActivity {

    private static final String EXTRA_CRITERIA = "extra_find_criteria";
    private static final String RESULT_CRITERIA = "result_find_criteria";

    /** 选择人员（复用公共人员选择页） */
    private static final int REQ_PICK_PERSON = 3001;
    /** 选择指定资产（复用公共资产选择页） */
    private static final int REQ_PICK_ASSETS = 3002;
    /** 指定资产校验分片大小：避免SQLite IN参数过多 */
    private static final int ID_CHUNK_SIZE = 400;

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.rgMode)
    RadioGroup rgMode;
    @BindView(R.id.tvModeHint)
    TextView tvModeHint;
    @BindView(R.id.layoutPerson)
    View layoutPerson;
    @BindView(R.id.editPerson)
    EditText editPerson;
    @BindView(R.id.btnPickPerson)
    TextView btnPickPerson;
    @BindView(R.id.rgMatchField)
    RadioGroup rgMatchField;
    @BindView(R.id.layoutTarget)
    View layoutTarget;
    @BindView(R.id.tvTargetSummary)
    TextView tvTargetSummary;
    @BindView(R.id.btnClearTargets)
    TextView btnClearTargets;
    @BindView(R.id.btnPickTargets)
    QMUIButton btnPickTargets;
    @BindView(R.id.btnApply)
    QMUIButton btnApply;

    /** 当前登录人姓名（找个人资产的目标） */
    private String loginName = "";
    /** 找他人资产输入的姓名：切换查找方式时保留，避免来回切换后需要重新输入 */
    private String otherPersonName = "";
    /** 指定资产Id（已过滤掉数据库中不存在的记录） */
    private long[] targetIds = new long[0];
    /** 指定资产中可通过条码扫描命中的数量 */
    private int scannableTargetCount = 0;
    /** 程序回填输入框标记：避免把程序赋值误当成用户输入 */
    private boolean fillingPerson;

    // ==================== 对外调用入口 ====================

    /**
     * 打开找资产设置页
     *
     * @param criteria 当前查找条件，null表示未开启
     */
    public static void open(Activity activity, int requestCode, FindAssetsCriteria criteria) {
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, FindAssetsSettingActivity.class);
        if (criteria != null) {
            intent.putExtra(EXTRA_CRITERIA, criteria);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 解析回传的查找条件，调用方无需关心 extra key
     */
    public static FindAssetsCriteria readResult(Intent data) {
        if (data == null) {
            return null;
        }
        Serializable extra = data.getSerializableExtra(RESULT_CRITERIA);
        return extra instanceof FindAssetsCriteria ? (FindAssetsCriteria) extra : null;
    }

    // ==================== 页面初始化 ====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_assets_setting);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("找资产");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        loginName = application.getLoginAccount() == null
                ? "" : safe(application.getLoginAccount().getYHXM());

        FindAssetsCriteria current = readCriteriaFromIntent();
        targetIds = current == null ? new long[0] : current.getTargetAssetIds();
        scannableTargetCount = current == null ? 0 : current.getScannableTargetCount();
        otherPersonName = current == null ? "" : current.getPersonName();

        // 查找已开启时提供顶部"关闭"入口，一键恢复普通盘点
        if (current != null && current.isEnabled()) {
            topBar.addRightTextButton("关闭", R.id.topbar_right_query_button)
                    .setOnClickListener(v -> deliverCriteria(new FindAssetsCriteria()));
        }

        int mode = current == null || !current.isEnabled() ? FindAssetsCriteria.MODE_SELF : current.getMode();
        int matchField = current == null ? FindAssetsCriteria.MATCH_ALL : current.getMatchField();
        rgMode.setOnCheckedChangeListener((group, checkedId) -> applyModeUi());
        rgMode.check(modeRadioId(mode));
        rgMatchField.check(matchRadioId(matchField));

        btnPickPerson.setOnClickListener(v ->
                CommonSelectActivity.pickUser(this, REQ_PICK_PERSON, editPerson.getText().toString().trim()));
        btnPickTargets.setOnClickListener(v -> pickTargetAssets());
        btnClearTargets.setOnClickListener(v -> {
            targetIds = new long[0];
            scannableTargetCount = 0;
            updateTargetSummary();
        });
        btnApply.setOnClickListener(v -> applyCriteria());

        applyModeUi();
        updateTargetSummary();
        // 初始化完成后再监听输入：切换查找方式时程序回填的姓名不写入他人姓名
        editPerson.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!fillingPerson && currentMode() == FindAssetsCriteria.MODE_OTHER) {
                    otherPersonName = s == null ? "" : s.toString().trim();
                }
            }
        });
    }

    /** 读取扫描页传入的当前查找条件 */
    private FindAssetsCriteria readCriteriaFromIntent() {
        Serializable extra = getIntent().getSerializableExtra(EXTRA_CRITERIA);
        return extra instanceof FindAssetsCriteria ? (FindAssetsCriteria) extra : null;
    }

    /**
     * 按当前查找方式切换可见区域：
     * 人员方式显示人员与匹配字段，指定资产方式显示资产选择区
     */
    private void applyModeUi() {
        int mode = currentMode();
        boolean personMode = mode != FindAssetsCriteria.MODE_TARGET;
        layoutPerson.setVisibility(personMode ? View.VISIBLE : View.GONE);
        layoutTarget.setVisibility(personMode ? View.GONE : View.VISIBLE);
        tvModeHint.setText(modeHint(mode));

        fillingPerson = true;
        if (mode == FindAssetsCriteria.MODE_SELF) {
            // 找个人资产：目标固定为当前登录人，不允许改写
            editPerson.setText(loginName);
            editPerson.setEnabled(false);
            editPerson.setFocusable(false);
            btnPickPerson.setVisibility(View.GONE);
        } else {
            editPerson.setEnabled(true);
            editPerson.setFocusableInTouchMode(true);
            editPerson.setText(otherPersonName);
            editPerson.setSelection(editPerson.getText().length());
            btnPickPerson.setVisibility(View.VISIBLE);
        }
        fillingPerson = false;
    }

    private String modeHint(int mode) {
        switch (mode) {
            case FindAssetsCriteria.MODE_OTHER:
                return "手动输入姓名，或点击「选择」从人员列表选择他人，仅按姓名匹配";
            case FindAssetsCriteria.MODE_TARGET:
                return "从资产列表中选择一个或多个资产，扫描时只认这些资产";
            default:
                return "仅查找管理人或使用人为当前登录人的资产";
        }
    }

    // ==================== 指定资产 ====================

    /** 打开公共资产选择页：回显已选资产，确认后回传统计 */
    private void pickTargetAssets() {
        AssetsPickerOptions options = AssetsPickerOptions.builder()
                .title("选择要查找的资产")
                .initialSelectedIds(targetIds)
                .emptyText("没有符合条件的资产")
                .build();
        AssetsPickerActivity.pick(this, REQ_PICK_ASSETS, options);
    }

    /**
     * 读取已选资产：只保留数据库中仍存在的记录，并统计有条码（可被扫描命中）的数量。
     * 资产可能较多，查询与统计放后台线程，结果回主线程刷新摘要
     */
    private void loadSelectedAssets(final long[] ids) {
        if (ids == null || ids.length == 0) {
            targetIds = new long[0];
            scannableTargetCount = 0;
            updateTargetSummary();
            return;
        }
        showProgressDialog("请稍候", "正在统计所选资产……");
        AsyncManager.getInstance().submitWithCallback(() -> {
            Set<Long> foundIds = new HashSet<>();
            int scannable = 0;
            for (int start = 0; start < ids.length; start += ID_CHUNK_SIZE) {
                int end = Math.min(start + ID_CHUNK_SIZE, ids.length);
                StringBuilder where = new StringBuilder("Id IN (");
                List<String> args = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    if (i > start) {
                        where.append(",");
                    }
                    where.append("?");
                    args.add(String.valueOf(ids[i]));
                }
                where.append(")");
                List<Assets> list = Assets.find(Assets.class, where.toString(), args.toArray(new String[0]));
                if (list == null) {
                    continue;
                }
                for (Assets assets : list) {
                    if (assets.getId() == null) {
                        continue;
                    }
                    foundIds.add(assets.getId());
                    if (!StringUtils.isNullOrEmpty(StringUtils.trim(assets.getBARCODEID()))) {
                        scannable++;
                    }
                }
            }
            // 按用户选择顺序保留仍存在的资产Id
            long[] validIds = new long[foundIds.size()];
            int index = 0;
            for (long id : ids) {
                if (foundIds.contains(id)) {
                    validIds[index++] = id;
                }
            }
            return new TargetSelection(validIds, scannable);
        }, result -> mainHandler.post(() -> {
            hideProgressDialog();
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!result.isSuccess() || result.getData() == null) {
                showShortText("读取所选资产失败，请重试");
                return;
            }
            TargetSelection selection = result.getData();
            targetIds = selection.ids;
            scannableTargetCount = selection.scannableCount;
            updateTargetSummary();
        }));
    }

    /** 刷新指定资产摘要：选择数量 + 无条码提示（无条码资产无法被扫描命中） */
    private void updateTargetSummary() {
        if (targetIds.length == 0) {
            tvTargetSummary.setText("尚未选择资产");
            btnClearTargets.setVisibility(View.GONE);
            return;
        }
        int noBarcodeCount = targetIds.length - scannableTargetCount;
        String text = "已选择 " + targetIds.length + " 项资产";
        if (noBarcodeCount > 0) {
            text += "，其中 " + noBarcodeCount + " 项无条码，无法通过扫描找到";
        }
        tvTargetSummary.setText(text);
        btnClearTargets.setVisibility(View.VISIBLE);
    }

    // ==================== 结果回传 ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQ_PICK_PERSON) {
            SelectItem item = CommonSelectActivity.readResult(data);
            if (item == null || StringUtils.isNullOrEmpty(item.getName())) {
                return;
            }
            fillingPerson = true;
            editPerson.setText(item.getName());
            editPerson.setSelection(editPerson.getText().length());
            fillingPerson = false;
            otherPersonName = item.getName();
        } else if (requestCode == REQ_PICK_ASSETS) {
            loadSelectedAssets(AssetsPickerActivity.readSelectedIds(data));
        }
    }

    /** 校验并回传查找条件 */
    private void applyCriteria() {
        int mode = currentMode();
        FindAssetsCriteria criteria = new FindAssetsCriteria();
        criteria.setMode(mode);
        criteria.setMatchField(currentMatchField());
        switch (mode) {
            case FindAssetsCriteria.MODE_SELF:
                if (StringUtils.isNullOrEmpty(loginName)) {
                    showShortText("未获取到当前登录人姓名，无法查找个人资产");
                    return;
                }
                criteria.setPersonName(loginName);
                break;
            case FindAssetsCriteria.MODE_OTHER:
                String personName = editPerson.getText().toString().trim();
                if (StringUtils.isNullOrEmpty(personName)) {
                    showShortText("请输入或选择人员姓名");
                    return;
                }
                criteria.setPersonName(personName);
                break;
            case FindAssetsCriteria.MODE_TARGET:
                if (targetIds.length == 0) {
                    showShortText("请先选择要查找的资产");
                    return;
                }
                if (scannableTargetCount == 0) {
                    showShortText("所选资产均无条码，无法通过扫描找到");
                    return;
                }
                criteria.setTargetAssetIds(targetIds);
                criteria.setScannableTargetCount(scannableTargetCount);
                break;
            default:
                break;
        }
        deliverCriteria(criteria);
    }

    private void deliverCriteria(FindAssetsCriteria criteria) {
        Intent data = new Intent();
        data.putExtra(RESULT_CRITERIA, criteria);
        setResult(RESULT_OK, data);
        finish();
    }

    // ==================== 选项映射 ====================

    private int currentMode() {
        int checkedId = rgMode.getCheckedRadioButtonId();
        if (checkedId == R.id.rbModeOther) {
            return FindAssetsCriteria.MODE_OTHER;
        }
        if (checkedId == R.id.rbModeTarget) {
            return FindAssetsCriteria.MODE_TARGET;
        }
        return FindAssetsCriteria.MODE_SELF;
    }

    private int modeRadioId(int mode) {
        switch (mode) {
            case FindAssetsCriteria.MODE_OTHER:
                return R.id.rbModeOther;
            case FindAssetsCriteria.MODE_TARGET:
                return R.id.rbModeTarget;
            default:
                return R.id.rbModeSelf;
        }
    }

    private int currentMatchField() {
        int checkedId = rgMatchField.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMatchCustodian) {
            return FindAssetsCriteria.MATCH_CUSTODIAN;
        }
        if (checkedId == R.id.rbMatchAssetsUser) {
            return FindAssetsCriteria.MATCH_ASSETS_USER;
        }
        return FindAssetsCriteria.MATCH_ALL;
    }

    private int matchRadioId(int matchField) {
        switch (matchField & FindAssetsCriteria.MATCH_ALL) {
            case FindAssetsCriteria.MATCH_CUSTODIAN:
                return R.id.rbMatchCustodian;
            case FindAssetsCriteria.MATCH_ASSETS_USER:
                return R.id.rbMatchAssetsUser;
            default:
                return R.id.rbMatchAll;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** 指定资产统计结果 */
    private static class TargetSelection {
        /** 数据库中仍存在的资产Id（按选择顺序） */
        final long[] ids;
        /** 其中有条码的资产数量 */
        final int scannableCount;

        TargetSelection(long[] ids, int scannableCount) {
            this.ids = ids;
            this.scannableCount = scannableCount;
        }
    }
}
