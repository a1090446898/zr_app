package com.gsoft.inventory.utils.bw_print;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.jbw.sdk.print.label.BitmapTools;

/**
 * 宝威标签模板："RFID移动端网页二维码"（网页二维码-二字段版式，60×25mm）
 * <p>
 * 版式基于老打印机"网页二维码-二字段"（PrintCompatActivity.printQRCodeAssets_JC_WebQR_Double），纸张调整为60×25mm：
 * 顶部抬头（超15字自动换行，"打印位置"设置控制居中/居左）+ 左侧19mm网页二维码 + 右侧圆角框内两行"字段 / 值"（取"打印字段"设置的前两个）。
 * 宝威SDK的文本元素无对齐/边框绘制能力，因此除二维码外全部用Canvas按mm坐标手绘（px/mm = 位图宽 ÷ 60）。
 * <p>
 * 打印机右侧边缘存在模糊带：右侧框/文字按"左侧锚点不动、右侧收窄2mm"处理，
 * 框右边缘实际落在55.5mm（距纸边4.5mm），值/分隔线/抬头右端统一对齐到53.5mm（距框右2mm）。
 */
public class BwRfidQr6025Template extends BwLabelTemplate {

    /** 纸张尺寸（mm）：60×25，内容左右各留1mm空隙 */
    private static final float PAPER_WIDTH = 60f;
    private static final float PAPER_HEIGHT = 25f;

    /** 网页二维码：位置(1, 2)与尺寸19×19mm，源自老版式 draw2DQRCode(url, 1, 4, 19)（位置按实测微调） */
    private static final float QR_X = 4f;
    private static final float QR_Y = 8f;
    private static final float QR_SIZE = 12f;

    /** 抬头（mm）：起始X / 顶部Y / 宽度 / 字号 / 单行文本框高；宽度34.5使右端与值/分隔线对齐 */
    private static final float TITLE_X = 20.5f;
    private static final float TITLE_Y = 4f;
    private static final float TITLE_WIDTH = 34.5f;
    private static final float TITLE_SIZE_MM = 3.3f;
    private static final float TITLE_BOX_H = 4f;
    /** 抬头超过该字节数时换行（对应老打印机"第15个字符插入\n"规则） */
    private static final int TITLE_WRAP_LENGTH = 15;

    /** 字段区（mm）：字段名X / 值X / 字段名宽 / 值宽 / 字号 / 行高 / 文本框高；值宽19使右端落在53.5mm */
    private static final float FIELD_LABEL_X = 20.5f;
    private static final float FIELD_VALUE_X = 36f;
    private static final float FIELD_LABEL_W = 15f;
    private static final float FIELD_VALUE_W = 19f;
    private static final float FIELD_SIZE_MM = 3f;
    private static final float FIELD_ROW_H = 5.5f;
    private static final float FIELD_BOX_H = 3.5f;
    /** 每行字段下方的分隔线：距该行顶部4.5mm，右端到55mm（实际53.5mm，与值文本右端对齐） */
    private static final float FIELD_LINE_OFFSET = 4.5f;
    private static final float FIELD_LINE_X2 = 55f;

    /**
     * 圆角边框（mm）：X范围19~57、圆角1.5、线宽0.2。
     * 绘制时整体左移 RIGHT_AREA_SHIFT_LEFT_MM(1.5mm)，实际框为17.5~55.5mm，
     * 右边缘距纸边4.5mm，避开打印机右侧模糊带；左边缘与二维码保持1.5mm间距。
     */
    private static final float BOX_X1 = 19f;
    private static final float BOX_X2 = 57f;
    private static final float BOX_RADIUS = 1.5f;
    private static final float LINE_WIDTH_MM = 0.2f;

    /** 版式纵向基准（mm）：正常抬头 框顶10/框底23；抬头换行时整体下移 框顶12/框底24（与老打印机一致） */
    private static final float CONTENT_TOP_SHORT = 11f;
    private static final float CONTENT_TOP_WRAP = 13f;
    private static final float CONTENT_BOTTOM_SHORT = 23f;
    private static final float CONTENT_BOTTOM_WRAP = 24f;

    /** 右侧框区（圆角框 + 框内两行"字段 / 值"及分隔线）整体上移量（mm）：
     * 正值上移、负值下移、0=与老打印机版式一致；框与框内内容共用同一纵向基准，改这一处即整体平移，框高不变
     */
    private static final float BOX_SHIFT_UP_MM = 0f;

    /**
     * 整版内容（二维码 + 右侧框区 + 顶部抬头）整体左移量（mm）：
     * 正值左移、负值右移、0=与老打印机版式一致；二维码（SDK绘制）与手绘部分共用该位移，整版同步平移
     */
    private static final float LABEL_SHIFT_LEFT_MM = 0f;

    /**
     * 右侧区域（顶部抬头 + 圆角框 + 框内两行"字段 / 值"及分隔线）整体左右移动量（mm）：
     * 正值左移、负值右移、0=与老打印机版式一致；仅作用于手绘部分，不影响左侧二维码，
     * 用于在不动整版的前提下微调右侧区域与二维码的水平相对位置；抬头/框/字段共用该位移，宽度不变
     */
    private static final float RIGHT_AREA_SHIFT_LEFT_MM = 1.5f;

    /** 字段区最多绘制行数（老版式固定两字段） */
    private static final int MAX_FIELD_ROWS = 2;

    @Override
    protected float getPaperWidth() {
        return PAPER_WIDTH;
    }

    @Override
    protected float getPaperHeight() {
        return PAPER_HEIGHT;
    }

