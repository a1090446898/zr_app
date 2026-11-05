package com.gsoft.inventory;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.gsoft.inventory.abs.InventoryScanBaseActivity;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.BarcodeResultUtils;
import com.gsoft.inventory.utils.CommonPermissionUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.view.scan.ScanFrameOverlay;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.ml.scan.HmsScan;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;

import static com.gsoft.inventory.const_data.ReqPageCodeConst.PERMISSION_REQUEST_CAMERA;

/**
 * 摄像头盘点扫描页（按存放地点盘点）
 * <p>
 * 与RFID/红外线盘点扫描页共用业务基类(InventoryScanBaseActivity)：
 * 盘点范围统计、标签卡片列表、一键变更、下钻明细等功能完全一致；
 * 本页差异：
 * 1. 识别方式为摄像头扫码（华为ScanKit自定义扫码RemoteView）：
 *    点击"开始扫码"后在统计卡片位置内嵌相机预览（不跳转扫码界面），
 *    连续扫码识别条码并实时新增卡片，点击"关闭摄像头扫码"才停止并恢复统计卡片
 * 2. 无右上角设置菜单（摄像头扫描无硬件参数）
 * <p>
 * 连续扫码说明：
 * 1. RemoteView连续模式对同一码值会高频回调，本次扫码会话内用集合去重（仅首次处理，与RFID页EPC去重口径一致），每次开始扫码清空
 * 2. 扫码框由ScanFrameOverlay自绘（四角式），随预览窗尺寸自动适配并居中；
 *    视觉框与识别区域(boundingBox)解耦：框只做引导，识别范围单独由SCAN_AREA_*_RATIO控制，缩小框不影响识别率
 * 3. 扫中一张后调用暂停/恢复扫码接口复位相机焦距（华为算法自动放大无法关闭，
 *    但"暂停扫码会重置相机焦距"），便于用户重新对准下一张；恢复后自动放大能力照旧生效
 *
 * @author 10904
 */
public class CameraReadActivity extends InventoryScanBaseActivity {

    private static final String TAG = "CameraReadActivity";

    /**
     * 预览窗高度（dp）：切换宽高比时高度保持不变、宽度按比例自动计算（可按需微调）
     */
    private static final int PREVIEW_WINDOW_HEIGHT_DP = 500;

    /**
     * 【实验开关】预览窗宽高比（竖持有，对应华为支持的取流比例；改此行后重新运行对比"画面变形+识别"）：
     * 4=4:3竖窗（宽:高=3:4）；1=1:1正方形；9=9:16（此前实测画面无变形）
     */
    private static final int DEBUG_PREVIEW_ASPECT = 9;

    /**
     * 识别区域相对扫码框的外扩值（dp）：华为建议识别区域比绘制的扫码框范围略大
     */
    private static final int SCAN_BOX_EXPAND_DP = 10;

    /**
     * 【可调】识别区域宽度占预览窗宽度的比例（1.0=整个预览窗）
     * <p>
     * 识别区域与视觉扫码框已解耦：框只做引导，真正决定识别率的是这两个比例。
     * 调参口径：识别率偏低 → 优先保持1.0（与加框前的识别范围一致）；
     * 若希望"只有框内才算识别成功" → 逐步调小到接近扫码框尺寸
     */
    private static final float SCAN_AREA_WIDTH_RATIO = 1.0f;

    /**
     * 【可调】识别区域高度占预览窗高度的比例（1.0=整个预览窗）
     */
    private static final float SCAN_AREA_HEIGHT_RATIO = 1.0f;

    /**
     * 扫中后复位焦距的节流间隔（毫秒）：连续模式下多码同帧/连帧回调时避免反复暂停恢复
     */
    private static final long ZOOM_RESET_THROTTLE_MS = 400;

    /**
     * 同一个码重复出现（去重命中）时复位焦距的节流间隔（毫秒）：
     * 镜头一直对着同码时SDK会持续自动放大，这里用较长间隔做"呼吸式"回弹，
     * 既能重新拿回1x视野，又不会因高频回调反复暂停/恢复导致画面抽搐
     */
    private static final long ZOOM_RESET_SAME_CODE_THROTTLE_MS = 1500;

