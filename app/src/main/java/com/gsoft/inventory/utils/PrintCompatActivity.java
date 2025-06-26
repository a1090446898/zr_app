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

        try{
            // 获取手机型号
            String model = Build.MODEL;
            // 获取制造商
            String manufacturer = Build.MANUFACTURER;

            // 获取手持设备序列号
            String serialNumber = android.os.Build.SERIAL;

            if("unknown".equals(serialNumber)){
                String brand = Build.BRAND;
                serialNumber = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
                serialNumber = brand +"-"+serialNumber;
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
            String account = acctSuiteId + "-"+ accountName;

            if(StringUtils.isNullOrEmpty(deviceName)){
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
            if (TextUtils.isEmpty(jsonStr)){
                List<PrintLogInfo> printLogInfoList = new ArrayList<>();
                printLogInfoList.add(printLogInfo);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, gson.toJson(printLogInfoList));
            }
            else{
                List<PrintLogInfo> printLogInfoList = gson.fromJson(jsonStr, new TypeToken<List<PrintLogInfo>>() {}.getType());
                printLogInfoList.add(printLogInfo);
                SharedPreferencesUtils.putString(mContext, SharedPreferencesUtils.KEY_SAVE_PRINT_INFO, gson.toJson(printLogInfoList));
            }

        }catch (Exception e){
           Log.e("printAssets", e.getMessage());
        }
        String printTitle = handleAssetsTitle(accountName, assets);

        if (TextUtils.isEmpty(printFields)) {
            printAssets(printTitle, assets.getBARCODEID(), assets.getASSETSNAME(), StringUtils.isNullOrEmpty(assets.getASSETSUSER()) ? assets.getCUSTODIAN() : assets.getASSETSUSER());
        } else {
            String[] fieldArray = printFields.split(",");
            printAssets(printTitle, assets, fieldArray);
        }
    }

    private String handleAssetsTitle(String accountName, Assets assets){
        String result = "";
        String assetsDept = assets.getASSETSDEPT();
        String printerTitle = SharedPreferencesUtils.getString(mContext, SharedPreferencesUtils.KEY_PRINT_TITLE, "单位名称");
        if("部门名称".equals(printerTitle)){
            result = assetsDept;
        }
        else if("单位+部门名称".equals(printerTitle)){
            result = accountName + "(" + assetsDept + ")";
        }
        else{
            result = accountName;
        }

        return result;
    }

    private void printAssets(String accountName, Assets assets, String[] fieldArray) {
        if (application.getPrinterDevice().equals("智软2号")) {  // 精臣
            if (application.getPrinterEffect().equals("大二维码")) {
                printQRCodeAssets_JC_define(accountName, assets, fieldArray);
            } else if (application.getPrinterEffect().equals("大一维码")) {
                printAssets_JC_define(accountName, assets, fieldArray);
            } else if (application.getPrinterEffect().equals("小一维码")) {
                printBarCode_JC(assets.getBARCODEID());
            } else if (application.getPrinterEffect().equals("小二维码")) {
                printQRCode_JC(accountName, assets, fieldArray);
            } else if (application.getPrinterEffect().equals("网页二维码-四字段")) {
                printQRCodeAssets_JC_WebQR(accountName, assets, fieldArray);
            }
            else if(application.getPrinterEffect().equals("网页二维码-二字段")){
                printQRCodeAssets_JC_WebQR_Double(accountName, assets, fieldArray);
            }
            else if (application.getPrinterEffect().equals("财政二维码")) {
                printBarCode_CZ(accountName, assets, fieldArray);
            }
            else if (application.getPrinterEffect().equals("大二维码-五字段")) {
                printBarCodeFive(accountName, assets, fieldArray);
            }

        }
    }

    private void printQRCodeAssets_JC_WebQR_Double(String accountName, Assets assets, String[] fieldArray) {
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
        if("居中".equals(printerPosition)){
            if(accountName.length() > 15){
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                // 在第十四个字符位置插入\n，同时titleHeight乘以2
                accountName = accountName.substring(0, 15) + "\n" + accountName.substring(15);
                titleHeight = titleHeight * 2;
                tY1 = tY1 + 2;
                y2 = y2 + 1;
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }

            else{
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }
        }
        else{
            if(accountName.length() > 15){
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                // 在第十四个字符位置插入\n，同时titleHeight乘以2
                accountName = accountName.substring(0, 15) + "\n" + accountName.substring(15);
                titleHeight = titleHeight * 2;
                tY1 = tY1 + 2;
                y2 = y2 + 1;
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }

            else{
                dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
                dtPrinter.drawText(accountName, tX1, 4, 45, titleHeight, 3.3);
            }
        }

        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        StringBuilder sb1 = new StringBuilder();
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));

