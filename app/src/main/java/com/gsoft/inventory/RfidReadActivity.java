package com.gsoft.inventory;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.adapter.RfidTagAdapter;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.CommonUtils;
import com.gsoft.inventory.utils.DownloadZrFileManager;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.hardware.rfid.Config;
import com.hardware.rfid.Connect;
import com.hardware.rfid.DataListener;
import com.hardware.rfid.Error;
import com.hardware.rfid.PhoneModuleController;
import com.hardware.rfid.RFIDModuleController;
import com.hardware.rfid.TagScanData;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RFID标签识别（得实DSPDA手持设备，R10/R15/R20/R30/R35/D6500，RFIDModuleController）
 * 按下设备RFID按键或屏幕按钮 → 循环盘点周围标签 → EPC(资产条码)查资产 → 卡片列表展示
 * 标题栏右侧"设置"：功率 / 盘点区域 / 会话 / 频率区域
 */
public class RfidReadActivity extends BaseCompatActivity {

    private static final String TAG = "RfidReadActivity";

    /** 盘点区域 */
    public static final int MODE_EPC = 1;
    public static final int MODE_EPC_TID = 2;
    /** 频率区域（setRFIDConfig(Config.PARA_REGION)参数） */
    public static final int REGION_CHN = Config.CHINA1;           // 中国920~925MHz
    public static final int REGION_EU = Config.EUROPE;            // 欧洲865~868MHz
    public static final int REGION_USA = Config.NORTH_AMERICAN;   // 美国902~928MHz

    @BindView(R.id.topbar)
    QMUITopBar topBar;
    @BindView(R.id.textScanState)
    TextView textScanState;
    @BindView(R.id.textTagCount)
    TextView textTagCount;
    @BindView(R.id.textEmpty)
    TextView textEmpty;
    @BindView(R.id.buttonScan)
    QMUIButton buttonScan;
    @BindView(R.id.recyclerTags)
    RecyclerView recyclerTags;

    private volatile RFIDModuleController rfidController;
    private final RfidTagAdapter tagAdapter = new RfidTagAdapter();
    private final Set<String> readEpcSet = Collections.synchronizedSet(new HashSet<String>());
    private volatile boolean scanning = false;
    private volatile boolean uhfReady = false;
    private volatile boolean connecting = false;
    private volatile boolean pendingScan = false;
    private int connectFailCount = 0;

    /** 扫到标签提示音（复用单个ToneGenerator，随页面销毁释放） */
    private ToneGenerator toneGenerator;
    private long lastBeepTime = 0;

    private static final int REQ_RFID_STORAGE = 9001;

    /** UHF专用线程：SDK内部基于调用线程创建Handler且connect为阻塞串口I/O，主线程调用会卡死导致ANR */
    private static HandlerThread sUhfThread;
    private static Handler sUhfHandler;

