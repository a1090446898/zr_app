package com.gsoft.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.gsoft.inventory.abs.InventoryScanBaseActivity;
import com.gsoft.inventory.service.scanner.IScannerReader;
import com.gsoft.inventory.service.scanner.ScanResult;
import com.gsoft.inventory.service.scanner.ScannerCallbacks;
import com.gsoft.inventory.service.scanner.ScannerError;
import com.gsoft.inventory.service.scanner.ScannerFactory;
import com.gsoft.inventory.utils.BarcodeResultUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 红外线盘点扫描页（按存放地点盘点）
 * <p>
 * 与RFID盘点扫描页(RfidReadActivity)共用业务基类(InventoryScanBaseActivity)：
 * 盘点范围统计、标签卡片列表、一键变更、下钻明细等功能完全一致；
 * 本页差异：
 * 1. 识别方式为红外线扫码，统一经ScannerFactory按设备型号创建适配器
 *    （优博讯：内置扫描服务广播；东集新机：系统扫描广播；东集旧机：RSC-JA解码器），
 *    页面不感知厂家SDK差异，新增厂家只需在工厂注册适配器；
 * 2. 支持软触发的适配器显示"开始/停止扫描"按钮，纯物理键设备自动隐藏；
 * 3. 物理按键统一转发给适配器，由适配器判断是否为其扫描键；
 * 4. 条码结果统一经BarcodeResultUtils清洗后走基类公共命中处理。
 *
 * @author 10904
 */
public class InfraredReadActivity extends InventoryScanBaseActivity {

    private static final String TAG = "InfraredReadActivity";

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 开始/停止扫描按钮（仅支持软触发的适配器显示） */
    @BindView(R.id.buttonScan)
    QMUIButton buttonScan;

    /** 红外识别器（厂家适配器由工厂按设备型号创建，本页不感知具体SDK） */
    private IScannerReader scanner;
    /** 是否正在扫描（软触发/物理键触发状态，用于按钮与状态文案联动） */
    private volatile boolean scanning = false;

    /**
     * 页面创建：绑定视图 → 初始化UI（识别器在页面创建时完成初始化）
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infrared_scan_read);
        ButterKnife.bind(this);
        initializeView();
    }

    /**
     * 初始化UI：标题栏、识别适配器、软触发按钮、扫描状态提示；
     * 公共部分（盘点范围参数、顶部统计、标签卡片列表、扫描开关）由基类initScanCommonView完成
     */
    @Override
    public void initializeView() {
        topBar.setTitle("红外盘点");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        // 右上角设置：扫描提示音开关（与RFID/摄像头页共用同一配置）
        topBar.addRightTextButton("设置", R.id.topbar_right_query_button)
                .setOnClickListener(v -> showScanSoundSettingSheet());

        // 工厂按设备型号创建识别适配器（优博讯/东集新机/东集旧机），异常时由兜底适配器提示
        scanner = ScannerFactory.create(mContext);
        scanner.init(this, scannerCallbacks);

        // 软触发按钮：仅支持程序触发扫描的适配器显示（纯广播设备只认物理扫描键）
        boolean softwareTrigger = scanner.supportsSoftwareTrigger();
        buttonScan.setVisibility(softwareTrigger ? View.VISIBLE : View.GONE);
        buttonScan.setOnClickListener(v -> {
            if (scanning) {
                stopScan();
            } else {
                startScan();
            }
        });

        // 扫描状态提示：显示当前识别厂家，便于现场排查设备通道
        updateScanUi(null);

        // 公共业务初始化：盘点范围参数/顶部统计/标签卡片列表/盘点扫描开关
        initScanCommonView();
    }

    /**
     * 统一扫码回调（适配器保证主线程投递）：
     * 结果清洗后交由基类公共命中处理（查库→按开关标记→刷新统计与卡片→提示音）
     */
    private final ScannerCallbacks scannerCallbacks = new ScannerCallbacks() {
        @Override
        public void onScanResult(ScanResult result) {
            String barCode = BarcodeResultUtils.parse(result.rawValue);
            if (StringUtils.isNullOrEmpty(barCode)) {
                Log.w(TAG, "扫码结果解析为空，原始值=" + result.rawValue);
                return;
            }
            handleBarcodeHit(barCode, "", "");
        }

        @Override
        public void onScanStateChanged(boolean scanningNow, String message) {
            // 物理键触发/停止时适配器主动通知，页面同步按钮与状态文案
            scanning = scanningNow;
            updateScanUi(message);
        }

        @Override
        public void onScannerError(ScannerError error) {
            Log.e(TAG, "[DIAG] 扫码错误：" + error.name() + " " + error.getMessage());
            scanning = false;
            updateScanUi(null);
            showShortText(error.getMessage());
        }
    };

    /** 页面恢复：基类刷新统计与卡片；识别器注册广播/恢复扫描服务 */
    @Override
    protected void onResume() {
        super.onResume();
        if (scanner != null) {
            scanner.onPageResume();
        }
    }

    /** 页面暂停：停止软触发扫描并交由识别器注销广播，避免后台重复响应 */
    @Override
    protected void onPause() {
        super.onPause();
        if (scanning) {
            scanning = false;
            if (scanner != null) {
                scanner.stopScan();
            }
            updateScanUi(null);
        }
        if (scanner != null) {
            scanner.onPagePause();
        }
    }

    /** 物理按键按下：转发适配器（如东集旧机139扫描键），未命中时交还系统默认处理 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (scanner != null && scanner.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** 物理按键抬起：转发适配器停止扫描 */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (scanner != null && scanner.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /** 页面销毁：释放识别器（提示音资源由基类释放） */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) {
            scanner.release();
            scanner = null;
        }
    }

    /** 软触发开始扫描（仅支持软触发的适配器） */
    private void startScan() {
        if (scanner == null || !scanner.supportsSoftwareTrigger() || scanning) {
            return;
        }
        scanning = true;
        updateScanUi("正在扫描…");
        scanner.startScan();
    }

    /** 软触发停止扫描 */
    private void stopScan() {
        if (scanner == null) {
            return;
        }
        scanning = false;
        updateScanUi(null);
        scanner.stopScan();
    }

    /**
     * 刷新扫描UI：按钮文案 + 状态提示（附识别厂家，便于现场确认设备通道）
     *
     * @param message 适配器指定的状态文案，为空时按扫描状态与触发能力生成默认文案
     */
    private void updateScanUi(String message) {
        if (scanner == null) {
            textScanState.setText("扫码模块初始化中…");
            return;
        }
        buttonScan.setText(scanning ? "停止扫描" : "开始扫描");
        if (!StringUtils.isNullOrEmpty(message)) {
            textScanState.setText(message);
            return;
        }
        String vendor = "（" + scanner.getVendorName() + "）";
        if (scanning) {
            textScanState.setText("正在扫描…");
        } else if (scanner.supportsSoftwareTrigger()) {
            textScanState.setText("当前设备" + vendor);
        } else {
            textScanState.setText("按设备扫描键扫描");
        }
    }
}