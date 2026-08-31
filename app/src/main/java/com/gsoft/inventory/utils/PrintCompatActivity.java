package com.gsoft.inventory.utils;

import android.app.Service;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import butterknife.BindArray;
import com.dothantech.lpapi.LPAPI;
import com.dothantech.printer.IDzPrinter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.PrintLogInfo;

import java.lang.reflect.Field;
import java.util.*;

public abstract class PrintCompatActivity extends BaseCompatActivity {

    private Gson gson = new Gson();

    private String printerPosition;


    IDzPrinter.PrinterAddress jcPrinterAddress;
    protected BluetoothConn share = BluetoothConn.getInstance();

    @BindArray(R.array.printfieldsTitle)
    protected String[] printfieldsTitle;

    @BindArray(R.array.printfieldsCode)
    protected String[] printfieldsCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String printerName = SharedPreferencesUtils.getString(PrintCompatActivity.this, SharedPreferencesUtils.KEY_PRINTER_NAME, "");
        printerPosition = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_POSITION, "打印位置");
        if (application.getPrinterStateLiveData().getValue() == null
                || application.getPrinterStateLiveData().getValue() == 0
                || application.getPrinterStateLiveData().getValue() == 5
                || !StringUtils.isNullOrEmpty(printerName)) {
            String macAddress = SharedPreferencesUtils.getString(PrintCompatActivity.this, SharedPreferencesUtils.KEY_PRINTER_MAC, "");
            int printerType = SharedPreferencesUtils.getInt(PrintCompatActivity.this, SharedPreferencesUtils.KEY_PRINTER_TYPE, IDzPrinter.AddressType.BLE.value());
            IDzPrinter.AddressType addressType = IDzPrinter.AddressType.valueOf(printerType);
            if (!StringUtils.isNullOrEmpty(macAddress)) {
                application.getDTPrinter().openPrinterByAddress(new IDzPrinter.PrinterAddress(printerName, macAddress, addressType));
            }
        }
        application.getPrinterStateLiveData().observe(this, state -> {
            if (state == null) {
                showShortText("打印机未连接");
            } else if (state == 2 || state == 1) {
                showShortText("打印机已连接");
            } else if (state == 0 || state == 5) {
                showShortText("打印机已断开");
            }
        });


    }


    protected boolean isPrinterConnected() {
        if (application.getPrinterStateLiveData().getValue() == null) return false;
        return application.getPrinterStateLiveData().getValue() == 1 || application.getPrinterStateLiveData().getValue() == 2;
    }

    protected void printAssets(String accountName, Assets assets) {
        String printFields = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");

        /*String printerName = application.getDTPrinter().getPrinterInfo().deviceName.replace("B50-", "");
        long fCount = JCPrinter.count(JCPrinter.class, "encryno=?", new String[]{SecurityUtils.md5(printerName).toUpperCase()});
        if (fCount == 0) {
            showShortText("打印失败，请检查设备是否正常！");
            return;
        }*/

        try {
            // 获取手机型号
            String model = Build.MODEL;
            // 获取制造商
            String manufacturer = Build.MANUFACTURER;

            // 获取手持设备序列号
            String serialNumber = android.os.Build.SERIAL;

            if ("unknown".equals(serialNumber)) {
                String brand = Build.BRAND;
                serialNumber = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
                serialNumber = brand + "-" + serialNumber;
            }

            // 获取设备码
            String deviceName = application.getDTPrinter().getPrinterInfo().deviceName;
            String zcqcyName = application.getZcqcyName();
            String zcqcyCode = application.getZcqcyCode();
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            // 获取条码编号
            String barCode = assets.getBARCODEID();
            // 获取账套ID和账套名称
            String acctSuiteId = assets.getACCTSUITEID();
            String account = acctSuiteId + "-" + accountName;

            if (StringUtils.isNullOrEmpty(deviceName)) {
                deviceName = "null";
            }

            PrintLogInfo printLogInfo = new PrintLogInfo();
            printLogInfo.setHandheldDeviceSerialNumber(serialNumber);
            printLogInfo.setDeviceName(deviceName);
            printLogInfo.setBarCode(barCode);
            printLogInfo.setAccount(account);
            printLogInfo.setZcqcyCode(zcqcyCode);
            printLogInfo.setZcqcyName(zcqcyName);
            printLogInfo.setVersion(versionName);
            printLogInfo.setCreateTime(new Date());

            // 记录当前到缓存或者数据库中
            String jsonStr = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, "");
            if (TextUtils.isEmpty(jsonStr)) {
                List<PrintLogInfo> printLogInfoList = new ArrayList<>();
                printLogInfoList.add(printLogInfo);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, gson.toJson(printLogInfoList));
            } else {
                List<PrintLogInfo> printLogInfoList = gson.fromJson(jsonStr, new TypeToken<List<PrintLogInfo>>() {
                }.getType());
                printLogInfoList.add(printLogInfo);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, gson.toJson(printLogInfoList));
            }

        } catch (Exception e) {
            Log.e("printAssets", e.getMessage());
        }
        String printTitle = handleAssetsTitle(accountName, assets);

  /*      if (TextUtils.isEmpty(printFields)) {
            printAssets(printTitle, assets.getBARCODEID(), assets.getASSETSNAME(), StringUtils.isNullOrEmpty(assets.getASSETSUSER()) ? assets.getCUSTODIAN() : assets.getASSETSUSER());
        } else {

        }*/

        String[] fieldArray = printFields.split(",");
        try {
            printAssets(printTitle, assets, fieldArray);
        } catch (Exception e) {
            e.printStackTrace();
            showShortText("打印功能错误，检查打印字段是否正常");
        }
    }

    private String handleAssetsTitle(String accountName, Assets assets) {
        String result = "";
        String assetsDept = assets.getASSETSDEPT();
        String printerTitle = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, "单位名称");
        if ("部门名称".equals(printerTitle)) {
            result = assetsDept;
        } else if ("单位+部门名称".equals(printerTitle)) {
            result = accountName + "(" + assetsDept + ")";
        } else {
            result = accountName;
        }

        return result;
    }

    private void printAssets(String accountName, Assets assets, String[] fieldArray) throws NoSuchFieldException, IllegalAccessException {
        if ("智软2号".equals(application.getPrinterDevice())) {  // 精臣
            String printerEffect = application.getPrinterEffect();
            if ("大二维码".equals(printerEffect)) {
                printQRCodeAssets_JC_define(accountName, assets, fieldArray);
            } else if ("大一维码".equals(printerEffect)) {
                printAssets_JC_define(accountName, assets, fieldArray);
            } else if ("小一维码".equals(printerEffect)) {
                printBarCode_JC(assets.getBARCODEID());
            } else if ("小二维码".equals(printerEffect)) {
                printQRCode_JC(accountName, assets, fieldArray);
            } else if ("网页二维码-四字段".equals(printerEffect)) {
                printQRCodeAssets_JC_WebQR(accountName, assets, fieldArray);
            } else if ("网页二维码-二字段".equals(printerEffect)) {
                printQRCodeAssets_JC_WebQR_Double(accountName, assets, fieldArray);
            } else if ("网页二维码-模板三".equals(printerEffect)) {
                printerWebpageQRCodeTemplate3(accountName, assets, fieldArray);
            } else if ("财政二维码".equals(printerEffect)) {
                printBarCode_CZ(accountName, assets, fieldArray);
            } else if ("大二维码-五字段".equals(printerEffect)) {
                printBarCodeFive(accountName, assets, fieldArray);
            }else if("大二维码-密级标签".equals(printerEffect)){
                printLargeQRCodeDenseLabel(accountName, assets, fieldArray);
            }
            else if("试剂二维码-四字段".equals(printerEffect)){
                String[] labelArr = new String[]{"试剂名称", "配置浓度", "配  置  人", "配置日期", "有效日期"};
                fieldArray = new String[]{"ASSETSNAME", "CONCENTRATION", "PREPARER", "ASSETSUSEDATE", "VALIDDATE"};
                printQRCode_Assets_SJ(accountName, assets, labelArr, fieldArray);
            }
            else if("大标签-五字段".equals(printerEffect)){
                big_five(accountName, assets, fieldArray);
            }

        }
    }

    private void printerWebpageQRCodeTemplate3(String accountName, Assets assets, String[] fieldArray) {
        /*Field field = assets.getClass().getDeclaredField(fieldArray[i]);
        field.setAccessible(true);
        String fieldValue = String.valueOf(field.get(assets));*/

        // 1. 定义动态变量（value值来自外部，此处模拟示例数据）
        String pointCodeValue = assets.getDWCODE();       // 点位编码值
        String sampleDateValue = assets.getSAMPLDATE();     // 采样日期值
        String sampleCodeValue = assets.getSAMPLCODE(); // 样品编码值
        String monitorItemsValue = assets.getMONITORPRO(); // 监测项目值
        String saveMethodValue = assets.getSAVEMETHOD(); // 保存方法值
        String statusValue = assets.getSAMPLESTATE();   // 状态值
        String samplersValue = assets.getSAMPLEUSER(); // 采样人值

//        sampleCodeValue = "QXC-BHBHHH-2025-10-23";
//        saveMethodValue = "采样瓶先加入乙酸锌（1ml），再加入水样至近满瓶，依次加入氢氧化钠溶液（0.5ml）和抗氧化剂溶液（1ml），加塞后不留液上空间";



        // 2. 获取LPAPI实例并校验打印机连接状态（文档5.17节：isPrinterOpened判断连接）
        LPAPI dtPrinter = application.getDTPrinter();
        if (dtPrinter == null || !dtPrinter.isPrinterOpened()) {
            showShortText("打印机未连接，请先确保连接成功");
            return;
        }

        try {
            // 3. 开始绘图任务（文档7.1节：startJob(宽, 高, 旋转角度)，此处宽70mm、高50mm、旋转90°）
            dtPrinter.startJob(70, 50, 90);

            // 4. 设置基础绘制参数（文档8章：对齐方式、画笔风格）
            dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT); // 水平左对齐
            dtPrinter.setItemPenAlignment(LPAPI.PenAlignment.CENTER);       // 线条居中对齐
            dtPrinter.setItemVerticalAlignment(LPAPI.ItemAlignment.TOP);    // 垂直靠上对齐

            // 5. 定义布局参数（边距、字体高度、行高，避免内容重叠）
            final double marginX = 2;          // 左右边距：2mm
            final double labelX = 4;          // 左右边距：2mm
            final double labelFontHeight = 3.5;// label字体高度：3.5mm（文档9.3节：fontHeight单位为mm）
            final double valueFontHeight = 3.5;// value字体高度：3.5mm（与label一致，保证排版整齐）
            final double lineHeight = 6.5;     // 行高：7.5mm（包含文本高度+间距）
            final double textMarginTop = 1.5;
            final double lineWidth = 0.2;      // 分隔线宽：0.2mm（文档11.9节：drawLine线宽参数）
            final double valueStartX = 17;     // value起始X坐标：15mm（固定位置，确保所有value对齐）
            double currentY = 2;               // 当前Y坐标（顶部边距2mm，动态递增）

            // 绘制矩形边框
            dtPrinter.drawRectangle(1, currentY, 66, 46, 0.2);

            // 前三行宽度
            final double headLineWidth = 43;


            // 标签字体打印，点位编码
            dtPrinter.drawTextRegular("点位编码：", labelX, currentY + textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0); // label：X=2mm，宽13mm（到valueStartX前）
            // 标签字体打印
            dtPrinter.drawTextRegular(pointCodeValue, valueStartX, currentY + textMarginTop, 26, 4, valueFontHeight,
                    LPAPI.FontStyle.REGULAR, 0); // value：X=15mm，宽53mm（70-2-15）
            currentY += lineHeight;

            dtPrinter.drawLine(marginX, currentY, headLineWidth, currentY, lineWidth); // 分隔线

            // 6.2 采样日期
            dtPrinter.drawTextRegular("采样日期：", labelX, currentY+ textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            dtPrinter.drawTextRegular(sampleDateValue, valueStartX, currentY+textMarginTop, 26, 4, valueFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            currentY += lineHeight;

            dtPrinter.drawLine(marginX, currentY, headLineWidth, currentY, lineWidth);

            // 6.3 样品编码
            dtPrinter.drawTextRegular("样品编码：", labelX, currentY+ textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            // 只保留sampleCodeValue前16位字符
            dtPrinter.drawTextRegular(sampleCodeValue, valueStartX, currentY+textMarginTop, 53, 4, 2,
                    LPAPI.FontStyle.REGULAR, 0);
            currentY += lineHeight;

            dtPrinter.drawLine(marginX, currentY , 70 - marginX, currentY, lineWidth);

            // 绘制一条竖线
            dtPrinter.drawLine(headLineWidth, 2, headLineWidth, 2 + (lineHeight * 3), lineWidth);

            // 绘制一个二维码
            String url = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s", SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
            dtPrinter.draw2DQRCode(url, 48, 4,15);

            // 6.4 监测项目
            dtPrinter.drawTextRegular("监测项目：", labelX, currentY+textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            if(monitorItemsValue.length()<24){
                dtPrinter.drawTextRegular(monitorItemsValue, valueStartX, currentY+textMarginTop, 53, 4, valueFontHeight,
                        LPAPI.FontStyle.REGULAR, 0);
            }
            else{
                // 对saveMethodValue在24字符位置加入\n换行符
                int minLen = Math.min(monitorItemsValue.length(),30);
                monitorItemsValue = monitorItemsValue.substring(0,minLen)+"\n"+monitorItemsValue.substring(minLen);
                dtPrinter.drawTextRegular(monitorItemsValue, valueStartX, currentY+textMarginTop, 53, 4, 2,
                        LPAPI.FontStyle.REGULAR, 0);
            }

            currentY += lineHeight;
            dtPrinter.drawLine(marginX, currentY , 70 - marginX, currentY , lineWidth);

            // 6.5 保存方法
            dtPrinter.drawTextRegular("保存方法：", labelX, currentY+textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            if(saveMethodValue.length()<24){
                dtPrinter.drawTextRegular(saveMethodValue, valueStartX, currentY+textMarginTop, 53, 4, valueFontHeight,
                        LPAPI.FontStyle.REGULAR, 0);
            }
            else{
                // 对saveMethodValue在24字符位置加入\n换行符
                int minLen = Math.min(saveMethodValue.length(),26);
                // 将saveMethodValue的长度按照31个字符，分为最多三行，每行进行单独打印
                double topY = currentY+textMarginTop-1;
                // 向上取整数
                double mod = Math.ceil((double) saveMethodValue.length() /minLen);
                int maxRow = Math.min(3, (int)mod);
                for(int i=0; i< maxRow; i++){
                    // 修复字符串截取越界问题
                    int startIndex = i*minLen;
                    int endIndex = Math.min(minLen*(i+1), saveMethodValue.length());
                    String tempStr = saveMethodValue.substring(startIndex, endIndex);
                    dtPrinter.drawTextRegular(tempStr, valueStartX, topY, 53, 7, 2.1,
                            LPAPI.FontStyle.REGULAR, 0);
                    topY += 2.3;

                }

            }

            currentY += lineHeight+2;
            dtPrinter.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            // 6.6 状态：label（正常字体）+ 三个单选框（调用drawRadioBox方法）
            // 6.6.1 绘制“状态：”label（与其他label排版一致）
            dtPrinter.drawTextRegular("样品状态：", labelX, currentY+textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);

            // 6.6.2 绘制“待检”文本+选中单选框
            String statusLabel1 = "待检";
            double label1X = valueStartX;
            dtPrinter.drawTextRegular(statusLabel1, label1X, currentY+textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            // 待检单选框X坐标：文本宽度（6mm）+ 文本X（15mm）+ 间距（1mm）=22mm
            drawRadioBox(dtPrinter, label1X + 6 , currentY + textMarginTop, statusLabel1.equals(statusValue));

            // 6.6.3 绘制“在检”文本+未选中单选框
            String statusLabel2 = "在检";
            double label2X = label1X + 6 + 1 + 3 + 3; // 前一个单选框宽（3mm）+ 间距（3mm）= 起始X=28mm
            dtPrinter.drawTextRegular(statusLabel2, label2X, currentY+textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            // 在检单选框X坐标：文本X（28mm）+ 文本宽（6mm）+ 间距（1mm）=35mm
            drawRadioBox(dtPrinter, label2X + 6, currentY+textMarginTop, statusLabel2.equals(statusValue));

            // 6.6.4 绘制“检毕”文本+未选中单选框
            String statusLabel3 = "检毕";
            double label3X = label2X + 6 + 1 + 3 + 3; // 前一个单选框宽（3mm）+ 间距（3mm）= 起始X=34mm
            dtPrinter.drawTextRegular(statusLabel3, label3X, currentY+textMarginTop, 6, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);
            // 检毕单选框X坐标：文本X（34mm）+ 文本宽（6mm）+ 间距（1mm）=41mm
            drawRadioBox(dtPrinter, label3X + 6 , currentY+textMarginTop, statusLabel3.equals(statusValue));

            // 6.6.5 绘制状态行分隔线（与其他行一致）
            currentY += lineHeight-1;
            dtPrinter.drawLine(marginX, currentY, 70 - marginX, currentY, lineWidth);

            // 6.7 采样人（最后一行无分隔线）
            dtPrinter.drawTextRegular("采样人员：", labelX, currentY+textMarginTop, 13, 4, labelFontHeight,
                    LPAPI.FontStyle.REGULAR, 0);
            dtPrinter.drawTextRegular(samplersValue, valueStartX, currentY+textMarginTop, 53, 4, 2.5,
                    LPAPI.FontStyle.REGULAR, 0);

            // 7. 提交打印任务（文档7.3节：commitJob提交实际打印）
            boolean isPrintSuccess = dtPrinter.commitJob();
            if (isPrintSuccess) {
                showShortText("打印任务提交成功，等待打印机执行");
            } else {
                showShortText("打印任务提交失败，请检查打印机状态");
            }

        } catch (Exception e) {
            // 异常时取消任务（文档7.2节：abortJob取消绘图任务）
            dtPrinter.abortJob();
            e.printStackTrace();
            Log.e("PrintError", "打印绘制异常：" + e.getMessage());
            showShortText("打印失败，发生异常");
        }
    }


    private void printLargeQRCodeDenseLabel (String accountName, Assets assets, String[] fieldArray){
        String assetsName = assets.getASSETSNAME();       // 资产名称
        String barcodeId = assets.getBARCODEID();     // 条码编号
        String assetsStandard = assets.getASSETSSTANDARD(); // 型号
        String classification = assets.getCLASSIFICATION(); // 密级
        if("01".equals(classification)){
            classification = "绝密";
        }
        else if("02".equals(classification)){
            classification = "机密";
        }
        else if("03".equals(classification)){
            classification = "秘密";
        }

        String useDept = assets.getASSETSDEPT(); // 使用部门
        String ip = assets.getIP();   // IP地址
        String assetsLayAdd = assets.getASSETSLAYADD(); // 存放地点
        String assetsSuDate = assets.getASSETSUSEDATE(); // 取得日期
        String custodian = assets.getCUSTODIAN(); // 管理人
        String purpose = assets.getPURPOSE(); // 用途

        // 如果purpose超过4个字只保留四个字
        if(purpose.length() > 4){
            purpose = purpose.substring(0, 4);
            purpose += "…";
        }


        // 标签纸的高宽
        double height = 48;
        double width = 70;

        LPAPI dtPrinter = application.getDTPrinter();
        dtPrinter.startJob(width, height, 90);



        double fontHeight = 2.5;

        // label名称宽
        double labelNameWidth = 14;
        double marginX = 1;
        double marginY = 0;

        // 行高，线宽
        double lineHeight = 6;
        double lineWidth = 0.2;
        // 先绘制一个矩形
        dtPrinter.drawRectangle(marginX, marginY+lineHeight, width-(2*marginX), height-(2*marginY)-lineHeight, lineWidth);
        // 初始起点x,y
        double startLineX = marginX;
        double startLineY = (2*lineHeight) + marginY;;
        // 初始终点x,y
        double endLineX = width - marginX;
        double endLineY = startLineY;
        for(int i = 0; i < 6; i++){

            // 先绘制一条线
            if(i <= 1){
                // 如果是这三个地方，终点的X坐标需要缩短20mm
                double tempEndLineX = endLineX - 20;
                dtPrinter.drawLine(startLineX, startLineY, tempEndLineX, endLineY, lineWidth);
            }
            else{
                dtPrinter.drawLine(startLineX, startLineY, endLineX, endLineY, lineWidth);
            }
            // 换行增加Y点距离
            startLineY += lineHeight;
            endLineY += lineHeight;
        }

        // 画两条竖线
        double startColLineX1 = marginX + labelNameWidth;
        dtPrinter.drawLine(startColLineX1, lineHeight + marginY, startColLineX1, height-marginY, lineWidth);

        double startColLineX2 = startColLineX1 + 34;
        dtPrinter.drawLine(startColLineX2, lineHeight + marginY, startColLineX2, marginY+(4*lineHeight), lineWidth);

        dtPrinter.drawLine(startColLineX2, marginY+(5*lineHeight), startColLineX2, height-marginY, lineWidth);

        double startColLineX3 = startColLineX1+20;
        dtPrinter.drawLine(startColLineX3, marginY+(5*lineHeight), startColLineX3, height-marginY, lineWidth);

        // 填充文字

        // 账套名称
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        dtPrinter.setItemVerticalAlignment(LPAPI.ItemAlignment.CENTER);
        dtPrinter.drawText(accountName, marginX, marginY, width-(2*marginX), lineHeight, fontHeight+1);

        // 资产名称
        double tempY = marginY + (lineHeight * 1);
        dtPrinter.drawText("设备名称", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(assetsName, marginX+labelNameWidth, tempY, 34, lineHeight, fontHeight);

        // 条码编号
        tempY = marginY + (lineHeight * 2);
        dtPrinter.drawText("设备编号", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(barcodeId, marginX+labelNameWidth, tempY, 34, lineHeight, fontHeight);

        // 型号
        tempY = marginY + (lineHeight * 3);
        dtPrinter.drawText("设备型号", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(assetsStandard, marginX+labelNameWidth, tempY, 34, lineHeight, fontHeight);

        // 密级
        tempY = marginY + (lineHeight * 4);
        dtPrinter.drawText("使用地点", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(assetsLayAdd, marginX+labelNameWidth, tempY, 34, lineHeight, fontHeight);

        // 管理部门
        tempY = marginY + (lineHeight * 5);
        dtPrinter.drawText("部门", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(useDept, marginX+labelNameWidth, tempY, 20, lineHeight, fontHeight);

        // IP地址
        dtPrinter.drawText("IP地址", marginX+34, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(ip, marginX+34+labelNameWidth, tempY, 20, lineHeight, fontHeight);

        // 存放地点
        tempY = marginY + (lineHeight * 6);
        dtPrinter.drawText("密级", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(classification, marginX+labelNameWidth, tempY, 20, lineHeight, fontHeight);

        // 管理人员
        dtPrinter.drawText("责任人", marginX+34, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(custodian, marginX+34+labelNameWidth, tempY, 20, lineHeight, fontHeight);

        // 用途
        tempY = marginY + (lineHeight * 7);
        dtPrinter.drawText("用途", marginX, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(purpose, marginX+labelNameWidth, tempY, 20, lineHeight, fontHeight);

        // 取得日期
        dtPrinter.drawText("启用日期", marginX+34, tempY, labelNameWidth, lineHeight, fontHeight);
        dtPrinter.drawText(assetsSuDate, marginX+34+labelNameWidth, tempY, 20, lineHeight, fontHeight);


        // 绘制二维码
        String qrStr = String.format("设备编号：%1$s  设备名称：%2$s", barcodeId, assetsName);
        dtPrinter.draw2DQRCode(qrStr, 51, 7.5,15);

        boolean isPrintSuccess = dtPrinter.commitJob();
        if (isPrintSuccess) {
            showShortText("打印任务提交成功，等待打印机执行");
        } else {
            showShortText("打印任务提交失败，请检查打印机状态");
        }
    }

    /**
     * 单独绘制单选框（选中/未选中）
     *
     * @param dtPrinter LPAPI打印机实例（需确保已连接，参考文档5.15节isPrinterOpened()）
     * @param x         单选框左上角水平坐标（单位：mm，参考文档所有绘制方法的坐标定义）
     * @param y         单选框左上角垂直坐标（单位：mm）
     * @param isChecked 是否选中：true=方框+√，false=空方框
     */
    private void drawRadioBox(LPAPI dtPrinter, double x, double y, boolean isChecked) {
        // 1. 定义单选框基础参数（适配打印尺寸，避免过大/过小）
        final double radioSize = 3.0;    // 单选框宽高：3mm（视觉协调，与字体高度3.5mm匹配）
        final double lineWidth = 0.2;    // 方框线宽：0.2mm（参考文档11.9节drawLine的线宽设计）
        final double checkmarkSize = 2.5;// “√”字体高度：2.5mm（确保在3mm方框内居中）

        // 2. 绘制单选框方框（文档11.1节：drawRectangle(x,y,宽,高,线宽)，单位均为mm）
        dtPrinter.drawRectangle(x, y, radioSize, radioSize, lineWidth);

        // 3. 若选中，绘制居中“√”（文档9.3节：drawTextRegular，字体风格设为粗体）
        if (isChecked) {
            // 计算“√”的居中坐标：X偏移0.5mm、Y偏移0.3mm（确保在3mm方框内上下左右居中）
            double checkmarkX = x + 0.5;
            double checkmarkY = y + 0.3;
            // 绘制粗体“√”：文本内容为“√”，字体风格BOLD（文档233节：FontStyle.BOLD=1），行间距0
            dtPrinter.drawTextRegular("√", checkmarkX, checkmarkY, radioSize - 1, radioSize - 0.5,
                    checkmarkSize, LPAPI.FontStyle.BOLD, 0);
        }
    }

    private void printQRCodeAssets_JC_WebQR_Double(String accountName, Assets assets, String[] fieldArray) throws NoSuchFieldException, IllegalAccessException {
        LPAPI dtPrinter = application.getDTPrinter();

        double x1 = 21;
        double x2 = 69;
        double y1 = 4;
        double y2 = 23;

        double tX1 = 22.5;
        double tX2 = 36;
        double tY1 = 11;

        double fontSize1 = 3.5;
        double fontWeight1 = 3;

//        accountName= "望谟县润谟水利工程建设";

        double titleHeight = 4;
        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        dtPrinter.startJob(70, 25, 90);
        // 开始一个页面的绘制，绘制文本字符串
        if ("居中".equals(printerPosition)) {
            if (accountName.length() > 15) {
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                // 在第十四个字符位置插入\n，同时titleHeight乘以2
                accountName = accountName.substring(0, 15) + "\n" + accountName.substring(15);
                titleHeight = titleHeight * 2;
                tY1 = tY1 + 2;
                y2 = y2 + 1;
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            } else {
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }
        } else {
            if (accountName.length() > 15) {
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                // 在第十四个字符位置插入\n，同时titleHeight乘以2
                accountName = accountName.substring(0, 15) + "\n" + accountName.substring(15);
                titleHeight = titleHeight * 2;
                tY1 = tY1 + 2;
                y2 = y2 + 1;
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            } else {
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }
        }

        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        // 椭圆边框
        dtPrinter.drawRoundRectangle(x1, tY1 - 1, (x2 - x1), (y2 - tY1 + 1), 1.5, 1.5, 0.2);

        List<String> printFields = Arrays.asList(printfieldsCode);

        int printSize = 0;
        int i = 0;

        // 添加边界检查，防止数组越界
        while (printSize < 2 && i < fieldArray.length) {
            String firstField = fieldArray[i];
            int ci = printFields.indexOf(firstField);
            i++;

            if (ci == -1) {
                continue; // 如果字段不在 printFields 中，跳过
            }
            try {
                Field field1 = assets.getClass().getDeclaredField(firstField);
                field1.setAccessible(true);
                String fieldValue1 = String.valueOf(field1.get(assets));

                if (field1.getName().equals("GROUNDMANAGEDEPT")) {
                    fieldValue1 = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue1);
                } else if (field1.getName().equals("ISCZ")) {
                    fieldValue1 = "02".equals(fieldValue1) ? "是" : "否";
                }

                dtPrinter.drawText(printfieldsTitle[ci] + " / ", tX1, tY1 + (printSize * 5.5), 15, fontSize1, fontWeight1);
                dtPrinter.drawText(fieldValue1, tX2, tY1 + (printSize * 5.5), 23, fontSize1, fontWeight1);

                if (printSize == 0) {
                    dtPrinter.drawLine(tX1, tY1 + 4.5, 66.5, tY1 + 4.5, 0.2);
                } else {
                    dtPrinter.drawLine(tX1, tY1 + 10, 66.5, tY1 + 10, 0.2);
                }

                printSize++;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                showShortText("打印字段出错");
            }
        }

        // 二维码
        String barCode = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s", SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
        dtPrinter.draw2DQRCode(barCode, 1, 4, 19);

        // 结束绘图任务提交打印
        dtPrinter.commitJob();
    }


    private void printAssets(String accountName, String barCode, String assetsName, String userMan) {
        if ("智软2号".equals(application.getPrinterDevice())) {  // 精臣
            if ("大二维码".equals(application.getPrinterEffect())) {
                printQRCodeAssets_JC(accountName, barCode, assetsName, userMan);
            } else if ("大一维码".equals(application.getPrinterEffect())) {
                printAssets_JC(accountName, barCode, assetsName, userMan);
            } else if ("小一维码".equals(application.getPrinterEffect())) {
                printBarCode_JC(barCode);
            } else if ("小二维码".equals(application.getPrinterEffect())) {
                String qrCode = String.format("条码编号：%1$s\n资产名称：%2$s\n使用人：%3$s\n", barCode, assetsName, userMan);
                printQRCode_JC(qrCode);
            }
            else{
                showShortText("请选择打印字段！");
            }
        }
    }

    private boolean printBarCode_CZ(String accountName, Assets assets, String[] fieldArray) {
        try {
            Map<String, String> keyMap = new HashMap<>();
            keyMap.put("ASSETSNAME", "资产名称");
            keyMap.put("ASSETSUSEDATE", "使用日期");
            keyMap.put("ASSETSDEPT", "使用部门");
            keyMap.put("ASSETSSTANDARD", "规格型号");
            keyMap.put("ASSETSLAYADD", "存放位置");


            LPAPI dtPrinter = application.getDTPrinter();
            // 设置宽高
            dtPrinter.startJob(70, 38, 90);
            // 设置文本的水平对齐方式为居中对齐。
            dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
            // 绘制文本框：通过调用drawText方法，传入需要绘制的文本字符串、文本框的左上角位置、宽度和高度等参数，绘制一个文本框。
            dtPrinter.drawText(accountName, 0, 1.5, 70, 4, 3.5);
            dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.RIGHT);
            String qrCode = String.format(assets.getBARCODEID());
            dtPrinter.draw2DQRCode(qrCode, 50, 8, 15);
            dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

            String keys = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_FIELDS_CODE, "");
            dtPrinter.drawText("资产编码：".concat(assets.getBARCODEID()).concat("\n"), 1, 8, 40, 4, 3);
            if (StringUtils.isNullOrEmpty(keys)) {
                dtPrinter.drawText("资产名称：".concat(assets.getASSETSNAME()).concat("\n"), 1, 13, 40, 4, 3);
                dtPrinter.drawText("使用日期：".concat(assets.getASSETSUSEDATE()).concat("\n"), 1, 18, 40, 4, 3);
                dtPrinter.drawText("使用部门：".concat(assets.getASSETSDEPT()).concat("\n"), 1, 23, 40, 4, 3);
                dtPrinter.drawText("规格型号：".concat(assets.getASSETSSTANDARD()).concat("\n"), 1, 28, 40, 4, 3);
                dtPrinter.drawText("存放位置：".concat(assets.getASSETSLAYADD()).concat("\n"), 1, 33, 40, 4, 3);
            } else {
                // 解析字符串keys，获取字段数组
                String[] fieldArray1 = keys.split(",");
                // 通过fieldArray字段数组，反射获取assets属性值
                for (int i = 0; i < fieldArray1.length; i++) {
                    String field = fieldArray1[i];
                    String fieldValue = String.valueOf(this.getField(assets, field));
                    dtPrinter.drawText(keyMap.get(field).concat("：").concat(fieldValue).concat("\n"), 1, 13 + (i * 5), 40, 4, 3);
                }
            }


            return dtPrinter.commitJob();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    private boolean printBarCodeFive(String accountName, Assets assets, String[] fieldArray) {
        StringBuilder sb1 = new StringBuilder();
        LPAPI dtPrinter = application.getDTPrinter();
        // 设置宽高
        dtPrinter.startJob(70, 25, 90);
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        dtPrinter.drawText(accountName, 0, 1.5, 70, 4, 3.5);
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));
        dtPrinter.drawLine(20.5, 7, 20.5, 24, 0.2);

        double tHeight = 7 + Math.min(fieldArray.length, 5) * 4;
        double y0 = 6;
        double d = (24 - y0) / 5;
        double y1 = y0 + d;
        double y2 = y0 + (2 * d);
        double y3 = y0 + (3 * d);
        double y4 = y0 + (4 * d);
        double y5 = y0 + (4 * d);
        // x,y,x,y,size
        application.getDTPrinter().drawLine(20.5, y0, 20.5, 24, 0.2);
        application.getDTPrinter().drawLine(20.5, y0, 68, y0, 0.2);
        application.getDTPrinter().drawLine(68, y0, 68, 24, 0.2);
        application.getDTPrinter().drawLine(20.5, y1, 68, y1, 0.2);
        application.getDTPrinter().drawLine(20.5, y2, 68, y2, 0.2);
        application.getDTPrinter().drawLine(20.5, y3, 68, y3, 0.2);
        application.getDTPrinter().drawLine(20.5, y4, 68, y4, 0.2);
        application.getDTPrinter().drawLine(20.5, 24, 68, 24, 0.2);
        application.getDTPrinter().drawLine(35, y0, 35, 24, 0.2);
        // 条码编号，x,y,w, h, size
        dtPrinter.drawText("条码编号", 20.5, y0 + 0.5, 15, 3.9, 2.5);
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        dtPrinter.drawText(assets.getBARCODEID(), 35.5, y0 + 0.5, 23, 3.9, 2.5);
        for (int i = 0; i < fieldArray.length; i++) {
            if (TextUtils.isEmpty(fieldArray[i])) {
                continue;
            }
            int ci = Arrays.asList(printfieldsCode).indexOf(fieldArray[i]);
            if (ci != -1) {
                try {
                    Field field = assets.getClass().getDeclaredField(fieldArray[i]);
                    //允许访问私有字段
                    field.setAccessible(true);
                    String fieldValue = String.valueOf(field.get(assets));
                    String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                    sb1.append(printLine);
                    if (i < 4) {
                        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                        String label = this.insertSpaces(printfieldsTitle[ci]);
                        dtPrinter.drawText(label, 20.5, y0 + 0.5 + (i + 1) * d, 15, 3.9, 2.5);
                        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                        dtPrinter.drawText(String.valueOf(fieldValue), 35.5, y0 + 0.5 + (i + 1) * d, 23, 3.9, 2.5);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        String barCode = sb1.toString();
        application.getDTPrinter().draw2DQRCode(barCode, 2, 7, 16);
        // 结束绘图任务提交打印
        return dtPrinter.commitJob();
    }

    /**
     * 如果字符串小于等于三个字符，则在每个字符间补空格，以达到四个字符的长度。
     *
     * @param input 原始字符串
     * @return 调整后的字符串
     */
    private String insertSpaces(String input) {
        if (input == null || input.length() > 3) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            if (i < 2) { // 避免在最后一个字符后添加空格
                sb.append("  ");
            }
        }

        // 如果原始字符串长度为1或2，需要在最后添加一个或两个空格以确保总长度为4
        if (input.length() == 1) {
            sb.append("    ");
        } else if (input.length() == 2) {
            sb.append("    ");
        }

        return sb.toString();
    }


    /**
     * 通过反射和字符串字段名从对象中获取属性值。
     *
     * @param obj       目标对象
     * @param fieldName 字段名
     * @return 包含属性值的Optional对象，如果属性不存在则为empty Optional
     * @throws IllegalAccessException 如果访问权限不足
     */
    private Object getField(Object obj, String fieldName) throws IllegalAccessException {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return "";
        }

        Class<?> clazz = obj.getClass();
        Field field = null;

        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass(); // 尝试从父类中查找
            }
        }

        if (field == null) {
            return ""; // 没有找到对应的字段
        }

        field.setAccessible(true); // 允许访问私有字段
        return field.get(obj);
    }

    protected void printQRCode(String barCode) {
        if (application.getPrinterDevice().equals("智软2号")) {  // 精臣
            printQRCode_JC(barCode);
        }
    }

    protected void printBarCode(String barCode) {
        if (application.getPrinterDevice().equals("智软1号")) {//博斯德
        } else if (application.getPrinterDevice().equals("智软2号")) {  // 精臣
            printBarCode_JC(barCode);
        } else if (application.getPrinterDevice().equals("智软3号")) {//普贴

        }
    }

    private boolean printBarCode_JC(String barCode) {
        application.getDTPrinter().startJob(25, 8, 0);
        // 开始一个页面的绘制，绘制文本字符串
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        application.getDTPrinter().startPage();
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().draw1DBarcode(barCode, LPAPI.BarcodeType.CODE128, 1.2, 1.2, 25, 5, 0);
        application.getDTPrinter().endPage();
        // 设置之后绘制的对象内容旋转180度
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    private boolean printQRCode_JC(String accountName, Assets assets, String[] fieldArray) {
        StringBuilder sb1 = new StringBuilder();
        for (int i = 0; i < fieldArray.length && i < 4; i++) {
            if (TextUtils.isEmpty(fieldArray[i])) continue;
            int ci = Arrays.asList(printfieldsCode).indexOf(fieldArray[i]);
            if (ci != -1) {
                try {
                    Field field = assets.getClass().getDeclaredField(fieldArray[i]);
                    //允许访问私有字段
                    field.setAccessible(true);
                    String fieldValue = String.valueOf(field.get(assets));
                    if (field.getName().equals("GROUNDMANAGEDEPT")) {
                        fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                    } else if (field.getName().equals("ISCZ")) {
                        fieldValue = "02".equals(fieldValue) ? "是" : "否";
                    }

                    String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                    sb1.append(printLine);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));
        String barCode = sb1.toString();
        return printQRCode_JC(barCode);
    }

    private boolean printQRCode_JC(String barCode) {
        application.getDTPrinter().startJob(25, 70, 0);
        // 开始一个页面的绘制，绘制文本字符串
        //application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().draw2DQRCode(barCode, 5, 1.5, 15);
        application.getDTPrinter().draw2DQRCode(barCode, 5, 21.5, 15);
        application.getDTPrinter().draw2DQRCode(barCode, 5, 41.5, 15);
        // 设置之后绘制的对象内容旋转180度
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    private boolean printAssets_JC(String accountName, String barCode, String assetsName, String userMan) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 4, 70, 5, 3.5);
        // 设置之后绘制的对象内容旋转180度
        //application.getDTPrinter().setItemOrientation(90);
        // 绘制一维码，此一维码绘制时内容会旋转180度，
        // 传入参数(需要绘制的一维码的数据, 绘制的一维码左上角水平位置, 绘制的一维码左上角垂直位置, 绘制的一维码水平宽度, 绘制的一维码垂直高度)
        application.getDTPrinter().draw1DBarcode(barCode, LPAPI.BarcodeType.AUTO, 15, 9, 40, 10, 3);
        application.getDTPrinter().drawText(String.format("资产名称：%1$s  使用人：%2$s", assetsName, userMan), 0, 20, 70, 5, 3);
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    private boolean printQRCodeAssets_JC(String accountName, String barCode, String assetsName, String userMan) {
        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 2.5, 70, 5, 3.5);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        String qrCode = String.format("条码编号：%1$s\n资产名称：%2$s\n使用人：%3$s\n", barCode, assetsName, userMan);
        application.getDTPrinter().draw2DQRCode(qrCode, 8, 8, 15);
        //application.getDTPrinter().drawText(accountName, 27, 8, 40, 4, 3);
        application.getDTPrinter().drawText("条码编号：".concat(barCode).concat("\n"), 27, 8, 40, 4, 3);
        application.getDTPrinter().drawText("资产名称：".concat(assetsName).concat("\n"), 27, 12, 40, 4, 3);
        application.getDTPrinter().drawText("使用人：".concat(userMan).concat("\n"), 27, 20, 16, 4, 3);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    private boolean printAssets_JC_define(String accountName, Assets assets, String[] fieldsArray) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 2, 70, 5, 3.5);
        // 设置之后绘制的对象内容旋转180度
        //application.getDTPrinter().setItemOrientation(90);
        // 绘制一维码，此一维码绘制时内容会旋转180度，
        // 传入参数(需要绘制的一维码的数据, 绘制的一维码左上角水平位置, 绘制的一维码左上角垂直位置, 绘制的一维码水平宽度, 绘制的一维码垂直高度)
        application.getDTPrinter().draw1DBarcode(assets.getBARCODEID(), LPAPI.BarcodeType.CODE128, 15, 7, 40, 10, 3);


        StringBuilder sb1 = new StringBuilder();
        for (int i = 0; i < fieldsArray.length && i < 2; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) continue;
            int ci = Arrays.asList(printfieldsCode).indexOf(fieldsArray[i]);
            if (ci != -1) {
                try {
                    Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                    field.setAccessible(true);//允许访问私有字段
                    String fieldValue = String.valueOf(field.get(assets));
                    if (field.getName().equals("GROUNDMANAGEDEPT")) {
                        fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                    } else if (field.getName().equals("ISCZ")) {
                        fieldValue = "02".equals(fieldValue) ? "是" : "否";
                    }
                    sb1.append(printfieldsTitle[ci]).append("：").append(fieldValue).append("  ");
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        String line1 = sb1.toString().trim();
        application.getDTPrinter().drawText(line1, 0, 17.5, 70, 5, 3);
        StringBuilder sb2 = new StringBuilder();
        if (fieldsArray.length > 2) {
            for (int i = 2; i < fieldsArray.length && i < 4; i++) {
                if (TextUtils.isEmpty(fieldsArray[i])) continue;
                int ci = Arrays.asList(printfieldsCode).indexOf(fieldsArray[i]);
                if (ci != -1) {
                    try {
                        Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                        field.setAccessible(true);//允许访问私有字段

                        String fieldValue = String.valueOf(field.get(assets));
                        if (field.getName().equals("GROUNDMANAGEDEPT")) {
                            fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                        } else if (field.getName().equals("ISCZ")) {
                            fieldValue = "02".equals(fieldValue) ? "是" : "否";
                        }

                        sb2.append(printfieldsTitle[ci]).append("：").append(fieldValue).append("  ");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String line2 = sb2.toString().trim();
        if (!TextUtils.isEmpty(line2))
            application.getDTPrinter().drawText(line2, 0, 21, 70, 5, 3);

        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    /**
     * 打印二维码资产标签（表格布局）
     *
     * @param accountName 账套名称
     * @param assets 资产对象
     * @param fieldsArray 需要打印的字段数组
     * @return 打印是否成功
     */
    private boolean printQRCodeAssets_JC_define(String accountName, Assets assets, String[] fieldsArray) {

        // ==================== 初始化绘图任务 ====================
        // 页面尺寸：宽70mm，高25mm，旋转90度（横向打印）
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);

        // ==================== 绘制标题（单位名称）====================
        // 参数：文本, x, y, 宽度, 高度, 字体大小(mm)
        application.getDTPrinter().drawText(accountName, 0, 1.5, 70, 5, 4);

        // 切换为左对齐模式，用于后续表格内容绘制
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        // ==================== 构建二维码内容 ====================
        StringBuilder sb1 = new StringBuilder();
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));

        // ==================== 表格布局参数配置（可调整）====================
        // 表格位置和尺寸（保持与原代码一致）
        double tableStartX = 20.5;      // 表格左上角X坐标（距页面左侧20.5mm）
        double tableStartY = 7;          // 表格左上角Y坐标（距页面顶部7mm）
        double tableWidth = 47.5;        // 表格总宽度（68 - 20.5 = 47.5mm）
        double tableBottomY = 24;        // 表格底部Y坐标（固定值，保持与原代码一致）

        double qrCodeWidth = 16; // 二维码宽度
        double qrCodeX = 2; // 二维码x坐标
        double qrCodeY = 7; // 二维码yz坐标

        // 行高配置
        int maxDataRows = 3;             // 最大数据行数（除条码编号外，最多3行，与原代码保持一致）
        double tableHeight = tableBottomY - tableStartY;  // 表格总高度（16mm）
        double rowHeight = tableHeight / (maxDataRows + 1);  // 自动计算每行高度，确保各行动态均分

        // 线条样式
        double lineWidth = 0.2;          // 边框线宽（0.2mm）

        // 列宽配置
        double labelWidth = 15;        // 左侧标签列宽度（36 - 20.5 = 15.5mm）
        double valueWidth = 32;          // 右侧数值列宽度（68 - 36 = 32mm）

        // 文字样式
        double fontSize = 3.0;           // 字体大小（3.0mm，约8号字）
        double textMarginTop = (rowHeight - fontSize) / 2;  // 文字垂直居中偏移量

        // ==================== 绘制表格外边框 ====================
        // 使用矩形框替代四条边线，代码更简洁
        application.getDTPrinter().drawRectangle(tableStartX, tableStartY, tableWidth, tableHeight, lineWidth);

        // ==================== 绘制中间竖线（分隔左右两列）====================
        application.getDTPrinter().drawLine(
                tableStartX + labelWidth,    // 竖线X坐标（36mm位置）
                tableStartY,                  // 竖线起点Y坐标
                tableStartX + labelWidth,    // 竖线终点X坐标
                tableStartY + tableHeight,   // 竖线终点Y坐标（23mm位置）
                lineWidth                     // 线宽
        );

        // ==================== 绘制行分隔线（横线）====================
        // 绘制4条横线（第1条在顶部，后面3条分隔各行）
        for (int i = 0; i <= maxDataRows; i++) {
            double lineY = tableStartY + i * rowHeight;  // 计算每条横线的Y坐标
            application.getDTPrinter().drawLine(tableStartX, lineY, tableStartX + tableWidth, lineY, lineWidth);
        }

        // ==================== 绘制第一行：条码编号（固定行）====================
        // 绘制标签（左侧列）
        application.getDTPrinter().drawText("条码编号", tableStartX + 1, tableStartY + textMarginTop, labelWidth - 2, rowHeight, fontSize);
        // 绘制条码值（右侧列）
        application.getDTPrinter().drawText(assets.getBARCODEID(), tableStartX + labelWidth + 1, tableStartY + textMarginTop, valueWidth - 2, rowHeight, fontSize);

        // ==================== 绘制动态字段行（最多3行）====================
        int printedRows = 0;  // 已打印的数据行数（从第2行开始，索引为1）
        for (int i = 0; i < fieldsArray.length && printedRows < maxDataRows; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) {
                continue;  // 跳过空字段
            }

            // 查找字段在预定义数组中的索引
            int ci = Arrays.asList(printfieldsCode).indexOf(fieldsArray[i]);
            if (ci != -1) {
                try {
                    // 通过反射获取字段值
                    Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                    field.setAccessible(true);  // 允许访问私有字段
                    String fieldValue = String.valueOf(field.get(assets));

                    // 特殊字段处理：部门名称转换、是否字段转换
                    if (field.getName().equals("GROUNDMANAGEDEPT")) {
                        fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                    } else if (field.getName().equals("ISCZ")) {
                        fieldValue = "02".equals(fieldValue) ? "是" : "否";
                    }

                    // 添加到二维码内容字符串
                    String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                    sb1.append(printLine);

                    // 计算当前行的Y坐标（从第2行开始，所以是 printedRows + 1）
                    double currentRowY = tableStartY + (printedRows + 1) * rowHeight;

                    // 绘制字段标签（左侧列）
                    application.getDTPrinter().drawText(
                            printfieldsTitle[ci],                    // 字段中文名
                            tableStartX + 1,                         // X坐标（留1mm边距）
                            currentRowY + textMarginTop,             // Y坐标（加上边距）
                            labelWidth - 2,                          // 宽度（减去2mm边距）
                            rowHeight,                               // 高度
                            fontSize                                 // 字体大小
                    );

                    // 绘制字段值（右侧列）
                    application.getDTPrinter().drawText(
                            fieldValue,                              // 字段值
                            tableStartX + labelWidth + 1,            // X坐标（标签列宽度+1mm边距）
                            currentRowY + textMarginTop,             // Y坐标
                            valueWidth - 2,                          // 宽度（减去2mm边距）
                            rowHeight,                               // 高度
                            fontSize                                 // 字体大小
                    );

                    printedRows++;  // 增加已打印行数
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();  // 字段不存在或访问失败时打印异常
                }
            }
        }

        // ==================== 绘制二维码 ====================
        // 参数：二维码内容, x, y, 宽度（高度自动计算）
        String barCode = sb1.toString();
        application.getDTPrinter().draw2DQRCode(barCode, qrCodeX, qrCodeY, qrCodeWidth);

        // ==================== 提交打印任务 ====================
        return application.getDTPrinter().commitJob();
    }

    private boolean printQRCodeAssets_JC_WebQR(String accountName, Assets assets, String[] fieldsArray) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 1.5, 70, 5, 3.5);

        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        StringBuilder sb1 = new StringBuilder();
        double tHeight = 7 + Math.min(fieldsArray.length, 4) * 4;
        application.getDTPrinter().drawLine(20.5, 7, 20.5, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 7, 68, 7, 0.2);
        application.getDTPrinter().drawLine(68, 7, 68, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 11, 68, 11, 0.2);
        application.getDTPrinter().drawLine(20.5, 15, 68, 15, 0.2);
        application.getDTPrinter().drawLine(20.5, 19, 68, 19, 0.2);
        application.getDTPrinter().drawLine(20.5, 23, 68, 23, 0.2);
        application.getDTPrinter().drawLine(36, 7, 36, 23, 0.2);

        // 已经打印的总数
        List<String> alreadyPrintFields = new ArrayList<>();
        // 所有打印字段
        List<String> printFields = Arrays.asList(printfieldsCode);
        for (int i = 0; i < fieldsArray.length; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) continue;
            // 查询打印字段是否存在
            int ci = printFields.indexOf(fieldsArray[i]);
            if (ci == -1) {
                continue;
            }
            int size = alreadyPrintFields.size();
            if (size > 4) {
                break;
            }
            try {
                Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                field.setAccessible(true);
                String fieldValue = String.valueOf(field.get(assets));
                if (field.getName().equals("GROUNDMANAGEDEPT")) {
                    fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                } else if (field.getName().equals("ISCZ")) {
                    fieldValue = "02".equals(fieldValue) ? "是" : "否";
                }
                String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                sb1.append(printLine);
                application.getDTPrinter().drawText(printfieldsTitle[ci], 21.5, 7.2 + (size) * 3.9, 15, 3.9, 3);
                application.getDTPrinter().drawText(String.valueOf(fieldValue), 37, 7.2 + (size) * 3.9, 29, 3.9, 3);
                alreadyPrintFields.add(fieldsArray[i]);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        // 二维码
        String barCode = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s", SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
        application.getDTPrinter().draw2DQRCode(barCode, 3, 7, 16);
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }


    private boolean printQRCode_Assets_SJ (String accountName, Assets assets, String[] labelArr,String[] fieldsArray) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 1.5, 70, 5, 3.5);


        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        StringBuilder sb1 = new StringBuilder();

        double tableStartX = 20.5;
        double tableStartY = 7;
        double tableWidth = 47.5;
        double rowHeight = 3.2;
        int maxRows = 5;
        double tableHeight = maxRows * rowHeight;
        tableHeight += 0.5;
        double lineWidth = 0.2;

        // 可动态调整的字体大小（单位：毫米），方便调试
        double fontSize = 2.3;
        // 可动态调整的垂直偏移量（正值向下，负值向上），用于微调文本垂直位置
        double verticalOffset = -0.4;

        // 绘制外边框矩形
        application.getDTPrinter().drawRectangle(tableStartX, tableStartY, tableWidth, tableHeight, lineWidth);

        // 绘制中间竖线分隔左右两列
        application.getDTPrinter().drawLine(36, tableStartY, 36, tableStartY + tableHeight, lineWidth);

        // 绘制行分隔线（从第二行开始，共4条横线）
        for (int i = 1; i < maxRows; i++) {
            double lineY = tableStartY + i * rowHeight;
            application.getDTPrinter().drawLine(tableStartX, lineY, tableStartX + tableWidth, lineY, lineWidth);
        }

        // 已经打印的总数
        List<String> alreadyPrintFields = new ArrayList<>();
        // 所有打印字段
        for (int i = 0; i < fieldsArray.length; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) continue;
            int size = alreadyPrintFields.size();
            if (size >= maxRows) {
                break;
            }
            try {
                Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                field.setAccessible(true);
                // 反射获取值
                String fieldValue = String.valueOf(field.get(assets));

                String printLine = labelArr[i].concat("：").concat(fieldValue).concat("\n");
                sb1.append(printLine);

                // 计算垂直居中位置：行起始y + (行高 - 字体高度) / 2 + 垂直偏移量
                double textY = tableStartY + size * rowHeight + (rowHeight - fontSize) / 2 + verticalOffset;
                application.getDTPrinter().drawText(labelArr[i], tableStartX + 1, textY, 13, 0, fontSize);
                application.getDTPrinter().drawText(fieldValue, 37, textY, 23, 0, fontSize);
                alreadyPrintFields.add(fieldsArray[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        String addRemark = "介质".concat("：").concat(assets.getMEDIA()).concat("\n");
        sb1.append(addRemark);
        addRemark = "贮存条件".concat("：").concat(assets.getSTORAGENEED()).concat("\n");
        sb1.append(addRemark);
        // 二维码
        String barCode = String.format("%1$s",sb1.toString());
        application.getDTPrinter().draw2DQRCode(barCode, 3, 7, 16);
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }


    private boolean big_five(String accountName, Assets assets, String[] fieldsArray) {

        // 标签纸的宽
        int label_height = 50;
        // 标签纸的长
        int label_width = 70;

        double tableTop = 7.0;
        double tableLeft = 3.0;
        double tableRight = 67.0;
        double titleColSplit = 26.0;
        double qrSeparatorX = 49.0;
        double qrWidth = 13.0;
        double qrX = 52.0;
        double bottomMargin = 3.0;
        int totalRows = 5;
        // 根据边距动态计算行高
        double rowHeight = (label_height - tableTop - bottomMargin) / totalRows;

        // 开始绘图任务
        application.getDTPrinter().startJob(label_width, label_height, 90);

        // 顶部标题居中
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        application.getDTPrinter().drawText(accountName, 0, 1.5, 70, 5, 3.5);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);

        // 单元格文本垂直居中
        application.getDTPrinter().setItemVerticalAlignment(LPAPI.ItemAlignment.CENTER);

        double tableHeight = totalRows * rowHeight;

        // 绘制表格外边框
        application.getDTPrinter().drawRectangle(tableLeft, tableTop, tableRight - tableLeft, tableHeight, 0.3);

        // 绘制竖线分隔标签列和值列（贯穿全表）
        application.getDTPrinter().drawLine(titleColSplit, tableTop, titleColSplit, tableTop + tableHeight, 0.2);

        // 绘制行间横线，最后一行顶边的横线不穿过二维码区域
        for (int i = 1; i < totalRows; i++) {
            double lineY = tableTop + i * rowHeight;
            double lineEnd = (i >= 4) ? qrSeparatorX : tableRight;
            application.getDTPrinter().drawLine(tableLeft, lineY, lineEnd, lineY, 0.2);
        }

        // 绘制最后两行的二维码分隔竖线
        double qrVerticalTop = tableTop + 3 * rowHeight;
        double qrVerticalBottom = tableTop + totalRows * rowHeight;
        application.getDTPrinter().drawLine(qrSeparatorX, qrVerticalTop, qrSeparatorX, qrVerticalBottom, 0.2);

        // 收集需要打印的字段数据（最多5个）
        List<String> printFields = Arrays.asList(printfieldsCode);
        List<String> fieldTitles = new ArrayList<>();
        List<String> fieldValues = new ArrayList<>();

        for (int i = 0; i < fieldsArray.length && fieldTitles.size() < totalRows; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) continue;
            int ci = printFields.indexOf(fieldsArray[i]);
            if (ci == -1) continue;
            try {
                Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                field.setAccessible(true);
                String fieldValue = String.valueOf(field.get(assets));
                if (field.getName().equals("GROUNDMANAGEDEPT")) {
                    fieldValue = AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), fieldValue);
                } else if (field.getName().equals("ISCZ")) {
                    fieldValue = "02".equals(fieldValue) ? "是" : "否";
                }
                fieldTitles.add(printfieldsTitle[ci]);
                fieldValues.add(fieldValue);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // 补齐空行至5行
        while (fieldTitles.size() < totalRows) {
            fieldTitles.add("");
            fieldValues.add("");
        }

        // 绘制二维码（右侧，与倒数两行并列，垂直居中）
        double qrTop = tableTop + 3 * rowHeight + (2 * rowHeight - qrWidth) / 2;
        String barCode = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s",
                SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
        application.getDTPrinter().draw2DQRCode(barCode, qrX, qrTop, qrWidth);

        // 逐行绘制标签名和值
        for (int i = 0; i < totalRows; i++) {
            double y = tableTop + i * rowHeight;
            boolean isQR_Row = i >= 3;
            double valWidth = isQR_Row ? (qrSeparatorX - titleColSplit - 3) : (tableRight - titleColSplit - 2);

            application.getDTPrinter().drawText(fieldTitles.get(i), 5, y, 19, rowHeight, 2.8);
            if (!TextUtils.isEmpty(fieldValues.get(i))) {
                application.getDTPrinter().drawText(fieldValues.get(i), titleColSplit + 2, y, valWidth, rowHeight, 2.8);
            }
        }

        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }






    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
