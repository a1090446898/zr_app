
package com.gsoft.inventory.utils.print_legacy;

import android.text.TextUtils;

import com.dothantech.lpapi.LPAPI;
import com.gsoft.inventory.utils.SharedPreferencesUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老打印机业务类标签版式：网页二维码-模板三、财政二维码、试剂二维码、密级标签、公安警告标签。
 * <p>
 * 与资产类版式分离维护：这类标签依赖特定业务字段，改动频率低，单独放一处便于溯源。
 *
 * @author 10904
 */
final class LegacyBusinessLabels extends LegacyLabelBase {

    LegacyBusinessLabels(LegacyPrintHost host) {
        super(host);
    }

    /** 网页二维码-模板三：样品流转标签（70×50，点位/日期/样品编码/监测项目/保存方法/状态/采样人） */
    void printWebQrTemplate3(String accountName, Assets assets, String[] fieldArray) {
        String pointCodeValue = assets.getDWCODE();
        String sampleDateValue = assets.getSAMPLDATE();
        String sampleCodeValue = assets.getSAMPLCODE();
        String monitorItemsValue = assets.getMONITORPRO();
        String saveMethodValue = assets.getSAVEMETHOD();
        String statusValue = assets.getSAMPLESTATE();
        String samplersValue = assets.getSAMPLEUSER();

        if (printer == null || !printer.isPrinterOpened()) {
            host.showPrintTip("打印机未连接，请先确保连接成功");
            return;
        }

        try {
            printer.startJob(70, 50, 90);
            printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
            printer.setItemPenAlignment(LPAPI.PenAlignment.CENTER);
            printer.setItemVerticalAlignment(LPAPI.ItemAlignment.TOP);

            final double marginX = 2;
            final double labelX = 4;
            final double labelFontHeight = 3.5;
            final double valueFontHeight = 3.5;
            final double lineHeight = 6.5;
            final double textMarginTop = 1.5;
            final double lineWidth = 0.2;
            final double valueStartX = 17;
            final double headLineWidth = 43;
            double currentY = 2;

            printer.drawRectangle(1, currentY, 66, 46, 0.2);

            printer.drawTextRegular("点位编码：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            printer.drawTextRegular(pointCodeValue, valueStartX, currentY + textMarginTop, 26, 4, valueFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            currentY += lineHeight;
            printer.drawLine(marginX, currentY, headLineWidth, currentY, lineWidth);

            printer.drawTextRegular("采样日期：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            printer.drawTextRegular(sampleDateValue, valueStartX, currentY + textMarginTop, 26, 4, valueFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            currentY += lineHeight;
            printer.drawLine(marginX, currentY, headLineWidth, currentY, lineWidth);

            printer.drawTextRegular("样品编码：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            printer.drawTextRegular(sampleCodeValue, valueStartX, currentY + textMarginTop, 53, 4, 2,
                    LPAPI.FontStyle.REGULAR, 0);
            currentY += lineHeight;
            printer.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            printer.drawLine(headLineWidth, 2, headLineWidth, 2 + lineHeight * 3, lineWidth);
            printer.draw2DQRCode(queryAssetUrl(assets), 48, 4, 15);

            printer.drawTextRegular("监测项目：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            if (monitorItemsValue.length() < 24) {
                printer.drawTextRegular(monitorItemsValue, valueStartX, currentY + textMarginTop, 53, 4,
                        valueFontHeight, LPAPI.FontStyle.REGULAR, 0);
            } else {
                int minLen = Math.min(monitorItemsValue.length(), 30);
                monitorItemsValue = monitorItemsValue.substring(0, minLen) + "\n"
                        + monitorItemsValue.substring(minLen);
                printer.drawTextRegular(monitorItemsValue, valueStartX, currentY + textMarginTop, 53, 4, 2,
                        LPAPI.FontStyle.REGULAR, 0);
            }
            currentY += lineHeight;
            printer.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            printer.drawTextRegular("保存方法：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            if (saveMethodValue.length() < 24) {
                printer.drawTextRegular(saveMethodValue, valueStartX, currentY + textMarginTop, 53, 4,
                        valueFontHeight, LPAPI.FontStyle.REGULAR, 0);
            } else {
                int minLen = Math.min(saveMethodValue.length(), 26);
                double topY = currentY + textMarginTop - 1;
                double mod = Math.ceil((double) saveMethodValue.length() / minLen);
                int maxRow = Math.min(3, (int) mod);
                for (int i = 0; i < maxRow; i++) {
                    int startIndex = i * minLen;
                    int endIndex = Math.min(minLen * (i + 1), saveMethodValue.length());
                    printer.drawTextRegular(saveMethodValue.substring(startIndex, endIndex), valueStartX, topY,
                            53, 7, 2.1, LPAPI.FontStyle.REGULAR, 0);
                    topY += 2.3;
                }
            }
            currentY += lineHeight + 2;
            printer.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            printer.drawTextRegular("样品状态：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);

            String statusLabel1 = "待检";
            double label1X = valueStartX;
            printer.drawTextRegular(statusLabel1, label1X, currentY + textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            drawRadioBox(label1X + 6, currentY + textMarginTop, statusLabel1.equals(statusValue));

            String statusLabel2 = "在检";
            double label2X = label1X + 6 + 1 + 3 + 3;
            printer.drawTextRegular(statusLabel2, label2X, currentY + textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            drawRadioBox(label2X + 6, currentY + textMarginTop, statusLabel2.equals(statusValue));

            String statusLabel3 = "检毕";
            double label3X = label2X + 6 + 1 + 3 + 3;
            printer.drawTextRegular(statusLabel3, label3X, currentY + textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            drawRadioBox(label3X + 6, currentY + textMarginTop, statusLabel3.equals(statusValue));

            currentY += lineHeight - 1;
            printer.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            printer.drawTextRegular("采样人员：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            printer.drawTextRegular(samplersValue, valueStartX, currentY + textMarginTop, 53, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);

            commitWithTip();
        } catch (Exception e) {
            printer.abortJob();
            e.printStackTrace();
            host.showPrintTip("打印失败，发生异常");
        }
    }

    /** 财政二维码：70×38，右侧二维码 + 左侧资产编码及打印字段 */
    void printFiscalQr(String accountName, Assets assets) {
        try {
            Map<String, String> keyMap = new HashMap<>();
            keyMap.put("ASSETSNAME", "资产名称");
            keyMap.put("ASSETSUSEDATE", "使用日期");
            keyMap.put("ASSETSDEPT", "使用部门");
            keyMap.put("ASSETSSTANDARD", "规格型号");
            keyMap.put("ASSETSLAYADD", "存放位置");

            printer.startJob(70, 38, 90);
            printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
            printer.drawText(accountName, 0, 1.5, 70, 4, 3.5);
            printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.RIGHT);
            printer.draw2DQRCode(assets.getBARCODEID(), 50, 8, 15);
            printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

            String keys = SharedPreferencesUtils.getString(host.getHostContext(),
                    SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");
            printer.drawText("资产编码：".concat(assets.getBARCODEID()).concat("\n"), 1, 8, 40, 4, 3);
            if (StringUtils.isNullOrEmpty(keys)) {
                printer.drawText("资产名称：".concat(assets.getASSETSNAME()).concat("\n"), 1, 13, 40, 4, 3);
                printer.drawText("使用日期：".concat(assets.getASSETSUSEDATE()).concat("\n"), 1, 18, 40, 4, 3);
                printer.drawText("使用部门：".concat(assets.getASSETSDEPT()).concat("\n"), 1, 23, 40, 4, 3);
                printer.drawText("规格型号：".concat(assets.getASSETSSTANDARD()).concat("\n"), 1, 28, 40, 4, 3);
                printer.drawText("存放位置：".concat(assets.getASSETSLAYADD()).concat("\n"), 1, 33, 40, 4, 3);
            } else {
                String[] fieldArray = keys.split(",");
                for (int i = 0; i < fieldArray.length; i++) {
                    String fieldValue = readFieldValue(assets, fieldArray[i]);
                    printer.drawText(keyMap.get(fieldArray[i]).concat("：").concat(fieldValue).concat("\n"),
                            1, 13 + i * 5, 40, 4, 3);
                }
            }
            commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 试剂二维码-四字段：70×25 五列表格 + 左侧二维码（标签名与字段由调用方指定） */
    void printReagent(String accountName, Assets assets, String[] labelArray, String[] fieldArray) {
        printer.startJob(70, 25, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 1.5, 70, 5, 3.5);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        StringBuilder sb = new StringBuilder();
        double tableStartX = 20.5;
        double tableStartY = 7;
        double tableWidth = 47.5;
        double rowHeight = 3.2;
        int maxRows = 5;
        double tableHeight = maxRows * rowHeight + 0.5;
        double lineWidth = 0.2;
        double fontSize = 2.3;
        double verticalOffset = -0.4;

        printer.drawRectangle(tableStartX, tableStartY, tableWidth, tableHeight, lineWidth);
        printer.drawLine(36, tableStartY, 36, tableStartY + tableHeight, lineWidth);
        for (int i = 1; i < maxRows; i++) {
            double lineY = tableStartY + i * rowHeight;
            printer.drawLine(tableStartX, lineY, tableStartX + tableWidth, lineY, lineWidth);
        }

        List<String> printedFields = new ArrayList<>();
        for (int i = 0; i < fieldArray.length; i++) {
            int size = printedFields.size();
            if (size >= maxRows) {
                break;
            }
            try {
                String fieldValue = readFieldValue(assets, fieldArray[i]);
                sb.append(labelArray[i]).append("：").append(fieldValue).append("\n");
                double textY = tableStartY + size * rowHeight + (rowHeight - fontSize) / 2 + verticalOffset;
                printer.drawText(labelArray[i], tableStartX + 1, textY, 13, 0, fontSize);
                printer.drawText(fieldValue, 37, textY, 23, 0, fontSize);
                printedFields.add(fieldArray[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sb.append("介质".concat("：").concat(assets.getMEDIA()).concat("\n"));
        sb.append("贮存条件".concat("：").concat(assets.getSTORAGENEED()).concat("\n"));
        printer.draw2DQRCode(sb.toString(), 3, 7, 16);
        commit();
    }

    /** 大二维码-密级标签：70×48 多行密级信息表 + 右下角二维码 */
    void printDense(String accountName, Assets assets) {
        String assetsName = assets.getASSETSNAME();
        String barcodeId = assets.getBARCODEID();
        String assetsStandard = assets.getASSETSSTANDARD();
        String classification = assets.getCLASSIFICATION();
        if ("01".equals(classification)) {
            classification = "绝密";
        } else if ("02".equals(classification)) {
            classification = "机密";
        } else if ("03".equals(classification)) {
            classification = "秘密";
        }

        String useDept = assets.getASSETSDEPT();
        String ip = assets.getIP();
        String assetsLayAdd = assets.getASSETSLAYADD();
        String assetsSuDate = assets.getASSETSUSEDATE();
        String custodian = assets.getCUSTODIAN();
        String purpose = assets.getPURPOSE();
        if (purpose.length() > 4) {
            purpose = purpose.substring(0, 4) + "…";
        }

        double height = 48;
        double width = 70;
        printer.startJob(width, height, 90);

        double fontHeight = 2.5;
        double labelNameWidth = 14;
        double marginX = 1;
        double marginY = 0;
        double lineHeight = 6;
        double lineWidth = 0.2;

        printer.drawRectangle(marginX, marginY + lineHeight,
                width - 2 * marginX, height - 2 * marginY - lineHeight, lineWidth);

        double startLineX = marginX;
        double startLineY = 2 * lineHeight + marginY;
        double endLineX = width - marginX;
        double endLineY = startLineY;
        for (int i = 0; i < 6; i++) {
            if (i <= 1) {
                printer.drawLine(startLineX, startLineY, endLineX - 20, endLineY, lineWidth);
            } else {
                printer.drawLine(startLineX, startLineY, endLineX, endLineY, lineWidth);
            }
            startLineY += lineHeight;
            endLineY += lineHeight;
        }

        double startColLineX1 = marginX + labelNameWidth;
        printer.drawLine(startColLineX1, lineHeight + marginY, startColLineX1, height - marginY, lineWidth);

        double startColLineX2 = startColLineX1 + 34;
        printer.drawLine(startColLineX2, lineHeight + marginY, startColLineX2, marginY + 4 * lineHeight, lineWidth);
        printer.drawLine(startColLineX2, marginY + 5 * lineHeight, startColLineX2, height - marginY, lineWidth);

        double startColLineX3 = startColLineX1 + 20;
        printer.drawLine(startColLineX3, marginY + 5 * lineHeight, startColLineX3, height - marginY, lineWidth);

        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.setItemVerticalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, marginX, marginY, width - 2 * marginX, lineHeight, fontHeight + 1);

        double tempY = marginY + lineHeight;
        printer.drawText("设备名称", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(assetsName, marginX + labelNameWidth, tempY, 34, lineHeight, fontHeight);

        tempY = marginY + 2 * lineHeight;
        printer.drawText("设备编号", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(barcodeId, marginX + labelNameWidth, tempY, 34, lineHeight, fontHeight);

        tempY = marginY + 3 * lineHeight;
        printer.drawText("设备型号", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(assetsStandard, marginX + labelNameWidth, tempY, 34, lineHeight, fontHeight);

        tempY = marginY + 4 * lineHeight;
        printer.drawText("使用地点", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(assetsLayAdd, marginX + labelNameWidth, tempY, 34, lineHeight, fontHeight);

        tempY = marginY + 5 * lineHeight;
        printer.drawText("部门", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(useDept, marginX + labelNameWidth, tempY, 20, lineHeight, fontHeight);
        printer.drawText("IP地址", marginX + 34, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(ip, marginX + 34 + labelNameWidth, tempY, 20, lineHeight, fontHeight);

        tempY = marginY + 6 * lineHeight;
        printer.drawText("密级", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(classification, marginX + labelNameWidth, tempY, 20, lineHeight, fontHeight);
        printer.drawText("责任人", marginX + 34, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(custodian, marginX + 34 + labelNameWidth, tempY, 20, lineHeight, fontHeight);

        tempY = marginY + 7 * lineHeight;
        printer.drawText("用途", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(purpose, marginX + labelNameWidth, tempY, 20, lineHeight, fontHeight);
        printer.drawText("启用日期", marginX + 34, tempY, labelNameWidth, lineHeight, fontHeight);
        printer.drawText(assetsSuDate, marginX + 34 + labelNameWidth, tempY, 20, lineHeight, fontHeight);

        printer.draw2DQRCode(String.format("设备编号：%1$s  设备名称：%2$s", barcodeId, assetsName),
                51, 7.5, 15);
        commitWithTip();
    }

    /** 公安警告标签：70×25，标题 + 圆角框三行警示语 */
    void printPoliceWarning(String accountName) {
        final double labelWidth = 70;
        final double labelHeight = 25;
        printer.startJob(labelWidth, labelHeight, 90);
        printer.startPage();

        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 2.0, labelWidth, 0, 3.2);

        final double borderMarginX = 3.0;
        final double borderTopY = 8.5;
        final double borderWidth = labelWidth - borderMarginX * 2;
        final double borderHeight = 16.0;
        printer.drawRoundRectangle(borderMarginX, borderTopY, borderWidth, borderHeight, 2.0, 2.0, 0.3);

        final double contentFontHeight = 3.2;
        final double lineSpacing = 1.5;
        double firstLineTopY = borderTopY + 1.2;

        String[] warnings = loadPoliceWarnings();
        for (int i = 0; i < warnings.length; i++) {
            double textY = firstLineTopY + i * (contentFontHeight + lineSpacing);
            double lineFontHeight = contentFontHeight;
            if (warnings[i].length() > 20) {
                lineFontHeight = Math.max(2.0, borderWidth / warnings[i].length());
            }
            printer.drawText(warnings[i], borderMarginX, textY, borderWidth, 0, lineFontHeight);
        }

        printer.endPage();
        commit();
    }

    /** 警示内容：优先取设置页自定义内容，未配置时使用默认文案（最多三行） */
    private String[] loadPoliceWarnings() {
        String content = SharedPreferencesUtils.getString(host.getHostContext(),
                SharedPreferencesUtils.KEY_POLICE_WARNING_CONTENT, "");
        if (StringUtils.isNullOrEmpty(content)) {
            content = SysConfig.POLICE_WARNING_DEFAULT;
        }
        List<String> validLines = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimLine = line.trim();
            if (!StringUtils.isNullOrEmpty(trimLine) && validLines.size() < 3) {
                validLines.add(trimLine);
            }
        }
        if (validLines.isEmpty()) {
            return SysConfig.POLICE_WARNING_DEFAULT.split("\n");
        }
        return validLines.toArray(new String[0]);
    }

    /** 单选框：未选中画空框，选中时框内补粗体"√" */
    private void drawRadioBox(double x, double y, boolean isChecked) {
        final double radioSize = 3.0;
        final double lineWidth = 0.2;
        printer.drawRectangle(x, y, radioSize, radioSize, lineWidth);
        if (isChecked) {
            printer.drawTextRegular("√", x + 0.5, y + 0.3, radioSize - 1, radioSize - 0.5,
                    2.5, LPAPI.FontStyle.BOLD, 0);
        }
    }
}