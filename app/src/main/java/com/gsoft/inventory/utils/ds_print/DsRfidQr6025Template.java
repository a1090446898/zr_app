package com.gsoft.inventory.utils.ds_print;

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

/**
 * 得实标签模板："得实二维码60*25"（60×25mm）
 * <p>
 * 版式：顶部抬头 + 左侧网页二维码 + 右侧圆角框内两行"字段 / 值"。
 * 坐标常量为本模板自有，不引用宝威类的任何常量，后续调整互不影响。
 * <p>
 * 对齐规则（全版统一 1.5mm 间距，避免逐项试调）：
 * 1. 水平：二维码右缘23 → 框左24.5 → 内容左26（框内1.5）→ 内容右55.5（框内1.5）→ 框右57；
 * 2. 垂直（单行抬头）：框10~23、字段首行11.5（框内1.5）、行高5.5、末行分隔线21.5（距框底1.5）；
 * 3. 垂直（双行抬头）：右侧框区整体下移2mm——框12~24、首行13，抬头两行占2~10、距框顶2。
 * <p>
 * 抬头自动排版：按抬头宽度逐字折行，行数或高度超出2~10mm时逐级缩小字号（最小2.0mm、最多3行），
 * 缩到最小字号仍放不下时末行截断补省略号，保证长标题不压到右侧框区、不打出标签区。
 * <p>
 * 右侧框区（圆角框+框内字段+分隔线）打印位置微调：
 * 改 RIGHT_AREA_SHIFT_X_MM / RIGHT_AREA_SHIFT_Y_MM 整体平移，左侧二维码与顶部抬头固定不动；
 * 整张标签的位置偏移仍走设置里的"整版偏移X/Y"。
 * <p>
 * 整张标签偏移（二维码 + 框区 + 抬头全部内容）：
 * 改 LABEL_SHIFT_X_MM / LABEL_SHIFT_Y_MM，与设置里的"整版偏移X/Y"叠加生效，用于版本级固定微调。
 *
 * @author 10904
 */
public class DsRfidQr6025Template extends DsLabelTemplate {

    /** 纸张尺寸（mm）：60×25 */
    private static final float PAPER_WIDTH = 60f;
    private static final float PAPER_HEIGHT = 25f;

    /** 网页二维码（mm）：位置(2,2)、尺寸21×21；上下各留2、底边与框底(23)对齐 */
    private static final float QR_X = 2f;
    private static final float QR_Y = 2f;
    private static final float QR_SIZE = 21f;

    /** 抬头（mm）：起始X=内容左 / 单行顶部Y（3~7）/ 多行顶部Y / 宽度=内容左~内容右 / 首选字号 / 单行框高 / 多行可用高度 / 最小字号 / 最大行数 */
    private static final float TITLE_X = 23.5f;
    private static final float TITLE_Y = 3f;
    private static final float TITLE_Y_WRAP = 2f;
    private static final float TITLE_WIDTH = 33f;
    private static final float TITLE_SIZE_MM = 3.3f;
    private static final float TITLE_BOX_H = 4f;
    private static final float TITLE_AREA_H = 8f;
    private static final float TITLE_MIN_SIZE_MM = 2f;
    private static final int TITLE_MAX_LINES = 3;

    /** 字段区（mm）：字段名X=内容左 / 值X / 字段名宽 / 值宽（右端=内容右55.5） / 字号 / 行高 / 文本框高 */
    private static final float FIELD_LABEL_X = 26f;
    private static final float FIELD_VALUE_X = 39f;
    private static final float FIELD_LABEL_W = 13f;
    private static final float FIELD_VALUE_W = 16.5f;
    private static final float FIELD_SIZE_MM = 3f;
    private static final float FIELD_ROW_H = 5.5f;
    private static final float FIELD_BOX_H = 3.5f;

    /** 每行字段下方分隔线（mm）：距行顶4.5，左端=字段名X，右端=内容右55.5 */
    private static final float FIELD_LINE_OFFSET = 4.5f;
    private static final float FIELD_LINE_X2 = 55.5f;

    /** 圆角边框（mm）：左24.5（距二维码1.5）、右57（距纸边3）、圆角1.5、线宽0.2 */
    private static final float BOX_X1 = 24.5f;
    private static final float BOX_X2 = 58f;
    private static final float BOX_RADIUS = 1.5f;
    private static final float LINE_WIDTH_MM = 0.2f;

    /**
     * 右侧框区（圆角框 + 框内两行"字段 / 值"及分隔线）整体水平位移（mm）：
     * 正值右移、负值左移、0=版式默认位置；只移动框区，左侧二维码与顶部抬头均不动
     */
    private static final float RIGHT_AREA_SHIFT_X_MM = -2f;

    /**
     * 右侧框区（圆角框 + 框内两行"字段 / 值"及分隔线）整体垂直位移（mm）：
     * 正值下移、负值上移、0=版式默认位置；建议 -2~1，下移过多时框底会顶到纸边被裁切
     */
    private static final float RIGHT_AREA_SHIFT_Y_MM = -2f;

    /**
     * 整张标签（左侧二维码 + 右侧框区 + 顶部抬头）整体偏移（mm），用于本模板的固定微调：
     * 正值X右移/Y下移、负值左移/上移；与设置页"整版偏移X/Y"叠加生效，建议 -3~3
     */
    private static final float LABEL_SHIFT_X_MM = 0f;
    private static final float LABEL_SHIFT_Y_MM = 2f;

