package com.gsoft.inventory.utils;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.io.File;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author 10904
 */
public class SysConfig {

    public static SysDefine.RuntimeModel runtimeModel = SysDefine.RuntimeModel.Online;

    public static String rootDirectory = "";
    /**
     * 导入导出文件夹
     */
    public static String fileDirectory = Environment.getExternalStorageDirectory().getPath() + "/inventory/files/";
    /**
     * 照片文件夹
     */
    public static String photoDirectory = Environment.getExternalStorageDirectory().getPath() + "/inventory/photos/";
    /**
     * 照片文件夹
     */
    public static String photo_temp_file = Environment.getExternalStorageDirectory().getPath() + "/inventory/files/temp.jpg";

    public static final String PHOTO_TEMP_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp/";
    /**
     * 照片文件夹
     */
    public static String apk_temp_file = Environment.getExternalStorageDirectory().getPath() + "/inventory/files/inventory.apk";

    /**
     * 检查SD卡目录
     */
    public static void checkSoftStorage() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {// 判断是否存在SD卡
            File file = new File(fileDirectory);
            if (!file.exists()) {
                file.mkdirs();
            }
            File filePhoto = new File(photoDirectory);
            if (!filePhoto.exists()) {
                filePhoto.mkdirs();
            }
        }
    }

    public static String getExportFilePath(String fileDirPath) {
        File dir = new File(fileDirPath);
        if (!dir.exists()){
            // 确保目录存在
            dir.mkdirs();
        }
        String datePart = DataConvert.FormatDate2Day();
        int maxNumber = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            String regex = "export_" + datePart + "_([0-9]+)\\.txt$";
            for (File file : files) {
                if (file.getName().matches(regex)) {
                    String numStr = file.getName().replaceAll(".*_(\\d+)\\.txt", "$1");
                    int num = Integer.parseInt(numStr);
                    if (num > maxNumber) {
                        maxNumber = num;
                    }
                }
            }
        }
        // 4位数格式（如0001）
        String number = String.format("%04d", maxNumber + 1);
        String fileName = String.format("export_%s_%s.txt", datePart, number);
        File exportFile = new File(fileDirPath, fileName);
        if (exportFile.exists()){
            exportFile.delete();
        }
        return exportFile.getAbsolutePath();
    }

    public static List<String> userArray;
    static List<Accounts> accountsList = null;
    static List<String> ztList = null;
    static List<String> bmList = null;
    static List<String> dwList = null;

    public static List<Accounts> getAccountList() {
        if (accountsList == null || accountsList.size() == 0) {
            accountsList = Accounts.listAll(Accounts.class, "ZTBH");
        }
        return accountsList;
    }

    public static Accounts getAccountsByZTMC(String ztmc) {
        for (Accounts account : getAccountList()) {
            if (account.getZTMC().equals(ztmc)) {
                return account;
            }
        }
        return null;
    }

    public static List<String> getZTList() {
        if (ztList == null || ztList.size() == 0) {
            ztList = new ArrayList<>();
            for (Accounts account : getAccountList()) {
                if (!ztList.contains(account.getZTMC())) {
                    ztList.add(account.getZTMC());
                }
            }
        }
        return ztList;
    }

    public static List<String> getDWList() {
        if (dwList == null || dwList.size() == 0) {
            dwList = new ArrayList<>();
            for (Accounts account : getAccountList()) {
                if (!dwList.contains(account.getDWMC())) {
                    dwList.add(account.getDWMC());
                }
            }
        }
        return dwList;
    }

    public static List<String> getBMList() {
        if (bmList == null || bmList.size() == 0) {
            bmList = new ArrayList<>();
            for (Accounts account : getAccountList()) {
                if (!bmList.contains(account.getBMMC())) {
                    bmList.add(account.getBMMC());
                }
            }
        }
        return bmList;
    }

    public static String[] assetsFieldsArray = null;

    public static String lastManageUnit = null;

    /**
     * 获取资产自动条码编号
     *
     * @param deviceNo       设备编号
     * @param departCode     部门编号
     * @param assetstypeCode 资产类别编号(01.02)
     * @return
     */
    public static String getNewAssetsBarcode(String deviceNo, String departCode, String assetstypeCode) {
        String[] paramsArray = {assetstypeCode, departCode};
        long iCount = Assets.count(Assets.class, "ASSETSTYPE = ? AND ASSETSDEPT = ?", paramsArray);
        //1+4+2+6  设备编号+部门代码3-6位+分类2位+6位顺序
        String barCode = String.format("%1$s%2$s%3$s%4$06d", deviceNo, departCode.substring(2, 6), assetstypeCode, iCount + 1);
        //String barCode = String.format("%1$s%2$s%3$07d", departCode.substring(0, 4), assetstypeCode, iCount + 1);
        String[] params_Count_Array = {barCode};
        while (Assets.count(Assets.class, "BARCODEID = ?", params_Count_Array) > 0) {
            iCount++;
            barCode = String.format("%1$s%2$s%3$s%4$06d", deviceNo, departCode.substring(2, 6), assetstypeCode, iCount + 1);
            params_Count_Array[0] = barCode;
        }
        return barCode;
    }


    private static long lastTimeValue = Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
    /**
     * 按设备、时间生成序列号
     *
     * @param deviceNo
     * @return
     */
    @SuppressLint("DefaultLocale")
    public static String getNewAssetsBarcodeByDevice(String deviceNo) {
        long currentTimeValue;
        synchronized (SysConfig.class) { currentTimeValue = ++lastTimeValue; }
        // 生成14位时间戳后截取后12位
        String timestampPart = String.format("%014d", currentTimeValue).substring(2);
        String barCode = deviceNo + timestampPart;
        // 检查重复
        while (Assets.count(Assets.class, "BARCODEID = ?", new String[]{barCode}) > 0) {
            synchronized (SysConfig.class) { currentTimeValue = ++lastTimeValue; }
            timestampPart = String.format("%014d", currentTimeValue).substring(2);
            barCode = deviceNo + timestampPart;
        }
        return barCode;
    }


}
