package com.gsoft.inventory.utils.ds_print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.gsoft.inventory.InventoryApplication;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 得实标签模板基类（纯Canvas实现，不依赖任何第三方打印SDK）
 * <p>
 * 与宝威打印栈完全独立：不继承BwLabelTemplate、不调用BitmapTools。
 * 位图尺寸按"纸张mm × 当前DPI"自建，位图像素=打印机dot，
 * 因此整张位图可原样通过 ZPL.printBitmap(0,0,bmp,1) 下发，无需换算。
 * <p>
 * 每种标签类型只需新建子类实现两个方法：
 * 1. getPaperWidthMm/getPaperHeightMm：纸张尺寸；
 * 2. drawLabel：按mm坐标绘制全部内容（canvas已按整版偏移平移）。
 * <p>
 * 新增得实标签类型的步骤：
 * 1. strings.xml的printe_effect数组新增一项；
 * 2. PrintEffectRouter.EFFECT_BRAND_MAP登记为BRAND_DS；
 * 3. 新建模板类继承本类，并在DsLabelRenderer.TEMPLATE_MAP注册。
 *
 * @author 10904
 */
public abstract class DsLabelTemplate {

    /** 二维码白边（module数）：0会导致码区贴边难识别，1在不明显占位的前提下留出静默区 */
    private static final int QR_QUIET_ZONE_MODULES = 1;

    /** 字号逐级缩小的步长（mm） */
    private static final float SIZE_STEP_MM = 0.1f;

    /** 缩到最小字号仍超行数时，末行补的省略号 */
    private static final String ELLIPSIS = "…";

    /** 纸张宽度（mm） */
    protected abstract float getPaperWidthMm();

    /** 纸张高度（mm） */
    protected abstract float getPaperHeightMm();

    /**
     * 绘制整张标签（canvas原点已按整版偏移平移）
     *
     * @param pxPerMm  每毫米像素数（= 位图宽 ÷ 纸宽mm）
     * @param title    打印抬头（调用方已按"打印抬头"设置计算好）
     * @param position 打印位置设置："居中"居中，其余居左
     */
    protected abstract void drawLabel(Canvas canvas, float pxPerMm, Context context,
                                      Assets assets, String title, String position);

