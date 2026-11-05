package com.gsoft.inventory.utils.ds_print;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import com.dstp2x.sdk.beans.ResultCode;
import com.dstp2x.sdk.command.sender.PrinterOperator;
import com.dstp2x.sdk.command.sender.RFID;
import com.dstp2x.sdk.command.sender.ZPL;
import com.dstp2x.sdk.connection.BluetoothPipe;
import com.dstp2x.sdk.connection.Pipe;
import com.dstp2x.sdk.utils.BluetoothDiscoverer;
import com.gsoft.inventory.utils.StringUtils;

/**
 * 得实RFID打印机能力门面（只做蓝牙，不含WiFi/USB）
 * <p>
 * 统一收口得实SDK的踩坑点，业务侧只调用本类：
 * 1. 线程规则：SDK所有命令调用在主线程会返回ERROR_IN_MAIN_THREAD，因此连接/扫描/打印/定位
 *    一律切工作线程执行，结果统一切回主线程回调，调用方可直接更新UI；
 * 2. 连接对象：Pipe为长连接，重连时必须重建Pipe与ZPL对象（SDK对象绑定Pipe，复用过期的会失败）；
 * 3. RFID一体打印只能走ZPL：得实TSPL类没有setRFIDDataForPrint，
 *    且setRFIDDataForPrint必须在setLabelStart之前调用；
 * 4. 中文标签必须整张位图下发：ZPL原生printText走内置字库不支持中文，
 *    这里统一"整张位图 printBitmap(0,0,bmp,1)"，位图像素即打印机dot；
 * 5. 不回读EPC：按业务要求只写不读，用无参setLabelEnd()收尾。
 * <p>
 * 代码固定、不对外暴露的参数：
 * - 二值化灰度阈值175（官方demo默认值）；
 * - 连接/读写超时20秒；
 * - 打印后等待出纸的最多次数40（约数秒，超时不影响成功判定）；
 * - 出纸方向修正0度（得实打印头方向与位图一致，如需翻转改SEND_ROTATE_DEGREES）。
 *
 * @author 10904
 */
public class DsPrintManager {

    /** 连接/读写超时（ms）：蓝牙配对+SPP建链在这个量级，过短会在配对弹窗未操作完就失败 */
    private static final int CONNECT_TIMEOUT_MS = 20000;

    /** 打印后等待打印就绪的最大轮询次数（超时不影响成功判定，只用于节流） */
    private static final int WAIT_PRINTER_OK_MAX_COUNT = 40;

    /** 二值化灰度阈值：固定175（官方demo默认值，不暴露到设置） */
    private static final int GRAY_THRESHOLD = 175;

    /**
     * 出纸方向修正角度：得实打印头方向与位图一致，默认0（不旋转）。
     * 若换机型后打出来上下颠倒/左右镜像，改这里（180/90/270），仅影响发送数据，预览不变。
     */
    private static final int SEND_ROTATE_DEGREES = 0;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Context sAppContext;
    private static volatile Pipe sPipe;
    private static BluetoothDiscoverer sDiscoverer;
    private static ScanListener sScanListener;
    private static volatile boolean sConnecting;

    private DsPrintManager() {
    }