    private int uhfPower;
    private int uhfMode;
    private int uhfSession;
    private int uhfRegion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_read);
        ButterKnife.bind(this);
        initializeView();
        initUhfReader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rfidController != null && !uhfReady && !connecting && connectFailCount > 0 && connectFailCount <= 3) {
            CommonUtils.getHandler().postDelayed(() -> {
                if (rfidController != null && !uhfReady && !connecting) {
                    getUhfHandler().post(() -> {
                        try {
                            rfidController.disconnect();
                            Log.i(TAG, "重连前已断开旧连接");
                        } catch (Exception e) {
                            Log.e(TAG, "重连前断开旧连接失败", e);
                        }
                    });
                    doConnectUhf();
                }
            }, 800);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_RFID_STORAGE) {
            onStoragePermissionResult();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RFID_STORAGE) {
            onStoragePermissionResult();
        }
    }

    private void onStoragePermissionResult() {
        if (DownloadZrFileManager.hasWritePermission(mContext)) {
            doConnectUhf();
        } else {
            showShortText("未授予存储权限，RFID模块无法连接");
        }
    }

    @Override
    public void initializeView() {
        topBar.setTitle("RFID盘点");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightTextButton("设置", R.id.topbar_right_query_button).setOnClickListener(v -> showSettingSheet());

        uhfPower = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_POWER, 26);
        uhfMode = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_MODE, MODE_EPC);
        uhfSession = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_SESSION, 1);
        uhfRegion = SharedPreferencesUtils.getInt(mContext, SharedPreferencesUtils.KEY_UHF_REGION, REGION_CHN);
        if (uhfRegion > 3) {
            // 旧版本保存的0x02/0x04/0x08 → 新SDK取值
            uhfRegion = uhfRegion == 0x04 ? REGION_EU : uhfRegion == 0x08 ? REGION_USA : REGION_CHN;
        }

        recyclerTags.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerTags.setAdapter(tagAdapter);
        tagAdapter.setOnItemClickListener(item -> {
            if (item.assets == null) {
                showShortText("该标签未在资产台账中登记");
                return;
            }
            Intent intentView = new Intent(mContext, AssetsViewActivity.class);
            intentView.putExtra(SysDefine.IntentExtra_AssetsID, item.assets.getId());
            startActivity(intentView);
        });

        buttonScan.setOnClickListener(v -> {
            if (scanning) {
                stopScan();
            } else {
                startScan();
            }
        });
        updateScanUi();
    }

    /**
     * RFID模块初始化：connect前先检查存储权限（SDK需读校验文件，未授权时错误码-3）
     * connect为阻塞串口I/O，放到UHF专用线程执行（SDK要求调用线程有Looper，HandlerThread满足），避免主线程ANR
     */
    private void initUhfReader() {
        if (!DownloadZrFileManager.hasWritePermission(mContext)) {
            Log.w(TAG, "存储权限未授予，先申请权限再连接RFID模块");
            DownloadZrFileManager.requestWritePermission(this, REQ_RFID_STORAGE);
            return;
        }
        doConnectUhf();
    }

    private static Handler getUhfHandler() {
        if (sUhfThread == null || !sUhfThread.isAlive()) {
            sUhfThread = new HandlerThread("uhf-sdk");
            sUhfThread.start();
            sUhfHandler = new Handler(sUhfThread.getLooper());
        }
        return sUhfHandler;
    }

    private void doConnectUhf() {
        if (connecting) {
            return;
        }
        connecting = true;
        getUhfHandler().post(() -> {
            try {
                String model = PhoneModuleController.getPDAModule();
                Log.i(TAG, "设备型号=" + model);
                rfidController = RFIDModuleController.connect(mContext, dataListener);
                if (rfidController == null) {
                    connecting = false;
                    Log.e(TAG, "connect返回null，当前设备无RFID模块");
                    showShortText("当前设备不支持RFID模块");
                } else {
                    Log.i(TAG, "RFID connect已提交");
                }
            } catch (Throwable e) {
                connecting = false;
                Log.e(TAG, "RFID模块连接异常", e);
                showShortText("RFID模块初始化失败：" + e.getClass().getSimpleName());
            }
        });
    }

    private final DataListener dataListener = new DataListener() {
        @Override
        public void onConnect(Connect connect) {
            int status = connect == null ? -1 : connect.getErr_status();
            Log.i(TAG, "RFID连接回调 err_status=" + status);
            CommonUtils.getHandler().post(() -> {
                connecting = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (status == 0) {
                    uhfReady = true;
                    connectFailCount = 0;
                    applyUhfSettings();
                    if (pendingScan) {
                        pendingScan = false;
                        startScan();
                    }
                } else {
                    uhfReady = false;
                    connectFailCount++;
                    pendingScan = false;
                    showShortText(connectErrorText(status));
                }
            });
        }

        @Override
        public void onScanTag(TagScanData data) {
            handleUhfTag(data);
        }

        @Override
        public void onError(Error error) {
            Log.e(TAG, "RFID错误回调:" + (error == null ? "null" : error.getErr_status()));
        }
    };

    private void startScan() {
        if (connecting) {
            pendingScan = true;
            showShortText("RFID模块正在初始化，请稍后重试");
            return;
        }
        if (rfidController == null) {
            showShortText("当前设备不支持RFID模块");
            return;
        }
        if (!uhfReady) {
            showShortText("RFID模块未就绪，请稍后重试");
            return;
        }
        if (scanning) {
            return;
        }
        scanning = true;
        updateScanUi();
        getUhfHandler().post(() -> {
            try {
                rfidController.clearTag();
                rfidController.startScanTag();
                Log.i(TAG, "开始盘点");
            } catch (Exception e) {
                scanning = false;
                Log.e(TAG, "启动盘点失败", e);
                CommonUtils.getHandler().post(() -> {
                    updateScanUi();
                    showShortText("启动盘点失败");
                });
            }
        });
    }

    private void stopScan() {
        scanning = false;
        pendingScan = false;
        updateScanUi();
        if (rfidController != null && uhfReady) {
            getUhfHandler().post(() -> {
                try {
                    rfidController.stopScanTag();
                    Log.i(TAG, "停止盘点");
                } catch (Exception e) {
                    Log.e(TAG, "停止盘点失败", e);
                }
            });
        }
    }

    /**
     * 盘点回调（SDK线程）：EPC去重 → 十六进制转条码(BARCODEID) → 查资产 → 命中才加入卡片列表
     */
    private void handleUhfTag(TagScanData data) {
        if (!scanning || data == null || data.getEpc() == null) {
            return;
        }
        String epc = RFIDModuleController.byteToHexStr(data.getEpc()).toUpperCase();
        if (StringUtils.isNullOrEmpty(epc) || readEpcSet.contains(epc)) {
            return;
        }
        readEpcSet.add(epc);
        String barCode = epcHexToBarcode(epc);
        if (StringUtils.isNullOrEmpty(barCode)) {
            Log.w(TAG, "EPC解码条码失败，已忽略 epc=" + epc);
            return;
        }
        List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=?", barCode);
        if (assetsList.isEmpty()) {
            Log.i(TAG, "BARCODEID未在资产台账中登记，已忽略 barCode=" + barCode);
            return;
        }
        RfidTagAdapter.RfidTagItem item = new RfidTagAdapter.RfidTagItem();
        item.epc = epc;
        item.barcode = barCode;
        item.rssi = String.valueOf(data.getRssi());
        item.assets = assetsList.get(0);
        CommonUtils.getHandler().post(() -> {
            tagAdapter.addItem(item);
            updateScanUi();
            playScanBeep();
        });
    }

    /**
     * 扫到标签提示音：短哔声，200ms节流避免连续标签刷成噪声
     */
    private void playScanBeep() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBeepTime < 200) {
            return;
        }
        lastBeepTime = now;
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        } catch (Exception e) {
            Log.e(TAG, "播放提示音失败", e);
        }
    }

    /**
     * EPC十六进制 → ASCII条码(BARCODEID)
     * 盘点读回的EPC可能带PC字头(前4位hex，仅当总长超32位时去除)，按字节转ASCII并去除尾部零填充
     */
    private String epcHexToBarcode(String epcHex) {
        try {
            String hex = epcHex.trim();
            if (hex.length() % 2 != 0 || hex.length() < 2) {
                return null;
            }
            if (hex.length() > 32) {
                hex = hex.substring(4);
            }
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            int len = bytes.length;
            while (len > 0 && bytes[len - 1] == 0) {
                len--;
            }
            if (len == 0) {
                return null;
            }
            String barCode = new String(bytes, 0, len).trim();
            return barCode.matches("[\\x20-\\x7E]+") ? barCode : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateScanUi() {
        buttonScan.setText(scanning ? "停止扫描" : "开始扫描");
        textScanState.setText(scanning ? "正在扫描周围RFID标签..." : "扫描已停止");
        textTagCount.setText("已识别 " + tagAdapter.getDataList().size() + " 个标签");
        textEmpty.setVisibility(tagAdapter.getDataList().isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * 设备RFID按键：得实R10 scanCode=261，D6500为563/564，其他机型为250/313/523
     * 保留旧机型keyCode 139/140/141/142/292/293 兼容；若无响应请从日志(onKeyDown打印)确认实际值
     */
    private boolean isUhfTriggerKey(int keyCode, KeyEvent event) {
        int scanCode = event.getScanCode();
        return scanCode == 261 || scanCode == 563 || scanCode == 564
                || scanCode == 250 || scanCode == 313 || scanCode == 523
                || keyCode == 139 || keyCode == 140 || keyCode == 141 || keyCode == 142
                || keyCode == 292 || keyCode == 293;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isUhfTriggerKey(keyCode, event)) {
            if (event.getRepeatCount() == 0) {
                startScan();
            }
            return true;
        }
        Log.i(TAG, "onKeyDown keyCode=" + keyCode + " scanCode=" + event.getScanCode());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isUhfTriggerKey(keyCode, event)) {
            stopScan();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanning = false;
        pendingScan = false;
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        final RFIDModuleController controller = rfidController;
        rfidController = null;
        if (controller != null) {
            getUhfHandler().post(() -> {
                try {
                    controller.stopScanTag();
                    controller.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "释放RFID模块失败", e);
                }
            });
        }
    }

    // ==================== 设置 ====================

    /**
     * 连接失败错误码 → 提示文案（SDK文档第4章错误码表）
     */
    private String connectErrorText(int status) {
        switch (status) {
            case -1:
                return "RFID连接失败：SDK校验失败";
            case -2:
                return "RFID连接失败：校验文件不正确，请更新应用";
            case -3:
                return "RFID连接失败：存储权限未授予，请在系统设置中开启「所有文件访问」";
            case -8:
                return "RFID连接失败：模块端口冲突或异常，请重启PDA";
            case -9:
                return "RFID连接失败：当前设备不在RFID支持名单中";
            case -10:
                return "RFID连接失败：当前设备不是RFID指定机型";
            default:
                return "RFID模块连接失败(错误码" + status + ")，请重启应用重试";
        }
    }

    private void showSettingSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("UHF扫描设置")
                .addItem("功率：" + uhfPower + "dBm")
                .addItem("盘点区域：" + modeName())
                .addItem("会话：" + (uhfSession == 0 ? "S0" : "S1"))
                .addItem("频率区域：" + regionName())
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    switch (position) {
                        case 0:
                            showPowerDialog();
                            break;
                        case 1:
                            showModeSheet();
                            break;
                        case 2:
                            showSessionSheet();
                            break;
                        case 3:
                            showRegionSheet();
                            break;
                        default:
                            break;
                    }
                })
                .build().show();
    }

    private void showPowerDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(mContext);
        builder.setTitle("设置UHF功率(dBm)")
                .setPlaceholder("0-30")
                .setDefaultText(String.valueOf(uhfPower))
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    String text = builder.getEditText().getText().toString().trim();
                    dialog.dismiss();
                    if (TextUtils.isEmpty(text)) {
                        return;
                    }
                    try {
                        int power = Integer.parseInt(text);
                        if (power < 0 || power > 30) {
                            showShortText("功率范围0-30");
                            return;
                        }
                        uhfPower = power;
                        SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_POWER, power);
                        applyUhfSettings();
                    } catch (NumberFormatException e) {
                        showShortText("请输入数字");
                    }
                })
                .create().show();
    }

    private void showModeSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择盘点区域")
                .addItem("EPC")
                .addItem("EPC+TID")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    uhfMode = position == 1 ? MODE_EPC_TID : MODE_EPC;
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_MODE, uhfMode);
                    applyUhfSettings();
                })
                .build().show();
    }

    private void showSessionSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择会话")
                .addItem("S0")
                .addItem("S1")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    uhfSession = position == 1 ? 1 : 0;
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_SESSION, uhfSession);
                    applyUhfSettings();
                })
                .build().show();
    }

    private void showRegionSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .setTitle("选择频率区域")
                .addItem("中国(920-925MHz)")
                .addItem("欧洲(865-868MHz)")
                .addItem("美国(902-928MHz)")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    switch (position) {
                        case 1:
                            uhfRegion = REGION_EU;
                            break;
                        case 2:
                            uhfRegion = REGION_USA;
                            break;
                        default:
                            uhfRegion = REGION_CHN;
                            break;
                    }
                    SharedPreferencesUtils.putInt(mContext, SharedPreferencesUtils.KEY_UHF_REGION, uhfRegion);
                    applyUhfSettings();
                })
                .build().show();
    }

    /**
     * 应用RFID设置到模块（UHF线程）：功率 / 盘点区域(TID) / 会话 / 频率区域
     * SDK内部会基于调用线程创建Handler，且串口I/O不能阻塞主线程
     */
    private void applyUhfSettings() {
        if (rfidController == null || !uhfReady) {
            return;
        }
        getUhfHandler().post(() -> {
            try {
                rfidController.setRFIDConfig(Config.PARA_POWER, uhfPower);
                rfidController.setRFIDConfig(Config.PARA_ENABLE_TID, uhfMode == MODE_EPC_TID ? Config.TID_YES : Config.TID_NO);
                rfidController.setRFIDConfig(Config.PARA_SESSION, uhfSession);
                rfidController.setRFIDConfig(Config.PARA_REGION, uhfRegion);
                Log.i(TAG, "RFID设置已应用：power=" + uhfPower + " mode=" + uhfMode + " session=" + uhfSession + " region=" + uhfRegion);
            } catch (Exception e) {
                Log.e(TAG, "应用RFID设置失败", e);
            }
        });
    }

    private String modeName() {
        return uhfMode == MODE_EPC_TID ? "EPC+TID" : "EPC";
    }

    private String regionName() {
        switch (uhfRegion) {
            case REGION_EU:
                return "欧洲(865-868MHz)";
            case REGION_USA:
                return "美国(902-928MHz)";
            default:
                return "中国(920-925MHz)";
        }
    }
}