package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.domain.AssetsCategory;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.ButterKnife;

/**
 * 资产详情页（数据驱动展示）
 * <p>
 * 1. 页面按分组卡片展示，字段配置集中在 {@link #buildSections()}；
 * 2. 值为空的字段自动隐藏，整个分组的字段都为空时整张卡片不显示（车辆/特种设备/样品/网络等）；
 * 3. 新增字段只需在对应分组加一行 field("标签", () -> assets.getXXX())，
 *    需要固定展示（空值也显示）的字段使用 statusField(...)；
 * 4. 打印条码、盘实/盘盈/盘亏后原地刷新状态，不重建整页。
 */
public class AssetsViewActivity extends PrintCompatActivity {

    /** 状态字段Key：盘点状态 */
    private static final String STATUS_KEY_ZT = "ZT";
    /** 状态字段Key：同步状态 */
    private static final String STATUS_KEY_ISUPLOAD = "ISUPLOAD";
    /** 状态字段Key：打印状态 */
    private static final String STATUS_KEY_PRINT = "ISPRINTBARCODE";

    Assets assets = null;
    QMUITopBar topBar;

    /** 分组容器：所有分组卡片动态添加到此容器 */
    private LinearLayout layoutSections;
    /** 当前登录账号：管理部门名称、账套名称转换用 */
    private Accounts loginAccount;
    /** 状态字段的值控件：打印/标记盘点后原地刷新 */
    private final Map<String, TextView> statusValueViews = new HashMap<>();