    /** 网页二维码（左下，位置尺寸与老打印机一致；扫码直达资产查询页） */
    @Override
    protected void drawSdkElements(BitmapTools tools, Context context, Assets assets, String title, String position) {
        String barCode = (assets == null || StringUtils.isNullOrEmpty(assets.getBARCODEID())) ? "" : assets.getBARCODEID().trim();
        String qrContent = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s",
                SysDefine.SERVERHOST, assets == null ? "" : assets.getACCTSUITEID(), barCode);
        tools.addQrcode(qrContent, QR_X - LABEL_SHIFT_LEFT_MM, QR_Y, QR_SIZE, QR_SIZE);
    }

    /**
     * 手绘标签的文本与线框（宝威SDK无此绘制能力），按老版式mm坐标绘制，顺序：
     * 右侧圆角框 → 框内两行"字段 / 值"及行下分隔线 → 顶部抬头。
     * 坐标系：位图左上角为原点，x向右/y向下，单位mm；画布已由基类按整体偏移平移
     */
    @Override
    protected void drawCanvasElements(Canvas canvas, float pxPerMm, Context context, Assets assets, String title, String position) {
        // 0. 整版水平校验：手绘部分整体左移/右移（与drawSdkElements中二维码的QR坐标偏移同一参数、同方向，保证整版同步）
        canvas.translate(-LABEL_SHIFT_LEFT_MM * pxPerMm, 0);
        // 0.1 右侧区域（抬头+圆角框+框内字段及分隔线）相对二维码的独立左右位移：正值左移、负值右移，仅作用于手绘部分
        canvas.translate(-RIGHT_AREA_SHIFT_LEFT_MM * pxPerMm, 0);

        // 1. 纵向基准：抬头超15字时换行占两行，右侧框与字段区整体下移2mm
        //    （正常：框顶=tY1-1=10、框底=23；换行：框顶=12、框底=24，与老打印机一致）
        //    再按 BOX_SHIFT_UP_MM 对右侧框区（框+框内内容）整体上移/下移
        boolean wrapTitle = title != null && title.length() > TITLE_WRAP_LENGTH;
        float tY1 = (wrapTitle ? CONTENT_TOP_WRAP : CONTENT_TOP_SHORT) - BOX_SHIFT_UP_MM;
        float y2 = (wrapTitle ? CONTENT_BOTTOM_WRAP : CONTENT_BOTTOM_SHORT) - BOX_SHIFT_UP_MM;

        // 2. 线框画笔：黑色空心描边（圆角框与分隔线共用），线宽0.2mm
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_WIDTH_MM * pxPerMm);

        // 3. 圆角边框：左21mm、上=tY1-1（字段区上方1mm）、右59mm、下=y2，圆角半径1.5mm
        RectF box = new RectF(BOX_X1 * pxPerMm, (tY1 - 1f) * pxPerMm, BOX_X2 * pxPerMm, y2 * pxPerMm);
        canvas.drawRoundRect(box, BOX_RADIUS * pxPerMm, BOX_RADIUS * pxPerMm, linePaint);

        // 4. 字段数据源（与老打印机完全一致）：selectedFields="打印字段"设置里按顺序勾选的字段代码；
        //    allCodes/allTitles=全部可选字段的代码与显示名（同下标对应），用于把代码翻译成打印文字
        String[] selectedFields = SharedPreferencesUtils.getString(context, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "").split(",");
        String[] allCodes = context.getResources().getStringArray(R.array.printfieldsCode);
        String[] allTitles = context.getResources().getStringArray(R.array.printfieldsTitle);

        // 已画行数：框内最多两行（MAX_FIELD_ROWS），多勾选的字段不打印
        int drawn = 0;
        for (String field : selectedFields) {
            if (drawn >= MAX_FIELD_ROWS) {
                break;
            }
            String code = field == null ? "" : field.trim();
            int ci = indexOf(allCodes, code);
            if (ci < 0 || ci >= allTitles.length) {
                continue; // 勾选代码不在可选字段列表内：跳过（与老打印机一致）
            }
            String value = readFieldValue(context, assets, code);
            if (value == null) {
                continue; // 资产类中无此字段（反射读取失败）：跳过，避免打出空行
            }
            // 5. 本行内容：行顶=tY1+行序号×5.5mm（第1行tY1、第2行tY1+5.5）；
            //    左列"字段名 /"（X22.5、宽15mm），右列值（X36、宽20.5mm），
            //    两列均垂直居中于3.5mm高文本框，字号3mm，超宽自动缩小（见基类drawTextInBox）
            float rowY = tY1 + drawn * FIELD_ROW_H;
            drawTextInBox(canvas, pxPerMm, allTitles[ci] + " / ", FIELD_LABEL_X, rowY, FIELD_LABEL_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            drawTextInBox(canvas, pxPerMm, value, FIELD_VALUE_X, rowY, FIELD_VALUE_W, FIELD_BOX_H, FIELD_SIZE_MM, false);
            // 6. 本行下分隔线：Y=行顶+4.5mm，从22.5mm画到56.5mm（右端距框2.5mm）
            canvas.drawLine(FIELD_LABEL_X * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm,
                    FIELD_LINE_X2 * pxPerMm, (rowY + FIELD_LINE_OFFSET) * pxPerMm, linePaint);
            drawn++;
        }

        // 7. 顶部抬头（账套名/部门名等，调用方已按"打印抬头"设置计算好）：
        //    位置X22.5、顶部Y=TITLE_Y（当前调为2）、限宽45mm、字号3.3mm；
        //    "打印位置"设置="居中"时在45mm宽度内水平居中，其余值居左；
        //    超15字换行——第二行画在TITLE_Y+TITLE_BOX_H（下移4mm）
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