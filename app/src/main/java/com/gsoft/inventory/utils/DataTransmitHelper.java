package com.gsoft.inventory.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.gsoft.inventory.entities.AssetDispose;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.entities.ZCQCY;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.gsoft.inventory.utils.FileSearchHelper.searchType;

/**
 *
 */
public class DataTransmitHelper {
    /**
     * 新增一种资产
     *
     * @param name 资产名称
     * @param code 资产条码
     */
    public static void addAssets(String name, String code) {
        Assets assets = new Assets();
        assets.save();
    }

    /**
     * 查询某账套下的资产明细
     *
     * @param ztCode 账套编号
     * @return
     */
    public static List<Assets> queryAssets(String ztCode) {
        return Assets.findWithQuery(Assets.class, "ACCTSUITEID=?", ztCode);
    }

    public static void onExportAssetsOver() {
        Iterator<Assets> assetsIterator = Assets.findAsIterator(Assets.class, "ISUPLOAD=?", "0");
        while (assetsIterator.hasNext()) {
            Assets assets = assetsIterator.next();
            assets.setISUPLOAD("1");
            assets.save();
        }
    }

    public static String exportAssets(Iterator<Assets> assetsIterator, String saveFilePath) {
        StringBuilder resultMessage = new StringBuilder();
        File exportFile = new File(saveFilePath);

        // 确保文件路径有效
        if (!exportFile.getParentFile().exists() && !exportFile.getParentFile().mkdirs()) {
            return "文件路径无效或无法创建父目录";
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(exportFile, false);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
             BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)) {
            while (assetsIterator.hasNext()) {
                Assets assets = assetsIterator.next();
                bufferedWriter.write(assets.toLineString());
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }  catch (IOException e) {
            resultMessage.append("文件创建或写入失败: ").append(e.getMessage());
        } catch (Exception e) {
            resultMessage.append("导出过程中发生异常: ").append(e.getMessage());
        }
        return resultMessage.toString();
    }

    public static String exportAssets(String saveFilePath, boolean isAll) {
        Iterator<Assets> assetsIterator = null;
        if (isAll) assetsIterator = Assets.findAll(Assets.class);
        else assetsIterator = Assets.findAsIterator(Assets.class, "ISUPLOAD=?", "0");
        return exportAssets(assetsIterator, saveFilePath);
    }

