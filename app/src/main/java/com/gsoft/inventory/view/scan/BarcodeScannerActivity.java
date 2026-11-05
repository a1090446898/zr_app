package com.gsoft.inventory.view.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gsoft.inventory.R;
import com.gsoft.inventory.service.rfid.IRfidReader;
import com.gsoft.inventory.service.rfid.RfidCallbacks;
import com.gsoft.inventory.service.rfid.RfidConfig;
import com.gsoft.inventory.service.rfid.RfidError;
import com.gsoft.inventory.service.rfid.RfidReaderFactory;
import com.gsoft.inventory.service.rfid.RfidTagData;
import com.gsoft.inventory.service.scanner.IScannerReader;
import com.gsoft.inventory.service.scanner.ScanResult;
import com.gsoft.inventory.service.scanner.ScannerCallbacks;
import com.gsoft.inventory.service.scanner.ScannerError;
import com.gsoft.inventory.service.scanner.ScannerFactory;
import com.gsoft.inventory.utils.CommonPermissionUtils;
import com.gsoft.inventory.utils.DownloadZrFileManager;
import com.gsoft.inventory.utils.RfidEpcUtils;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_HUAWEI_CODE_SCAN;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_RFID_LOCATION_PERMISSION;
import static com.gsoft.inventory.const_data.ReqPageCodeConst.REQUEST_RFID_STORAGE_PERMISSION;

/**
 * 条码扫描页面（通用扫码入口）
 * <p>
 * 1、扫码枪识别统一经ScannerFactory按设备型号创建适配器
 *    （优博讯：内置扫描服务广播；东集新机：系统扫描广播；东集旧机：RSC-JA解码器），
 *    页面不再判断设备型号、不再直接持有厂家SDK；
 * 2、摄像头扫码仍走华为 HMS ScanKit；
 * 3、两条路径扫码成功后统一通过 {@link #handleBackResult(String)} 设置结果并关闭页面，
 *    结果通过 Intent 的 "barcode_result" 字段返回给调用方；
 * 4、RFID机型不显示额外按钮：页面自动连接IRfidReader，直接扣动设备扳机开始盘点，
 *    首个标签按“ASCII EPC转条码、非ASCII用EPC原值”的既有口径回传，H5契约不变；
 *    RFID不可用时自动回退到原红外扫码适配器，避免页面无可用识别方式。
 *
 * @author 10904
 */
