package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.abs.AssetsBaseActivity;
import com.gsoft.inventory.service.asset_code_scan.ScanCodeResultHandle;
import com.gsoft.inventory.service.scanner.IScannerReader;
import com.gsoft.inventory.service.scanner.ScanResult;
import com.gsoft.inventory.service.scanner.ScannerCallbacks;
import com.gsoft.inventory.service.scanner.ScannerError;
import com.gsoft.inventory.service.scanner.ScannerFactory;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.qmuiteam.qmui.widget.QMUITopBar;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_HUAWEI_CODE_SCAN;

/**
 * 资产条码扫描页（新增/查看资产信息）
 * <p>
 * 扫码枪识别统一经ScannerFactory按设备型号创建适配器
 * （优博讯：内置扫描服务广播；东集新机：系统扫描广播；东集旧机：RSC-JA解码器），
 * 页面不再判断设备型号、不再直接持有厂家SDK；摄像头扫码仍走华为ScanKit。
 * 两条识别路径最终都进入handleScanResult，由ScanCodeResultHandle查询资产并填充表单。
 *
 * @author 10904
 */
public class AssetsCodeScanActivity extends AssetsBaseActivity {
    QMUITopBar topBar;
    public Boolean isSustain = false;
    @BindView(R.id.button_scan)
    Button buttonScan;

    @BindView(R.id.buttonPrint)
    Button buttonPrint;

    @BindView(R.id.buttonMore)
    Button buttonMore;

    @BindView(R.id.buttonSaveSelf)
    Button buttonSaveSelf;
    @BindView(R.id.checkSustain)
    CheckBox checkSustain;

    /** 扫码结果处理（查询资产/填充表单/连续盘实），扫码枪与摄像头两条路径共用 */
    private Handler scanCodeResultHandler;
    /** 扫码枪识别器：厂家适配器由工厂按设备型号创建，页面不感知具体SDK */
    private IScannerReader scanner;

