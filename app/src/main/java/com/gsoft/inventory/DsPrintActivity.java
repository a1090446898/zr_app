package com.gsoft.inventory;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.ds_print.DsLabelRenderer;
import com.gsoft.inventory.utils.ds_print.DsPrintManager;
import com.gsoft.inventory.utils.ds_print.DsPrintSettings;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 得实RFID打印机专用打印页（打印类型："得实二维码60*25"、"得实二维码70*25"）
 * <p>
 * 1. 预览待打印标签（所见即所得，预览与发送打印机的为同一张位图）；
 * 2. 只做蓝牙打印：优先列出已配对设备，可勾选"显示全部设备"查看未配对设备；
 * 3. 一键打印：同一任务内写入EPC（BARCODEID→ASCII hex）并出纸，不回读；
 * 4. 设置弹窗只有三项：打印分辨率、整版偏移X/Y、定位标签；
 * 5. 打印成功后标记ISPRINTBARCODE=02（待上传），并携带资产Id返回调用页。
 * <p>
 * 得实SDK回调均已在DsPrintManager内切回主线程，本页可直接更新UI。
 *
 * @author 10904
 */
public class DsPrintActivity extends BaseCompatActivity {

    /** 待打印资产（序列化副本，打印成功仅标记副本，调用页返回后同步自身内存对象） */
    public static final String EXTRA_PRINT_ASSETS = "EXTRA_PRINT_ASSETS";
    /** 打印抬头（调用页已按"打印抬头"设置计算好） */
    public static final String EXTRA_PRINT_TITLE = "EXTRA_PRINT_TITLE";
    /** 打印成功返回时携带的资产Id */
    public static final String EXTRA_PRINTED_ASSETS_ID = "EXTRA_PRINTED_ASSETS_ID";
    /** 仅连接模式：批量打印前先完成连接，连接成功即返回调用页，不加载资产、不显示预览、不打印 */
    public static final String EXTRA_CONNECT_ONLY = "EXTRA_CONNECT_ONLY";

    /** Android 12及以上使用新蓝牙权限模型（compileSdk=30无系统常量，用字符串） */
    private static final int API_ANDROID_12 = 31;
    private static final String PERMISSION_BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN";
    private static final String PERMISSION_BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    /** targetSdk=30走旧扫描权限模型，发现设备实际校验的是定位权限 */
    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQ_PERMISSIONS = 2001;

    private QMUITopBar topbar;
    private ImageView ivPreview;
    private TextView tvLabelInfo;
    private LinearLayout cardPreview;
    private TextView tvBtStatus;
    private QMUIButton btnBtScan;
    private CheckBox cbShowAll;
    private LinearLayout btDeviceContainer;
    private TextView tvDeviceHint;
    private QMUIButton btnPrint;

