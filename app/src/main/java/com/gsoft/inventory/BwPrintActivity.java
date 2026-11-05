package com.gsoft.inventory;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.bw_print.BwLabelRenderer;
import com.gsoft.inventory.utils.bw_print.BwPrintManager;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITabSegment;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝威打印机专用打印页（RFID标签打印类型："RFID二维码60*25"、"RFID二维码70*25"）
 * <p>
 * 1. 预览待打印标签（所见即所得，预览与发送打印机的为同一张位图）；
 * 2. 用户自选"蓝牙打印/WiFi打印"页签，在其内扫描/输入完成连接（保存连接信息，下次自动重连）；
 * 3. 一键打印：同一任务内写入EPC（BARCODEID→ASCII hex，由BwLabelRenderer生成）；
 * 4. 打印成功后标记ISPRINTBARCODE=02（待上传），并携带资产Id返回调用页。
 * <p>
 * 宝威SDK回调均已在BwPrintManager内切回主线程，本页可直接更新UI。
 */
public class BwPrintActivity extends BaseCompatActivity {

    /** 待打印资产（序列化副本，打印成功仅标记副本，调用页返回后同步自身内存对象） */
    public static final String EXTRA_PRINT_ASSETS = "EXTRA_PRINT_ASSETS";
    /** 打印抬头（调用页已按"打印抬头"设置计算好） */
    public static final String EXTRA_PRINT_TITLE = "EXTRA_PRINT_TITLE";
    /** 打印成功返回时携带的资产Id（供调用页同步列表中的内存对象） */
    public static final String EXTRA_PRINTED_ASSETS_ID = "EXTRA_PRINTED_ASSETS_ID";
    /**
     * 仅连接模式：批量打印前先完成连接，连接成功即返回调用页，不加载资产、不显示预览、不打印
     */
    public static final String EXTRA_CONNECT_ONLY = "EXTRA_CONNECT_ONLY";

    /** Android 12及以上使用新蓝牙权限模型（compileSdk=30无系统常量，用字符串） */
    private static final int API_ANDROID_12 = 31;
    private static final String PERMISSION_BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN";
    private static final String PERMISSION_BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    /** targetSdk=30走旧扫描权限模型，发现设备实际校验的是定位权限，不申请则扫描无结果 */
    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQ_PERMISSIONS = 1001;

    /** 页签：蓝牙打印 */
    private static final int TAB_BT = 0;
    /** 页签：WiFi打印 */
    private static final int TAB_WIFI = 1;

    private QMUITopBar topbar;
    private ImageView ivPreview;
    private TextView tvLabelInfo;
    private QMUITabSegment tabSegment;
    private LinearLayout panelBt;
    private LinearLayout panelWifi;
    private TextView tvBtStatus;
    private QMUIButton btnBtScan;
    private LinearLayout btDeviceContainer;
    private EditText etWifiIp;
    private EditText etWifiPort;
    private TextView tvWifiStatus;
    private QMUIButton btnWifiConnect;
    private QMUIButton btnPrint;
    private LinearLayout cardPreview;

    /** 仅连接模式（批量打印前连接打印机） */
    private boolean connectOnly;
    /** 已通知连接成功（防蓝牙+WiFi同时回调重复返回） */
    private boolean connectNotified;

