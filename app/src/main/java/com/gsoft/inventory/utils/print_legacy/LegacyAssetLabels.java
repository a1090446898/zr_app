
package com.gsoft.inventory.utils.print_legacy;

import android.text.TextUtils;

import com.dothantech.lpapi.LPAPI;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 老打印机资产类标签版式：大二维码、大一维码、小二维码、小一维码、网页二维码（四字段/二字段）、
 * 大二维码-五字段、大标签-五字段。
 * <p>
 * 每种版式的纸张尺寸、坐标、字号均与拆分前逐字一致，改版式只改对应方法即可。
 *
 * @author 10904
 */
final class LegacyAssetLabels extends LegacyLabelBase {

    LegacyAssetLabels(LegacyPrintHost host) {
        super(host);
    }

    /** 大二维码：左侧网页二维码 + 右侧四行表格（首行条码编号 + 最多三行业务字段） */
    void printBigQr(String accountName, Assets assets, String[] fieldArray) {
        printer.startJob(70, 25, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 1.5, 70, 5, 4);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        StringBuilder sb = new StringBuilder();
        sb.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));

        double tableStartX = 20.5;
        double tableStartY = 7;
        double tableWidth = 47.5;
        double tableBottomY = 24;
        double qrCodeWidth = 16;
        double qrCodeX = 2;
        double qrCodeY = 7;
        int maxDataRows = 3;
        double tableHeight = tableBottomY - tableStartY;
        double rowHeight = tableHeight / (maxDataRows + 1);
        double lineWidth = 0.2;
        double labelWidth = 15;
        double valueWidth = 32;
        double fontSize = 3.0;
        double textMarginTop = (rowHeight - fontSize) / 2;

        printer.drawRectangle(tableStartX, tableStartY, tableWidth, tableHeight, lineWidth);
        printer.drawLine(tableStartX + labelWidth, tableStartY,
                tableStartX + labelWidth, tableStartY + tableHeight, lineWidth);
        for (int i = 0; i <= maxDataRows; i++) {
            double lineY = tableStartY + i * rowHeight;
            printer.drawLine(tableStartX, lineY, tableStartX + tableWidth, lineY, lineWidth);
        }

        printer.drawText("条码编号", tableStartX + 1, tableStartY + textMarginTop,
                labelWidth - 2, rowHeight, fontSize);
        printer.drawText(assets.getBARCODEID(), tableStartX + labelWidth + 1,
                tableStartY + textMarginTop, valueWidth - 2, rowHeight, fontSize);

        int printedRows = 0;
        for (int i = 0; i < fieldArray.length && printedRows < maxDataRows; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            String fieldValue = readDisplayValue(assets, fieldArray[i]);
            sb.append(fieldTitles[ci]).append("：").append(fieldValue).append("\n");
            double currentRowY = tableStartY + (printedRows + 1) * rowHeight;
            printer.drawText(fieldTitles[ci], tableStartX + 1, currentRowY + textMarginTop,
                    labelWidth - 2, rowHeight, fontSize);
            printer.drawText(fieldValue, tableStartX + labelWidth + 1, currentRowY + textMarginTop,
                    valueWidth - 2, rowHeight, fontSize);
            printedRows++;
        }

        printer.draw2DQRCode(sb.toString(), qrCodeX, qrCodeY, qrCodeWidth);
        commit();
    }

    /** 大一维码：标题 + CODE128 一维码 + 两行字段 */
    void printBigBarcode(String accountName, Assets assets, String[] fieldArray) {
        printer.startJob(70, 25, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 2, 70, 5, 3.5);
        printer.draw1DBarcode(assets.getBARCODEID(), LPAPI.BarcodeType.CODE128, 15, 7, 40, 10, 3);

        StringBuilder line1 = new StringBuilder();
        for (int i = 0; i < fieldArray.length && i < 2; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            line1.append(fieldTitles[ci]).append("：")
                    .append(readDisplayValue(assets, fieldArray[i])).append("  ");
        }
        printer.drawText(line1.toString().trim(), 0, 17.5, 70, 5, 3);

        StringBuilder line2 = new StringBuilder();
        for (int i = 2; i < fieldArray.length && i < 4; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            line2.append(fieldTitles[ci]).append("：")
                    .append(readDisplayValue(assets, fieldArray[i])).append("  ");
        }
        String secondLine = line2.toString().trim();
        if (!TextUtils.isEmpty(secondLine)) {
            printer.drawText(secondLine, 0, 21, 70, 5, 3);
        }
        commit();
    }

    /** 小二维码：竖版小标签，同一内容打印三个二维码 */
    void printSmallQr(Assets assets, String[] fieldArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldArray.length && i < 4; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            sb.append(fieldTitles[ci]).append("：")
                    .append(readDisplayValue(assets, fieldArray[i])).append("\n");
        }
        sb.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));

        printer.startJob(25, 70, 0);
        printer.draw2DQRCode(sb.toString(), 5, 1.5, 15);
        printer.draw2DQRCode(sb.toString(), 5, 21.5, 15);
        printer.draw2DQRCode(sb.toString(), 5, 41.5, 15);
        commit();
    }

    /** 小一维码：25×8 小标签，仅条码 */
    void printSmallBarcode(String barCode) {
        printer.startJob(25, 8, 0);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        printer.startPage();
        printer.draw1DBarcode(barCode, LPAPI.BarcodeType.CODE128, 1.2, 1.2, 25, 5, 0);
        printer.endPage();
        commit();
    }

    /** 网页二维码-四字段：左侧网页二维码 + 右侧四行"字段：值" */
    void printWebQr4(String accountName, Assets assets, String[] fieldArray) {
        printer.startJob(70, 25, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 1.5, 70, 5, 3.5);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        printer.drawLine(20.5, 7, 20.5, 23, 0.2);
        printer.drawLine(20.5, 7, 68, 7, 0.2);
        printer.drawLine(68, 7, 68, 23, 0.2);
        printer.drawLine(20.5, 11, 68, 11, 0.2);
        printer.drawLine(20.5, 15, 68, 15, 0.2);
        printer.drawLine(20.5, 19, 68, 19, 0.2);
        printer.drawLine(20.5, 23, 68, 23, 0.2);
        printer.drawLine(36, 7, 36, 23, 0.2);

        StringBuilder sb = new StringBuilder();
        List<String> printedFields = new ArrayList<>();
        for (int i = 0; i < fieldArray.length; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            int size = printedFields.size();
            if (size > 4) {
                break;
            }
            String fieldValue = readDisplayValue(assets, fieldArray[i]);
            sb.append(fieldTitles[ci]).append("：").append(fieldValue).append("\n");
            printer.drawText(fieldTitles[ci], 21.5, 7.2 + size * 3.9, 15, 3.9, 3);
            printer.drawText(fieldValue, 37, 7.2 + size * 3.9, 29, 3.9, 3);
            printedFields.add(fieldArray[i]);
        }
        printer.draw2DQRCode(queryAssetUrl(assets), 3, 7, 16);
        commit();
    }

    /** 网页二维码-二字段：左侧大二维码 + 右侧圆角框两行"字段 / 值" */
    void printWebQr2(String accountName, Assets assets, String[] fieldArray) {
        double x1 = 21;
        double x2 = 69;
        double y2 = 23;
        double tX1 = 22.5;
        double tX2 = 36;
        double tY1 = 11;
        double fontSize1 = 3.5;
        double fontWeight1 = 3;
        double titleHeight = 4;

        printer.startJob(70, 25, 90);
        boolean centerTitle = "居中".equals(host.getPrintPosition());
        printer.setItemHorizontalAlignment(centerTitle
                ? LPAPI.ItemAlignment.CENTER : LPAPI.ItemAlignment.LEFT);
        if (accountName.length() > 15) {
            accountName = accountName.substring(0, 15) + "\n" + accountName.substring(15);
            titleHeight = titleHeight * 2;
            tY1 = tY1 + 2;
            y2 = y2 + 1;
        }
        printer.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        printer.drawRoundRectangle(x1, tY1 - 1, (x2 - x1), (y2 - tY1 + 1), 1.5, 1.5, 0.2);

        int printSize = 0;
        int i = 0;
        while (printSize < 2 && i < fieldArray.length) {
            String fieldName = fieldArray[i];
            int ci = indexOfField(fieldName);
            i++;
            if (ci == -1) {
                continue;
            }
            String fieldValue = readDisplayValue(assets, fieldName);
            printer.drawText(fieldTitles[ci] + " / ", tX1, tY1 + printSize * 5.5, 15, fontSize1, fontWeight1);
            printer.drawText(fieldValue, tX2, tY1 + printSize * 5.5, 23, fontSize1, fontWeight1);
            if (printSize == 0) {
                printer.drawLine(tX1, tY1 + 4.5, 66.5, tY1 + 4.5, 0.2);
            } else {
                printer.drawLine(tX1, tY1 + 10, 66.5, tY1 + 10, 0.2);
            }
            printSize++;
        }

        printer.draw2DQRCode(queryAssetUrl(assets), 1, 4, 19);
        commit();
    }

    /** 大二维码-五字段：条码编号 + 四行字段表格 + 左侧二维码 */
    void printBigQrFive(String accountName, Assets assets, String[] fieldArray) {
        printer.startJob(70, 25, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 1.5, 70, 4, 3.5);

        StringBuilder sb = new StringBuilder();
        sb.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));

        double y0 = 6;
        double d = (24 - y0) / 5;
        double y1 = y0 + d;
        double y2 = y0 + 2 * d;
        double y3 = y0 + 3 * d;
        double y4 = y0 + 4 * d;
        printer.drawLine(20.5, y0, 20.5, 24, 0.2);
        printer.drawLine(20.5, y0, 68, y0, 0.2);
        printer.drawLine(68, y0, 68, 24, 0.2);
        printer.drawLine(20.5, y1, 68, y1, 0.2);
        printer.drawLine(20.5, y2, 68, y2, 0.2);
        printer.drawLine(20.5, y3, 68, y3, 0.2);
        printer.drawLine(20.5, y4, 68, y4, 0.2);
        printer.drawLine(20.5, 24, 68, 24, 0.2);
        printer.drawLine(35, y0, 35, 24, 0.2);

        printer.drawText("条码编号", 20.5, y0 + 0.5, 15, 3.9, 2.5);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        printer.drawText(assets.getBARCODEID(), 35.5, y0 + 0.5, 23, 3.9, 2.5);

        for (int i = 0; i < fieldArray.length; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            String fieldValue = readFieldValue(assets, fieldArray[i]);
            sb.append(fieldTitles[ci]).append("：").append(fieldValue).append("\n");
            if (i < 4) {
                printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                printer.drawText(padLabel(fieldTitles[ci]), 20.5, y0 + 0.5 + (i + 1) * d, 15, 3.9, 2.5);
                printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                printer.drawText(fieldValue, 35.5, y0 + 0.5 + (i + 1) * d, 23, 3.9, 2.5);
            }
        }

        printer.draw2DQRCode(sb.toString(), 2, 7, 16);
        commit();
    }

    /** 大标签-五字段：70×50 五列表格 + 右下角二维码 */
    void printBigFive(String accountName, Assets assets, String[] fieldArray) {
        int labelHeight = 50;
        int labelWidth = 70;
        double tableTop = 7.0;
        double tableLeft = 3.0;
        double tableRight = 67.0;
        double titleColSplit = 26.0;
        double qrSeparatorX = 49.0;
        double qrWidth = 13.0;
        double qrX = 52.0;
        double bottomMargin = 3.0;
        int totalRows = 5;
        double rowHeight = (labelHeight - tableTop - bottomMargin) / totalRows;

        printer.startJob(labelWidth, labelHeight, 90);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        printer.drawText(accountName, 0, 1.5, 70, 5, 3.5);
        printer.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        printer.setItemVerticalAlignment(LPAPI.ItemAlignment.CENTER);

        double tableHeight = totalRows * rowHeight;
        printer.drawRectangle(tableLeft, tableTop, tableRight - tableLeft, tableHeight, 0.3);
        printer.drawLine(titleColSplit, tableTop, titleColSplit, tableTop + tableHeight, 0.2);
        for (int i = 1; i < totalRows; i++) {
            double lineY = tableTop + i * rowHeight;
            double lineEnd = (i >= 4) ? qrSeparatorX : tableRight;
            printer.drawLine(tableLeft, lineY, lineEnd, lineY, 0.2);
        }
        double qrVerticalTop = tableTop + 3 * rowHeight;
        double qrVerticalBottom = tableTop + totalRows * rowHeight;
        printer.drawLine(qrSeparatorX, qrVerticalTop, qrSeparatorX, qrVerticalBottom, 0.2);

        List<String> titles = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < fieldArray.length && titles.size() < totalRows; i++) {
            int ci = indexOfField(fieldArray[i]);
            if (ci == -1) {
                continue;
            }
            titles.add(fieldTitles[ci]);
            values.add(readDisplayValue(assets, fieldArray[i]));
        }
        while (titles.size() < totalRows) {
            titles.add("");
            values.add("");
        }

        double qrTop = tableTop + 3 * rowHeight + (2 * rowHeight - qrWidth) / 2;
        printer.draw2DQRCode(queryAssetUrl(assets), qrX, qrTop, qrWidth);

        for (int i = 0; i < totalRows; i++) {
            double y = tableTop + i * rowHeight;
            double valueWidth = i >= 3
                    ? (qrSeparatorX - titleColSplit - 3)
                    : (tableRight - titleColSplit - 2);
            printer.drawText(titles.get(i), 5, y, 19, rowHeight, 2.8);
            if (!TextUtils.isEmpty(values.get(i))) {
                printer.drawText(values.get(i), titleColSplit + 2, y, valueWidth, rowHeight, 2.8);
            }
        }
        commit();
    }

    /** 只打二维码：内容由调用方给定（手工测试用，与原 printQRCode_JC(String) 一致） */
    void printSmallQrContent(String content) {
        printer.startJob(25, 70, 0);
        printer.draw2DQRCode(content, 5, 1.5, 15);
        printer.draw2DQRCode(content, 5, 21.5, 15);
        printer.draw2DQRCode(content, 5, 41.5, 15);
        commit();
    }
}