//        dtPrinter.drawRoundRectangle(x1, y1, (x2-x1), (y2-y1), 1.5, 1.5, 0.2);
        dtPrinter.drawRoundRectangle(x1, tY1-1, (x2-x1), (y2-tY1+1), 1.5, 1.5, 0.2);


        dtPrinter.drawText("条码编号 / ", tX1, tY1, 15, fontSize1, fontWeight1);
        dtPrinter.drawText(assets.getBARCODEID(), tX2, tY1, 23, fontSize1, fontWeight1);
        // 中间一条线
        dtPrinter.drawLine(tX1, tY1+4.5, 66.5, tY1+4.5, 0.2);
        // 下方文字
        dtPrinter.drawText("资产名称 / ", tX1, tY1+5.5, 15, fontSize1, fontWeight1);
        dtPrinter.drawText(assets.getASSETSNAME(), tX2, tY1+5.5, 23, fontSize1, fontWeight1);
        // 下方画线
        dtPrinter.drawLine(tX1, tY1+10, 66.5, tY1+10, 0.2);

        String barCode = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s", SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
        dtPrinter.draw2DQRCode(barCode, 1, 4, 19);


        // 结束绘图任务提交打印
        dtPrinter.commitJob();
    }


    private void printAssets(String accountName, String barCode, String assetsName, String userMan) {
        if (application.getPrinterDevice().equals("智软2号")) {  // 精臣
            if (application.getPrinterEffect().equals("大二维码")) {
                printQRCodeAssets_JC(accountName, barCode, assetsName, userMan);
            } else if (application.getPrinterEffect().equals("大一维码")) {
                printAssets_JC(accountName, barCode, assetsName, userMan);
            } else if (application.getPrinterEffect().equals("小一维码")) {
                printBarCode_JC(barCode);
            } else if (application.getPrinterEffect().equals("小二维码")) {
                String qrCode = String.format("条码编号：%1$s\n资产名称：%2$s\n使用人：%3$s\n", barCode, assetsName, userMan);
                printQRCode_JC(qrCode);
            }

        }
    }

    private boolean printBarCode_CZ(String accountName, Assets assets, String[] fieldArray) {
        try{
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
            if(StringUtils.isNullOrEmpty(keys)){
                dtPrinter.drawText("资产名称：".concat(assets.getASSETSNAME()).concat("\n"), 1, 13, 40, 4, 3);
                dtPrinter.drawText("使用日期：".concat(assets.getASSETSUSEDATE()).concat("\n"), 1, 18, 40, 4, 3);
                dtPrinter.drawText("使用部门：".concat(assets.getASSETSDEPT()).concat("\n"), 1, 23, 40, 4, 3);
                dtPrinter.drawText("规格型号：".concat(assets.getASSETSSTANDARD()).concat("\n"), 1, 28, 40, 4, 3);
                dtPrinter.drawText("存放位置：".concat(assets.getASSETSLAYADD()).concat("\n"), 1, 33, 40, 4, 3);
            }
            else{
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
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }


    private boolean printBarCodeFive(String accountName, Assets assets, String[] fieldArray){
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
        double d = (24-y0)/5;
        double y1 = y0 + d;
        double y2 = y0 + (2*d);
        double y3 = y0 + (3*d);
        double y4 = y0 + (4*d);
        double y5 = y0 + (4*d);
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
        dtPrinter.drawText("条码编号", 20.5, y0+0.5, 15, 3.9, 2.5);
        dtPrinter.setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        dtPrinter.drawText(assets.getBARCODEID(), 35.5, y0+0.5, 23, 3.9, 2.5);
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
                        dtPrinter.drawText(String.valueOf(fieldValue), 35.5, y0+0.5 + (i + 1) * d, 23, 3.9, 2.5);
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

    private boolean printQRCodeAssets_JC_define(String accountName, Assets assets, String[] fieldsArray) {

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        application.getDTPrinter().startJob(70, 25, 90);
        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.CENTER);
        // 开始一个页面的绘制，绘制文本字符串
        // 传入参数(需要绘制的文本字符串, 绘制的文本框左上角水平位置, 绘制的文本框左上角垂直位置, 绘制的文本框水平宽度, 绘制的文本框垂直高度, 文字大小, 字体风格)
        application.getDTPrinter().drawText(accountName, 0, 1.5, 70, 5, 3.5);

        application.getDTPrinter().setItemHorizontalAlignment(LPAPI.ItemAlignment.LEFT);
        StringBuilder sb1 = new StringBuilder();
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));
        double tHeight = 7 + Math.min(fieldsArray.length, 4) * 4;
        application.getDTPrinter().drawLine(20.5, 7, 20.5, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 7, 68, 7, 0.2);
        application.getDTPrinter().drawLine(68, 7, 68, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 11, 68, 11, 0.2);
        application.getDTPrinter().drawLine(20.5, 15, 68, 15, 0.2);
        application.getDTPrinter().drawLine(20.5, 19, 68, 19, 0.2);
        application.getDTPrinter().drawLine(20.5, 23, 68, 23, 0.2);
        application.getDTPrinter().drawLine(36, 7, 36, 23, 0.2);
        application.getDTPrinter().drawText("条码编号", 21.5, 7.5, 15, 3.9, 3);
        application.getDTPrinter().drawText(assets.getBARCODEID(), 37, 7.5, 23, 3.9, 3);
        for (int i = 0; i < fieldsArray.length; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])){
                continue;
            }
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
                    String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                    sb1.append(printLine);
                    if (i < 3) {
                        application.getDTPrinter().drawText(printfieldsTitle[ci], 21.5, 7.5 + (i + 1) * 3.9, 15, 3.9, 3);
                        application.getDTPrinter().drawText(String.valueOf(fieldValue), 37, 7.5 + (i + 1) * 3.9, 23, 3.9, 3);
                    }
                    //application.getDTPrinter().drawLine(21, 6 + i * 4, 42, 1, 1);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        //application.getDTPrinter().drawLine(43.5, 6, 1, Math.min(fieldsArray.length, 4) * 4, 1);
        //application.getDTPrinter().drawLine(68, 6, 1, Math.min(fieldsArray.length, 4) * 4, 1);
        //application.getDTPrinter().drawLine(25, 6 + Math.min(fieldsArray.length, 4) * 4, 42, 1, 1);

        /*application.getDTPrinter().drawLine(25, 6, 1, Math.min(fieldsArray.length, 4) * 4, 1);
        for (int i = 0; i < fieldsArray.length && i < 4; i++) {
            if (TextUtils.isEmpty(fieldsArray[i])) continue;
            int ci = Arrays.asList(printfieldsCode).indexOf(fieldsArray[i]);
            if (ci != -1) {
                try {
                    Field field = assets.getClass().getDeclaredField(fieldsArray[i]);
                    field.setAccessible(true);//允许访问私有字段
                    String printLine = printfieldsTitle[ci].concat("：").concat(String.valueOf(field.get(assets))).concat("\n");
                    sb1.append(printLine);
                    application.getDTPrinter().drawText(printLine, 27, 8 + i * 4, 40, 4, 3);
                    application.getDTPrinter().drawLine(25, 6 + i * 4, 42, 1, 1);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        application.getDTPrinter().drawLine(25, 6 + Math.min(fieldsArray.length, 4) * 4, 42, 1, 1);*/
        String barCode = sb1.toString();
        application.getDTPrinter().draw2DQRCode(barCode, 2, 7, 16);
        // 结束绘图任务提交打印
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
        sb1.insert(0, "条码编号：".concat(assets.getBARCODEID()).concat("\n"));
        double tHeight = 7 + Math.min(fieldsArray.length, 4) * 4;
        application.getDTPrinter().drawLine(20.5, 7, 20.5, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 7, 68, 7, 0.2);
        application.getDTPrinter().drawLine(68, 7, 68, 23, 0.2);
        application.getDTPrinter().drawLine(20.5, 11, 68, 11, 0.2);
        application.getDTPrinter().drawLine(20.5, 15, 68, 15, 0.2);
        application.getDTPrinter().drawLine(20.5, 19, 68, 19, 0.2);
        application.getDTPrinter().drawLine(20.5, 23, 68, 23, 0.2);
        application.getDTPrinter().drawLine(36, 7, 36, 23, 0.2);
        application.getDTPrinter().drawText("条码编号", 21.5, 7.5, 15, 3.9, 3);
        application.getDTPrinter().drawText(assets.getBARCODEID(), 37, 7.5, 23, 3.9, 3);
        for (int i = 0; i < fieldsArray.length; i++) {
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
                    String printLine = printfieldsTitle[ci].concat("：").concat(fieldValue).concat("\n");
                    sb1.append(printLine);
                    if (i < 3) {
                        application.getDTPrinter().drawText(printfieldsTitle[ci], 21.5, 7.5 + (i + 1) * 3.9, 15, 3.9, 3);
                        application.getDTPrinter().drawText(String.valueOf(fieldValue), 37, 7.5 + (i + 1) * 3.9, 23, 3.9, 3);
                    }
                    //application.getDTPrinter().drawLine(21, 6 + i * 4, 42, 1, 1);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        String barCode = String.format("%1$squeryAssetInfo.do?STATUS=EDITQUERYASSET&ACCTSUITEID=%2$s&BARCODE=%3$s", SysDefine.SERVERHOST, assets.getACCTSUITEID(), assets.getBARCODEID());
        application.getDTPrinter().draw2DQRCode(barCode, 3, 7, 16);
        // 结束绘图任务提交打印
        return application.getDTPrinter().commitJob();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
