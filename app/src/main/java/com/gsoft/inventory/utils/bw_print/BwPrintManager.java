package com.gsoft.inventory.utils.bw_print;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.jbw.sdk.print.bluetooth.BwbtManager;
import com.jbw.sdk.print.bluetooth.callback.ConnectDeviceCallback;
import com.jbw.sdk.print.bluetooth.callback.ScanCallback;
import com.jbw.sdk.print.bluetooth.callback.TidEpcListener;
import com.jbw.sdk.print.model.ReturnType;
import com.jbw.sdk.print.wifi.WifiDevice;
import com.jbw.sdk.print.wifi.WifiManager;
import com.jbw.sdk.print.wifi.callback.WifiConnectCallback;

import java.util.List;

/**
 * 宝威打印机能力门面（蓝牙 + WiFi）
 * <p>
 * 统一收口宝威SDK的踩坑点，业务侧只调用本类：
 * 1. 线程规则：connectDevice/printBitmap 为同步阻塞调用，内部一律切工作线程执行，避免主线程ANR；
 *    SDK的连接/搜索/RFID回调不在主线程，统一用主线程Handler切回后回调，调用方可直接更新UI；
 * 2. 连接信息持久化：蓝牙MAC/名称、WiFi IP/端口保存到SharedPreferences（只存最后一次，重新连接其他设备即覆盖），
 *    下次打开宝威打印页时自动重连；
 * 3. 打印返回码：ReturnType.NO_ERROR 即发送成功（含EPC写入指令下发），与老打印机"发起打印即视为完成"的语义一致。
 */
public class BwPrintManager {

    /** 最后一次连接方式：蓝牙 */
    public static final String CONN_TYPE_BT = "BT";
    /** 最后一次连接方式：WiFi */
    public static final String CONN_TYPE_WIFI = "WIFI";
    /** 宝威WiFi打印机默认端口 */
    public static final int DEFAULT_WIFI_PORT = 9100;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * 出纸方向修正角度：威手持设备打印头倒装，预览方向的位图直发会打出上下颠倒的标签，
     * 发送前需整体预旋转180°（仅影响发送数据，预览位图不变）；换机型方向不符时改此角度
     */
    private static final int PRINTER_ROTATE_DEGREES = 180;

    private static boolean sInited = false;
    private static Context sAppContext;
    private static TidEpcResultListener sTidEpcListener;

    private BwPrintManager() {
    }

    /**
     * 初始化宝威SDK（蓝牙与WiFi均为单例，全局只初始化一次，重复调用直接忽略）
     * <p>
     * 注意：SDK的init要求传入Activity（与已验证的BwPrintTestActivity写法一致），
     * 传普通Context会编译报"无法解析方法 init(Context)"；applicationContext仅用于本类持久化。
     */
    public static void ensureInit(Activity activity) {
        if (sInited) {
            return;
        }
        sAppContext = activity.getApplicationContext();
        BwbtManager.getInstance().init(activity);
        WifiManager.getInstance().init(activity);
        sInited = true;
    }

    // ==================== 连接状态 ====================

    public static boolean isBtConnected() {
        return BwbtManager.getInstance().isConnection();
    }

    public static boolean isWifiConnected() {
        return WifiManager.getInstance().isConnected();
    }

    /** 蓝牙或WiFi任一已连接即视为可打印 */
    public static boolean isConnected() {
        return isBtConnected() || isWifiConnected();
    }

    // ==================== 蓝牙：搜索与连接 ====================

    /** 搜索宝威蓝牙打印机（SDK只返回宝威公司设备），回调已切回主线程 */
    public static void scanBt(final ScanListener listener) {
        BwbtManager.getInstance().scan(new ScanCallback() {
            @Override
            public void onScanStart(final boolean isOn) {
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onScanStart(isOn);
                    }
                });
            }

