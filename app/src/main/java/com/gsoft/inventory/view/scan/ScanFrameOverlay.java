package com.gsoft.inventory.view.scan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 摄像头扫码框覆盖层（四角式简约样式）
 * <p>
 * 叠加在华为ScanKit的RemoteView之上（相机表面在窗口下层，普通View可正常覆盖在预览画面上）：
 * 1. 扫码框按控件实测宽高实时计算并居中，因此预览窗高度（PREVIEW_WINDOW_HEIGHT_DP）或宽高比
 *    调整后无需改动本类，扫码框自动跟随缩放并保持居中
 * 2. 样式：框外浅压暗 + 极淡整框描边 + 四角高亮 + 上下循环的扫描光带
 * 3. 对外提供 {@link #calcScanRect(int, int)}：按比例计算居中的框位置供绘制；
 *    视觉框只做引导，实际识别范围由页面侧SCAN_AREA_*_RATIO决定（两者解耦，缩小框不影响识别率）
 *
 * @author 10904
 */
public class ScanFrameOverlay extends View {

    /**
     * 【可调】扫码框宽度占控件宽度的比例：
     * 只影响视觉框，不影响识别范围；识别率偏低时不要去调它，改页面侧SCAN_AREA_*_RATIO
     */
    private static final float FRAME_WIDTH_RATIO = 0.86f;
    /**
     * 【可调】扫码框高度占控件高度的比例：
     * 0.30≈1.6:1横向矩形（适配资产标签一维条码）；想要更接近正方形就调大，如0.55
     */
    private static final float FRAME_HEIGHT_RATIO = 0.55f;
    /** 四角长度 dp */
    private static final float CORNER_LENGTH_DP = 22f;
    /** 四角线宽 dp */
    private static final float CORNER_STROKE_DP = 3f;
    /** 扫码框圆角 dp */
    private static final float FRAME_RADIUS_DP = 10f;
    /** 框外压暗颜色（浅压暗：突出扫描区域又不遮挡画面） */
    private static final int MASK_COLOR = 0x33000000;
    /** 整框描边颜色（极淡：浅色背景下也能看出区域边界） */
    private static final int BORDER_COLOR = 0x33FFFFFF;
    /** 四角颜色 */
    private static final int CORNER_COLOR = 0xFFFFFFFF;
    /** 扫描光带高度 dp */
    private static final float SCAN_LINE_GLOW_DP = 30f;
    /** 扫描线动画时长（毫秒） */
    private static final long SCAN_LINE_DURATION = 2200L;

    private final float density;

    /** 扫码框区域（控件内坐标，居中） */
    private final Rect scanRect = new Rect();
    private final RectF scanRectF = new RectF();
    /** 框外压暗路径（外矩形 + 内圆角矩形，EVEN_ODD挖孔） */
    private final Path maskPath = new Path();
    /** 四角路径 */
    private final Path cornerPath = new Path();

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** 扫描光带动画与当前进度（0=框顶，1=框底） */
    private ValueAnimator lineAnimator;
    private float lineProgress = 0f;

    public ScanFrameOverlay(Context context) {
        this(context, null);
    }

    public ScanFrameOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(MASK_COLOR);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1f));
        borderPaint.setColor(BORDER_COLOR);

        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dp(CORNER_STROKE_DP));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);
        cornerPaint.setColor(CORNER_COLOR);

        glowPaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setColor(0xCCFFFFFF);
    }

    /**
     * 计算扫码框在指定尺寸控件内的位置（居中）：
     * 宽高分别按FRAME_WIDTH_RATIO、FRAME_HEIGHT_RATIO换算，因此预览窗尺寸变化时自动跟随
     *
     * @param width  控件宽度（预览窗宽度）
     * @param height 控件高度（预览窗高度）
     * @return 居中的扫码框矩形
     */
    public static Rect calcScanRect(int width, int height) {
        int frameWidth = (int) (width * FRAME_WIDTH_RATIO);
        int frameHeight = (int) (height * FRAME_HEIGHT_RATIO);
        int left = (width - frameWidth) / 2;
        int top = (height - frameHeight) / 2;
        return new Rect(left, top, left + frameWidth, top + frameHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scanRect.set(calcScanRect(w, h));
        scanRectF.set(scanRect);
        buildMaskPath(w, h);
        buildCornerPath();
        buildGlowPaint();
        // 尺寸首次确定后才能开始动画（此前框区域为空）
        if (isShown()) {
            startLineAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (scanRect.isEmpty()) {
            return;
        }
        // 框外浅压暗：突出扫描区域
        canvas.drawPath(maskPath, maskPaint);
        // 极淡整框描边：浅色背景下也能看出区域边界
        canvas.drawRoundRect(scanRectF, frameRadius(), frameRadius(), borderPaint);
        // 四角高亮
        canvas.drawPath(cornerPath, cornerPaint);
        // 扫描光带
        drawScanLine(canvas);
    }

    /**
     * 绘制扫描光带：光带最亮处正好落在扫描线上，形成"光在扫"的柔和效果
     */
    private void drawScanLine(Canvas canvas) {
        float glowHeight = dp(SCAN_LINE_GLOW_DP);
        float lineY = scanRect.top + scanRect.height() * lineProgress;
        canvas.save();
        // 裁剪到框内，避免光带溢出到框外
        canvas.clipRect(scanRect);
        canvas.translate(0, lineY - glowHeight * 0.75f);
        canvas.drawRect(scanRect.left, 0, scanRect.right, glowHeight, glowPaint);
        // 回到以扫描线为原点的坐标，绘制一条清晰亮线
        canvas.translate(0, glowHeight * 0.75f);
        canvas.drawRect(scanRect.left, -dp(0.75f), scanRect.right, dp(0.75f), linePaint);
        canvas.restore();
    }

    /**
     * 框外压暗路径：外矩形与内圆角矩形使用EVEN_ODD填充，内框自然形成"挖孔"
     */
    private void buildMaskPath(int width, int height) {
        maskPath.reset();
        maskPath.setFillType(Path.FillType.EVEN_ODD);
        maskPath.addRect(0, 0, width, height, Path.Direction.CW);
        maskPath.addRoundRect(scanRectF, frameRadius(), frameRadius(), Path.Direction.CW);
    }

    /**
     * 四角路径：每个角两段直线 + 一个圆角，圆角半径与整框描边一致
     */
    private void buildCornerPath() {
        cornerPath.reset();
        float len = dp(CORNER_LENGTH_DP);
        float r = frameRadius();
        float l = scanRect.left;
        float t = scanRect.top;
        float rt = scanRect.right;
        float b = scanRect.bottom;

        // 左上角
        cornerPath.moveTo(l, t + len);
        cornerPath.lineTo(l, t + r);
        cornerPath.quadTo(l, t, l + r, t);
        cornerPath.lineTo(l + len, t);
        // 右上角
        cornerPath.moveTo(rt - len, t);
        cornerPath.lineTo(rt - r, t);
        cornerPath.quadTo(rt, t, rt, t + r);
        cornerPath.lineTo(rt, t + len);
        // 右下角
        cornerPath.moveTo(rt, b - len);
        cornerPath.lineTo(rt, b - r);
        cornerPath.quadTo(rt, b, rt - r, b);
        cornerPath.lineTo(rt - len, b);
        // 左下角
        cornerPath.moveTo(l + len, b);
        cornerPath.lineTo(l + r, b);
        cornerPath.quadTo(l, b, l, b - r);
        cornerPath.lineTo(l, b - len);
    }

    /**
     * 扫描光带渐变：#00FFFFFF → #40FFFFFF（最亮，位于0.75处即扫描线位置）→ #00FFFFFF
     */
    private void buildGlowPaint() {
        float glowHeight = dp(SCAN_LINE_GLOW_DP);
        glowPaint.setShader(new LinearGradient(0, 0, 0, glowHeight,
                new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.75f, 1f},
                Shader.TileMode.CLAMP));
    }

    /**
     * 启动扫描线动画（同一实例重复调用不会重复创建）
     */
    private void startLineAnimation() {
        if (scanRect.isEmpty() || (lineAnimator != null && lineAnimator.isRunning())) {
            return;
        }
        lineAnimator = ValueAnimator.ofFloat(0f, 1f);
        lineAnimator.setDuration(SCAN_LINE_DURATION);
        lineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        lineAnimator.setRepeatMode(ValueAnimator.RESTART);
        lineAnimator.setInterpolator(new LinearInterpolator());
        lineAnimator.addUpdateListener(animation -> {
            lineProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        lineAnimator.start();
    }

    /**
     * 停止扫描线动画（控件不可见/销毁时调用，避免后台空转刷新）
     */
    private void stopLineAnimation() {
        if (lineAnimator != null) {
            lineAnimator.cancel();
            lineAnimator = null;
        }
        lineProgress = 0f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isShown()) {
            startLineAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLineAnimation();
        super.onDetachedFromWindow();
    }

    /**
     * 跟随父容器（预览容器）显示/隐藏启停动画：扫码关闭时预览容器GONE，这里同步停止刷新
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startLineAnimation();
        } else {
            stopLineAnimation();
        }
    }

    private float frameRadius() {
        return dp(FRAME_RADIUS_DP);
    }

    private float dp(float value) {
        return value * density;
    }
}