    /** 初始化（只需ApplicationContext，得实SDK无全局init要求） */
    public static void ensureInit(Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
        }
    }

    // ==================== 连接状态 ====================

    public static boolean isConnected() {
        Pipe pipe = sPipe;
        try {
            return pipe != null && pipe.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /** 主动断开（换设备或排查问题时用；正常打印后可保持连接便于连续打印） */
    public static void disconnect() {
        final Pipe pipe = sPipe;
        sPipe = null;
        if (pipe == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pipe.close();
                } catch (Exception ignored) {
                }
            }
        }, "ds-disconnect").start();
    }

    // ==================== 蓝牙：连接 ====================

    /**
     * 连接蓝牙打印机：needBond=true 走系统配对弹窗（首次需要用户确认），
     * open() 为同步阻塞调用，内部切工作线程，结果切回主线程
     *
     * @param mac 设备地址
     * @param name 设备名称（仅用于状态提示与持久化）
     */
    public static void connectBt(final String mac, final String name, final ConnectCallback callback) {
        if (StringUtils.isNullOrEmpty(mac)) {
            postConnectFail(callback, "蓝牙设备地址为空，请重新搜索");
            return;
        }
        if (sConnecting) {
            return;
        }
        sConnecting = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pipe old = sPipe;
                sPipe = null;
                if (old != null) {
                    try {
                        old.close();
                    } catch (Exception ignored) {
                    }
                }
                BluetoothPipe pipe = null;
                try {
                    pipe = new BluetoothPipe(sAppContext, mac, true, null);
                    pipe.setTimeOut(CONNECT_TIMEOUT_MS);
                    int result = pipe.open();
                    if (result == ResultCode.SUCCESS && pipe.isConnected()) {
                        sPipe = pipe;
                        if (sAppContext != null) {
                            DsPrintSettings.setLastDevice(sAppContext, mac, name);
                        }
                        postConnectSuccess(callback, "蓝牙已连接：" + safeName(name) + "，可打印");
                    } else {
                        try {
                            pipe.close();
                        } catch (Exception ignored) {
                        }
                        postConnectFail(callback, "蓝牙连接失败：" + ResultCode.getMessage(result));
                    }
                } catch (Exception e) {
                    if (pipe != null) {
                        try {
                            pipe.close();
                        } catch (Exception ignored) {
                        }
                    }
                    postConnectFail(callback, "蓝牙连接异常：" + e.getMessage());
                } finally {
                    sConnecting = false;
                }
            }
        }, "ds-bt-connect").start();
    }

    // ==================== 蓝牙：搜索 ====================

    /**
     * 开始搜索蓝牙设备（得实SDK只提供原始设备回调，不做型号过滤，过滤由页面负责）
     * 回调已切回主线程
     */
    public static void startScan(final ScanListener listener) {
        if (sAppContext == null) {
            return;
        }
        sScanListener = listener;
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                stopScanInternal();
                try {
                    BluetoothDiscoverer discoverer = new BluetoothDiscoverer(sAppContext);
                    discoverer.setScanBluetoothDeviceListener(device -> MAIN.post(() -> {
                        if (sScanListener != null) {
                            sScanListener.onFindDevice(device);
                        }
                    }));
                    sDiscoverer = discoverer;
                    if (sScanListener != null) {
                        sScanListener.onScanStart();
                    }
                    discoverer.startDiscoverBluetoothDevice();
                } catch (Exception e) {
                    if (sScanListener != null) {
                        sScanListener.onScanStop();
                    }
                }
            }
        });
    }

    /** 停止搜索 */
    public static void stopScan() {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                stopScanInternal();
            }
        });
    }

    private static void stopScanInternal() {
        BluetoothDiscoverer discoverer = sDiscoverer;
        sDiscoverer = null;
        if (discoverer == null) {
            return;
        }
        try {
            discoverer.stopDiscoverBluetoothDevice();
        } catch (Exception ignored) {
        }
    }

    /** 清空搜索回调（页面销毁时调用，避免回调打到已销毁的页面） */
    public static void releaseScanListener() {
        sScanListener = null;
        stopScan();
    }

    // ==================== 打印 ====================

    /**
     * 打印标签：epcHex非空时在同一打印任务内写入EPC并出纸（不回读）
     * <p>
     * 成功判定：setLabelStart / printBitmap / setLabelEnd 全部成功（不含回读），
     * 与"打印指令已下发"语义一致；打印后会等待出纸完成，用于批量节流。
     */
    public static void print(final Bitmap bitmap, final String epcHex, final PrintCallback callback) {
        if (bitmap == null) {
            postPrintResult(callback, false, "标签图未生成，无法打印");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pipe pipe = sPipe;
                if (pipe == null || !isConnected()) {
                    postPrintResult(callback, false, "未连接打印机，请先连接蓝牙");
                    return;
                }
                Bitmap sendBitmap = rotateForPrinter(bitmap);
                try {
                    ZPL zpl = new ZPL(pipe);
                    zpl.setGray(GRAY_THRESHOLD);

                    // 写EPC必须在setLabelStart之前（SDK要求，放在之后会被丢弃）
                    if (!StringUtils.isNullOrEmpty(epcHex)) {
                        int rfidResult = zpl.setRFIDDataForPrint(RFID.WRITE_TYPE_EPC,
                                RFID.WRITE_DATA_TYPE_HEX_STRING, epcHex, "");
                        if (rfidResult != ResultCode.SUCCESS) {
                            postPrintResult(callback, false,
                                    "EPC写入指令失败：" + ResultCode.getMessage(rfidResult));
                            return;
                        }
                    }

                    if (!zpl.setLabelStart()) {
                        postPrintResult(callback, false, "标签开始指令发送失败");
                        return;
                    }
                    zpl.setLabelWidth(sendBitmap.getWidth());
                    zpl.setLabelLength(sendBitmap.getHeight());
                    int printResult = zpl.printBitmap(0, 0, sendBitmap, 1);
                    boolean endOk = zpl.setLabelEnd();
                    if (printResult != ResultCode.SUCCESS) {
                        postPrintResult(callback, false, "打印失败：" + ResultCode.getMessage(printResult));
                        return;
                    }
                    if (!endOk) {
                        postPrintResult(callback, false, "标签结束指令发送失败");
                        return;
                    }
                    // 等待出纸完成：让批量队列自然节流，超时不改变成功结果
                    waitPrinterReady(pipe);
                    postPrintResult(callback, true, "打印指令已发送");
                } catch (Exception e) {
                    postPrintResult(callback, false, "打印异常：" + e.getMessage());
                } finally {
                    if (sendBitmap != bitmap && !sendBitmap.isRecycled()) {
                        sendBitmap.recycle();
                    }
                }
            }
        }, "ds-print").start();
    }

    /** 定位标签：让打印机走一张空白标签校准间隙（设置弹窗里的"定位标签"按钮） */
    public static void locateLabel(final PrintCallback callback) {
        if (!isConnected()) {
            postPrintResult(callback, false, "未连接打印机，无法定位标签");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pipe pipe = sPipe;
                if (pipe == null) {
                    postPrintResult(callback, false, "未连接打印机，无法定位标签");
                    return;
                }
                try {
                    PrinterOperator operator = PrinterOperator.getPrinterOperator(pipe);
                    operator.locateLabel();
                    postPrintResult(callback, true, "已发送定位标签指令");
                } catch (Exception e) {
                    postPrintResult(callback, false, "定位标签失败：" + e.getMessage());
                }
            }
        }, "ds-locate").start();
    }

    /**
     * 出纸方向修正：按 SEND_ROTATE_DEGREES 生成旋转副本供发送（原预览位图不改动），
     * 角度为0的倍数或旋转失败时返回原图
     */
    private static Bitmap rotateForPrinter(Bitmap src) {
        if (src == null || SEND_ROTATE_DEGREES % 360 == 0) {
            return src;
        }
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(SEND_ROTATE_DEGREES);
            Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            return rotated == null ? src : rotated;
        } catch (Exception e) {
            return src;
        }
    }

    /** 等待打印机就绪（失败/超时都不改变打印结果，仅避免连续下发堆积） */
    private static void waitPrinterReady(Pipe pipe) {
        try {
            PrinterOperator operator = PrinterOperator.getPrinterOperator(pipe);
            operator.waitPrinterOk(WAIT_PRINTER_OK_MAX_COUNT);
        } catch (Exception ignored) {
        }
    }

    // ==================== 回调接口（均已在主线程） ====================

    public interface ConnectCallback {
        void onSuccess(String message);

        void onFail(String message);
    }

    public interface PrintCallback {
        void onResult(boolean success, String message);
    }

    public interface ScanListener {
        /** 搜索已启动 */
        void onScanStart();

        /** 发现一个设备 */
        void onFindDevice(BluetoothDevice device);

        /** 搜索结束 */
        void onScanStop();
    }

    private static String safeName(String name) {
        return StringUtils.isNullOrEmpty(name) ? "未知设备" : name;
    }

    private static void postConnectSuccess(final ConnectCallback callback, final String message) {
        if (callback == null) {
            return;
        }
        MAIN.post(new Runnable() {
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
        MAIN.post(new Runnable() {
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
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(success, message);
            }
        });
    }
}