            @Override
            public void onScanStop(List<BluetoothDevice> deviceList) {
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onScanStop();
                    }
                });
            }

            @Override
            public void onFindDevice(final BluetoothDevice device) {
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFindDevice(device);
                    }
                });
            }
        });
    }

    /** 停止蓝牙搜索 */
    public static void stopScanBt() {
        BwbtManager.getInstance().stopScan();
    }

    /** 在SDK已搜索过的设备列表中按地址取设备（用于自动重连上次设备），未找到返回null */
    public static BluetoothDevice getBtDeviceByAddress(String address) {
        if (StringUtils.isNullOrEmpty(address)) {
            return null;
        }
        try {
            return BwbtManager.getInstance().getBluetoothDevice(address);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 连接蓝牙打印机：connectDevice 为同步阻塞调用（实测主线程直接调用会卡死甚至ANR），
     * 内部切工作线程执行，连接结果切回主线程回调
     */
    public static void connectBt(final BluetoothDevice device, final ConnectCallback callback) {
        if (device == null) {
            postConnectFail(callback, "蓝牙设备为空");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BwbtManager.getInstance().connectDevice(device, new ConnectDeviceCallback() {
                        @Override
                        public void connectSuccess(BluetoothDevice connected) {
                            // 连接成功即保存为"上次设备"，后续重新连接其他设备会自动覆盖
                            saveBtDevice(connected);
                            postConnectSuccess(callback, "蓝牙已连接：" + safeDeviceName(connected) + "，可打印");
                        }

                        @Override
                        public void disConnect() {
                            postConnectFail(callback, "蓝牙连接已断开，请重新连接");
                        }

                        @Override
                        public void connectError(Throwable throwable) {
                            postConnectFail(callback, "蓝牙连接失败：" + (throwable == null ? "未知错误" : throwable.getMessage()));
                        }
                    });
                } catch (Exception e) {
                    postConnectFail(callback, "蓝牙连接异常：" + e.getMessage());
                }
            }
        }, "bw-bt-connect").start();
    }

    // ==================== WiFi：连接 ====================

    /** 连接WiFi打印机（端口默认9100）；SDK回调不在主线程，统一切回主线程 */
    public static void connectWifi(final String ip, final int port, final ConnectCallback callback) {
        if (StringUtils.isNullOrEmpty(ip)) {
            postConnectFail(callback, "请输入打印机IP");
            return;
        }
        final int finalPort = port > 0 ? port : DEFAULT_WIFI_PORT;
        WifiManager.getInstance().connect(new WifiDevice(ip, finalPort), new WifiConnectCallback() {
            @Override
            public void onConnectSuccess(WifiDevice device) {
                saveWifiDevice(ip, finalPort);
                postConnectSuccess(callback, "WiFi已连接：" + ip + ":" + finalPort + "，可打印");
            }

            @Override
            public void onDisConnect() {
                postConnectFail(callback, "WiFi连接已断开，请重新连接");
            }

            @Override
            public void onConnectError(Throwable throwable) {
                postConnectFail(callback, "WiFi连接失败：" + (throwable == null ? "未知错误" : throwable.getMessage()));
            }
        });
    }

    // ==================== 打印 ====================

    /**
     * 打印标签（epcHex非空时在同一打印任务内写入RFID的EPC）；
     * printBitmap 为同步阻塞发送（位图数据经蓝牙/WiFi传输），内部切工作线程，结果切回主线程回调
     * 默认蓝牙优先
     */
    public static void print(final Bitmap bitmap, final String epcHex, final PrintCallback callback) {
        print(bitmap, epcHex, false, callback);
    }

    /**
     * 打印标签，支持指定优先通道
     * @param preferWifi true 时强制优先走 WiFi 通道（用户当前在 WiFi 页签），false 时蓝牙优先
     */
    public static void print(final Bitmap bitmap, final String epcHex, final boolean preferWifi, final PrintCallback callback) {
        if (bitmap == null) {
            postPrintResult(callback, false, "标签图未生成，无法打印");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean viaWifi = preferWifi ? isWifiConnected() : (!isBtConnected() && isWifiConnected());
                final boolean viaBt = !viaWifi && isBtConnected();
                if (!viaBt && !viaWifi) {
                    postPrintResult(callback, false, "未连接打印机，请先连接蓝牙或WiFi");
                    return;
                }
                final String channel = viaBt ? "蓝牙" : "WiFi";
                final Bitmap sendBitmap = rotateForPrinter(bitmap);
                try {
                    ReturnType ret;
                    if (StringUtils.isNullOrEmpty(epcHex)) {
                        ret = viaBt ? BwbtManager.getInstance().printBitmap(sendBitmap) : WifiManager.getInstance().printBitmap(sendBitmap);
                    } else {
                        attachTidEpcListener(viaBt);
                        ret = viaBt ? BwbtManager.getInstance().printBitmap(sendBitmap, epcHex) : WifiManager.getInstance().printBitmap(sendBitmap, epcHex);
                    }
                    if (ret == ReturnType.NO_ERROR) {
                        postPrintResult(callback, true, channel + "打印指令已发送");
                    } else {
                        postPrintResult(callback, false, channel + "打印失败：" + describeReturn(ret));
                    }
                } finally {
                    // printBitmap为同步发送，发送结束即回收旋转副本；原图归调用方（预览页/批量执行器）管理
                    if (sendBitmap != bitmap && !sendBitmap.isRecycled()) {
                        sendBitmap.recycle();
                    }
                }
            }
        }, "bw-print").start();
    }

    /**
     * 出纸方向修正：按 PRINTER_ROTATE_DEGREES 生成一张旋转副本供发送（原预览位图不改动），
     * 角度为0的倍数或旋转失败时返回原图
     */
    private static Bitmap rotateForPrinter(Bitmap src) {
        if (src == null || PRINTER_ROTATE_DEGREES % 360 == 0) {
            return src;
        }
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(PRINTER_ROTATE_DEGREES);
            Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            return rotated == null ? src : rotated;
        } catch (Exception e) {
            return src;
        }
    }

    /** 打印返回码转中文提示 */
    private static String describeReturn(ReturnType ret) {
        if (ret == ReturnType.BLUE_NO_CONNECTED) {
            return "蓝牙未连接";
        }
        if (ret == ReturnType.WIFI_NO_CONNECTED) {
            return "WiFi未连接";
        }
        if (ret == ReturnType.NO_BITMAP) {
            return "标签图无效";
        }
        if (ret == ReturnType.NO_SET_PAPER) {
            return "未设置纸张";
        }
        return "打印出错";
    }

    // ==================== RFID写入结果回读（TID/EPC） ====================

    /** EPC写入结果监听：errCode 0成功 1通讯错误 2写入失败 3TID缺失 4EPC缺失（SDK文档定义） */
    public interface TidEpcResultListener {
        void onTidEpc(String tid, String epc, int errCode);
    }

    /** 注册EPC写入结果监听（页面销毁时请调用removeTidEpcListener释放，避免内存泄漏） */
    public static void setTidEpcListener(TidEpcResultListener listener) {
        sTidEpcListener = listener;
    }

    /** 释放EPC写入结果监听 */
    public static void removeTidEpcListener() {
        sTidEpcListener = null;
        BwbtManager.getInstance().removeTidEpcListener();
        WifiManager.getInstance().removeTidEpcListener();
    }

    /** 打印前把监听挂到当前使用的通道上（SDK回调不在主线程，这里统一切回主线程） */
    private static void attachTidEpcListener(boolean viaBt) {
        if (sTidEpcListener == null) {
            return;
        }
        TidEpcListener sdkListener = new TidEpcListener() {
            @Override
            public void onTidEpcReceived(final String tid, final String epc, final int errCode) {
                MAIN_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        if (sTidEpcListener != null) {
                            sTidEpcListener.onTidEpc(tid, epc, errCode);
                        }
                    }
                });
            }
        };
        if (viaBt) {
            BwbtManager.getInstance().setTidEpcListener(sdkListener);
        } else {
            WifiManager.getInstance().setTidEpcListener(sdkListener);
        }
    }

    // ==================== 连接信息持久化（保存上次，重连可覆盖） ====================

    private static void saveBtDevice(BluetoothDevice device) {
        if (sAppContext == null || device == null) {
            return;
        }
        String address = "";
        try {
            address = device.getAddress();
        } catch (SecurityException ignored) {
        }
        SharedPreferencesUtils.putString(sAppContext, SharedPreferencesUtils.KEY_BW_CONN_TYPE, CONN_TYPE_BT);
        SharedPreferencesUtils.putString(sAppContext, SharedPreferencesUtils.KEY_BW_BT_MAC, address);
        SharedPreferencesUtils.putString(sAppContext, SharedPreferencesUtils.KEY_BW_BT_NAME, safeDeviceName(device));
    }

    private static void saveWifiDevice(String ip, int port) {
        if (sAppContext == null) {
            return;
        }
        SharedPreferencesUtils.putString(sAppContext, SharedPreferencesUtils.KEY_BW_CONN_TYPE, CONN_TYPE_WIFI);
        SharedPreferencesUtils.putString(sAppContext, SharedPreferencesUtils.KEY_BW_WIFI_IP, ip);
        SharedPreferencesUtils.putInt(sAppContext, SharedPreferencesUtils.KEY_BW_WIFI_PORT, port);
    }

    /** 上次连接方式：BT / WIFI，空串表示无记录 */
    public static String getLastConnType() {
        return sAppContext == null ? "" : SharedPreferencesUtils.getString(sAppContext, SharedPreferencesUtils.KEY_BW_CONN_TYPE, "");
    }

    public static String getLastBtMac() {
        return sAppContext == null ? "" : SharedPreferencesUtils.getString(sAppContext, SharedPreferencesUtils.KEY_BW_BT_MAC, "");
    }

    public static String getLastBtName() {
        return sAppContext == null ? "" : SharedPreferencesUtils.getString(sAppContext, SharedPreferencesUtils.KEY_BW_BT_NAME, "");
    }

    public static String getLastWifiIp() {
        return sAppContext == null ? "" : SharedPreferencesUtils.getString(sAppContext, SharedPreferencesUtils.KEY_BW_WIFI_IP, "");
    }

    public static int getLastWifiPort() {
        return sAppContext == null ? DEFAULT_WIFI_PORT : SharedPreferencesUtils.getInt(sAppContext, SharedPreferencesUtils.KEY_BW_WIFI_PORT, DEFAULT_WIFI_PORT);
    }

    /** 安全读取设备名（Android 12+ 未授BLUETOOTH_CONNECT权限时getName会抛SecurityException） */
    public static String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device == null ? null : device.getName();
            return StringUtils.isNullOrEmpty(name) ? "未知设备" : name;
        } catch (SecurityException e) {
            return "未知设备";
        }
    }

    // ==================== 回调接口 ====================

    /** 连接结果回调（已在主线程） */
    public interface ConnectCallback {
        void onSuccess(String message);

        void onFail(String message);
    }

    /** 打印结果回调（已在主线程） */
    public interface PrintCallback {
        void onResult(boolean success, String message);
    }

    /** 蓝牙搜索回调（已在主线程） */
    public interface ScanListener {
        void onScanStart(boolean isOn);

        void onFindDevice(BluetoothDevice device);

        void onScanStop();
    }

    private static void postConnectSuccess(final ConnectCallback callback, final String message) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(message);
            }
        });
    }

    private static void postConnectFail(final ConnectCallback callback, final String message) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onFail(message);
            }
        });
    }

    private static void postPrintResult(final PrintCallback callback, final boolean success, final String message) {
        if (callback == null) {
            return;
        }
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(success, message);
            }
        });
    }
}