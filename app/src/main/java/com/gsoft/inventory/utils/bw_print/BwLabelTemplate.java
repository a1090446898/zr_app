package com.gsoft.inventory.utils.bw_print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.gsoft.inventory.InventoryApplication;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.StringUtils;
import com.jbw.sdk.print.label.BitmapTools;
import com.jbw.sdk.print.model.RotateType;

import java.lang.reflect.Field;

/**
 * 宝威标签模板基类（模板方法模式）
 * <p>
 * 固化"设置纸张 → 整体偏移 → SDK元素(二维码等) → 取预览位图 → 平移画布 → 手绘元素"的公共渲染流程，
 * 每种标签类型只需新建子类实现两个绘制钩子：
 * 1. drawSdkElements：BitmapTools提供的元素（如二维码），在生成位图之前调用；
 * 2. drawCanvasElements：拿到位图后用Canvas按mm坐标手绘（SDK不支持的文本对齐/线框等）。
 * <p>
 * 后续新增宝威标签类型的步骤：
 * 1. strings.xml的printe_effect数组新增一项（登录页/设置页下拉自动出现）；
 * 2. PrintEffectRouter.EFFECT_BRAND_MAP登记为BRAND_BW；
 * 3. 新建模板类继承本类，并在BwLabelRenderer.TEMPLATE_MAP注册。
 */
public abstract class BwLabelTemplate {

    /**
     * 打印内容整体偏移（mm）：校准打印位置用，预览与打印同时生效。
     * 二维码（SDK绘制）与手绘元素共用该偏移，保证整版同步平移（所有模板共用）；
     * 调试时先用较大值（如 5mm）打印一张，确认两部分同步移动后再调准数值，负值反向
     */
    private static final float OFFSET_X_MM = 0f;
    private static final float OFFSET_Y_MM = -2f;

    /** 纸张宽度（mm） */
    protected abstract float getPaperWidth();

    /** 纸张高度（mm） */
    protected abstract float getPaperHeight();

    /**
     * SDK绘制阶段：通过BitmapTools添加元素（如二维码），在生成预览位图之前调用；
     * 整体偏移已由基类设置，子类无需重复调用setOffset
     */
    protected abstract void drawSdkElements(BitmapTools tools, Context context, Assets assets, String title, String position);

    /**
     * 手绘阶段：拿到位图后用Canvas按mm坐标绘制；canvas已按整体偏移平移；
     * pxPerMm = 位图实际宽度 ÷ 纸张宽度，所有mm坐标需乘以该比例换算为像素
     */
    protected abstract void drawCanvasElements(Canvas canvas, float pxPerMm, Context context, Assets assets, String title, String position);

    /**
     * 模板渲染入口（公共流程，子类无需重写）：
     * 设置纸张 → 设置整体偏移 → 子类画SDK元素 → 生成预览位图 → 平移画布 → 子类手绘
     *
     * @return 标签位图（预览与发送打印的为同一张，所见即所得），生成失败返回null
     */
    public final Bitmap render(Context context, Assets assets, String title, String position) {
        BitmapTools tools = new BitmapTools(context);
        tools.setPaper(getPaperWidth(), getPaperHeight());
        // 整体偏移：二维码由SDK绘制，偏移在SDK内部生效（与手绘部分的平移共用同一组常量）
        tools.setOffset(OFFSET_X_MM, OFFSET_Y_MM);

        // 绘制二维码
        drawSdkElements(tools, context, assets, title, position);

        Bitmap labelBitmap = tools.getPreviewBitmap(RotateType.ROTATE0);
        if (labelBitmap == null) {
            return null;
        }
        Canvas canvas = new Canvas(labelBitmap);
        // 整体偏移：手绘部分与SDK的setOffset共用同一组毫米偏移，保证随SDK元素同步平移
        float pxPerMm = labelBitmap.getWidth() / getPaperWidth();
        canvas.translate(OFFSET_X_MM * pxPerMm, OFFSET_Y_MM * pxPerMm);
        drawCanvasElements(canvas, pxPerMm, context, assets, title, position);
        return labelBitmap;
    }

    /**
     * 在指定mm文本框内绘制文本：水平（可选居中）/垂直居中于文本框，超宽时逐级缩小字号（最小1.6mm）
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
     * 读取资产字段值，并做与老打印机相同的转换；字段不存在/读取失败返回null
     * （老代码：GROUNDMANAGEDEPT转单位名称、ISCZ 02→"是"其余→"否"，其余取 String.valueOf）
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

    /** 在字符串数组中查找下标，未找到返回-1（替代 Arrays.asList().indexOf，避免依赖装箱） */
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