    public HmsScanAnalyzerOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_code_scan);
        ButterKnife.bind(this);
        // 扫码结果处理器统一创建：扫码枪与摄像头两条识别路径共用，避免旧设备路径为空
        scanCodeResultHandler = new ScanCodeResultHandle(this);
        // 工厂按设备型号创建扫码识别器（优博讯/东集新机/东集旧机），异常时由兜底适配器提示
        scanner = ScannerFactory.create(mContext);
        scanner.init(this, scannerCallbacks);
        initializeView();
    }

    /**
     * 统一扫码回调（适配器保证主线程投递）：
     * 扫码枪结果与摄像头结果一样，统一清洗后交给ScanCodeResultHandle处理
     */
    private final ScannerCallbacks scannerCallbacks = new ScannerCallbacks() {
        @Override
        public void onScanResult(ScanResult result) {
            handleScanResult(result.rawValue);
        }

        @Override
        public void onScanStateChanged(boolean scanning, String message) {
            // 本页无软触发按钮，扫描状态变化无需更新UI
        }

        @Override
        public void onScannerError(ScannerError error) {
            showShortText(error.getMessage());
        }
    };

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("查看资产信息");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("签名", R.id.topbar_right_query_button).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SignatureActivity.class));
        });
        this.initializeFromData();
    }

    public void initializeFromData() {
        // 初始化数据
        initData();
        iconSetting();
        viewStateSetting();
        viewClickSetting();
        initCategoryPicker();

        options = new HmsScanAnalyzerOptions.Creator()
                .setHmsScanTypes(HmsScan.ALL_SCAN_TYPE)
                .setViewType(0)
                .setErrorCheck(true).create();
        CommonPermissionUtils.requestCameraPermission(this);
        CommonPermissionUtils.requestReadFilePermission(this);

        checkSustain.setOnCheckedChangeListener((compoundButton, b) -> {isSustain = b;});

        buttonScan.setOnClickListener(v -> {this.onScanNew();});

        buttonMore.setOnClickListener(v -> {btnMoreClick();});

        buttonSaveSelf.setOnClickListener(view -> {saveData();});
        if (buttonSave != null) {
            buttonSave.setOnClickListener(view -> {saveData();});
        }

        if (buttonCopy != null) {
            buttonCopy.setOnClickListener(view -> copyData());
        }

        if(buttonPrint != null){
            buttonPrint.setOnClickListener(view -> printAsset());
        }


    }

    private void saveData() {
        String docId = assetsCreating.getDOCID();
        if (StringUtils.isNullOrEmpty(docId)) {
            showShortText("请扫码选择数据！");
            return;
        }
        if (!checkRequiredFields()) {
            return;
        }
        this.setBaseAssetProperties();

        /*备注*/
        String remarks = editREMARKS.getText().toString();
        if (StringUtils.isNullOrEmpty(remarks)) {editREMARKS.setText(String.format("%s盘盈", remarks));}
        assetsCreating.setREMARKS(remarks);
        assetsCreating.setZT("0");
        // 更改为盘实
        assetsCreating.setPDZT("02");
        // 更新
        AssetsUtils.uploadAddAssets(assetsCreating, mContext);
        // 本地保存
        assetsCreating.save();

        if (StringUtils.isNullOrEmpty(editBARCODEID.getText().toString())) {editBARCODEID.setText(assetsCreating.getBARCODEID());}
        if (StringUtils.isNullOrEmpty(editSERIALCODE.getText().toString())) {editSERIALCODE.setText(assetsCreating.getSERIALCODE());}
        SysConfig.lastManageUnit = assetsCreating.getGROUNDMANAGEDEPT();
        showShortText("保存成功");


    }


    private void printAsset () {
        if (assetsCreating == null || assetsCreating.getDOCID() == null) {
            showShortText("请先扫码资产");
            return;
        }
        String accountId = assetsCreating.getACCTSUITEID();

        // 通过账套id查询账套名称
        Accounts accounts = AccountHelper.getACCTSUITEID(accountId);
        String accountName = accounts == null ? "" : accounts.getZTMC();

        // 打印统一走基类requestPrintAssets：品牌分发/连接检查/连接后续打全在基类（新增打印机无需改动本页）
        requestPrintAssets(accountName, assetsCreating, () -> markBarcodePrinted(assetsCreating));
    }

    /**
     * 复制数据
     */
    private void copyData(){
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
        String assetsGROUNDMANAGEDEPT = assetsCreating.getGROUNDMANAGEDEPT();
        String assetsISSueDate = assetsCreating.getISSUEDATE();
        String assetsClassification = assetsCreating.getCLASSIFICATION();
        assetsCreating = new Assets();
        assetsCreating.setBARCODEID("");
        assetsCreating.setSERIALCODE("");
        editSERIALCODE.setText("");
        editBARCODEID.setText("");

        buttonPrintLabel.setVisibility(View.INVISIBLE);
//        buttonCopy.setVisibility(View.INVISIBLE);
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
        showShortText("复制完毕，请设置资产编号");
    }


    private void btnMoreClick() {
        if (assetsCreating == null || assetsCreating.getDOCID() == null) {
            showShortText("请先扫码资产");
            return;
        }
        Long id = assetsCreating.getId();
        Intent intentAssetsView = new Intent(mContext, AssetsViewActivity.class);
        intentAssetsView.putExtra(SysDefine.IntentExtra_AssetsID, id);
        startActivity(intentAssetsView);
        releaseScanner();
        finish();
    }


    /**
     * 新版本扫描二维码（华为摄像头，独立于扫码枪适配器）
     */
    private void onScanNew(){
        boolean hasPermission = CommonPermissionUtils.requestCameraPermission(this);
        if(hasPermission){
            ScanUtil.startScan(this, REQUEST_HUAWEI_CODE_SCAN, options);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HUAWEI_CODE_SCAN && data != null) {
            int errorCode = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS);
            if (errorCode == ScanUtil.SUCCESS) {
                HmsScan resultObj = data.getParcelableExtra(ScanUtil.RESULT);
                if (resultObj != null) {
                    // 摄像头结果与扫码枪结果走同一处理入口
                    handleScanResult(resultObj.getOriginalValue());
                }
            }
        }
    }

    /**
     * 统一处理扫码结果：清洗条码 → 交给ScanCodeResultHandle查询资产并填充表单
     * <p>
     * 清洗口径与原扫码页一致（HTTP/BARCODE/BID、条码编号长文本、换行空格），
     * 无法解析时回调扫码失败提示
     */
    private void handleScanResult(String rawValue) {
        Message msgScanResult = new Message();
        String barCode = BarcodeResultUtils.parse(rawValue);
        if (StringUtils.isNullOrEmpty(barCode)) {
            msgScanResult.arg1 = 0;
            msgScanResult.obj = "无法识别的条码";
        } else {
            showShortText(barCode);
            msgScanResult.arg1 = 1;
            msgScanResult.obj = barCode;
        }
        scanCodeResultHandler.sendMessage(msgScanResult);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) {
            scanner.onPageResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanner != null) {
            scanner.onPagePause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseScanner();
    }

    /** 释放扫码识别器（页面销毁/页面跳转前调用，幂等） */
    public void destroy(){
        releaseScanner();
    }

    /** 释放识别器：停止扫描并注销广播/关闭解码器，置空避免重复释放 */
    private void releaseScanner() {
        if (scanner != null) {
            scanner.release();
            scanner = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (scanner != null && scanner.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (scanner != null && scanner.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}



