package com.gsoft.inventory.utils.bw_print;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.gsoft.inventory.utils.domain.Assets;
import com.jbw.sdk.print.label.BitmapTools;

/**
 * 宝威标签模板："RFID二维码70*25"（二字段版式，70×25mm）
 * <p>
 * 版式沿用"RFID移动端网页二维码"（BwWebQrDoubleTemplate，60×25mm）：
 * 顶部抬头 + 左侧12mm网页二维码 + 右侧圆角框内两行"字段 / 值"（取"打印字段"设置前两个）。
 * 纸张宽度由60mm加宽到70mm后，右侧框、字段值、分隔线、抬头宽度同步向右扩展10mm，
 * 右侧边距与60×25版式保持一致（框右边缘距纸边2.5mm，内容右端距框右2mm）。
 * <p>
 * 宝威SDK的文本元素无对齐/边框绘制能力，因此除二维码外全部用Canvas按mm坐标手绘（px/mm = 位图宽 ÷ 70）。
 */
public class BwRfidQr7025Template extends BwLabelTemplate {

    /** 纸张尺寸（mm）：70×25，内容左右各留1mm空隙 */
    private static final float PAPER_WIDTH = 70f;
    private static final float PAPER_HEIGHT = 25f;

    /** 网页二维码：位置(4, 8)与尺寸12×12mm，与60×25版式一致 */
    private static final float QR_X = 3f;
    private static final float QR_Y = 8f;
    private static final float QR_SIZE = 13f;

    /** 抬头（mm）：起始X / 顶部Y / 宽度44.5（右端与值/分隔线对齐）/ 字号 / 单行文本框高 */
    private static final float TITLE_X = 20.5f;
    private static final float TITLE_Y = 4f;
    private static final float TITLE_WIDTH = 44.5f;
    private static final float TITLE_SIZE_MM = 3.3f;
    private static final float TITLE_BOX_H = 4f;
    /** 抬头超过该字节数时换行（对应老打印机"第15个字符插入\n"规则） */
    private static final int TITLE_WRAP_LENGTH = 15;

    /** 字段区（mm）：字段名X / 值X / 字段名宽 / 值宽29（右端落在65.5mm）/ 字号 / 行高 / 文本框高 */
    private static final float FIELD_LABEL_X = 20.5f;
    private static final float FIELD_VALUE_X = 36f;
    private static final float FIELD_LABEL_W = 15f;
    private static final float FIELD_VALUE_W = 29f;
    private static final float FIELD_SIZE_MM = 3f;
    private static final float FIELD_ROW_H = 5.5f;
    private static final float FIELD_BOX_H = 3.5f;
    /** 每行字段下方的分隔线：距该行顶部4.5mm，右端到65mm（实际63.5mm，与值文本右端对齐） */
    private static final float FIELD_LINE_OFFSET = 4.5f;
    private static final float FIELD_LINE_X2 = 65f;

    /**
     * 圆角边框（mm）：X范围19~67、圆角1.5、线宽0.2。
     * 绘制时整体左移 RIGHT_AREA_SHIFT_LEFT_MM(1.5mm)，实际框为19.5~67.5mm，
     * 右边缘距纸边2.5mm，左侧与二维码（右边界18mm）保持1.5mm间隙。
     */
    private static final float BOX_X1 = 19f;
    private static final float BOX_X2 = 67f;
    private static final float BOX_RADIUS = 1.5f;
    private static final float LINE_WIDTH_MM = 0.2f;

    /** 版式纵向基准（mm）：正常抬头 框顶11/框底23；抬头换行时整体下移 框顶13/框底24（与60×25版式一致） */
    private static final float CONTENT_TOP_SHORT = 11f;
    private static final float CONTENT_TOP_WRAP = 13f;
    private static final float CONTENT_BOTTOM_SHORT = 23f;
    private static final float CONTENT_BOTTOM_WRAP = 24f;

    /** 右侧框区整体上移量（mm）：正值上移、负值下移、0=与60×25版式一致 */
    private static final float BOX_SHIFT_UP_MM = 0f;

    /** 整版内容整体左右偏移（mm）：正值右移、负值左移、0=不偏移；二维码与手绘部分共用，整版同步平移 */
    private static final float LABEL_SHIFT_X_MM = 1f;

    /** 整版内容整体上下偏移（mm）：正值下移、负值上移、0=不偏移；二维码与手绘部分共用，整版同步平移 */
    private static final float LABEL_SHIFT_Y_MM = 2f;

    /** 右侧区域相对二维码的独立左右位移（mm）：仅作用于手绘部分 */
    private static final float RIGHT_AREA_SHIFT_LEFT_MM = 1.5f;

    /** 字段区最多绘制行数（固定两字段） */
    private static final int MAX_FIELD_ROWS = 2;

    @Override
    protected float getPaperWidth() {
        return PAPER_WIDTH;
    }

    @Override
    protected float getPaperHeight() {
        return PAPER_HEIGHT;
    }