    public static String exportAssets(String saveFilePath) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Assets> assetsIterator = Assets.findAll(Assets.class);
        return exportAssets(assetsIterator, saveFilePath);
    }

    public static void clearAssets() {
        Assets.deleteAll(Assets.class);
    }

    public static void clearPrinters() {
        JCPrinter.deleteAll(JCPrinter.class);
    }

    public static void clearAccounts() {
        Accounts.deleteAll(Accounts.class);
    }

    public static void clearAssetsCategory() {
        AssetsCategory.deleteAll(AssetsCategory.class);
    }

    public static void clearQCY() {
        ZCQCY.deleteAll(ZCQCY.class);
    }

    /**
     * 导入资产信息
     *
     * @param strFilePath
     */
    @NonNull
    public static String importAssets(String strFilePath) {
        File dataFile = new File(strFilePath);
        StringBuilder result = new StringBuilder();
        if (!dataFile.isDirectory() && dataFile.exists()) {
            InputStream instream = null;
            try {
                instream = new FileInputStream(dataFile);
                InputStreamReader inputreader = new InputStreamReader(instream, "utf-8");
                BufferedReader buffreader = new BufferedReader(inputreader);
                String dataLine;
                int lineIndex = 0;
                while ((dataLine = buffreader.readLine()) != null) {
                    long newID = insertAssets(dataLine);
                    if (newID == -1) {
                        result.append("第" + lineIndex + "行导入失败；");
                    }
                    lineIndex++;
                }
            } catch (java.io.FileNotFoundException e) {
                result.append("文件打开失败，未找到该文件：" + strFilePath);
            } catch (IOException e) {
                result.append("文件打开失败，请检查文件是否正确");
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result.toString();
    }

    public static String importAssets(String strFilePath, boolean clearLocal) {
        if (clearLocal) {
            Assets.deleteAll(Assets.class);
        }
        return importAssets(strFilePath);
    }

    public static long insertAssets(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        if (rowStrArray.length > 57) {
            Assets assets = new Assets();
            setAssets(assets, rowStrArray);
            newID = assets.save();
        }
        return newID;
    }

    public static long updateAssets(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        if (rowStrArray.length > 57) {
            // 通过docId去查询数据
            String docId = rowStrArray[49].trim();
            List<Assets> assetsList = Assets.find(Assets.class, "DOCID=?", docId);
            Assets assets = null;
            assets = assetsList.get(0);
            setAssets(assets, rowStrArray);
            newID = assets.save();

        }
        return newID;
    }

    private static void setAssets(Assets assets, String[] rowStrArray) {
        /*条码编号*/
        assets.setBARCODEID(rowStrArray[0].trim());
        /*CardID*/
        assets.setCARDID(rowStrArray[1].trim());
        /*取得方式*/
        assets.setGETMODE(rowStrArray[2].trim());
        /*备注*/
        assets.setREMARKS(rowStrArray[3].trim());
        /*产权单位*/
        assets.setPROPERTY(rowStrArray[4].trim());
        /*发动机号*/
        assets.setFDJH(rowStrArray[5].trim());
        /*所在楼层，空值*/
        assets.setSZLC(rowStrArray[6].trim());
        /*使用(保管)人*/
        assets.setCUSTODIAN(rowStrArray[7].trim());
        /*车架号*/
        assets.setCJHM(rowStrArray[8].trim());
        /*账套编号*/
        assets.setACCTSUITEID(rowStrArray[9].trim());
        /*卡片序号*/
        assets.setSERIALNUM(rowStrArray[10].trim());
        /*资产类别*/
        assets.setASSETSTYPE(rowStrArray[11].trim());
        /*资产分类代码*/
        assets.setASSETSSORTCODE(rowStrArray[12].trim());
        /*计量单位*/
        assets.setASSETSMEASURE(rowStrArray[13].trim());
        /*卡片编号*/
        assets.setSERIALCODE(rowStrArray[14].trim());
        /*资产名称*/
        assets.setASSETSNAME(rowStrArray[15].trim());
        /*规格型号*/
        assets.setASSETSSTANDARD(rowStrArray[16].trim());
        /*取得日期*/
        assets.setASSETSUSEDATE(rowStrArray[17].trim());
        /*保修截止日期*/
        assets.setASSETSENDUSEDATE(rowStrArray[18].trim());
        /*预计使用月份*/
        assets.setYEARCOUNT(rowStrArray[19].trim());
        /*使用状况*/
        assets.setUSESTATE(rowStrArray[20].trim());
        /*折旧状态*/
        assets.setDPRECIATIONTYPE(rowStrArray[21].trim());
        /*存放地点*/
        assets.setASSETSLAYADD(rowStrArray[22].trim());
        /*使用人*/
        assets.setASSETSUSER(rowStrArray[23].trim());
        /*使用部门*/
        assets.setASSETSDEPT(rowStrArray[24].trim());
        /*卡片金额-原值，设为空值*/
        assets.setKPJE(rowStrArray[25].trim());
        /*累计折旧*/
        assets.setDPRECIATIONADD(rowStrArray[26].trim());
        /*资产原值*/
        assets.setASSETSCURRPRICE(rowStrArray[27].trim());
        /*当前累计折旧*/
        assets.setCURRDPRECIATIONADD(rowStrArray[28].trim());
        /*净值*/
        assets.setLEFTPRICE(rowStrArray[29].trim());
        /*管理部门*/
        assets.setGROUNDMANAGEDEPT(rowStrArray[30].trim());
        /*文物等级*/
        assets.setASSETSCULGRADE(rowStrArray[31].trim());
        /*车辆产地*/
        assets.setASSETSPRODAREA(rowStrArray[32].trim());
        /*车牌号*/
        assets.setVEHICLENO(rowStrArray[33].trim());
        /*发证时间*/
        assets.setASSETSDISPENSEDATE(rowStrArray[34].trim());
        /*权属所有人*/
        assets.setASSETSMANAGER(rowStrArray[35].trim());
        /*厂牌型号*/
        assets.setASSETSFACTORYNO(rowStrArray[36].trim());
        /*排气量*/
        assets.setVEHEXH(rowStrArray[37].trim());
        /*行驶里程*/
        assets.setLONGMILES(rowStrArray[38].trim());
        /*分类用途*/
        assets.setVEHPURPOSE(rowStrArray[39].trim());
        /*编制情况*/
        assets.setASSETSORGANIZATION(rowStrArray[40].trim());
        /*数量*/
        try {
            assets.setASSETSNUM(Integer.parseInt(rowStrArray[41].trim()));
        } catch (NumberFormatException e) {
            assets.setASSETSNUM(0);
        }

        /*录入人*/
        assets.setASSETSWRITER(rowStrArray[42].trim());
        /*录入日期*/
        assets.setASSETSWRITERDATE(rowStrArray[43].trim());
        /*采购组织形式*/
        assets.setPURCHASINGORGANIZATION(rowStrArray[44].trim());
        /*品牌*/
        assets.setBRAND(rowStrArray[45].trim());
            /*盘点表2此字段对应“盘点情况”的值，该字段为字典数据，该字段的值有5中类型：
            空值、01：待盘点、02：盘实、03：盘盈、04：盘亏，导入模板中只显示：空值、01、02、03、04等内容*/
        assets.setPDZT(rowStrArray[46].trim());
        /*盘点表1*/
        assets.setZT(rowStrArray[47].trim());
        /*是否财政资产*/
        assets.setISCZ(rowStrArray[48].trim());
        assets.setISUPLOAD("1");
        /**
         * DOCID资产主键编号，唯一标拾
         */
        assets.setDOCID(rowStrArray[49].trim());
        assets.setASSETSMODEL(rowStrArray[50].trim());
        /**
         * 23-11-25号追加10个字段
         */
        assets.setASSETSSERIALNUMBER(rowStrArray[51].trim());
        assets.setASSETSSUPPLIER(rowStrArray[52].trim());
        assets.setFACTORYNO(rowStrArray[53].trim());
        assets.setMANAGENO(rowStrArray[54].trim());
        assets.setPARAMETERS(rowStrArray[55].trim());
        assets.setTRACEABILITY(rowStrArray[56].trim());
        assets.setTRACEABILITYPE(rowStrArray[57].trim());
        assets.setCALIBRATIONDATE(rowStrArray[58].trim());
        assets.setVALIDDATE(rowStrArray[59].trim());
        assets.setPROBELONG(rowStrArray[60].trim());
        assets.setCLASSIFICATION(rowStrArray[61].trim());
        assets.setISSUEDATE(rowStrArray[62].trim());
        try{
            assets.setBOOKKEEPINGTIME(rowStrArray[63].trim());
        }catch (Exception e){
            assets.setBOOKKEEPINGTIME("");
        }
        try {
            assets.setVOUCHERNO(rowStrArray[64].trim());
        }catch (Exception e){
            assets.setVOUCHERNO("");
        }
        try {
            assets.setVENDOR(rowStrArray[65].trim());
        }catch (Exception e){
            assets.setVOUCHERNO("");
        }

    }

    /**
     * 导入部门、人员信息
     *
     * @param strFilePath
     */
    @NonNull
    public static String importAccounts(String strFilePath) {
        ArrayList dataList = new ArrayList<String>();
        File dataFile = new File(strFilePath);
        StringBuilder stringBuilder = new StringBuilder();
        if (!dataFile.isDirectory() && dataFile.exists()) {
            InputStream instream = null;
            try {
                instream = new FileInputStream(dataFile);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream, "utf-8");
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String dataLine;
                    int lineIndex = 0;
                    while ((dataLine = buffreader.readLine()) != null) {
                        long newID = insertAccounts(dataLine);
                        if (newID == -1) {
                            stringBuilder.append("第" + lineIndex + "行导入失败；");
                        }
                        lineIndex++;
                    }
                    instream.close();
                }
            } catch (java.io.FileNotFoundException e) {
                stringBuilder.append("文件打开失败，未找到该文件：" + strFilePath);
            } catch (IOException e) {
                stringBuilder.append("文件打开失败，请检查文件是否正确");
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    public static long insertZCQCY(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        if (rowStrArray.length > 1) {
            ZCQCY zcqcy = new ZCQCY();
            /** 账套编号*/
            zcqcy.CODE = rowStrArray[0];
            zcqcy.NAME = rowStrArray[1];
            zcqcy.userType = rowStrArray[2];
            newID = zcqcy.save();
        }
        return newID;
    }

    public static long insertAssetDispose(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        if (rowStrArray.length > 3) {
            AssetDispose dispose = new AssetDispose();
            /** 账套编号*/
            dispose.setCode(rowStrArray[0]);
            dispose.setName(rowStrArray[1]);
            dispose.setDate(rowStrArray[2]);
            dispose.setDepart(rowStrArray[3]);
            dispose.setRemark(rowStrArray[4]);
            newID = dispose.save();
        }
        return newID;
    }

    public static AssetDispose addAssetDispose(String dataLine) {
        AssetDispose dispose = null;
        String[] rowStrArray = dataLine.split("\\|", -1);
        if (rowStrArray.length > 3) {
            dispose = new AssetDispose();
            /** 账套编号*/
            dispose.setCode(rowStrArray[0]);
            dispose.setName(rowStrArray[1]);
            dispose.setDate(rowStrArray[2]);
            dispose.setDepart(rowStrArray[3]);
            dispose.setRemark(rowStrArray[4]);
            dispose.save();
        }
        return dispose;
    }

    public static DisposeAsset addDisposeAsset(String dataLine) {
        DisposeAsset dispose = null;
        String[] rowStrArray = dataLine.split("\\|", -1);
        if (rowStrArray.length > 5) {
            dispose = new DisposeAsset();
            /** 账套编号*/
            dispose.setCode(rowStrArray[0]);
            dispose.setName(rowStrArray[1]);
            dispose.setSerial(rowStrArray[2]);
            dispose.setSpecification(rowStrArray[3]);
            dispose.setOwner(rowStrArray[4]);
            dispose.setPlace(rowStrArray[5]);
            dispose.setPrice(rowStrArray[6]);
            dispose.setStatus(0);
            dispose.save();
        }
        return dispose;
    }

    public static long insertAccounts(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        if (rowStrArray.length > 8) {
            Accounts account = new Accounts();
            /** 账套编号*/
            account.setZTBH(rowStrArray[0].trim());
            /*** 账套名称*/
            account.setZTMC(rowStrArray[1].trim());
            /*** 单位编号*/
            account.setDWBH(rowStrArray[2].trim());
            /*** 单位名称*/
            account.setDWMC(rowStrArray[3].trim());
            /*** 部门编号*/
            account.setBMBH(rowStrArray[4].trim());
            /*** 部门名称*/
            account.setBMMC(rowStrArray[5].trim());
            /*** 用户编号*/
            account.setYHBH(rowStrArray[6].trim());
            /*** 用户姓名*/
            account.setYHXM(rowStrArray[7].trim());
            /*** 密码*/
            account.setDLMM(rowStrArray[8].trim());
            newID = account.save();
        }
        return newID;
    }

    /**
     * 导入打印设备信息
     *
     * @param strFilePath
     */
    @NonNull
    public static String importPrinters(String strFilePath) {
        File dataFile = new File(strFilePath);
        StringBuilder stringBuilder = new StringBuilder();
        if (!dataFile.isDirectory() && dataFile.exists()) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(dataFile);
                InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader buffReader = new BufferedReader(inputReader);
                String dataLine;
                int lineIndex = 0;
                while ((dataLine = buffReader.readLine()) != null) {
                    long newID = insertPrinters(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("第".concat(String.valueOf(lineIndex)).concat("行导入失败；"));
                    }
                    lineIndex++;
                }

            } catch (java.io.FileNotFoundException e) {
                stringBuilder.append("文件打开失败，未找到该文件：".concat(strFilePath));
            } catch (IOException e) {
                stringBuilder.append("文件打开失败，请检查文件是否正确");
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 导入资产分类信息
     *
     * @param strFilePath
     */
    @NonNull
    public static String importAssetsCategory(String strFilePath) {
        ArrayList dataList = new ArrayList<String>();
        File dataFile = new File(strFilePath);
        StringBuilder stringBuilder = new StringBuilder();
        if (!dataFile.isDirectory() && dataFile.exists()) {
            InputStream instream = null;
            try {
                instream = new FileInputStream(dataFile);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream, "utf-8");
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String dataLine;
                    int lineIndex = 0;
                    while ((dataLine = buffreader.readLine()) != null) {
                        long newID = insertAssetsCategory(dataLine);
                        if (newID == -1) {
                            stringBuilder.append("第" + lineIndex + "行导入失败；");
                        }
                        lineIndex++;
                    }
                    instream.close();
                }
            } catch (java.io.FileNotFoundException e) {
                stringBuilder.append("文件打开失败，未找到该文件：" + strFilePath);
            } catch (IOException e) {
                stringBuilder.append("文件打开失败，请检查文件是否正确");
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    public static long insertAssetsCategory(String dataLine) {
        String[] rowStrArray = dataLine.split("\\|", -1);
        long newID = -1;
        try {
            if (rowStrArray.length > 3) {

                AssetsCategory category = new AssetsCategory();

                // 如果是字母开头的编号，说明
                String code = rowStrArray[0];
                if (code != null && !code.isEmpty() && Character.isLetter(code.charAt(0))) {
                    // 第一个字符是字母
                    category.setNewOrOld(1);
                } else {
                    // 第一个字符不是字母
                    category.setNewOrOld(0);
                }

                category.setCODE(code);
                category.setNAME(rowStrArray[1]);
                category.setLEVEL(Integer.parseInt(rowStrArray[2]));
                category.setISSHOW(rowStrArray[3].equals("1"));
                if (rowStrArray.length > 4) {
                    category.setDEFUNIT(rowStrArray[4]);
                } else {
                    category.setDEFUNIT("");
                }

                newID = category.save();
            }
        } catch (Exception ex) {
            return -1;
        }
        return newID;
    }

    @NonNull
    public static String importAssetQCY(String strFilePath) {
        File dataFile = new File(strFilePath);
        StringBuilder stringBuilder = new StringBuilder();
        if (!dataFile.isDirectory() && dataFile.exists()) {
            InputStream instream = null;
            try {
                instream = new FileInputStream(dataFile);
                InputStreamReader inputreader = new InputStreamReader(instream, "utf-8");
                BufferedReader buffreader = new BufferedReader(inputreader);
                String dataLine;
                int lineIndex = 0;
                while ((dataLine = buffreader.readLine()) != null) {
                    long newID = insertZCQCY(dataLine);
                    if (newID == -1) {
                        stringBuilder.append("第" + lineIndex + "行导入失败；");
                    }
                    lineIndex++;
                }
                instream.close();
            } catch (java.io.FileNotFoundException e) {
                stringBuilder.append("文件打开失败，未找到该文件：" + strFilePath);
            } catch (IOException e) {
                stringBuilder.append("文件打开失败，请检查文件是否正确");
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    public static long insertPrinters(String dataLine) {
        long newID = -1;
        try {
            JCPrinter printer = new JCPrinter();
            printer.encryno = dataLine;
            printer.printerno = "B50-";
            newID = printer.save();
        } catch (Exception ex) {
            newID = -1;
            Log.e("SAVE PRINTER", ex.getMessage());
        }
        return newID;
    }


    public static List<FileBean> searchImportFiles(Context context) {
        List<FileBean> fileBeans = FileSearchHelper.searchType(context, "txt");
        return fileBeans;
    }

    public static SignaturePerson transferSignaturePerson(String line) {
        String[] vals = line.split("\\|", -1);
        if (vals.length < 3) return null;
        SignaturePerson person = new SignaturePerson();
        person.setDepart(vals[0]);
        person.setName(vals[1]);
        person.setTelephone(vals[2]);
        if (vals.length > 3) person.setStatus(vals[3]);
        return person;
    }
}