public class BarcodeScannerActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeScannerActivity";

    /** 切换到华为摄像头扫码的按钮 */
    private Button btnSwitchMode;
    /** 当前扫描方式提示 */
    private TextView tvScanMode;
    /** 当前扫描操作提示 */
    private TextView tvScanTip;
    /** 扫码成功提示音播放器 */
    private MediaPlayer mediaPlayer;
    /** 华为扫码配置项（扫码类型、视图类型、错误校验等） */
    public HmsScanAnalyzerOptions options;
    /** 扫码枪识别器：厂家适配器由工厂按设备型号创建，页面不感知具体SDK */
    private IScannerReader scanner;

    /** RFID读取器：厂家适配器由工厂按设备型号创建 */
    private IRfidReader rfidReader;
    /** 当前设备是否支持RFID */
    private boolean rfidSupported;
    /** RFID是否作为当前识别源（连接失败后置false，回退红外扫码） */
    private boolean rfidUsable;
    /** RFID是否正在盘点 */
    private boolean rfidScanning;
    /** 本次页面是否已回传RFID结果，避免多标签重复回调 */
    private boolean rfidResultDelivered;
    /** 是否已发起RFID连接（页面销毁时据此决定是否释放模块） */
    private boolean rfidConnectRequested;
    /** RFID连接中，避免重复open */
    private boolean rfidConnecting;
    /** RFID权限申请中，避免onResume重复拉起权限页 */
    private boolean rfidPermissionRequested;
    /** 扳机在RFID未就绪时按下：连接成功后补扫一次 */
    private boolean pendingRfidScan;
    /** 页面是否处于前台，避免后台启动盘点或回传结果 */
    private boolean pageResumed;

    /** UHF参数（与RFID盘点页共用同一份本地配置） */
    private int uhfPower;
    private boolean powerUserSet;
    private int uhfMode;
    private int uhfSession;
    private int uhfRegion;

    /**
     * 页面创建：初始化华为扫码配置 → 初始化RFID识别源 → 创建扫码枪识别器 → 申请相机和文件读取权限
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        initHuaweiScan();
        // 先初始化RFID：RFID机型不注册红外广播，避免同一物理键触发两路识别
        initRfidScan();

        // 工厂按设备型号创建扫码识别器（优博讯/东集新机/东集旧机），异常时由兜底适配器提示
        scanner = ScannerFactory.create(this);
        scanner.init(this, scannerCallbacks);

        CommonPermissionUtils.requestCameraPermission(this);
        CommonPermissionUtils.requestReadFilePermission(this);
    }

    /**
     * 统一扫码回调（适配器保证主线程投递）：
     * RFID可用时忽略条码结果；RFID不可用时按原逻辑回传
     */
    private final ScannerCallbacks scannerCallbacks = new ScannerCallbacks() {
        @Override
        public void onScanResult(ScanResult result) {
            if (rfidUsable || !pageResumed) {
                return;
            }
            playScanSound();
            handleBackResult(result.rawValue);
        }

        @Override
        public void onScanStateChanged(boolean scanning, String message) {
            // 扫码枪触发方式由适配器处理，页面无需更新状态UI
        }

        @Override
        public void onScannerError(ScannerError error) {
            // RFID机型可能没有红外模块，此时不弹“红外不支持”的误导提示
            if (rfidUsable || error == null) {
                return;
            }
            showShortText(error.getMessage());
        }
    };

    /**
     * 统一RFID回调（适配器保证主线程投递）
     */
    private final RfidCallbacks rfidCallbacks = new RfidCallbacks() {
        @Override
        public void onConnectResult(RfidError error) {
            rfidConnecting = false;
            if (rfidReader == null || isFinishing() || isDestroyed()) {
                return;
            }
            if (error == RfidError.SUCCESS) {
                if (!rfidUsable) {
                    return;
                }
                // 用户从未手动设置过功率：连接成功后按适配器默认值同步
                if (!powerUserSet) {
                    int defaultPower = rfidReader.getDefaultPower();
                    if (defaultPower != uhfPower) {
                        uhfPower = defaultPower;
                    }
                }
                applyRfidConfig();
                // 优博讯枪柄监听依赖pageActive，连接成功后主动恢复一次页面状态
                if (pageResumed) {
                    rfidReader.onPageResume();
                }
                if (pendingRfidScan && pageResumed) {
                    pendingRfidScan = false;
                    startRfidInventory();
                } else {
                    updateScanModeUi();
                }
            } else if (rfidUsable) {
                failRfid(error == null ? "RFID模块连接失败" : error.getMessage());
            }
        }

        @Override
        public void onTagScanned(RfidTagData tag) {
            handleRfidTag(tag);
        }

        @Override
        public void onHardwareError(RfidError error) {
            if (!rfidUsable || !pageResumed) {
                return;
            }
            rfidScanning = false;
            pendingRfidScan = false;
            updateScanModeUi();
            if (error != null) {
                showShortText(error.getMessage());
            }
        }

        @Override
        public void onTriggerKeyDown() {
            if (rfidUsable && pageResumed && !rfidScanning) {
                startRfidInventory();
            }
        }

        @Override
        public void onTriggerKeyUp() {
            if (rfidUsable && pageResumed) {
                stopRfidInventory();
                updateScanModeUi();
            }
        }
    };

    /**
     * 初始化华为 HMS 扫码配置及切换扫码方式按钮
     */
    private void initHuaweiScan() {
        // 支持所有条码类型，开启错误校验
        options = new HmsScanAnalyzerOptions.Creator()
                .setHmsScanTypes(HmsScan.ALL_SCAN_TYPE)
                .setViewType(0)
                .setErrorCheck(true).create();

        btnSwitchMode = findViewById(R.id.btn_switch_mode);
        // 点击后切换到华为摄像头扫码
        btnSwitchMode.setOnClickListener(e -> toHuaweiScan());
    }

    /**
     * 初始化RFID识别源：探测当前设备是否支持RFID，支持时读取UHF本地参数；
     * 不显示按钮，RFID机型进入页面即自动连接，等待扳机触发
     */
    private void initRfidScan() {
        tvScanMode = findViewById(R.id.tv_scan_mode);
        tvScanTip = findViewById(R.id.tv_scan_tip);

        rfidReader = RfidReaderFactory.create(this);
        rfidSupported = rfidReader != null && rfidReader.isSupported(this);
        rfidUsable = rfidSupported;
        if (!rfidSupported) {
            updateScanModeUi();
            return;
        }
        readRfidConfig();
        updateScanModeUi();
    }

    /**
     * 读取UHF本地参数（与RFID盘点页共用配置；旧版本0x04/0x08区域值做兼容映射）
     */
    private void readRfidConfig() {
        int savedPower = SharedPreferencesUtils.getInt(this, SharedPreferencesUtils.KEY_UHF_POWER, -1);
        powerUserSet = savedPower >= 0;
        uhfPower = powerUserSet ? savedPower : rfidReader.getDefaultPower();
        uhfMode = SharedPreferencesUtils.getInt(this, SharedPreferencesUtils.KEY_UHF_MODE, RfidConfig.MODE_EPC);
        uhfSession = SharedPreferencesUtils.getInt(this, SharedPreferencesUtils.KEY_UHF_SESSION, 1);
        uhfRegion = SharedPreferencesUtils.getInt(this, SharedPreferencesUtils.KEY_UHF_REGION, RfidConfig.REGION_CHN);
        if (uhfRegion > 3) {
            uhfRegion = uhfRegion == 0x04 ? RfidConfig.REGION_EU
                    : uhfRegion == 0x08 ? RfidConfig.REGION_USA : RfidConfig.REGION_CHN;
        }
    }

    /**
     * 切换为华为摄像头扫码：先申请相机权限，授权后启动华为扫码界面
     */
    private void toHuaweiScan() {
        boolean hasPermission = CommonPermissionUtils.requestCameraPermission(this);
        if (hasPermission) {
            ScanUtil.startScan(this, REQUEST_HUAWEI_CODE_SCAN, options);
        }
    }

    /**
     * 页面恢复：RFID机型自动连接模块并恢复休眠状态；非RFID机型恢复红外扫码广播
     */
    @Override
    protected void onResume() {
        super.onResume();
        pageResumed = true;
        if (rfidUsable) {
            if (rfidReader != null) {
                if (rfidConnectRequested) {
                    rfidReader.onPageResume();
                } else {
                    connectRfidIfNeed();
                }
            }
        } else if (scanner != null) {
            scanner.onPageResume();
        }
        updateScanModeUi();
    }

    /**
     * 页面暂停：停止盘点并交由适配器做休眠保护（东集模块休眠断电需close）
     */
    @Override
    protected void onPause() {
        pageResumed = false;
        super.onPause();
        if (rfidScanning) {
            rfidScanning = false;
            if (rfidReader != null) {
                rfidReader.stopScan();
            }
        }
        pendingRfidScan = false;
        if (rfidReader != null && rfidConnectRequested) {
            rfidReader.onPagePause();
        }
        if (scanner != null) {
            scanner.onPagePause();
        }
    }

    /**
     * 播放扫码成功提示音（单例复用 MediaPlayer，播放中则先停止并回到起始位置）
     */
    private void playScanSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.scan_sound);
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "播放音频失败", e);
        }
    }

    /**
     * 华为扫码界面返回结果处理：校验错误码后取出原始扫码内容并回传；
     * RFID存储权限设置页返回后继续连接流程
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HUAWEI_CODE_SCAN && data != null) {
            playScanSound();

            // default 默认为 SUCCESS，避免部分机型不带错误码时被误判为失败
            int errorCode = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS);
            if (errorCode == ScanUtil.SUCCESS) {
                HmsScan resultObj = data.getParcelableExtra(ScanUtil.RESULT);
                if (resultObj != null) {
                    handleBackResult(resultObj.getOriginalValue());
                }
            }
        } else if (requestCode == REQUEST_RFID_STORAGE_PERMISSION) {
            onRfidPermissionResult(requestCode);
        }
    }

    /**
     * RFID权限申请结果：存储权限拒绝则回退红外扫码；定位权限无论是否授予都继续连接
     * （已缓存枪柄MAC时BLE直连不依赖定位，未缓存时由适配器返回统一错误）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RFID_STORAGE_PERMISSION
                || requestCode == REQUEST_RFID_LOCATION_PERMISSION) {
            onRfidPermissionResult(requestCode);
        }
    }

    /**
     * 统一回传扫码结果：非空时通过 Intent 携带 "barcode_result" 返回并关闭页面
     *
     * @param result 扫码内容
     */
    private void handleBackResult(String result) {
        if (!StringUtils.isNullOrEmpty(result)) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("barcode_result", result);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    /**
     * 物理按键按下：RFID机型优先按RFID触发键处理；
     * RFID不可用时才转发给原扫码枪适配器
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (rfidUsable && rfidReader != null
                && rfidReader.isTriggerKey(keyCode, event.getScanCode())) {
            if (event.getRepeatCount() == 0 && pageResumed) {
                startRfidInventory();
            }
            return true;
        }
        if (!rfidUsable && scanner != null && scanner.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 物理按键抬起：RFID机型停止盘点；RFID不可用时才转发给原扫码枪适配器
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (rfidUsable && rfidReader != null
                && rfidReader.isTriggerKey(keyCode, event.getScanCode())) {
            if (pageResumed) {
                stopRfidInventory();
                updateScanModeUi();
            }
            return true;
        }
        if (!rfidUsable && scanner != null && scanner.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 开始RFID盘点：模块未就绪时自动连接，连接成功后补扫
     */
    private void startRfidInventory() {
        if (!rfidUsable || rfidResultDelivered || !pageResumed || rfidReader == null) {
            return;
        }
        if (!rfidReader.isReady()) {
            pendingRfidScan = true;
            updateScanModeUi();
            connectRfidIfNeed();
            return;
        }
        if (rfidScanning) {
            return;
        }
        rfidScanning = true;
        rfidReader.startScan();
        updateScanModeUi();
    }

    /**
     * 停止RFID盘点
     */
    private void stopRfidInventory() {
        rfidScanning = false;
        pendingRfidScan = false;
        if (rfidReader != null && rfidConnectRequested) {
            rfidReader.stopScan();
        }
    }

    /**
     * 按适配器能力补权限后连接RFID：当前东集/优博讯均不需要存储和定位权限，
     * 这里保留通用串接，避免以后新增厂家时漏权限
     */
    private void connectRfidIfNeed() {
        if (!rfidUsable || rfidReader == null || rfidReader.isReady()
                || rfidConnecting || rfidReader.isConnecting() || rfidPermissionRequested) {
            return;
        }
        if (rfidReader.requiresStoragePermission()
                && !DownloadZrFileManager.hasWritePermission(this)) {
            rfidPermissionRequested = true;
            DownloadZrFileManager.requestWritePermission(this, REQUEST_RFID_STORAGE_PERMISSION);
            return;
        }
        if (rfidReader.requiresLocationPermission()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            rfidPermissionRequested = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_RFID_LOCATION_PERMISSION);
            return;
        }
        connectRfid();
    }

    /**
     * 发起RFID连接（防止重复open）
     */
    private void connectRfid() {
        if (!rfidUsable || rfidReader == null) {
            return;
        }
        rfidConnecting = true;
        rfidConnectRequested = true;
        rfidReader.connect(this, rfidCallbacks);
    }

    /**
     * RFID权限结果处理：存储权限是硬门槛，拒绝则回退红外扫码；
     * 定位权限拒绝仍继续连接，由适配器给出统一错误文案
     */
    private void onRfidPermissionResult(int requestCode) {
        rfidPermissionRequested = false;
        if (!rfidUsable || rfidReader == null) {
            return;
        }
        if (requestCode == REQUEST_RFID_STORAGE_PERMISSION
                && rfidReader.requiresStoragePermission()
                && !DownloadZrFileManager.hasWritePermission(this)) {
            failRfid("未授予存储权限，RFID模块无法使用");
            return;
        }
        if (requestCode == REQUEST_RFID_LOCATION_PERMISSION) {
            connectRfid();
            return;
        }
        connectRfidIfNeed();
    }

    /**
     * RFID不可用统一回退：恢复红外扫码与页面提示，避免页面无可用识别方式
     */
    private void failRfid(String message) {
        rfidUsable = false;
        rfidScanning = false;
        rfidResultDelivered = false;
        rfidConnecting = false;
        pendingRfidScan = false;
        rfidPermissionRequested = false;
        if (rfidReader != null && rfidConnectRequested) {
            rfidReader.stopScan();
        }
        if (pageResumed && scanner != null) {
            scanner.onPageResume();
        }
        updateScanModeUi();
        showShortText(message);
    }

    /**
     * 刷新扫描方式与操作提示（RFID机型不再显示按钮）
     */
    private void updateScanModeUi() {
        if (tvScanMode == null || tvScanTip == null) {
            return;
        }
        if (!rfidSupported || !rfidUsable) {
            tvScanMode.setText("红外线扫码");
            if (rfidSupported) {
                tvScanTip.setText("RFID不可用，请按住侧边按钮扫码");
            } else {
                tvScanTip.setText("手持设备在本页面按住侧边按钮");
            }
            return;
        }
        tvScanMode.setText("RFID扫描");
        if (rfidScanning) {
            tvScanTip.setText("正在读取RFID标签…");
        } else if (rfidReader != null && rfidReader.isReady()) {
            tvScanTip.setText("请扣动设备扳机读取标签");
        } else {
            tvScanTip.setText("RFID模块准备中，请稍候…");
        }
    }

    /**
     * 应用RFID设置到模块：组装统一配置交给适配器映射下发
     */
    private void applyRfidConfig() {
        if (rfidReader == null) {
            return;
        }
        RfidConfig config = new RfidConfig();
        config.power = uhfPower;
        config.mode = uhfMode;
        config.session = uhfSession;
        config.region = uhfRegion;
        rfidReader.applyConfig(config);
    }

    /**
     * RFID标签回调：首个有效标签即回传并关闭页面
     * <p>
     * 与RFID盘点页口径一致：ASCII EPC转条码，非ASCII直接用EPC十六进制原值
     */
    private void handleRfidTag(RfidTagData tag) {
        if (!rfidUsable || !pageResumed || rfidResultDelivered
                || tag == null || StringUtils.isNullOrEmpty(tag.epc)) {
            return;
        }
        String epc = tag.epc.trim();
        String barCode = RfidEpcUtils.epcHexToBarcode(epc);
        if (StringUtils.isNullOrEmpty(barCode)) {
            barCode = epc;
        }
        if (StringUtils.isNullOrEmpty(barCode)) {
            return;
        }
        rfidResultDelivered = true;
        rfidScanning = false;
        pendingRfidScan = false;
        if (rfidReader != null) {
            rfidReader.stopScan();
        }
        playScanSound();
        handleBackResult(barCode);
    }

    /**
     * 短提示统一入口
     */
    private void showShortText(String message) {
        if (StringUtils.isNullOrEmpty(message)) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 页面销毁：释放识别器、RFID模块与提示音播放器
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        pageResumed = false;
        rfidResultDelivered = true;
        rfidScanning = false;
        rfidConnecting = false;
        pendingRfidScan = false;
        if (rfidReader != null) {
            if (rfidConnectRequested) {
                rfidReader.disconnect();
            }
            rfidReader = null;
        }
        if (scanner != null) {
            scanner.release();
            scanner = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