    /** 仅连接模式（批量打印前连接打印机） */
    private boolean connectOnly;
    /** 已通知连接成功（防重复返回） */
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
    /** 是否显示全部蓝牙设备（默认只显示已配对 + 名称像打印机的） */
    private boolean showAllDevices;
    /** 已配对设备 */
    private final List<BluetoothDevice> bondedDevices = new ArrayList<>();
    /** 扫描到的设备 */
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();
    /** 当前列表已展示的地址（去重） */
    private final List<String> shownAddresses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ds_print);
        DsPrintManager.ensureInit(this);

        connectOnly = getIntent().getBooleanExtra(EXTRA_CONNECT_ONLY, false);
        if (!connectOnly) {
            printAssets = (Assets) getIntent().getSerializableExtra(EXTRA_PRINT_ASSETS);
            if (printAssets == null) {
                showShortText("资产信息错误，无法打印");
                finish();
                return;
            }
            // 条码必须14位：不合法时提示并退出，让用户回资产页修改后重新打印
            if (!DsLabelRenderer.isValidBarcode(printAssets.getBARCODEID())) {
                showShortText(DsLabelRenderer.buildBarcodeError(printAssets.getBARCODEID()));
                finish();
                return;
            }
            epcHex = DsLabelRenderer.buildEpcHex(printAssets.getBARCODEID());
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

        requestPermissionsIfNeeded();
        loadBondedDevices();
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
        topbar.addRightTextButton("设置", View.generateViewId()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        ivPreview = findViewById(R.id.ivPreview);
        tvLabelInfo = findViewById(R.id.tvLabelInfo);
        cardPreview = findViewById(R.id.cardPreview);
        tvBtStatus = findViewById(R.id.tvBtStatus);
        btnBtScan = findViewById(R.id.btnBtScan);
        cbShowAll = findViewById(R.id.cbShowAll);
        btDeviceContainer = findViewById(R.id.btDeviceContainer);
        tvDeviceHint = findViewById(R.id.tvDeviceHint);
        btnPrint = findViewById(R.id.btnPrint);

        cardPreview.setVisibility(connectOnly ? View.GONE : View.VISIBLE);
        btnPrint.setVisibility(connectOnly ? View.GONE : View.VISIBLE);

        if (!connectOnly) {
            // 默认值与登录设置页一致：未保存过"打印位置"时按"居中"，避免标题被按居左绘制
            String position = SharedPreferencesUtils.getString(mContext,
                    SharedPreferencesUtils.KEY_PRINT_POSITION, "居中").trim();
            String printTitle = getIntent().getStringExtra(EXTRA_PRINT_TITLE);
            String labelType = application.getPrinterEffect();
            if (!DsLabelRenderer.hasTemplate(labelType)) {
                showShortText("当前打印类型暂未适配得实标签模板");
                finish();
                return;
            }
            labelBitmap = DsLabelRenderer.renderLabel(mContext, labelType, printAssets, printTitle, position);
            if (labelBitmap == null) {
                showShortText("标签生成失败，请检查资产信息");
                finish();
                return;
            }
            ivPreview.setImageBitmap(labelBitmap);
            tvLabelInfo.setText("条码编号：" + printAssets.getBARCODEID() + "\nEPC：" + epcHex);
        }

        cbShowAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showAllDevices = isChecked;
                rebuildDeviceList();
            }
        });

        btnBtScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtScan();
            }
        });
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPrint();
            }
        });

        if (DsPrintManager.isConnected()) {
            setBtStatus("蓝牙已连接，可直接打印");
        }
        updatePrintButton();
    }

    /** 打印按钮可用性：已连接 + 标签已生成 + 非打印中 */
    private void updatePrintButton() {
        btnPrint.setEnabled(!printing && labelBitmap != null && DsPrintManager.isConnected());
        btnPrint.setText(printing ? "打印中…" : "打印标签");
    }

    // ==================== 蓝牙：设备列表 ====================

    /** 加载已配对设备（优先显示，标注"已配对"） */
    private void loadBondedDevices() {
        bondedDevices.clear();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                return;
            }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded != null) {
                bondedDevices.addAll(bonded);
            }
        } catch (SecurityException ignored) {
            // Android 12+ 未授BLUETOOTH_CONNECT权限时读取会抛异常，授权回调里会重新加载
        } catch (Exception ignored) {
        }
        rebuildDeviceList();
    }

    /** 重建设备列表：已配对在上、扫描到的在下，按"显示全部"开关过滤 */
    private void rebuildDeviceList() {
        if (btDeviceContainer == null) {
            return;
        }
        btDeviceContainer.removeAllViews();
        shownAddresses.clear();
        for (BluetoothDevice device : bondedDevices) {
            appendDeviceView(device, true);
        }
        for (BluetoothDevice device : foundDevices) {
            appendDeviceView(device, false);
        }
        updateDeviceHint();
    }

    private void appendDeviceView(final BluetoothDevice device, boolean bonded) {
        final String address = safeAddress(device);
        if (StringUtils.isNullOrEmpty(address) || shownAddresses.contains(address)) {
            return;
        }
        String name = safeName(device);
        // 未配对设备默认只显示名称像打印机的，其余由"显示全部设备"打开
        if (!bonded && !showAllDevices && !looksLikePrinter(name)) {
            return;
        }
        shownAddresses.add(address);
        TextView item = new TextView(mContext);
        item.setText(name + (bonded ? "（已配对）" : "") + "\n" + address);
        item.setTextColor(Color.parseColor("#0077FF"));
        item.setTextSize(14);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice(device);
            }
        });
        btDeviceContainer.addView(item);
    }

    /** 设备列表为空时的引导：多数情况是打印机未配对且被过滤掉了 */
    private void updateDeviceHint() {
        if (tvDeviceHint == null) {
            return;
        }
        boolean empty = shownAddresses.isEmpty();
        if (!empty) {
            tvDeviceHint.setVisibility(View.GONE);
            return;
        }
        tvDeviceHint.setVisibility(View.VISIBLE);
        tvDeviceHint.setText(showAllDevices
                ? "未发现蓝牙设备，请确认打印机已开机并点击\"搜索蓝牙打印机\""
                : "未发现打印机，可勾选\"显示全部设备\"后重试");
    }

    /** 名称是否像打印机：DS/DASCOM/得实/Printer/BP/TP */
    private boolean looksLikePrinter(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return false;
        }
        String upper = name.toUpperCase();
        return upper.contains("DS") || upper.contains("DASCOM") || name.contains("得实")
                || upper.contains("PRINTER") || name.contains("打印机")
                || upper.contains("BP") || upper.contains("TP");
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
        foundDevices.clear();
        setBtStatus("正在搜索得实蓝牙打印机…");
        DsPrintManager.startScan(new DsPrintManager.ScanListener() {
            @Override
            public void onScanStart() {
                btnBtScan.setEnabled(false);
                btnBtScan.setText("搜索中…");
            }

            @Override
            public void onFindDevice(BluetoothDevice device) {
                onBtDeviceFound(device);
            }

            @Override
            public void onScanStop() {
                btnBtScan.setEnabled(true);
                btnBtScan.setText("搜索蓝牙打印机");
            }
        });
    }

    /** 扫描到设备：自动重连目标直接连接，其余去重后加入列表 */
    private void onBtDeviceFound(BluetoothDevice device) {
        String address = safeAddress(device);
        if (StringUtils.isNullOrEmpty(address)) {
            return;
        }
        if (!StringUtils.isNullOrEmpty(autoConnectMac) && autoConnectMac.equalsIgnoreCase(address)) {
            DsPrintManager.stopScan();
            connectDevice(device);
            return;
        }
        for (BluetoothDevice found : foundDevices) {
            if (address.equalsIgnoreCase(safeAddress(found))) {
                return;
            }
        }
        foundDevices.add(device);
        rebuildDeviceList();
    }

    private void connectDevice(final BluetoothDevice device) {
        if (btConnecting) {
            return;
        }
        String address = safeAddress(device);
        if (StringUtils.isNullOrEmpty(address)) {
            setBtStatus("无法读取该设备的蓝牙地址");
            return;
        }
        // 已连接且目标正是上次成功连接的设备：直接复用现有连接，避免重复open把连接断开
        if (DsPrintManager.isConnected() && address.equalsIgnoreCase(DsPrintSettings.getLastMac(mContext))) {
            setBtStatus("蓝牙已连接，可直接打印");
            updatePrintButton();
            notifyConnectedInConnectOnlyMode();
            return;
        }
        btConnecting = true;
        DsPrintManager.stopScan();
        setBtStatus("正在连接 " + safeName(device) + "…");
        DsPrintManager.connectBt(address, safeName(device), new DsPrintManager.ConnectCallback() {
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

    /** 按上次连接记录自动重连（手动连接其他设备即覆盖） */
    private void autoReconnectLast() {
        if (DsPrintManager.isConnected()) {
            updatePrintButton();
            notifyConnectedInConnectOnlyMode();
            return;
        }
        String mac = DsPrintSettings.getLastMac(mContext);
        if (StringUtils.isNullOrEmpty(mac)) {
            return;
        }
        autoConnectMac = mac;
        setBtStatus("正在自动重连上次设备 " + DsPrintSettings.getLastMacName(mContext) + "…");
        startBtScan();
    }

    // ==================== 打印 ====================

    /** 仅连接模式：连接成功后延时返回调用页，调用页在onActivityResult中续跑批量队列 */
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
        if (!DsPrintManager.isConnected()) {
            showShortText("请先连接蓝牙打印机");
            return;
        }
        printing = true;
        updatePrintButton();
        DsPrintManager.print(labelBitmap, epcHex, new DsPrintManager.PrintCallback() {
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

    // ==================== 设置弹窗（分辨率 / 整版偏移 / 定位标签） ====================

    private void showSettingsDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_ds_print_settings, null);
        final RadioGroup rgDpi = content.findViewById(R.id.rgDpi);
        final EditText etOffsetX = content.findViewById(R.id.etOffsetX);
        final EditText etOffsetY = content.findViewById(R.id.etOffsetY);
        QMUIButton btnLocateLabel = content.findViewById(R.id.btnLocateLabel);

        if (DsPrintSettings.getDpi(mContext) == DsPrintSettings.DPI_300) {
            rgDpi.check(R.id.rbDpi300);
        } else {
            rgDpi.check(R.id.rbDpi203);
        }
        etOffsetX.setText(String.valueOf(DsPrintSettings.getOffsetX(mContext)));
        etOffsetY.setText(String.valueOf(DsPrintSettings.getOffsetY(mContext)));

        btnLocateLabel.setEnabled(DsPrintManager.isConnected());
        btnLocateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!DsPrintManager.isConnected()) {
                    showShortText("请先连接蓝牙打印机");
                    return;
                }
                DsPrintManager.locateLabel(new DsPrintManager.PrintCallback() {
                    @Override
                    public void onResult(boolean success, String message) {
                        showShortText(message);
                    }
                });
            }
        });

        new AlertDialog.Builder(mContext)
                .setTitle("得实打印设置")
                .setView(content)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        int dpi = rgDpi.getCheckedRadioButtonId() == R.id.rbDpi300
                                ? DsPrintSettings.DPI_300 : DsPrintSettings.DPI_203;
                        float offsetX = parseOffset(etOffsetX.getText().toString());
                        float offsetY = parseOffset(etOffsetY.getText().toString());
                        boolean dpiChanged = dpi != DsPrintSettings.getDpi(mContext);
                        DsPrintSettings.setDpi(mContext, dpi);
                        DsPrintSettings.setOffsetX(mContext, offsetX);
                        DsPrintSettings.setOffsetY(mContext, offsetY);
                        // 分辨率/偏移都会改变位图，保存后按新参数重绘预览（打印发送的就是这张）
                        rerenderPreview();
                        if (dpiChanged) {
                            showShortText("分辨率已切换，请先打印一张确认版式");
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 偏移输入解析：非法输入按0处理，超范围按上限限幅 */
    private float parseOffset(String text) {
        try {
            return DsPrintSettings.clampOffset(Float.parseFloat(text.trim()));
        } catch (Exception e) {
            return 0f;
        }
    }

    /** 参数变化后重绘预览位图 */
    private void rerenderPreview() {
        if (connectOnly || printAssets == null) {
            return;
        }
        String position = SharedPreferencesUtils.getString(mContext,
                SharedPreferencesUtils.KEY_PRINT_POSITION, "居中").trim();
        String printTitle = getIntent().getStringExtra(EXTRA_PRINT_TITLE);
        Bitmap old = labelBitmap;
        Bitmap fresh = DsLabelRenderer.renderLabel(mContext, application.getPrinterEffect(),
                printAssets, printTitle, position);
        if (fresh == null) {
            showShortText("标签重绘失败，请检查打印类型");
            return;
        }
        labelBitmap = fresh;
        ivPreview.setImageBitmap(fresh);
        if (old != null && old != fresh && !old.isRecycled()) {
            old.recycle();
        }
        updatePrintButton();
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

    /** 蓝牙扫描所需的全部权限：旧模型下真正缺的是定位权限 */
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
            // 授权后补加载已配对设备，并继续自动重连流程
            loadBondedDevices();
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

    /** 安全读取蓝牙地址（Android 12+ 未授BLUETOOTH_CONNECT权限时会抛SecurityException） */
    private String safeAddress(BluetoothDevice device) {
        try {
            return device == null ? "" : device.getAddress();
        } catch (SecurityException e) {
            return "";
        }
    }

    /** 安全读取设备名 */
    private String safeName(BluetoothDevice device) {
        try {
            String name = device == null ? null : device.getName();
            return StringUtils.isNullOrEmpty(name) ? "未知设备" : name;
        } catch (SecurityException e) {
            return "未知设备";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        // 停止扫描并清空回调；不主动断开连接，便于连续打印
        DsPrintManager.releaseScanListener();
        if (labelBitmap != null && !labelBitmap.isRecycled()) {
            labelBitmap.recycle();
            labelBitmap = null;
        }
        super.onDestroy();
    }
}