    /**
     * 渲染标签位图（预览与发送打印的为同一张，所见即所得）
     *
     * @return 位图；生成失败返回null
     */
    public final Bitmap render(Context context, Assets assets, String title, String position) {
        float paperWidthMm = getPaperWidthMm();
        float paperHeightMm = getPaperHeightMm();
        if (paperWidthMm <= 0 || paperHeightMm <= 0) {
            return null;
        }
        try {
            float dotsPerMm = DsPrintSettings.getDotsPerMm(context);
            int widthPx = Math.max(1, Math.round(paperWidthMm * dotsPerMm));
            // 纵向沿用横向比例，避免宽高分别取整造成版式纵向轻微拉伸
            float pxPerMm = widthPx / paperWidthMm;
            int heightPx = Math.max(1, Math.round(paperHeightMm * pxPerMm));

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            canvas.translate(DsPrintSettings.getOffsetX(context) * pxPerMm,
                    DsPrintSettings.getOffsetY(context) * pxPerMm);
            drawLabel(canvas, pxPerMm, context, assets, title, position);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 绘制二维码：按目标mm尺寸生成锐利位图（不缩放、不抗锯齿），保证扫码率
     */
    protected static void drawQrCode(Canvas canvas, float pxPerMm, String content,
                                     float leftMm, float topMm, float sizeMm) {
        if (StringUtils.isNullOrEmpty(content)) {
            return;
        }
        int sizePx = Math.round(sizeMm * pxPerMm);
        if (sizePx <= 0) {
            return;
        }
        Bitmap qrBitmap = createQrBitmap(content, sizePx);
        if (qrBitmap == null) {
            return;
        }
        try {
            Paint paint = new Paint();
            // 二维码是黑白模块，必须关闭滤波，否则缩放后边缘灰化导致识别率下降
            paint.setFilterBitmap(false);
            paint.setAntiAlias(false);
            RectF dst = new RectF(leftMm * pxPerMm, topMm * pxPerMm,
                    leftMm * pxPerMm + sizePx, topMm * pxPerMm + sizePx);
            canvas.drawBitmap(qrBitmap, null, dst, paint);
        } finally {
            if (!qrBitmap.isRecycled()) {
                qrBitmap.recycle();
            }
        }
    }

    /** 用zxing生成二维码位图，失败返回null */
    private static Bitmap createQrBitmap(String content, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, QR_QUIET_ZONE_MODULES);
            BitMatrix matrix = new QRCodeWriter().encode(
                    content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在指定mm文本框内绘制文本：水平（可选居中）/垂直居中，超宽时逐级缩小字号（最小1.6mm）
     */
    protected static void drawTextInBox(Canvas canvas, float pxPerMm, String text,
                                        float leftMm, float topMm, float maxWidthMm, float boxHeightMm,
                                        float sizeMm, boolean center) {
        if (StringUtils.isNullOrEmpty(text)) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        float sizePx = sizeMm * pxPerMm;
        paint.setTextSize(sizePx);
        float maxWidthPx = maxWidthMm * pxPerMm;
        while (paint.measureText(text) > maxWidthPx && sizePx > 1.6f * pxPerMm) {
            sizePx -= 0.1f * pxPerMm;
            paint.setTextSize(sizePx);
        }
        Paint.FontMetrics fm = paint.getFontMetrics();
        float boxTopPx = topMm * pxPerMm;
        float boxHeightPx = boxHeightMm * pxPerMm;
        float baseline = boxTopPx + (boxHeightPx - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        float x = center
                ? leftMm * pxPerMm + (maxWidthPx - paint.measureText(text)) / 2f
                : leftMm * pxPerMm;
        canvas.drawText(text, x, baseline, paint);
    }

    /**
     * 自适应排版结果：折行后的每行文本 + 实际使用的字号（mm）
     */
    protected static final class TextLayout {
        /** 折行后的各行文本，空标题时长度为0 */
        public final String[] lines;
        /** 实际使用的字号（mm），可能小于首选字号 */
        public final float sizeMm;

        TextLayout(String[] lines, float sizeMm) {
            this.lines = lines;
            this.sizeMm = sizeMm;
        }
    }

    /**
     * 计算自适应排版（标题用）：按可用宽度逐字折行，行数或总高度超出时逐级缩小字号，
     * 直到"行数 ≤ maxLines 且总高度 ≤ maxHeightMm"；缩到最小字号仍放不下时
     * 只保留前maxLines行、末行按宽度截断并补省略号，避免文字打出标签区或被裁切
     *
     * @param text         待排版文本
     * @param pxPerMm      每毫米像素数
     * @param maxWidthMm   单行可用宽度（mm）
     * @param maxHeightMm  整块可用高度（mm）
     * @param preferSizeMm 首选字号（mm）
     * @param minSizeMm    最小字号（mm）
     * @param maxLines     最大行数
     */
    protected static TextLayout fitText(String text, float pxPerMm, float maxWidthMm, float maxHeightMm,
                                        float preferSizeMm, float minSizeMm, int maxLines) {
        if (StringUtils.isNullOrEmpty(text) || maxLines <= 0) {
            return new TextLayout(new String[0], preferSizeMm);
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float maxWidthPx = maxWidthMm * pxPerMm;
        float maxHeightPx = maxHeightMm * pxPerMm;
        float minSizePx = minSizeMm * pxPerMm;
        float stepPx = SIZE_STEP_MM * pxPerMm;
        float sizePx = preferSizeMm * pxPerMm;
        List<String> lines;
        while (true) {
            paint.setTextSize(sizePx);
            lines = wrapLines(text, paint, maxWidthPx);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float fontHeightPx = fm.descent - fm.ascent;
            if (lines.size() <= maxLines && fontHeightPx * lines.size() <= maxHeightPx) {
                break;
            }
            if (sizePx - stepPx < minSizePx) {
                lines = limitLines(lines, paint, maxWidthPx, maxLines);
                break;
            }
            sizePx -= stepPx;
        }
        return new TextLayout(lines.toArray(new String[0]), sizePx / pxPerMm);
    }

    /** 按当前字号的字符宽度逐字折行（中英文混排均按实际宽度累加，不硬切字符数） */
    private static List<String> wrapLines(String text, Paint paint, float maxWidthPx) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(c);
            if (paint.measureText(line.toString()) > maxWidthPx && line.length() > 1) {
                line.setLength(line.length() - 1);
                lines.add(line.toString());
                line.setLength(0);
                line.append(c);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /** 行数超上限时只保留前maxLines行，末行按可用宽度截断并补省略号 */
    private static List<String> limitLines(List<String> lines, Paint paint, float maxWidthPx, int maxLines) {
        if (lines.size() <= maxLines) {
            return lines;
        }
        List<String> limited = new ArrayList<>(lines.subList(0, maxLines));
        String tail = limited.get(maxLines - 1);
        while (tail.length() > 0 && paint.measureText(tail + ELLIPSIS) > maxWidthPx) {
            tail = tail.substring(0, tail.length() - 1);
        }
        limited.set(maxLines - 1, tail + ELLIPSIS);
        return limited;
    }

    /**
     * 读取资产字段值（转换规则与老打印机、宝威版式保持一致）：
     * GROUNDMANAGEDEPT转单位名称、ISCZ 02→"是"其余→"否"，其余取String.valueOf；
     * 字段不存在/读取失败返回null（调用方跳过该行，避免打出空行）
     */
    protected static String readFieldValue(Context context, Assets assets, String fieldCode) {
        if (assets == null) {
            return "";
        }
        try {
            Field field = assets.getClass().getDeclaredField(fieldCode);
            field.setAccessible(true);
            Object raw = field.get(assets);
            String value = raw == null ? "" : String.valueOf(raw);
            if ("GROUNDMANAGEDEPT".equals(fieldCode)) {
                String ztbh = "";
                if (context.getApplicationContext() instanceof InventoryApplication) {
                    InventoryApplication app = (InventoryApplication) context.getApplicationContext();
                    if (app.getLoginAccount() != null) {
                        ztbh = app.getLoginAccount().getZTBH();
                    }
                }
                value = AccountHelper.getDanweiName(ztbh, value);
            } else if ("ISCZ".equals(fieldCode)) {
                value = "02".equals(value) ? "是" : "否";
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    /** 在字符串数组中查找下标，未找到返回-1 */
    protected static int indexOf(String[] array, String value) {
        if (array == null || value == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (value.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
}