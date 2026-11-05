package com.gsoft.inventory;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.jbw.sdk.print.bluetooth.BwbtManager;
import com.jbw.sdk.print.bluetooth.callback.ConnectDeviceCallback;
import com.jbw.sdk.print.bluetooth.callback.ScanCallback;
import com.jbw.sdk.print.label.BitmapTools;
import com.jbw.sdk.print.model.ReturnType;
import com.jbw.sdk.print.model.RotateType;
import com.jbw.sdk.print.wifi.WifiDevice;
import com.jbw.sdk.print.wifi.WifiManager;
import com.jbw.sdk.print.wifi.callback.WifiConnectCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝威打印机可行性验证页（临时测试类，验证完成后连同Manifest注册一并删除）
 * <p>
 * 验证内容：
 * 1. BaoWei_PrintSdk.jar 在 AndroidX 工程下可编译、可运行（若运行时崩溃报 android.support.* 类缺失，
 *    说明 jar 引用了旧 Support 库，需开启 jetifier 或向厂家索取 AndroidX 版本 SDK）；
 * 2. 蓝牙链路：BwbtManager 扫描 → 连接 → printBitmap；
 * 3. WiFi 链路：WifiManager 连接（IP:端口）→ printBitmap；
 * 4. BitmapTools 文本/二维码/一维码渲染（顺带验证 zxing 依赖链路）。
 * <p>
 * 前置条件：BaoWei_PrintSdk.jar 已放入 app/libs/ 目录
 * <p>
 * 启动方式（任选其一）：
 * - adb shell am start -n com.gsoft.inventory/com.gsoft.inventory.BwPrintTestActivity
 * - IDE 运行配置中指定启动本 Activity
 */
public class BwPrintTestActivity extends AppCompatActivity {

    private static final String TAG = "BwPrintTest";
    private static final int REQ_PERMISSIONS = 1001;

    /** Android 12（API 31）起蓝牙扫描/连接为"附近设备"运行时权限。
     *  项目compileSdk=30，编译期SDK中不存在BLUETOOTH_SCAN/CONNECT常量与VERSION_CODES.S，
     *  这里用权限全名字符串与API数字规避（名称与Manifest中声明的一致） */
    private static final int API_ANDROID_12 = 31;
    private static final String PERMISSION_BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN";
    private static final String PERMISSION_BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";

    /** 测试标签纸张尺寸（单位mm），按实际标签纸调整 */
    private static final float PAPER_WIDTH = 50;
    private static final float PAPER_HEIGHT = 30;

    private TextView tvStatus;
    private ImageView ivPreview;
    private LinearLayout deviceContainer;
    private EditText etWifiIp;
    private EditText etWifiPort;