    /** 网页二维码（左下，扫码直达资产查询页） */
    @Override
    protected void drawSdkElements(BitmapTools tools, Context context, Assets assets, String title, String position) {
        String barCode = (assets == null || StringUtils.isNullOrEmpty(assets.getBARCODEID())) ? "" : assets.getBARCODEID().trim();
        String qrContent = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s",
                SysDefine.SERVERHOST, assets == null ? "" : assets.getACCTSUITEID(), barCode);
        // 二维码同样叠加整版偏移，与手绘部分保持同步平移
        tools.addQrcode(qrContent, QR_X + LABEL_SHIFT_X_MM, QR_Y + LABEL_SHIFT_Y_MM, QR_SIZE, QR_SIZE);
    }

    /**
     * 手绘标签的文本与线框（宝威SDK无此绘制能力），按60×25版式mm坐标绘制并加宽右侧区域，顺序：
     * 右侧圆角框 → 框内两行"字段 / 值"及行下分隔线 → 顶部抬头。
     */
    @Override
    protected void drawCanvasElements(Canvas canvas, float pxPerMm, Context context, Assets assets, String title, String position) {
        // 0. 整版偏移：手绘部分与二维码共用同一组毫米偏移，保证整版同步平移
        canvas.translate(LABEL_SHIFT_X_MM * pxPerMm, LABEL_SHIFT_Y_MM * pxPerMm);
        // 0.1 右侧区域（抬头+圆角框+框内字段及分隔线）相对二维码的独立左右位移
        canvas.translate(-RIGHT_AREA_SHIFT_LEFT_MM * pxPerMm, 0);

        // 1. 纵向基准：抬头超15字时换行占两行，右侧框与字段区整体下移2mm
        boolean wrapTitle = title != null && title.length() > TITLE_WRAP_LENGTH;
        float tY1 = (wrapTitle ? CONTENT_TOP_WRAP : CONTENT_TOP_SHORT) - BOX_SHIFT_UP_MM;
        float y2 = (wrapTitle ? CONTENT_BOTTOM_WRAP : CONTENT_BOTTOM_SHORT) - BOX_SHIFT_UP_MM;

        // 2. 线框画笔：黑色空心描边（圆角框与分隔线共用），线宽0.2mm
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_WIDTH_MM * pxPerMm);

        // 3. 圆角边框：左19mm、上=tY1-1（字段区上方1mm）、右67mm、下=y2，圆角半径1.5mm
        RectF box = new RectF(BOX_X1 * pxPerMm, (tY1 - 1f) * pxPerMm, BOX_X2 * pxPerMm, y2 * pxPerMm);
        canvas.drawRoundRect(box, BOX_RADIUS * pxPerMm, BOX_RADIUS * pxPerMm, linePaint);

        // 4. 字段数据源（与老打印机一致）：selectedFields="打印字段"设置里按顺序勾选的字段代码
        String[] selectedFields = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "").split(",");
        String[] allCodes = context.getResources().getStringArray(R.array.printfieldsCode);
        String[] allTitles = context.getResources().getStringArray(R.array.printfieldsTitle);

        // 已画行数：框内最多两行，多勾选的字段不打印
        int drawn = 0;
        for (String field : selectedFields) {
            if (drawn >= MAX_FIELD_ROWS) {
                break;
            }
            String code = field == null ? "" : field.trim();
            int ci = indexOf(allCodes, code);
            if (ci < 0 || ci >= allTitles.length) {
                continue;
            }
            String value = readFieldValue(context, assets, code);
            if (value == null) {
                continue;
            }
            // 5. 本行内容：行顶=tY1+行序号×5.5mm；左列"字段名 /"，右列值，两列均垂直居中于文本框
            float rowY = tY1 + drawn * FIELD_ROW_H;
            drawTextInBox(canvas, pxPerMm, allTitles[ci] + " / ", FIELD_LABEL_X, rowY, FIELD_LABEL_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            drawTextInBox(canvas, pxPerMm, value, FIELD_VALUE_X, rowY, FIELD_VALUE_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            // 6. 本行下分隔线：Y=行顶+4.5mm，右端与值文本右端对齐
            canvas.drawLine(FIELD_LABEL_X * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm,
                    FIELD_LINE_X2 * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm, linePaint);
            drawn++;
        }

        // 7. 顶部抬头："打印位置"设置="居中"时在44.5mm宽度内水平居中，其余居左；超15字换行
        if (!StringUtils.isNullOrEmpty(title)) {
            boolean center = "居中".equals(position);
            String firstLine = wrapTitle ? title.substring(0, TITLE_WRAP_LENGTH) : title;
            drawTextInBox(canvas, pxPerMm, firstLine, TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_BOX_H, TITLE_SIZE_MM, center);
            if (wrapTitle) {
                drawTextInBox(canvas, pxPerMm, title.substring(TITLE_WRAP_LENGTH), TITLE_X, TITLE_Y + TITLE_BOX_H, TITLE_WIDTH, TITLE_BOX_H, TITLE_SIZE_MM, center);
            }
        }
    }
}