    /**
     * 暂停扫码后等待相机完成焦距复位的延时（毫秒），之后才恢复识别
     */
    private static final long ZOOM_RESET_RESUME_DELAY_MS = 1000;

    /** 标题栏 */
    @BindView(R.id.topbar)
    QMUITopBar topBar;
    /** 开始扫码/关闭摄像头扫码按钮 */
    @BindView(R.id.buttonScan)
    QMUIButton buttonScan;
    /** 统计卡片区域（扫码时隐藏，关闭扫码后恢复） */
    @BindView(R.id.layoutStatsCard)
    View layoutStatsCard;
    /** 相机预览容器（开始扫码时显示，承载RemoteView） */
    @BindView(R.id.layoutCameraPreview)
    FrameLayout layoutCameraPreview;

    /** 华为ScanKit自定义扫码视图（懒创建，随页面销毁释放） */
    private RemoteView remoteView;
    /** 扫码框覆盖层（四角式，与RemoteView同尺寸同位置，保证视觉框与识别区域对齐） */
    private ScanFrameOverlay scanOverlay;
    /** 是否正在摄像头扫码（相机预览显示中） */
    private boolean cameraScanning = false;
    /** 本次扫码会话已处理条码集合（连续模式同码高频回调仅首次处理；每次开始扫码清空） */
    private final Set<String> scannedCodeSet = new HashSet<>();
    /** 上一次复位焦距的时间戳（SystemClock.elapsedRealtime），用于节流 */
    private long lastZoomResetAt = 0;

    /**
     * 页面创建：绑定视图 → 初始化UI（相机在点击"开始扫码"时才启动）
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan_read);
        ButterKnife.bind(this);
        initializeView();
    }

    /**
     * 初始化UI：标题栏、扫码按钮；
     * 公共部分（盘点范围参数、顶部统计、标签卡片列表、扫描开关）由基类initScanCommonView完成
     */
    @Override
    public void initializeView() {
        topBar.setTitle("摄像头盘点");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        // 右上角设置：扫描提示音开关（与RFID/红外页共用同一配置）
        topBar.addRightTextButton("设置", R.id.topbar_right_query_button)
                .setOnClickListener(v -> showScanSoundSettingSheet());
        // 开始/关闭摄像头扫码切换
        buttonScan.setOnClickListener(v -> {
            if (cameraScanning) {
                stopCameraScan();
            } else {
                startCameraScan();
            }
        });

        // 公共业务初始化：盘点范围参数/顶部统计/标签卡片列表/盘点扫描开关
        initScanCommonView();
        updateCameraUi();
    }