    /** 待打印资产 */
    private Assets printAssets;
    /** 待写入的EPC（十六进制字符串） */
    private String epcHex;
    /** 标签位图（预览与打印同一张） */
    private Bitmap labelBitmap;
    /** 蓝牙连接中标志（防列表重复点击发起多次连接） */
    private volatile boolean btConnecting;
    /** 打印中标志（防重复点击） */
    private boolean printing;
    /** 自动重连目标MAC：扫描到该设备后不入列表，直接连接 */
    private String autoConnectMac;
    /** 已加入列表的设备地址（去重） */
    private final List<String> btDeviceAddrs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bw_print);
        BwPrintManager.ensureInit(this);

        // 仅连接模式：不校验资产、不生成EPC
        connectOnly = getIntent().getBooleanExtra(EXTRA_CONNECT_ONLY, false);
        if (!connectOnly) {
            // 解析调用页传入的资产与抬头（资产经Intent序列化传递）
            printAssets = (Assets) getIntent().getSerializableExtra(EXTRA_PRINT_ASSETS);
            if (printAssets == null || StringUtils.isNullOrEmpty(printAssets.getBARCODEID())) {
                showShortText("资产信息错误，无法打印");
                finish();
                return;
            }
            // 生成待写入EPC：BARCODEID → ASCII hex（与RFID盘点epcHexToBarcode严格互逆）
            epcHex = BwLabelRenderer.buildEpcHex(printAssets.getBARCODEID());
            if (StringUtils.isNullOrEmpty(epcHex)) {
                showShortText("EPC生成失败，无法打印");
                finish();
                return;
            }
        }

        initializeView();
        if (isFinishing()) {
            return;
        }

        // EPC写入结果回读（SDK回调已切主线程）；仅连接模式不打印，无需回读
        if (!connectOnly) {
            BwPrintManager.setTidEpcListener(new BwPrintManager.TidEpcResultListener() {
                @Override
                public void onTidEpc(String tid, String epc, int errCode) {
                    showShortText(errCode == 0 ? "EPC写入成功：" + epc : "EPC写入异常（状态码：" + errCode + "）");
                }
            });
        }

        // Android 12+ 蓝牙权限
        requestPermissionsIfNeeded();
        // 按上次连接记录自动重连（手动连接其他设备即覆盖）
        autoReconnectLast();
    }

    @Override
    public void initializeView() {
        topbar = findViewById(R.id.topbar);
        topbar.setTitle(connectOnly ? "打印机连接" : "标签打印");
        topbar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivPreview = findViewById(R.id.ivPreview);
        tvLabelInfo = findViewById(R.id.tvLabelInfo);
        cardPreview = findViewById(R.id.cardPreview);
        tabSegment = findViewById(R.id.tabSegment);
        panelBt = findViewById(R.id.panelBt);
        panelWifi = findViewById(R.id.panelWifi);
        tvBtStatus = findViewById(R.id.tvBtStatus);
        btnBtScan = findViewById(R.id.btnBtScan);
        btDeviceContainer = findViewById(R.id.btDeviceContainer);
        etWifiIp = findViewById(R.id.etWifiIp);
        etWifiPort = findViewById(R.id.etWifiPort);
        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        btnWifiConnect = findViewById(R.id.btnWifiConnect);
        btnPrint = findViewById(R.id.btnPrint);

        // 仅连接模式：隐藏预览卡片与打印按钮，只保留连接面板（批量打印不显示预览）
        cardPreview.setVisibility(connectOnly ? View.GONE : View.VISIBLE);
        btnPrint.setVisibility(connectOnly ? View.GONE : View.VISIBLE);

        // 无条件回填上次保存的WiFi IP/端口，切换页签时用户无需重复输入
        String savedWifiIp = BwPrintManager.getLastWifiIp();
        if (!StringUtils.isNullOrEmpty(savedWifiIp)) {
            etWifiIp.setText(savedWifiIp);
            etWifiPort.setText(String.valueOf(BwPrintManager.getLastWifiPort()));
        }

        if (!connectOnly) {
            // 生成标签位图：按"打印效果(标签类型)"选择版式模板（打印位置设置只影响抬头对齐，见BwLabelRenderer）
            String position = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, "打印位置");
            String printTitle = getIntent().getStringExtra(EXTRA_PRINT_TITLE);
            String labelType = application.getPrinterEffect();
            if (!BwLabelRenderer.hasTemplate(labelType)) {
                showShortText("当前打印类型暂未适配宝威标签模板");
                finish();
                return;
            }
            labelBitmap = BwLabelRenderer.renderLabel(mContext, labelType, printAssets, printTitle, position);
            if (labelBitmap == null) {
                showShortText("标签生成失败，请检查资产信息");
                finish();
                return;
            }
            ivPreview.setImageBitmap(labelBitmap);
            tvLabelInfo.setText("条码编号：" + printAssets.getBARCODEID() + "\nEPC：" + epcHex);
        }

        setupTabs();

        // 若已连接（如本次会话前面的页面刚连过），直接显示状态
        if (BwPrintManager.isBtConnected()) {
            setBtStatus("蓝牙已连接，可直接打印");
        }
        if (BwPrintManager.isWifiConnected()) {
            setWifiStatus("WiFi已连接，可直接打印");
        }

        btnBtScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtScan();
            }
        });
        btnWifiConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectWifiByInput();
            }
        });
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPrint();
            }
        });

        updatePrintButton();
    }

    /** 双页签：蓝牙打印 / WiFi打印 */
    private void setupTabs() {
        tabSegment.addTab(new QMUITabSegment.Tab("蓝牙打印"));
        tabSegment.addTab(new QMUITabSegment.Tab("WiFi打印"));
        tabSegment.notifyDataChanged();
        tabSegment.addOnTabSelectedListener(new QMUITabSegment.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                switchPanel(index);
            }

            @Override
            public void onTabUnselected(int index) {
            }

            @Override
            public void onTabReselected(int index) {
            }

            @Override
            public void onDoubleTap(int index) {
            }
        });
        tabSegment.selectTab(TAB_BT);
        switchPanel(TAB_BT);
    }

    private void switchPanel(int index) {
        boolean isWifi = index == TAB_WIFI;
        panelBt.setVisibility(isWifi ? View.GONE : View.VISIBLE);
        panelWifi.setVisibility(isWifi ? View.VISIBLE : View.GONE);
    }

    /** 打印按钮可用性：已连接 + 标签已生成 + 非打印中 */
    private void updatePrintButton() {
        btnPrint.setEnabled(!printing && labelBitmap != null && BwPrintManager.isConnected());
        btnPrint.setText(printing ? "打印中…" : "打印标签");
    }

    // ==================== 蓝牙：扫描与连接 ====================

    private void startBtScan() {
        if (!hasScanPermissions()) {
            requestPermissions(new String[]{
                    PERMISSION_FINE_LOCATION,
                    PERMISSION_BLUETOOTH_SCAN,
                    PERMISSION_BLUETOOTH_CONNECT}, REQ_PERMISSIONS);
            setBtStatus("请先授予定位和蓝牙权限");
            return;
        }
        if (!isLocationServiceOn()) {
            setBtStatus("系统定位服务未开启，无法搜索蓝牙打印机");
            showShortText("请在系统设置中打开定位信息后再搜索");
            return;
        }
        btDeviceAddrs.clear();
        btDeviceContainer.removeAllViews();
        setBtStatus("正在搜索宝威蓝牙打印机…");
        BwPrintManager.scanBt(new BwPrintManager.ScanListener() {
            @Override
            public void onScanStart(boolean isOn) {
                btnBtScan.setEnabled(!isOn);
                btnBtScan.setText(isOn ? "搜索中…" : "搜索蓝牙打印机");
                if (!isOn) {
                    setBtStatus("搜索启动失败，请检查蓝牙是否已开启");
                }
            }

            @Override
            public void onFindDevice(BluetoothDevice device) {
                onBtDeviceFound(device);
            }

            @Override
            public void onScanStop() {
                btnBtScan.setEnabled(true);
                btnBtScan.setText("搜索蓝牙打印机");
                if (btDeviceAddrs.isEmpty()) {
                    setBtStatus("未搜索到宝威蓝牙打印机，请确认打印机已开机且未被其他设备连接");
                }
            }
        });
    }

    /** 搜索到设备：自动重连目标直接连接，其余去重后加入列表（点击即连接） */
    private void onBtDeviceFound(final BluetoothDevice device) {
        final String address = safeAddress(device);
        if (StringUtils.isNullOrEmpty(address)) {
            return;
        }
        if (!StringUtils.isNullOrEmpty(autoConnectMac) && autoConnectMac.equalsIgnoreCase(address)) {
            BwPrintManager.stopScanBt();
            connectBtFlow(device);
            return;
        }
        if (btDeviceAddrs.contains(address)) {
            return;
        }
        btDeviceAddrs.add(address);
        TextView item = new TextView(mContext);
        item.setText(BwPrintManager.safeDeviceName(device) + "\n" + address);
        item.setTextColor(Color.parseColor("#0077FF"));
        item.setTextSize(14);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBtFlow(device);
            }
        });
        btDeviceContainer.addView(item);
    }

    private void connectBtFlow(final BluetoothDevice device) {
        if (btConnecting) {
            return;
        }
        // 已连接且目标正是上次成功连接的设备：直接复用现有连接，避免重复connect把连接断开
        // （宝威SDK的connectDevice会先关掉旧连接再重建，触发disConnect回调导致连接状态丢失、打印按钮变灰）
        String address = safeAddress(device);
        if (BwPrintManager.isBtConnected() && !StringUtils.isNullOrEmpty(address)
                && address.equalsIgnoreCase(BwPrintManager.getLastBtMac())) {
            setBtStatus("蓝牙已连接，可直接打印");
            updatePrintButton();
            notifyConnectedInConnectOnlyMode();
            return;
        }
        btConnecting = true;
        BwPrintManager.stopScanBt();
        setBtStatus("正在连接 " + BwPrintManager.safeDeviceName(device) + "…");
        BwPrintManager.connectBt(device, new BwPrintManager.ConnectCallback() {
            @Override
            public void onSuccess(String message) {
                btConnecting = false;
                autoConnectMac = null;
                setBtStatus(message);
                updatePrintButton();
                notifyConnectedInConnectOnlyMode();
            }

            @Override
            public void onFail(String message) {
                btConnecting = false;
                setBtStatus(message);
                updatePrintButton();
            }
        });
    }

    // ==================== WiFi：连接 ====================

    private void connectWifiByInput() {
        String ip = etWifiIp.getText().toString().trim();
        if (StringUtils.isNullOrEmpty(ip) || !ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            showShortText("请输入正确的打印机IP");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(etWifiPort.getText().toString().trim());
        } catch (Exception e) {
            port = BwPrintManager.DEFAULT_WIFI_PORT;
        }
        connectWifiFlow(ip, port);
    }

    private void connectWifiFlow(final String ip, final int port) {
        setWifiStatus("正在连接 " + ip + ":" + port + "…");
        BwPrintManager.connectWifi(ip, port, new BwPrintManager.ConnectCallback() {
            @Override
            public void onSuccess(String message) {
                setWifiStatus(message);
                updatePrintButton();
                notifyConnectedInConnectOnlyMode();
            }

            @Override
            public void onFail(String message) {
                setWifiStatus(message);
                updatePrintButton();
            }
        });
    }

    // ==================== 自动重连（上次连接，可覆盖） ====================

    private void autoReconnectLast() {
        // 上次连接仍保持（打印后未主动断开，连续打印场景）：直接复用现有连接，不做任何重连
        // 重复对已连接设备connectDevice会触发断开等回调，导致"已连接但点击打印无反应"
        if (BwPrintManager.isConnected()) {
            updatePrintButton();
            notifyConnectedInConnectOnlyMode();
            return;
        }
        String lastType = BwPrintManager.getLastConnType();
        if (BwPrintManager.CONN_TYPE_BT.equals(lastType)) {
            String mac = BwPrintManager.getLastBtMac();
            BluetoothDevice device = BwPrintManager.getBtDeviceByAddress(mac);
            if (device != null) {
                // SDK缓存中已有该设备，直接重连
                connectBtFlow(device);
            } else if (!StringUtils.isNullOrEmpty(mac)) {
                // 已连接过的设备：扫描（SDK会直接返回系统配对列表），扫到目标即自动连接
                autoConnectMac = mac;
                setBtStatus("正在自动重连上次设备 " + BwPrintManager.getLastBtName() + "…");
                startBtScan();
            }
        } else if (BwPrintManager.CONN_TYPE_WIFI.equals(lastType)) {
            tabSegment.selectTab(TAB_WIFI);
            String ip = BwPrintManager.getLastWifiIp();
            if (!StringUtils.isNullOrEmpty(ip)) {
                etWifiIp.setText(ip);
                etWifiPort.setText(String.valueOf(BwPrintManager.getLastWifiPort()));
                connectWifiFlow(ip, BwPrintManager.getLastWifiPort());
            }
        }
    }

    // ==================== 打印 ====================

    /**
     * 仅连接模式：连接成功后延时返回调用页（留出状态提示可见时间），
     * 调用页在onActivityResult中续跑批量队列
     */
    private void notifyConnectedInConnectOnlyMode() {
        if (!connectOnly || connectNotified || isFinishing()) {
            return;
        }
        connectNotified = true;
        showShortText("打印机连接成功，即将开始批量打印");
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        }, 600);
    }

    private void doPrint() {
        if (printing) {
            return;
        }
        if (labelBitmap == null) {
            showShortText("标签未生成，无法打印");
            return;
        }
        if (!BwPrintManager.isConnected()) {
            showShortText("请先连接蓝牙或WiFi打印机");
            return;
        }
        printing = true;
        updatePrintButton();
        // 当前在WiFi页签时强制走WiFi通道，避免BT残留连接状态抢走打印通道
        final boolean preferWifi = tabSegment.getSelectedIndex() == TAB_WIFI;
        BwPrintManager.print(labelBitmap, epcHex, preferWifi, new BwPrintManager.PrintCallback() {
            @Override
            public void onResult(boolean success, String message) {
                printing = false;
                updatePrintButton();
                showShortText(message);
                if (!success) {
                    return;
                }
                PrintCompatActivity.markBarcodePrinted(printAssets);
                Intent data = new Intent();
                if (printAssets.getId() != null) {
                    data.putExtra(EXTRA_PRINTED_ASSETS_ID, printAssets.getId().longValue());
                }
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    // ==================== 权限 ====================

    private void requestPermissionsIfNeeded() {
        if (!hasScanPermissions()) {
            requestPermissions(new String[]{
                    PERMISSION_FINE_LOCATION,
                    PERMISSION_BLUETOOTH_SCAN,
                    PERMISSION_BLUETOOTH_CONNECT}, REQ_PERMISSIONS);
        }
    }

    /** 蓝牙扫描所需的全部权限：旧模型下BLUETOOTH_SCAN/CONNECT声明即自动授予，真正缺的是定位权限 */
    private boolean hasScanPermissions() {
        if (checkSelfPermission(PERMISSION_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= API_ANDROID_12) {
            return checkSelfPermission(PERMISSION_BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Android 10及以上定位总开关关闭时，扫描能启动但永远拿不到结果 */
    private boolean isLocationServiceOn() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                return lm != null && lm.isLocationEnabled();
            }
            return Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                    != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMISSIONS) {
            return;
        }
        if (hasScanPermissions()) {
            // 授权后若处于自动重连扫描流程，继续执行
            if (!StringUtils.isNullOrEmpty(autoConnectMac)) {
                startBtScan();
            }
        } else {
            setBtStatus("未授予定位权限，无法搜索蓝牙打印机");
        }
    }

    // ==================== 工具方法 ====================

    private void setBtStatus(String text) {
        tvBtStatus.setText(text);
    }

    private void setWifiStatus(String text) {
        tvWifiStatus.setText(text);
    }

    /** 安全读取蓝牙地址（Android 12+ 未授BLUETOOTH_CONNECT权限时会抛SecurityException） */
    private String safeAddress(BluetoothDevice device) {
        try {
            return device == null ? "" : device.getAddress();
        } catch (SecurityException e) {
            return "";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        // 页面销毁：停止扫描、释放EPC监听；不主动断开连接，便于连续打印（WiFi空闲会自动断开）
        try {
            BwPrintManager.stopScanBt();
        } catch (Exception ignored) {
        }
        BwPrintManager.removeTidEpcListener();
        super.onDestroy();
    }
}