    @BindArray(R.array.qdfs)
    String[] qdfsArray;
    @BindArray(R.array.syzk)
    String[] syzkArray;
    @BindArray(R.array.wwdj)
    String[] wwdjArray;
    @BindArray(R.array.bzqk)
    String[] bzqkArray;
    @BindArray(R.array.flyt)
    String[] flytArray;
    @BindArray(R.array.cgzzxs)
    String[] cgzzxsArray;
    @BindArray(R.array.zjzt)
    String[] zjztArray;
    @BindArray(R.array.syfs)
    String[] syfsArray;
    @BindArray(R.array.classifications)
    String[] classifications;
    @BindArray(R.array.accountBookFields)
    String[] accountBookFields;
    @BindArray(R.array.sampleStateFields)
    String[] sampleStateFields;
    @BindArray(R.array.downInfoFields)
    String[] downInfoFields;
    @BindArray(R.array.deviceTypeFields)
    String[] deviceTypeFields;
    @BindArray(R.array.specialDeviceFields)
    String[] specialDeviceFields;
    @BindArray(R.array.madeInChinaFields)
    String[] madeInChinaFields;
    @BindArray(R.array.printStateFields)
    String[] printStateFields;
    @BindArray(R.array.pdztFields)
    String[] pdztFields;
    @BindArray(R.array.ztFields)
    String[] ztFields;

    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_view);
        ButterKnife.bind(this);
        try {
            initializeView();
        } catch (Exception | UnknownError ex) {
            showShortText("解析信息失败，" + ex.getMessage());
        }
    }

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        layoutSections = (LinearLayout) findViewById(R.id.layoutSections);
        topBar.setTitle("查看资产详情");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightImageButton(R.mipmap.icon_topbar_overflow, R.id.topbar_right_change_button)
                .setOnClickListener(v -> showBottomSheet());

        long assetsID = getIntent().getLongExtra(SysDefine.IntentExtra_AssetsID, 0);
        if (assetsID == 0) {
            showShortText("资产信息错误，请退出重试！");
            return;
        }
        assets = Assets.findById(Assets.class, assetsID);
        if (assets == null) {
            showShortText("资产信息未找到，请退出重试！");
            return;
        }
        loginAccount = application.getLoginAccount();
        renderAssetDetail();
    }

    /**
     * 渲染全部字段：按分组生成卡片，空字段自动隐藏
     */
    private void renderAssetDetail() {
        layoutSections.removeAllViews();
        statusValueViews.clear();
        for (SectionSpec section : buildSections()) {
            LinearLayout sectionView = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.item_assets_detail_section, layoutSections, false);
            TextView tvSectionTitle = (TextView) sectionView.findViewById(R.id.tvSectionTitle);
            LinearLayout rowsLayout = (LinearLayout) sectionView.findViewById(R.id.layoutRows);
            tvSectionTitle.setText(section.title);

            for (FieldSpec field : section.fields) {
                String value = StringUtils.trim(field.provider.value());
                // 空值默认不展示，保持页面简洁；状态类字段固定展示
                if (StringUtils.isNullOrEmpty(value) && !field.alwaysShow) {
                    continue;
                }
                View row = getLayoutInflater().inflate(R.layout.item_assets_detail_row, rowsLayout, false);
                TextView tvLabel = (TextView) row.findViewById(R.id.tvLabel);
                TextView tvValue = (TextView) row.findViewById(R.id.tvValue);
                tvLabel.setText(field.label);
                tvValue.setText(StringUtils.isNullOrEmpty(value) ? "-" : value);
                if (field.key != null) {
                    statusValueViews.put(field.key, tvValue);
                }
                rowsLayout.addView(row);
            }

            // 分组内没有任何字段时不展示整张卡片
            if (rowsLayout.getChildCount() == 0) {
                continue;
            }
            // 最后一行不画分割线
            rowsLayout.getChildAt(rowsLayout.getChildCount() - 1)
                    .findViewById(R.id.viewDivider).setVisibility(View.GONE);
            layoutSections.addView(sectionView);
        }
    }

    /**
     * 详情字段配置：新增字段只需在对应分组中加一行，不需要改布局和绑定
     */
    private List<SectionSpec> buildSections() {
        List<SectionSpec> sections = new ArrayList<>();

        sections.add(section("基本信息",
                field("资产名称", () -> assets.getASSETSNAME()),
                field("资产编号", () -> assets.getSERIALCODE()),
                field("条码编号", () -> assets.getBARCODEID()),
                field("资产类别", () -> AssetsCategoryHelper.getAssetsCategoryTitle(assets.getASSETSTYPE())),
                field("资产分类代码", () -> {
                    AssetsCategory category = AssetsCategoryHelper.getAssetsCategory(assets.getASSETSSORTCODE());
                    return category == null ? "" : category.getNAME();
                }),
                field("规格", () -> assets.getASSETSMODEL()),
                field("型号", () -> assets.getASSETSSTANDARD()),
                field("计量单位", () -> assets.getASSETSMEASURE()),
                field("数量", () -> String.valueOf(assets.getASSETSNUM())),
                field("卡片序号", () -> assets.getSERIALNUM()),
                field("取得方式", () -> DictionaryHelper.getDictionaryName(assets.getGETMODE(), qdfsArray)),
                field("取得日期", () -> assets.getASSETSUSEDATE()),
                field("保修截止日期", () -> assets.getASSETSENDUSEDATE()),
                field("预计使用月份", () -> assets.getYEARCOUNT()),
                field("DOCID", () -> assets.getDOCID()),
                field("CardID", () -> assets.getCARDID())
        ));

        sections.add(section("使用信息",
                field("管理人", () -> assets.getCUSTODIAN()),
                field("使用人", () -> assets.getASSETSUSER()),
                field("使用部门", () -> assets.getASSETSDEPT()),
                field("所属处室", () -> assets.getDEPARTMENT()),
                field("管理部门", this::managedDeptName),
                field("权属所有人", () -> assets.getASSETSMANAGER()),
                field("管理编号", () -> assets.getMANAGENO()),
                field("使用状况", () -> DictionaryHelper.getDictionaryName(assets.getUSESTATE(), syzkArray)),
                field("存放地点", () -> assets.getASSETSLAYADD()),
                field("资产位置", () -> assets.getGROUNDSETPOS()),
                field("产权单位", () -> assets.getPROPERTY()),
                field("是否公物仓", () -> "02".equals(assets.getISGWC()) ? "否" : "是"),
                field("备注", () -> assets.getREMARKS())
        ));

        sections.add(section("财务信息",
                field("资产原值", () -> assets.getASSETSCURRPRICE()),
                field("当前累计折旧", () -> assets.getCURRDPRECIATIONADD()),
                field("净值", () -> assets.getLEFTPRICE()),
                field("累计折旧", () -> assets.getDPRECIATIONADD()),
                field("卡片金额", () -> assets.getKPJE()),
                field("折旧状态", () -> DictionaryHelper.getDictionaryName(assets.getDPRECIATIONTYPE(), zjztArray)),
                field("使用年限", () -> assets.getSUMCOMPLETEMONTH()),
                field("已计提折旧月数", () -> assets.getCOMPLETEMONTH()),
                field("月折旧额", () -> assets.getMONDEPREVALUE()),
                field("是否财政资产", () -> "02".equals(assets.getISCZ()) ? "是" : "否")
        ));

        sections.add(section("扩展信息",
                field("品牌", () -> assets.getBRAND()),
                field("采购组织形式", () -> DictionaryHelper.getDictionaryName(assets.getPURCHASINGORGANIZATION(), cgzzxsArray)),
                field("文物等级", () -> DictionaryHelper.getDictionaryName(assets.getASSETSCULGRADE(), wwdjArray)),
                field("设备分类", () -> DictionaryHelper.getDictionaryName(assets.getDEVICETYPE(), deviceTypeFields)),
                field("序列号", () -> assets.getASSETSSERIALNUMBER()),
                field("生产厂家", () -> assets.getASSETSSUPPLIER()),
                field("供货商", () -> assets.getVENDOR()),
                field("出厂编号", () -> assets.getFACTORYNO()),
                field("出厂日期", () -> assets.getFACTORYDATE()),
                field("项目名称", () -> assets.getPRO()),
                field("会计账簿", () -> DictionaryHelper.getDictionaryName(assets.getACCOUNTBOOK(), accountBookFields)),
                field("下发情况", () -> DictionaryHelper.getDictionaryName(assets.getDOWNINFO(), downInfoFields)),
                field("凭证号", () -> assets.getVOUCHERNO()),
                field("财务记账日期", () -> assets.getBOOKKEEPINGTIME()),
                field("入账单号", () -> assets.getPRINTNO()),
                field("点位编码", () -> assets.getDWCODE()),
                field("密级", () -> DictionaryHelper.getDictionaryName(assets.getCLASSIFICATION(), classifications)),
                field("配发日期", () -> assets.getISSUEDATE())
        ));

        sections.add(section("车辆信息",
                field("车牌号", () -> assets.getVEHICLENO()),
                field("车辆产地", () -> assets.getASSETSPRODAREA()),
                field("厂牌型号", () -> assets.getASSETSFACTORYNO()),
                field("发动机号", () -> assets.getFDJH()),
                field("车架号", () -> assets.getCJHM()),
                field("排气量", () -> assets.getVEHEXH()),
                field("行驶里程", () -> assets.getLONGMILES()),
                field("分类用途", () -> DictionaryHelper.getDictionaryName(assets.getVEHPURPOSE(), flytArray)),
                field("发证时间", () -> assets.getASSETSDISPENSEDATE()),
                field("编制情况", () -> DictionaryHelper.getDictionaryName(assets.getASSETSORGANIZATION(), bzqkArray))
        ));

        sections.add(section("特种设备",
                field("是否特种设备", () -> DictionaryHelper.getDictionaryName(assets.getSPECIALDEVICE(), specialDeviceFields)),
                field("设备注册代码", () -> assets.getDEVICECODE()),
                field("特种设备出厂编号", () -> assets.getSPECIALFACTORYNO()),
                field("使用登记证编号", () -> assets.getCERTIFICATENUMBER()),
                field("是否国产设备", () -> DictionaryHelper.getDictionaryName(assets.getMADEINCHINA(), madeInChinaFields)),
                field("参数", () -> assets.getPARAMETERS()),
                field("溯源周期", () -> assets.getTRACEABILITY()),
                field("溯源方式", () -> DictionaryHelper.getDictionaryName(assets.getTRACEABILITYPE(), syfsArray)),
                field("检定校准日期", () -> assets.getCALIBRATIONDATE()),
                field("有效期至", () -> assets.getVALIDDATE()),
                field("所属项目", () -> assets.getPROBELONG())
        ));

        sections.add(section("样品信息",
                field("采样日期", () -> assets.getSAMPLDATE()),
                field("样品编码", () -> assets.getSAMPLCODE()),
                field("监测项目", () -> assets.getMONITORPRO()),
                field("保存方法", () -> assets.getSAVEMETHOD()),
                field("样品状态", () -> DictionaryHelper.getDictionaryName(assets.getSAMPLESTATE(), sampleStateFields)),
                field("采样人员", () -> assets.getSAMPLEUSER()),
                field("配置浓度", () -> assets.getCONCENTRATION()),
                field("配置人", () -> assets.getPREPARER()),
                field("介质", () -> assets.getMEDIA()),
                field("贮存条件", () -> assets.getSTORAGENEED())
        ));

        sections.add(section("网络设备",
                field("硬盘号", () -> assets.getHARDID()),
                field("IP地址", () -> assets.getIP()),
                field("MAC地址", () -> assets.getMAC()),
                field("入网账号", () -> assets.getINTERNETID()),
                field("端口号", () -> assets.getPORT()),
                field("设备用途", () -> assets.getPURPOSE()),
                field("硬盘序列号", () -> assets.getHARDSN()),
                field("网络类型", () -> assets.getNETTYPE()),
                field("操作系统", () -> assets.getOS()),
                field("安装时间", () -> assets.getINSTALLDATE())
        ));

        sections.add(section("系统信息",
                field("账套编号", this::accountName),
                field("录入人", () -> AccountHelper.getUserName(assets.getASSETSWRITER())),
                field("录入日期", () -> assets.getASSETSWRITERDATE()),
                statusField("是否同步", STATUS_KEY_ISUPLOAD, this::syncStateName),
                statusField("盘点状态", STATUS_KEY_ZT, () -> ztName(assets.getZT())),
                field("盘点情况", () -> pdztName(assets.getPDZT())),
                statusField("打印状态", STATUS_KEY_PRINT, this::printStateName)
        ));

        return sections;
    }

    /**
     * 打印后刷新同步与打印状态：重新读取数据库，避免打印页修改的数据在本页内存中未更新
     */
    private void refreshUploadAndPrintState() {
        Assets latest = Assets.findById(Assets.class, assets.getId());
        if (latest != null) {
            assets = latest;
        }
        TextView uploadView = statusValueViews.get(STATUS_KEY_ISUPLOAD);
        if (uploadView != null) {
            uploadView.setText(syncStateName());
        }
        TextView printView = statusValueViews.get(STATUS_KEY_PRINT);
        if (printView != null) {
            printView.setText(printStateName());
        }
    }

    /** 同步状态文案 */
    private String syncStateName() {
        return Const.NOT_UPLOAD.equals(assets.getISUPLOAD()) ? "未同步" : "已同步";
    }

    /** 打印状态文案：空值按未打印显示 */
    private String printStateName() {
        String printStateCode = StringUtils.trim(assets.getISPRINTBARCODE());
        if (StringUtils.isNullOrEmpty(printStateCode)) {
            printStateCode = Const.NOT_PRINT;
        }
        return DictionaryHelper.getDictionaryName(printStateCode, printStateFields);
    }

    /** 账套名称 */
    private String accountName() {
        Accounts account = AccountHelper.getACCTSUITEID(assets.getACCTSUITEID());
        return account == null ? "" : account.getZTMC();
    }

    /**
     * 管理部门名称：历史数据存在部门编号 BMBH、单位编号 DWBH、中文名称三种取值，
     * 按业务口径优先按部门编号 BMBH 反查部门名称，再兼容单位编号 DWBH 反查单位名称，
     * 都查不到时原样展示
     */
    private String managedDeptName() {
        String raw = assets.getGROUNDMANAGEDEPT();
        if (StringUtils.isNullOrEmpty(raw)) {
            return "";
        }
        String name = AccountHelper.getDepartName(raw);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        if (loginAccount != null) {
            name = AccountHelper.getDanweiName(loginAccount.getZTBH(), raw);
            if (!StringUtils.isNullOrEmpty(name)) {
                return name;
            }
        }
        return raw;
    }

    /**
     * 盘点情况字典：01待盘点 02盘实 03盘盈 04待核实
     */
    private String pdztName(String pdzt) {
        return DictionaryHelper.getDictionaryName(pdzt, pdztFields);
    }

    /**
     * 盘点状态字典：02已盘，空值或非法值按字典第一项（未盘）显示
     */
    private String ztName(String zt) {
        String name = DictionaryHelper.getDictionaryName(zt, ztFields);
        return StringUtils.isNullOrEmpty(name) ? ztFields[0] : name;
    }

    private void showBottomSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .addItem("打印条码")
                .addItem("编辑")
                .addItem("删除")
                .addItem("照片")
                .addItem("盘实")
                .addItem("盘盈")
                .addItem("盘亏")
                .addItem("复制")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    switch (position) {
                        case 0:
                            // 打印条码处理：品牌分发、连接检查、连接后续打统一由基类处理
                            requestPrintAssets(accountName(), assets, () -> {
                                // 打印后标记已打印，并刷新同步情况与打印状态显示
                                markBarcodePrinted(assets);
                                refreshUploadAndPrintState();
                            });
                            break;
                        case 1:
                            Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                            intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                            startActivity(intentEdit);
                            finish();
                            break;
                        case 2:
                            new QMUIDialog.MessageDialogBuilder(mContext)
                                    .setTitle("系统提示")
                                    .setMessage("确定要删除吗？")
                                    .addAction("取消", (dialog12, index) -> dialog12.dismiss())
                                    .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog12, index) -> {
                                        assets.setREMARKS("待删除");
                                        assets.setISUPLOAD(Const.NOT_UPLOAD);
                                        assets.save();
                                        AssetsUtils.uploadAssets(assets, mContext);
                                        showShortText("删除请求已提交");
                                        dialog12.dismiss();
                                        finish();
                                    })
                                    .create(mCurrentDialogStyle).show();
                            break;
                        case 3:
                            Intent intentPhoto = new Intent(mContext, PhotoActivity.class);
                            intentPhoto.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                            startActivity(intentPhoto);
                            finish();
                            break;
                        case 4:
                            assets.setREMARKS("盘实");
                            assets.setISUPLOAD(Const.NOT_UPLOAD);
                            assets.setZT("02");
                            assets.save();
                            renderAssetDetail();
                            break;
                        case 5:
                            assets.setREMARKS("盘盈");
                            assets.setISUPLOAD(Const.NOT_UPLOAD);
                            assets.setZT("03");
                            assets.save();
                            renderAssetDetail();
                            break;
                        case 6:
                            assets.setREMARKS("盘亏");
                            assets.setISUPLOAD(Const.NOT_UPLOAD);
                            assets.setZT("04");
                            assets.save();
                            renderAssetDetail();
                            break;
                        case 7:
                            Intent intentCreate = new Intent(mContext, NewAssetsActivity.class);
                            intentCreate.putExtra(SysDefine.IntentExtra_AssetsView, AssetsUtils.cloneAssets(assets));
                            startActivity(intentCreate);
                            finish();
                            break;
                        default:
                            break;
                    }
                    dialog.dismiss();
                })
                .build().show();
    }

    private static SectionSpec section(String title, FieldSpec... fields) {
        SectionSpec spec = new SectionSpec();
        spec.title = title;
        spec.fields = new ArrayList<>();
        for (FieldSpec field : fields) {
            spec.fields.add(field);
        }
        return spec;
    }

    private static FieldSpec field(String label, ValueProvider provider) {
        return new FieldSpec(label, provider, null, false);
    }

    private static FieldSpec statusField(String label, String key, ValueProvider provider) {
        return new FieldSpec(label, provider, key, true);
    }

    /** 字段取值接口：避免使用 java.util.function（minSdk 23 兼容） */
    private interface ValueProvider {
        String value();
    }

    /** 分组配置 */
    private static class SectionSpec {
        String title;
        List<FieldSpec> fields;
    }

    /** 字段配置 */
    private static class FieldSpec {
        final String label;
        final ValueProvider provider;
        /** 状态字段Key，非状态字段为null */
        final String key;
        /** true：空值也展示（状态类字段） */
        final boolean alwaysShow;

        FieldSpec(String label, ValueProvider provider, String key, boolean alwaysShow) {
            this.label = label;
            this.provider = provider;
            this.key = key;
            this.alwaysShow = alwaysShow;
        }
    }
}