    /**
     * 相机权限申请结果：授权成功自动开始扫码（首次点击弹权限框，无需再点第二次）；
     * 拒绝时提示失败（首次点击的"申请中"状态不提示，避免误导）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (CommonPermissionUtils.checkPermissionResult(grantResults)) {
                startCameraScan();
            } else {
                showShortText("未授予相机权限，无法进行摄像头扫码");
            }
        }
    }

    /**
     * 开始摄像头扫码：申请相机权限 → 统计卡片区域切换为相机预览 → 布局完成后启动连续扫码
     * <p>
     * 注意：必须先让预览容器可见并等布局完成后再创建/启动RemoteView，
     * 否则容器GONE时控件尺寸为0，相机按错误区域取流会导致预览拉伸变形且无法识别
     */
    private void startCameraScan() {
        // 相机权限：首次点击会弹出系统权限框（此处不提示失败，授权成功经回调自动进入扫码）
        if (!CommonPermissionUtils.requestCameraPermission(this)) {
            return;
        }
        if (cameraScanning) {
            return;
        }
        cameraScanning = true;
        // 先切换显示：预览容器可见并完成测量后，RemoteView 才能拿到真实尺寸
        layoutStatsCard.setVisibility(View.GONE);
        layoutCameraPreview.setVisibility(View.VISIBLE);
        updateCameraUi();
        // 等预览容器完成真实布局后再创建扫码视图：
        // post仅代表下一条消息、不保证布局已执行（此时getWidth仍为0），
        // boundingBox 取实测宽度才能与控件显示区域严格一致，避免画面拉伸变形
        layoutCameraPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layoutCameraPreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!cameraScanning) {
                    return;
                }
                ensureRemoteView();
                if (remoteView == null) {
                    return;
                }
                // 新一轮扫码会话：清空条码去重集合，本次会话内同码仅处理一次
                scannedCodeSet.clear();
                lastZoomResetAt = 0;
                startRemoteViewPreview();
            }
        });
    }

    /**
     * 启动 RemoteView 相机预览（关键时序）：
     * RemoteView 刚 addView 时尚未 attach、未完成首轮布局、Surface 未创建，
     * 此时直接调用 onStart/onResume 会因预览表面未就绪导致相机启动失败且不会自动重试（表现为黑屏无法识别）；
     * 因此首次启动必须等待 RemoteView 布局完成（OnGlobalLayout）后再调用，已布局过（关闭后再次开启）则直接启动
     */
    private void startRemoteViewPreview() {
        if (remoteView == null || !cameraScanning) {
            return;
        }
        // 已布局过（再次开启场景，尺寸沿用上次布局结果）：直接启动相机
        if (remoteView.getWidth() > 0 && remoteView.getHeight() > 0) {
            remoteView.onStart();
            remoteView.onResume();
            return;
        }
        // 首次启动：监听首轮布局完成（attach + 测量），之后再启动相机
        remoteView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (remoteView == null) {
                    return;
                }
                remoteView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!cameraScanning) {
                    return;
                }
                remoteView.onStart();
                remoteView.onResume();
            }
        });
    }

    /**
     * 关闭摄像头扫码：停止相机取流，恢复统计卡片显示（数据在扫码期间已实时刷新）
     */
    private void stopCameraScan() {
        cameraScanning = false;
        if (remoteView != null) {
            remoteView.onPause();
            remoteView.onStop();
        }
        layoutCameraPreview.setVisibility(View.GONE);
        layoutStatsCard.setVisibility(View.VISIBLE);
        updateCameraUi();
        refreshScopeStats();
    }

    /**
     * 懒创建华为自定义扫码视图RemoteView（仅首次开始扫码、且预览容器已完成布局后调用）：
     * 1. 预览窗尺寸：高度按dp基准固定（PREVIEW_WINDOW_HEIGHT_DP，改它即可整体缩小/放大预览窗），
     *    宽度按实验比例计算，小窗在容器中居中
     * 2. 识别区域按SCAN_AREA_*_RATIO计算（默认整窗），视觉扫码框按ScanFrameOverlay的比例绘制，
     *    二者解耦：调整扫码框大小不会影响识别率
     * 3. 连续扫码模式：持续识别回调，同码去重交由scannedCodeSet处理
     */
    private void ensureRemoteView() {
        if (remoteView != null) {
            return;
        }

        // 预览窗尺寸：高度按dp基准固定（PREVIEW_WINDOW_HEIGHT_DP，改它即可整体缩小/放大预览窗），
        // 宽度按实验比例计算，小窗在容器中居中；窗口内画面呈现不变（随窗口整体等比缩放）
        int previewHeight = QMUIDisplayHelper.dp2px(mContext, PREVIEW_WINDOW_HEIGHT_DP);
        int aspectWidth;
        int aspectHeight;
        if (DEBUG_PREVIEW_ASPECT == 4) {
            aspectWidth = 3;
            aspectHeight = 4;
        } else if (DEBUG_PREVIEW_ASPECT == 1) {
            aspectWidth = 1;
            aspectHeight = 1;
        } else {
            aspectWidth = 9;
            aspectHeight = 16;
        }
        int previewWidth = previewHeight * aspectWidth / aspectHeight;
        Log.i(TAG, "预览窗=" + previewWidth + "x" + previewHeight
                + "（宽高比" + aspectWidth + ":" + aspectHeight + "）");

        // 识别区域：与视觉框解耦，按比例直接取预览窗的一部分（默认整窗，识别范围与加框前一致）
        Rect boundingBox = new Rect(0, 0,
                Math.round(previewWidth * SCAN_AREA_WIDTH_RATIO),
                Math.round(previewHeight * SCAN_AREA_HEIGHT_RATIO));
        Rect scanFrame = ScanFrameOverlay.calcScanRect(previewWidth, previewHeight);
        Log.i(TAG, "视觉扫码框=" + toDpSize(scanFrame.width(), scanFrame.height())
                + "dp，识别区域=" + toDpSize(boundingBox.width(), boundingBox.height()) + "dp");

        remoteView = new RemoteView.Builder()
                .setContext(this)
                .setBoundingBox(boundingBox)
                .setContinuouslyScan(true)
                .setFormat(HmsScan.ALL_SCAN_TYPE)
                .build();
        // 与华为官方示例调用顺序一致（onCreate → 回调 → addView），保证识别回调正常注册
        remoteView.onCreate(null);
        remoteView.setOnResultCallback(this::handleScanResult);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(previewWidth, previewHeight);
        lp.gravity = Gravity.CENTER;
        layoutCameraPreview.addView(remoteView, lp);

        // 扫码框覆盖层：必须与RemoteView同尺寸同位置，且后添加（位于上层），
        // 其几何规则与上面的boundingBox同源，保证"看到的框"与"实际识别区域"对齐
        scanOverlay = new ScanFrameOverlay(this);
        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(previewWidth, previewHeight);
        overlayLp.gravity = Gravity.CENTER;
        layoutCameraPreview.addView(scanOverlay, overlayLp);
    }

    /**
     * 像素尺寸转dp文案（仅用于日志：调SCAN_AREA_*_RATIO、FRAME_*_RATIO时对照实际大小）
     */
    private String toDpSize(int widthPx, int heightPx) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(widthPx / density) + "x" + Math.round(heightPx / density);
    }

    /**
     * 连续扫码结果回调（主线程）：逐个解析条码 → 本次会话去重 → 走基类公共命中处理
     * （查库→按开关标记盘实/已盘→下方新增卡片→提示音），相机保持开启继续扫
     * <p>
     * 识别排查日志（每次SDK识别回调都会打印）：
     * 1. 完全没有"扫码回调：结果数"日志 → SDK未回调识别结果（识别引擎未出结果）；
     * 2. 有回调但无"识别到新条码"→ 按"解析为空/已处理去重"日志定位内容环节
     */
    private void handleScanResult(HmsScan[] result) {
        Log.i(TAG, "扫码回调：结果数=" + (result == null ? 0 : result.length));
        if (result == null || result.length == 0) {
            return;
        }
        for (HmsScan scan : result) {
            if (scan == null) {
                continue;
            }
            String rawValue = scan.getOriginalValue();
            String barCode = BarcodeResultUtils.parse(rawValue);
            Log.i(TAG, "扫码回调：原始值=" + rawValue + "，解析后=" + barCode);
            if (StringUtils.isNullOrEmpty(barCode)) {
                Log.w(TAG, "扫码结果解析为空，原始值=" + rawValue);
                continue;
            }
            // 连续模式同一码值高频回调：本次会话内同码只处理一次（不重复出卡片/提示音），
            // 但"复位焦距"不能跟着去重一起被掐掉：镜头对着同码时SDK会再次自动放大，
            // 若去重命中就跳过复位，画面会一直停在放大状态（表现为只有第一次会复位）
            if (scannedCodeSet.contains(barCode)) {
                Log.i(TAG, "条码本次会话已处理过（去重忽略）：" + barCode);
                resetZoomForNextCode(false);
                continue;
            }
            scannedCodeSet.add(barCode);
            Log.i(TAG, "识别到新条码：" + barCode);
            handleBarcodeHit(barCode, "", "");
            // 每扫中一张就复位焦距：画面回到1x，方便重新对准下一张
            resetZoomForNextCode(true);
        }
    }

    /**
     * 扫中一张后复位相机焦距（官方"暂停/恢复扫码"接口）：
     * RemoteView连续模式的自动放大由SDK算法控制、无法关闭（华为FAQ），但官方文档明确
     * "暂停扫码会重置相机焦距"，因此识别成功后暂停再恢复，即让画面回到1x，
     * 恢复后自动放大能力照旧生效，不影响远处/小码识别
     * <p>
     * 触发口径（复位不能绑在业务去重上，否则同码第二次扫中不会复位）：
     * 1. 新条码：立即复位，节流ZOOM_RESET_THROTTLE_MS，避免一帧多码时连续暂停/恢复
     * 2. 同一码重复回调（去重命中）：同样复位，但节流ZOOM_RESET_SAME_CODE_THROTTLE_MS，
     *    避免镜头一直对着同码时频繁回弹
     * 3. 暂停后延时ZOOM_RESET_RESUME_DELAY_MS再恢复：给相机留出完成焦距复位的时间
     *
     * @param newCode true=本次是新条码；false=同码重复回调（只复位，不做业务处理）
     */
    private void resetZoomForNextCode(boolean newCode) {
        if (remoteView == null || !cameraScanning) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long throttle = newCode ? ZOOM_RESET_THROTTLE_MS : ZOOM_RESET_SAME_CODE_THROTTLE_MS;
        if (now - lastZoomResetAt < throttle) {
            return;
        }
        lastZoomResetAt = now;
        // 切到视图线程执行，避免在SDK识别回调线程直接操作相机
        remoteView.post(() -> {
            if (remoteView == null || !cameraScanning) {
                return;
            }
            remoteView.pauseContinuouslyScan();
            Log.i(TAG, "扫中后暂停识别，复位相机焦距");
            remoteView.postDelayed(() -> {
                if (remoteView != null && cameraScanning) {
                    remoteView.resumeContinuouslyScan();
                }
            }, ZOOM_RESET_RESUME_DELAY_MS);
        });
    }

    /**
     * 覆写基类条码命中处理：条码未在资产台账登记时给出明确提示
     * （基类默认静默忽略；摄像头页为人工逐个扫描、不存在RFID标签流刷屏问题，
     * 提示"未登记"可避免现场被误认为扫码功能失效）
     */
    @Override
    protected void handleBarcodeHit(String barCode, String epc, String rssi) {
        if (!StringUtils.isNullOrEmpty(barCode)
                && Assets.find(Assets.class, "BARCODEID=?", barCode).isEmpty()) {
            Log.i(TAG, "条码未查询到：" + barCode);
            showShortText("该资产未查询到：" + barCode);
            return;
        }
        super.handleBarcodeHit(barCode, epc, rssi);
    }

    /**
     * 刷新扫码UI：按钮文案 / 扫描状态 / 列表数量与空视图（后两项在基类updateListUi）
     */
    private void updateCameraUi() {
        buttonScan.setText(cameraScanning ? "关闭摄像头扫码" : "开始扫码");
        textScanState.setText(cameraScanning ? "摄像头扫码中..." : "按下方按钮开始扫码");
        updateListUi();
    }

    /**
     * 页面启动：扫码中则恢复相机（与onStop配对）
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (cameraScanning && remoteView != null) {
            remoteView.onStart();
        }
    }

    /**
     * 页面恢复：基类刷新统计与卡片；扫码中则恢复相机取流（与onPause配对）
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (cameraScanning && remoteView != null) {
            remoteView.onResume();
        }
    }

    /**
     * 页面暂停：扫码中则暂停相机取流（进后台或切页时避免占用相机）
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraScanning && remoteView != null) {
            remoteView.onPause();
        }
    }

    /**
     * 页面停止：扫码中则停止相机（与onStart配对）
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (cameraScanning && remoteView != null) {
            remoteView.onStop();
        }
    }

    /**
     * 页面销毁：释放相机扫码视图资源（提示音资源由基类释放）
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraScanning = false;
        if (remoteView != null) {
            remoteView.onDestroy();
            remoteView = null;
        }
        scanOverlay = null;
    }
}