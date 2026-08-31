package com.gsoft.inventory;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.zxing.client.android.Intents;
import com.gsoft.inventory.abs.AssetsBaseActivity;
import com.gsoft.inventory.service.asset_code_scan.ScanCodeResultHandle;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.view.scan.BarcodeScannerActivity;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_CODE_BARCODE_SCAN;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_HUAWEI_CODE_SCAN;
import static com.gsoft.inventory.utils.SysDefine.REQESTCODE_WAITINGBLUETOOTH;

/**
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
    private ScanUtils scanUtils;

    // ==================== 新设备广播扫码 ====================
    private static final String SCAN_ACTION = "com.android.server.scannerservice.broadcast";
    private BroadcastReceiver scanReceiver;
    private boolean isScanReceiverRegistered = false;
    private Handler scanCodeResultHandler;
    private boolean isNewDevice;

    public HmsScanAnalyzerOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_code_scan);
        ButterKnife.bind(this);
        isNewDevice = ScanDeviceUtils.isNewDevice();
        if (isNewDevice) {
            scanCodeResultHandler = new ScanCodeResultHandle(this);
            initBroadcastScan();
        }
        initializeView();
    }

    private void initBroadcastScan() {
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SCAN_ACTION.equals(intent.getAction())) {
                    String code = intent.getStringExtra("scannerdata");
                    if (!StringUtils.isNullOrEmpty(code)) {
                        code = code.replace("\n", "").replace("\r", "");
                        onScanResult(code);
                    }
                }
            }
        };
    }

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("查看资产信息");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("签名", R.id.topbar_right_query_button).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SignatureActivity.class));
        });
        this.initializeFromData();
        if (!isNewDevice) {
            scanUtils = new ScanUtils(this);
        }
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
        String docId = assetsCreating.getDOCID();
        String accountId = assetsCreating.getACCTSUITEID();

        // 通过账套id查询账套名称
        Accounts accounts = AccountHelper.getACCTSUITEID(accountId);
        String accountName = accounts == null ? "" : accounts.getZTMC();

        if (!isPrinterConnected()) {
            Intent intentBluetooth = new Intent(mContext, BluetoothDeviceActivity.class);
            startActivityForResult(intentBluetooth, REQESTCODE_WAITINGBLUETOOTH);
            return;
        }
        printAssets(accountName, assetsCreating);

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
        scanUtils.onDestroy();
        finish();
    }


    /**
     * 新版本扫描二维码
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
                    String result = resultObj.getOriginalValue();
                    if (scanUtils != null) {
                        scanUtils.onScanResult(result);
                    } else {
                        onScanResult(result);
                    }
                }
            }
        }
    }

    /**
     * 处理扫码结果（新设备无ScanUtils时使用）
     */
    private void onScanResult(String barCode) {
        Message msgScanResult = new Message();
        try {
            if (barCode.toUpperCase().startsWith("HTTP")) {
                if (barCode.toUpperCase().contains("BARCODE")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BARCODE") + 8);
                } else if (barCode.toUpperCase().contains("BID")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BID") + 4);
                }
            } else if (barCode.length() > 25 && barCode.contains("条码编号：")) {
                barCode = barCode.substring(5, barCode.indexOf("\n"));
            }
            showShortText(barCode);
            msgScanResult.arg1 = 1;
            msgScanResult.obj = barCode;
        } catch (Exception ex) {
            msgScanResult.arg1 = 0;
            msgScanResult.obj = ex.getMessage();
        }
        scanCodeResultHandler.sendMessage(msgScanResult);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNewDevice && scanReceiver != null && !isScanReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SCAN_ACTION);
            intentFilter.setPriority(Integer.MAX_VALUE);
            registerReceiver(scanReceiver, intentFilter);
            isScanReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNewDevice && isScanReceiverRegistered) {
            try {
                unregisterReceiver(scanReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("AssetsCodeScanActivity", "注销广播接收器时出错", e);
            }
            isScanReceiverRegistered = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isNewDevice && isScanReceiverRegistered) {
            try {
                unregisterReceiver(scanReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("AssetsCodeScanActivity", "注销广播接收器时出错", e);
            }
            isScanReceiverRegistered = false;
        }
        if (scanUtils != null) {
            scanUtils.onDestroy();
        }
    }

    public void destroy(){
        if (scanUtils != null) {
            scanUtils.onDestroy();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isNewDevice && scanUtils != null && scanUtils.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isNewDevice && scanUtils != null && scanUtils.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}