    /** 纵向基准（mm）：框顶 / 框底 / 字段首行顶；双行抬头时整体下移2mm（12 / 24 / 13） */
    private static final float BOX_TOP_SHORT = 10f;
    private static final float BOX_TOP_WRAP = 12f;
    private static final float BOX_BOTTOM_SHORT = 23f;
    private static final float BOX_BOTTOM_WRAP = 24f;
    private static final float ROW_TOP_SHORT = 11.5f;
    private static final float ROW_TOP_WRAP = 13f;

    /** 框内最多两行字段 */
    private static final int MAX_FIELD_ROWS = 2;

    @Override
    protected float getPaperWidthMm() {
        return PAPER_WIDTH;
    }

    @Override
    protected float getPaperHeightMm() {
        return PAPER_HEIGHT;
    }

    @Override
    protected void drawLabel(Canvas canvas, float pxPerMm, Context context,
                             Assets assets, String title, String position) {
        // 0. 整张标签整体偏移：canvas已按设置页"整版偏移"平移，这里再叠加模板级常量，
        //    作用范围含二维码、右侧框区、抬头全部内容（save/restore限定本方法，不影响其他模板）
        canvas.save();
        canvas.translate(LABEL_SHIFT_X_MM * pxPerMm, LABEL_SHIFT_Y_MM * pxPerMm);

        String barCode = (assets == null || StringUtils.isNullOrEmpty(assets.getBARCODEID()))
                ? "" : assets.getBARCODEID().trim();
        String qrContent = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s",
                SysDefine.SERVERHOST, assets == null ? "" : assets.getACCTSUITEID(), barCode);

        // 1. 左侧网页二维码（扫码直达资产查询页）
        drawQrCode(canvas, pxPerMm, qrContent, QR_X, QR_Y, QR_SIZE);

        // 2. 右侧框区整体位移：只作用框+框内字段+分隔线，二维码与顶部抬头不参与（save/restore限定范围）
        canvas.save();
        canvas.translate(RIGHT_AREA_SHIFT_X_MM * pxPerMm, RIGHT_AREA_SHIFT_Y_MM * pxPerMm);

        // 3. 纵向基准：抬头按宽度自动折行、超限逐级缩小字号；多行时右侧框区整体下移2mm（框12~24、首行13）
        TextLayout titleLayout = fitText(title, pxPerMm, TITLE_WIDTH, TITLE_AREA_H,
                TITLE_SIZE_MM, TITLE_MIN_SIZE_MM, TITLE_MAX_LINES);
        boolean wrapTitle = titleLayout.lines.length > 1;
        float boxTop = wrapTitle ? BOX_TOP_WRAP : BOX_TOP_SHORT;
        float boxBottom = wrapTitle ? BOX_BOTTOM_WRAP : BOX_BOTTOM_SHORT;
        float rowTop = wrapTitle ? ROW_TOP_WRAP : ROW_TOP_SHORT;

        // 4. 线框画笔：黑色空心描边（圆角框与分隔线共用）
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_WIDTH_MM * pxPerMm);

        // 5. 圆角边框：左24.5mm、上=boxTop、右57mm、下=boxBottom
        RectF box = new RectF(BOX_X1 * pxPerMm, boxTop * pxPerMm,
                BOX_X2 * pxPerMm, boxBottom * pxPerMm);
        canvas.drawRoundRect(box, BOX_RADIUS * pxPerMm, BOX_RADIUS * pxPerMm, linePaint);

        // 6. 框内两行"字段 / 值"及行下分隔线（字段取自"打印字段"设置的前两个）
        String[] selectedFields = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "").split(",");
        String[] allCodes = context.getResources().getStringArray(R.array.printfieldsCode);
        String[] allTitles = context.getResources().getStringArray(R.array.printfieldsTitle);

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
            float rowY = rowTop + drawn * FIELD_ROW_H;
            drawTextInBox(canvas, pxPerMm, allTitles[ci] + " / ", FIELD_LABEL_X, rowY, FIELD_LABEL_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            drawTextInBox(canvas, pxPerMm, value,
                    FIELD_VALUE_X, rowY, FIELD_VALUE_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            canvas.drawLine(FIELD_LABEL_X * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm,
                    FIELD_LINE_X2 * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm, linePaint);
            drawn++;
        }

        // 结束框区位移，后续抬头按原始坐标绘制，不受 X/Y 微调影响
        canvas.restore();

        // 7. 顶部抬头：单行固定占3~7mm、多行在2~10mm内按行数等分；
        //    单行按"打印位置"设置居中/居左，换行后统一左对齐（短行不再居中，避免参差）
        if (titleLayout.lines.length > 0) {
            boolean center = !wrapTitle && "居中".equals(position);
            float lineH = wrapTitle ? TITLE_AREA_H / titleLayout.lines.length : TITLE_BOX_H;
            float lineY = wrapTitle ? TITLE_Y_WRAP : TITLE_Y;
            for (int i = 0; i < titleLayout.lines.length; i++) {
                drawTextInBox(canvas, pxPerMm, titleLayout.lines[i], TITLE_X,
                        lineY + i * lineH, TITLE_WIDTH, lineH, titleLayout.sizeMm, center);
            }
        }

        // 结束整版偏移，恢复画布（与步骤0的save配对；框区自身的save/restore在其内部已配对）
        canvas.restore();
    }
}