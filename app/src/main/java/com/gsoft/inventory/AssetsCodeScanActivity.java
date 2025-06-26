package com.gsoft.inventory;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.zxing.client.android.Intents;
import com.gsoft.inventory.abs.AssetsBaseActivity;
import com.gsoft.inventory.utils.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author 10904
 */
@RuntimePermissions
public class AssetsCodeScanActivity extends AssetsBaseActivity {

    QMUITopBar topBar;

    BarcodeDecoder barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();

    private ScanManager mScanManager = null;
    private final ScanCodeHandler scanCodeHandler = new ScanCodeHandler(this);

    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    //优博讯
    int[] idbuf = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
    int[] idmodebuf = new int[]{PropertyID.WEDGE_KEYBOARD_ENABLE, PropertyID.TRIGGERING_MODES, PropertyID.LABEL_APPEND_ENTER};
    String[] action_value_buf = new String[]{ScanManager.ACTION_DECODE, ScanManager.BARCODE_STRING_TAG};
    int[] idmode;
    Boolean isSustain = false;

    @BindView(R.id.button_scan)
    Button buttonScan;
//    Assets assetsCreating = new Assets();

    @BindView(R.id.buttonMore)
    Button buttonMore;

    @BindView(R.id.buttonSaveSelf)
    Button buttonSaveSelf;
    @BindView(R.id.checkSustain)
    CheckBox checkSustain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_code_scan);
        ButterKnife.bind(this);
        initializeView();
    }


    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("查看资产信息");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("签名", R.id.topbar_right_query_button).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SignatureActivity.class));
        });

        if ("DT50Q".equals(Build.MODEL)) initYBX();
        this.initializeFromData();
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
                    if (StringUtils.isNullOrEmpty(result.getContents())) {
                        Intent originalIntent = result.getOriginalIntent();
                        if (originalIntent == null) {
                            showShortText("取消扫码");
                        } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                            Toast.makeText(mContext, "授权失败，请允许使用您的摄像头扫码", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    String scanString = StringUtils.decodeQRCode(result.getContents());
                    onScanResult(scanString);
                });
    }
    public void initializeFromData() {
        initData();
        // 图标设置
        iconSetting();
        // 组件状态设置
        viewStateSetting();
        // 监听设置
        viewClickSetting();
        // 初始化选择器
        initCategoryPicker();

        checkSustain.setOnCheckedChangeListener((compoundButton, b) -> {isSustain = b;});

        buttonScan.setOnClickListener(v -> {
            this.onScanOld();
//           this.onScanNew();
        });

        buttonMore.setOnClickListener(v -> {btnMoreClick();});

        buttonSaveSelf.setOnClickListener(view -> {saveData();});
        buttonSave.setOnClickListener(view -> {saveData();});


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


    private void btnMoreClick() {
        if (assetsCreating == null) {
            showShortText("请先扫码资产");
            return;
        }
        Long id = assetsCreating.getId();
        if (id == null) {
            showShortText("请先扫码资产");
            return;
        }
        Intent intentAssetsView = new Intent(mContext, AssetsViewActivity.class);
        intentAssetsView.putExtra(SysDefine.IntentExtra_AssetsID, id);
        startActivity(intentAssetsView);
        closeScanDevice();
        finish();
    }


    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    public void cameraScan() {
        ScanOptions options = new ScanOptions();
        barcodeLauncher.launch(options);
    }



    // 权限请求结果回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        AssetsCodeScanActivityPermissionsDispatcher.onRequestPermissionsResult(AssetsCodeScanActivity.this, requestCode, grantResults);
    }


    private boolean checkScan () {
        // 进行判断，如果当前设备安卓版本为7.0以上，正常调用扫码功能，
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showShortText("请按手持设备侧边进行扫码");
            return false;
        }
        return true;
    }

    /**
     * 旧版本扫描二维码
     */
    private void onScanOld(){
        if(!checkScan()){return;}
        // 扫码处理
        AssetsCodeScanActivityPermissionsDispatcher.cameraScanWithPermissionCheck(AssetsCodeScanActivity.this);
    }

    /**
     * 新版本扫描二维码
     */
    private void onScanNew(){
//        if(!checkScan()){return;}
        // 跳转到ScanPhotoActivity
        startActivity(new Intent(this, ScanPhotoActivity.class));
        finish();
    }

    private void initYBX() {
        mScanManager = new ScanManager();
        mScanManager.openScanner();
        action_value_buf = mScanManager.getParameterString(idbuf);
        idmode = mScanManager.getParameterInts(idmodebuf);
        idmode[0] = 0;
        mScanManager.setParameterInts(idmodebuf, idmode);
        registerYBX();
    }

    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(action_value_buf[1]);
            if (result != null) {
                onScanResult(result);
            }
        }

    };

    private void registerYBX() {
        IntentFilter filter = new IntentFilter();
        action_value_buf = mScanManager.getParameterString(idbuf);
        filter.addAction(action_value_buf[0]);
        registerReceiver(mScanReceiver, filter);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (Build.MODEL.equals("DT50Q")) {
            registerYBX();
        } else {
            new InitTask().execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (Build.MODEL.equals("DT50Q"))
                unregisterReceiver(mScanReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeScanDevice();
    }

    private void closeScanDevice() {
        if (barcodeDecoder != null) {
            barcodeDecoder.close();
        }
        try {
            if (Build.MODEL.equals("DT50Q")) {
                unregisterReceiver(mScanReceiver);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("onKeyDown", String.valueOf(keyCode));
        if (keyCode == 139) {
            if (event.getRepeatCount() == 0) {
                ScanBarcode();
                return true;
            }
        } else if (keyCode == 520 || keyCode == 521) {
            if (event.getRepeatCount() == 0) {
                mScanManager.startDecode();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == 139) {
            if (event.getRepeatCount() == 0) {
                this.stop();
                return true;
            }
        } else if (keyCode == 520 || keyCode == 521) {
            if (event.getRepeatCount() == 0) {
                mScanManager.stopDecode();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void ScanBarcode() {
        this.start();
    }

    private void start() {
        barcodeDecoder.startScan();
    }

    private void stop() {
        barcodeDecoder.stopScan();
    }

    private void open() {
        barcodeDecoder.open(this);

        barcodeDecoder.setDecodeCallback(barcodeEntity -> {
            if (barcodeEntity.getResultCode() == BarcodeDecoder.DECODE_SUCCESS) {
                Message msgScanResult = new Message();
                try {
                    String barCode = barcodeEntity.getBarcodeData();
                    byte[] barcodeBytesData = barcodeEntity.getBarcodeBytesData();
                    if (barCode.toUpperCase().startsWith("HTTP") && barCode.toUpperCase().contains("BARCODE")) {   // 网络型二维码
                        onScanResult(barCode);
                    } else if (barCode.length() > 25) {
                        if (!barCode.contains("条码编号：")) {
                            // 过滤掉无法解码的字节
                            byte[] filteredBytes = filterInvalidUtf8Bytes(barcodeBytesData);
                            String utf8String = new String(filteredBytes, StandardCharsets.UTF_8);
                            String[] parts = utf8String.split("\n");
                            if (parts.length > 0) {
                                barCode = parts[0];
                            }
                        }
                        onScanResult(barCode);
                    } else {
                        showShortText(barCode);
                        msgScanResult.arg1 = 1;
                        msgScanResult.obj = barCode;
                        scanCodeHandler.sendMessage(msgScanResult);
                    }

                } catch (Exception ex) {
                    msgScanResult.arg1 = 0;
                    msgScanResult.obj = ex.getMessage();
                }
            }
        });
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static byte[] filterInvalidUtf8Bytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 0) {
                sb.append((char) b);
            }
        }
        return sb.toString().getBytes();
    }

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
        scanCodeHandler.sendMessage(msgScanResult);
    }


    /**
     * 设备上电异步类
     * @author liuruifeng
     */
    class InitTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            boolean reuslt = false;
            open();
            return reuslt;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mypDialog.cancel();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mypDialog = new ProgressDialog(mContext);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("正在初始化设备");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.show();
        }

    }

    private static class ScanCodeHandler extends Handler {
        private final WeakReference<AssetsCodeScanActivity> mActivty;

        private ScanCodeHandler(AssetsCodeScanActivity mActivty) {
            this.mActivty = new WeakReference<AssetsCodeScanActivity>(mActivty);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            AssetsCodeScanActivity activity = mActivty.get();
            if (msg.arg1 == 1) {
                String barCode = msg.obj.toString();
                if (activity != null) {
                    Accounts loginAccount = activity.loginAccount;
                    if (loginAccount == null) {
                        activity.showShortText("登录失效，请退出重新登录");
                        return;
                    }
                    List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=? AND ACCTSUITEID=?", barCode, loginAccount.getZTBH());
                    if (assetsList.size() == 1) {
                        if (activity.isSustain) {
                            Assets assets = assetsList.get(0);
                            if (!StringUtils.isNullOrEmpty(assets.getREMARKS()) && !assets.getREMARKS().contains("盘实")) {
                                assets.setREMARKS("盘实");
                            }
                            assets.setISUPLOAD("0");
                            assets.setPDZT("02");
                            assets.save();
                            AssetsUtils.uploadAssets(assets, activity);
                            activity.showShortText(barCode.concat("已盘实"));
                            activity.assetsCreating = assets;
                            activity.fillAssetsData();
                        } else {
                            activity.assetsCreating = assetsList.get(0);
                            activity.fillAssetsData();
                        }
                    } else if (assetsList.size() > 1) {
                        Intent intentRecode = new Intent(activity, AssetsRecodeActivity.class);
                        intentRecode.putExtra(SysDefine.IntentExtra_SERIALCODE, barCode);
                        activity.startActivity(intentRecode);
                        activity.closeScanDevice();
                        activity.finish();
                    } else {
                        activity.showShortText("未找到此编码资产！");
                    }
                }
            } else {
                activity.showShortText("扫码失败！");
            }
        }
    }


}



