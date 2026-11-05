package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.gsoft.inventory.abs.AssetsBaseActivity;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.DataConvert;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import butterknife.ButterKnife;

/**
 * @author 10904
 */
public class NewAssetsActivity extends AssetsBaseActivity {
    private boolean isAdd = true;

    View queryButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_assets);
        ButterKnife.bind(this);

        // 初始化相机（父类方法）
        initCameraLauncher();

        initializeView();
    }


    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("新增资产");

        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        topBar.addRightImageButton(android.R.drawable.ic_menu_camera, R.id.topbar_right_change_button)
                .setOnClickListener(view -> {
                    if (assetsCreating == null || StringUtils.isNullOrEmpty(assetsCreating.getBARCODEID())) {
                        showShortText("请先保存基本信息");
                        return;
                    }
                    Intent intentPhoto = new Intent(mContext, PhotoActivity.class);
                    intentPhoto.putExtra(SysDefine.IntentExtra_AssetsID, assetsCreating.getId());
                    startActivity(intentPhoto);
                });

        // 新增联想搜索
        topBar.addRightImageButton(android.R.drawable.ic_menu_search, R.id.topbar_right_query_button)
                .setOnClickListener(view -> {
                    if (assetsCreating == null) {
                        showShortText("请先保存基本信息");
                        return;
                    }
                    jumpToQuery();
                });

        // 初始化AI拍照工具类（父类方法）
        initAIPhotoHelper();

        // 初始化数据
        this.initData();
        this.initData2();
        // 图标设置
        iconSetting();
        // 组件状态设置
        viewStateSetting();
        // 监听设置
        viewClickSetting();

    }


    private void initData2() {
        buttonPrintLabel = (QMUIButton) findViewById(R.id.buttonPrintLabel);

        buttonPrintLabel.setOnClickListener(view -> {
            if (assetsCreating == null || assetsCreating.getId() <= 0 || TextUtils.isEmpty(assetsCreating.getBARCODEID())) {
                showShortText("资产信息错误，请先填写信息并保存！");
                return;
            }
            // 打印统一走基类requestPrintAssets：品牌分发/连接检查/连接后续打全在基类（新增打印机无需改动本页）
            requestPrintAssets(editACCTSUITEID.getText().toString(), assetsCreating, () -> markBarcodePrinted(assetsCreating));
        });

        assetsCreating = (Assets) getIntent().getSerializableExtra(SysDefine.IntentExtra_AssetsView);
        if (assetsCreating == null) {
            assetsCreating = new Assets();
            /*初始化数据*/
            editREMARKS.setText("盘盈");
            editACCTSUITEID.setText(loginAccount.getZTMC());
            editPROPERTY.setText(loginAccount.getZTMC());
            assetsCreating.setACCTSUITEID(loginAccount.getZTBH());
            editASSETSNUM.setText("1");
            editGETMODE.setText(qdfsArray[0]);
            assetsCreating.setGETMODE("01");
            assetsCreating.setUSESTATE("01");
            editUSESTATE.setText(syzkArray[0]);
            editDPRECIATIONTYPE.setText(zjztArray[0]);
            assetsCreating.setDPRECIATIONTYPE("01");
            assetsCreating.setASSETSORGANIZATION("01");
            editASSETSORGANIZATION.setText(bzqkArray[0]);
            editASSETSUSEDATE.setText(DataConvert.FormatDate2Day());
            assetsCreating.setASSETSUSEDATE(DataConvert.FormatDate2Day());
            assetsCreating.setISCZ("01");
            toggleISCZ.setChecked(false);
            assetsCreating.setISGWC("02");
            editISGWC.setText(gwcFields[1]);
        } else {
            fillAssetsData();
        }
        // 管理部门统一存部门编号 BMBH：新增时不继承上一张卡片的值
        editGROUNDMANAGEDEPT.setText("");
        assetsCreating.setGROUNDMANAGEDEPT("");

        editASSETSWRITER.setText(application.getZcqcyName());
        assetsCreating.setASSETSWRITER(application.getZcqcyCode());
        editASSETSWRITERDATE.setText(DataConvert.FormatDate2HMS());

        // 保存监听处理
        if (buttonSave != null) {
            buttonSave.setOnClickListener(view -> {
                saveData();
            });
        }
        // 复制监听处理
        if (buttonCopy != null) {
            buttonCopy.setOnClickListener(view -> copyData());
        }

    }



    /**
     * 保存数据
     */
    private void saveData() {
        if (!checkRequiredFields()) {
            return;
        }
        // 输入框赋值
        this.setAssetProperties();
        // 联网上传
        uploadData();
        // 本地保存
        assetsCreating.save();
        showShortText("保存成功");

        // 联想搜索按钮
        removeTopQueryButton();

        if (StringUtils.isNullOrEmpty(editBARCODEID.getText().toString())) {
            editBARCODEID.setText(assetsCreating.getBARCODEID());
        }
        if (StringUtils.isNullOrEmpty(editSERIALCODE.getText().toString())) {
            editSERIALCODE.setText(assetsCreating.getSERIALCODE());
        }
        SysConfig.lastManageUnit = assetsCreating.getGROUNDMANAGEDEPT();

        buttonPrintLabel.setVisibility(View.VISIBLE);
        buttonCopy.setVisibility(View.VISIBLE);

        new QMUIDialog.MessageDialogBuilder(NewAssetsActivity.this)
                .setTitle("条码打印")
                .setMessage("是否打印此资产条码？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "打印", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    dialog.dismiss();
                    // 打印统一走基类requestPrintAssets：品牌分发/连接检查/连接后续打全在基类（新增打印机无需改动本页）
                    requestPrintAssets(editACCTSUITEID.getText().toString(), assetsCreating, () -> markBarcodePrinted(assetsCreating));
                })
                .create(mCurrentDialogStyle).show();
    }

    private void setAssetProperties() {
        this.setBaseAssetProperties();

        // 特殊处理
        assetsCreating.setPDZT("03");
        assetsCreating.setZT("0");
        String docId = assetsCreating.getDOCID();
        if (StringUtils.isNullOrEmpty(docId)) {
            docId = AssetsUtils.generalDocId(application.getDeviceNo());
        }
        String remarks = editREMARKS.getText().toString();
        if (StringUtils.isNullOrEmpty(remarks)) {
            editREMARKS.setText(String.format("%s盘盈", remarks));
        }
        assetsCreating.setDOCID(docId);
        assetsCreating.setREMARKS(remarks);
        assetsCreating.setPDZT(isAdd ? "03" : "02");
    }

    /**
     * 复制数据
     */
    private void copyData() {
        String assetsACCTSUITEID = assetsCreating.getACCTSUITEID();
        String assetsTypeCode = assetsCreating.getASSETSTYPE();
        String assetsASSETSSORTCODE = assetsCreating.getASSETSSORTCODE();
        String assetsASSETSORGANIZATION = assetsCreating.getASSETSORGANIZATION();
        String assetsASSETSCULGRADE = assetsCreating.getASSETSCULGRADE();
        String assetsPURCHASINGORGANIZATION = assetsCreating.getPURCHASINGORGANIZATION();
        String assetsUSESTATE = assetsCreating.getUSESTATE();
        String assetsDPRECIATIONTYPE = assetsCreating.getDPRECIATIONTYPE();
        String assetsGETMODE = assetsCreating.getGETMODE();
        String assetsUseMan = assetsCreating.getASSETSUSER();
        String assetsUseDepart = assetsCreating.getASSETSDEPT();
        String assetsISGWC = assetsCreating.getISGWC();
        String assetsDepartment = assetsCreating.getDEPARTMENT();
        String assetsMadeInChina = assetsCreating.getMADEINCHINA();
        String assetsGROUNDMANAGEDEPT = assetsCreating.getGROUNDMANAGEDEPT();
        String assetsISSueDate = assetsCreating.getISSUEDATE();
        String assetsClassification = assetsCreating.getCLASSIFICATION();
        assetsCreating = new Assets();
        assetsCreating.setBARCODEID("");
        assetsCreating.setSERIALCODE("");
        editSERIALCODE.setText("");
        editBARCODEID.setText("");

        buttonPrintLabel.setVisibility(View.INVISIBLE);
        //  buttonCopy.setVisibility(View.INVISIBLE);
        assetsCreating.setASSETSDEPT(assetsUseDepart);
        assetsCreating.setASSETSUSER(assetsUseMan);
        assetsCreating.setACCTSUITEID(assetsACCTSUITEID);
        assetsCreating.setASSETSTYPE(assetsTypeCode);
        assetsCreating.setASSETSSORTCODE(assetsASSETSSORTCODE);
        assetsCreating.setGETMODE(assetsGETMODE);
        assetsCreating.setASSETSORGANIZATION(assetsASSETSORGANIZATION);
        assetsCreating.setASSETSCULGRADE(assetsASSETSCULGRADE);
        assetsCreating.setPURCHASINGORGANIZATION(assetsPURCHASINGORGANIZATION);
        assetsCreating.setUSESTATE(assetsUSESTATE);
        assetsCreating.setDPRECIATIONTYPE(assetsDPRECIATIONTYPE);
        assetsCreating.setGROUNDMANAGEDEPT(assetsGROUNDMANAGEDEPT);
        assetsCreating.setISSUEDATE(assetsISSueDate);
        assetsCreating.setCLASSIFICATION(assetsClassification);
        assetsCreating.setISGWC(assetsISGWC);
        assetsCreating.setDEPARTMENT(assetsDepartment);
        assetsCreating.setMADEINCHINA(assetsMadeInChina);
        showShortText("复制完毕，请设置资产编号");
        addQueryButton();
    }

    private void removeTopQueryButton() {
        queryButton = topBar.findViewById(R.id.topbar_right_query_button);
        if (queryButton != null) {
            queryButton.setVisibility(View.GONE);
        }
    }

    private void addQueryButton() {
        if (queryButton != null) {
            queryButton.setVisibility(View.VISIBLE);
        }

    }

    private void uploadData() {
        if (isAdd) {
            AssetsUtils.uploadAssets(assetsCreating, mContext);
        } else {
            AssetsUtils.uploadAddAssets(assetsCreating, mContext);
        }
    }

    private void jumpToQuery() {
        // 创建 Intent
        Intent intent = new Intent(NewAssetsActivity.this, AssetsQueryActivity.class);

        // 获取输入框的值
        String assetsName = editASSETSNAME.getText().toString();
        String assetsModel = editASSETSMODEL.getText().toString();
        String assetsStandard = editASSETSSTANDARD.getText().toString();
        String assetsType = editASSETSTYPE.getText().toString();
        String assetManager = editASSETSMANAGER.getText().toString();
        String assetsUser = editASSETSUSER.getText().toString();
        String assetsDept = editASSETSDEPT.getText().toString();
        String assetsLayAdd = editASSETSLAYADD.getText().toString();
        String groundManageDept = editGROUNDMANAGEDEPT.getText().toString();
        String assetsCurrPrice = editASSETSCURRPRICE.getText().toString();
        // 获取对应的单位code
        String deptCode = assetsCreating.getGROUNDMANAGEDEPT();
        Boolean isJumpPage = true;
        // 将值放入 Intent 中
        intent.putExtra("ASSETS_NAME", assetsName);
        intent.putExtra("ASSETS_MODEL", assetsModel);
        intent.putExtra("ASSETS_STANDARD", assetsStandard);
        intent.putExtra("ASSETS_TYPE", assetsType);
        intent.putExtra("ASSET_MANAGER", assetManager);
        intent.putExtra("ASSETS_USER", assetsUser);
        intent.putExtra("ASSETS_DEPT", assetsDept);
        intent.putExtra("ASSETS_LAYADD", assetsLayAdd);
        intent.putExtra("GROUND_MANAGE_DEPT", groundManageDept);
        intent.putExtra("GROUND_MANAGE_DEPT_CODE", deptCode);
        intent.putExtra("ASSETS_CURRPRICE", assetsCurrPrice);
        String pdzt = assetsCreating.getPDZT();
        if (StringUtils.isNullOrEmpty(pdzt)) {
            pdzt = "";
        }
        intent.putExtra("PDZT", pdzt);


        intent.putExtra("isJumpPage", isJumpPage);

        // 启动 AssetsQueryActivity
        startActivityForResult(intent, 10);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 10) {
                receivedPageValue(data);
            }
        }

    }

    /**
     * 接收盘实页面带过来的值
     *
     * @param data
     */
    private void receivedPageValue(Intent data) {
        String assetsIdStr = null;
        if (data != null) {
            assetsIdStr = data.getStringExtra(SysDefine.IntentExtra_AssetsID);
        }

        if (!StringUtils.isNullOrEmpty(assetsIdStr)) {
            isAdd = false;

            long assetsID = Long.parseLong(assetsIdStr);
            // 通过ID查询相关数据
            Assets assets = Assets.findById(Assets.class, assetsID);
            String remarks = assets.getREMARKS();
            // 处理remarks，如果remarks为空，则赋值为盘实，如果remarks不为空，则匹配内容是否包含盘盈，盘亏，如果有则替换为盘实，如果没有则在末尾曾加,盘实
            if (StringUtils.isNullOrEmpty(remarks)) {
                assets.setREMARKS("盘实");
            } else {
                if (remarks.contains("盘盈") || remarks.contains("盘亏")) {
                    assets.setREMARKS(remarks.replace("盘盈", "盘实").replace("盘亏", "盘实"));
                }
                // 检查是否包含盘实，如果有盘实了则不处理
                if (assets.getREMARKS().contains("盘实")) {
                    assets.setREMARKS(assets.getREMARKS());
                } else {
                    assets.setREMARKS(remarks + ",盘实");
                }
            }
            assets.setPDZT("02");

            // 深拷贝赋值
            assetsCreating = assets;
            // 提交给表单页面
            fillAssetsData();

        }
    }


}