    private final List<BluetoothDevice> btDevices = new ArrayList<>();
    private Bitmap labelBitmap;
    /** 蓝牙连接进行中标志，防止阻塞连接期间重复点击设备 */
    private volatile boolean btConnecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化宝威SDK：蓝牙与WiFi均为单例，各初始化一次
        BwbtManager.getInstance().init(this);
        WifiManager.getInstance().init(this);
        buildViews();
        requestPermissionsIfNeeded();
        buildLabel();
    }

    // ==================== 界面构建（动态创建，不依赖布局文件） ====================

    private void buildViews() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(root);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("宝威打印机可行性验证（临时测试页）");
        tvTitle.setTextSize(17);
        root.addView(tvTitle);

        tvStatus = new TextView(this);
        tvStatus.setText("状态：未连接");
        tvStatus.setTextSize(14);
        tvStatus.setPadding(0, dp(8), 0, dp(8));
        root.addView(tvStatus);

        // 标签预览
        ivPreview = new ImageView(this);
        ivPreview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(160)));
        ivPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivPreview.setBackgroundColor(Color.LTGRAY);
        root.addView(ivPreview);

        // ① 蓝牙
        addSectionTitle(root, "① 蓝牙连接（请点击名称以BW开头的宝威打印机）");
        Button btnScan = new Button(this);
        btnScan.setText("开始搜索蓝牙打印机");
        btnScan.setAllCaps(false);
        btnScan.setOnClickListener(v -> startBtScan());
        root.addView(btnScan);
        deviceContainer = new LinearLayout(this);
        deviceContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceContainer);

        // ② WiFi
        addSectionTitle(root, "② WiFi连接（打印机默认端口9100）");
        etWifiIp = new EditText(this);
        etWifiIp.setHint("打印机IP，如 192.168.1.100");
        etWifiIp.setText("192.168.1.100");
        root.addView(etWifiIp);
        etWifiPort = new EditText(this);
        etWifiPort.setHint("端口");
        etWifiPort.setText("9100");
        etWifiPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(etWifiPort);
        Button btnWifiConnect = new Button(this);
        btnWifiConnect.setText("连接WiFi打印机");
        btnWifiConnect.setAllCaps(false);
        btnWifiConnect.setOnClickListener(v -> connectWifi());
        root.addView(btnWifiConnect);

        // ③ 打印
        addSectionTitle(root, "③ 打印测试标签");
        Button btnPrint = new Button(this);
        btnPrint.setText("打印测试标签");
        btnPrint.setAllCaps(false);
        btnPrint.setOnClickListener(v -> doPrint());
        root.addView(btnPrint);

        setContentView(scrollView);
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setPadding(0, dp(16), 0, dp(4));
        parent.addView(tv);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ==================== 标签绘制与预览 ====================

    /** 绘制测试标签：中文文本 + 二维码 + CODE128一维码 */
    private void buildLabel() {
        BitmapTools tools = new BitmapTools(this);
        // 纸张尺寸（mm）
        tools.setPaper(PAPER_WIDTH, PAPER_HEIGHT);
        // 标题文本（自适应字号）
        tools.addAdText("宝威打印机测试", 2, 2, PAPER_WIDTH - 4, 7);
        // 正文文本
        tools.addAdText("Hello BW 123456", 2, 10, 26, 7);
        // 二维码
        tools.addQrcode("BW-TEST-QR-12345678", 32, 9, 16, 16);
        // 一维码 CODE128（9参版本，顺带验证zxing的BarcodeFormat链路）
        tools.addBarcode("12345678", 2, 20, PAPER_WIDTH - 4, 8,
                RotateType.ROTATE0, true, 3, BarcodeFormat.CODE_128);
        // 出纸方向与实际不符时，调整这里的 RotateType（ROTATE0/90/180/270）再试
        labelBitmap = tools.getPreviewBitmap(RotateType.ROTATE0);
        ivPreview.setImageBitmap(labelBitmap);
    }

    // ==================== 蓝牙链路 ====================

    private void startBtScan() {
        // 权限守卫：Android 12+ 未授予 BLUETOOTH_SCAN 时先申请再扫描。
        // 若直接调用，BwbtManager.scan() 内部 BluetoothAdapter.isDiscovering() 会抛 SecurityException 崩溃
        if (Build.VERSION.SDK_INT >= API_ANDROID_12
                && checkSelfPermission(PERMISSION_BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    PERMISSION_BLUETOOTH_SCAN,
                    PERMISSION_BLUETOOTH_CONNECT}, REQ_PERMISSIONS);
            toast("请先授予蓝牙权限，授权后再点击搜索");
            return;
        }
        deviceContainer.removeAllViews();
        btDevices.clear();
        setStatus("正在搜索宝威蓝牙打印机...");
        BwbtManager.getInstance().scan(new ScanCallback() {
            @Override
            public void onScanStart(boolean isOn) {
                setStatus(isOn ? "搜索中..." : "搜索启动失败（检查蓝牙是否可用）");
            }

            @Override
            public void onScanStop(List<BluetoothDevice> deviceList) {
                setStatus(btDevices.isEmpty() ? "未搜索到蓝牙设备" : "搜索结束，点击BW开头的打印机连接");
            }

            @Override
            public void onFindDevice(BluetoothDevice device) {
                addBtDeviceView(device);
            }
        });
    }

    private void addBtDeviceView(final BluetoothDevice device) {
        if (btDevices.contains(device)) {
            return;
        }
        btDevices.add(device);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = new TextView(BwPrintTestActivity.this);
                tv.setText("点击连接：" + safeDeviceLabel(device));
                tv.setTextColor(Color.parseColor("#0077FF"));
                tv.setPadding(dp(12), dp(10), dp(12), dp(10));
                tv.setOnClickListener(v -> connectBtDevice(device));
                deviceContainer.addView(tv);
            }
        });
    }

    private void connectBtDevice(final BluetoothDevice device) {
        if (btConnecting) {
            toast("正在连接中，请稍候");
            return;
        }
        btConnecting = true;
        setStatus("正在连接蓝牙设备...");
        // 宝威SDK的connectDevice内部为同步阻塞的BluetoothSocket连接（实测主线程直接调用会卡死甚至ANR），
        // 必须放到工作线程执行；回调中setStatus/toast已内部切回主线程，安全
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BwbtManager.getInstance().connectDevice(device, new ConnectDeviceCallback() {
                        @Override
                        public void connectSuccess(final BluetoothDevice device) {
                            setStatus("蓝牙已连接：" + safeDeviceName(device) + "，可执行打印");
                        }

                        @Override
                        public void disConnect() {
                            setStatus("蓝牙已断开");
                        }

                        @Override
                        public void connectError(final Throwable throwable) {
                            setStatus("蓝牙连接失败：" + (throwable == null ? "未知错误" : throwable.getMessage()));
                        }
                    });
                } finally {
                    btConnecting = false;
                }
            }
        }, "bw-bt-connect").start();
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return (name == null || name.isEmpty()) ? "未知设备" : name;
        } catch (SecurityException e) {
            return "未知设备";
        }
    }

    private String safeDeviceLabel(BluetoothDevice device) {
        String address = "";
        try {
            address = device.getAddress();
        } catch (SecurityException ignored) {
        }
        return safeDeviceName(device) + "\n" + address;
    }

    // ==================== WiFi链路 ====================

    private void connectWifi() {
        String ip = etWifiIp.getText().toString().trim();
        String portStr = etWifiPort.getText().toString().trim();
        if (ip.isEmpty()) {
            toast("请输入打印机IP");
            return;
        }
        int port = 9100;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {
        }
        setStatus("WiFi连接中：" + ip + ":" + port);
        // 注意：WifiConnectCallback 回调不在主线程，UI更新必须切主线程
        WifiManager.getInstance().connect(new WifiDevice(ip, port), new WifiConnectCallback() {
            @Override
            public void onConnectSuccess(final WifiDevice device) {
                setStatus("WiFi已连接：" + device.getIp() + ":" + device.getPort() + "，可执行打印");
            }

            @Override
            public void onDisConnect() {
                setStatus("WiFi已断开（长时间不使用会自动断开，打印前请重新连接）");
            }

            @Override
            public void onConnectError(final Throwable throwable) {
                setStatus("WiFi连接失败：" + (throwable == null ? "未知错误" : throwable.getMessage()));
            }
        });
    }

    // ==================== 打印 ====================

    private void doPrint() {
        if (labelBitmap == null) {
            toast("预览图未生成，无法打印");
            return;
        }
        // printBitmap为同步阻塞发送（位图数据经蓝牙/WiFi传输），放工作线程避免主线程ANR；结果回主线程提示
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean viaBt = BwbtManager.getInstance().isConnection();
                final boolean viaWifi = !viaBt && WifiManager.getInstance().isConnected();
                if (!viaBt && !viaWifi) {
                    toast("未连接打印机：请先完成蓝牙或WiFi连接");
                    return;
                }
                final ReturnType ret = viaBt
                        ? BwbtManager.getInstance().printBitmap(labelBitmap)
                        : WifiManager.getInstance().printBitmap(labelBitmap);
                final String channel = viaBt ? "蓝牙" : "WiFi";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handlePrintResult(channel, ret);
                    }
                });
            }
        }, "bw-print").start();
    }

    private void handlePrintResult(String channel, ReturnType ret) {
        if (ret == ReturnType.NO_ERROR) {
            setStatus(channel + "打印指令已发送，请查看打印机是否出纸");
            toast("打印指令已发送");
        } else {
            setStatus(channel + "打印失败：" + ret.name());
            toast("打印失败：" + ret.name());
        }
    }

    // ==================== 权限与工具 ====================

    private void requestPermissionsIfNeeded() {
        List<String> need = new ArrayList<>();
        // Android 12+（API 31）：蓝牙扫描/连接为"附近设备"运行时权限。
        // 项目Manifest已声明BLUETOOTH_SCAN/CONNECT，在Android 12+设备上系统按新权限模型强制检查，必须运行时申请
        if (Build.VERSION.SDK_INT >= API_ANDROID_12) {
            if (checkSelfPermission(PERMISSION_BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                need.add(PERMISSION_BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(PERMISSION_BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                need.add(PERMISSION_BLUETOOTH_CONNECT);
            }
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!need.isEmpty()) {
            requestPermissions(need.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMISSIONS) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])
                    && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                toast("未授予定位权限，蓝牙可能搜索不到设备");
            }
            if (Build.VERSION.SDK_INT >= API_ANDROID_12
                    && (PERMISSION_BLUETOOTH_SCAN.equals(permissions[i])
                    || PERMISSION_BLUETOOTH_CONNECT.equals(permissions[i]))
                    && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                toast("未授予蓝牙权限，无法搜索/连接打印机");
            }
        }
    }

    private void setStatus(final String text) {
        Log.i(TAG, text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvStatus != null) {
                    tvStatus.setText("状态：" + text);
                }
            }
        });
    }

    private void toast(String text) {
        runOnUiThread(() -> Toast.makeText(BwPrintTestActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 测试页不主动断开连接，便于反复打印验证；正式集成时需按需调用 disconnect
    }
}