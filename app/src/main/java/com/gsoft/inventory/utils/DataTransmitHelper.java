package com.gsoft.inventory.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.entities.AssetDispose;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.entities.ZCQCY;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.domain.AssetsCategory;

import java.io.*;
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
        Iterator<Assets> assetsIterator = Assets.findAsIterator(Assets.class, "ISUPLOAD=?", Const.NOT_UPLOAD);
        while (assetsIterator.hasNext()) {
            Assets assets = assetsIterator.next();
            assets.setISUPLOAD(Const.UPLOADED);
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
        else assetsIterator = Assets.findAsIterator(Assets.class, "ISUPLOAD=?", Const.NOT_UPLOAD);
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
            } catch (FileNotFoundException e) {
                result.append("文件打开失败，未找到该文件：" + strFilePath);
            } catch (IOException e) {
                result.append("文件打开失败，请检查文件是否正确");
            }catch (Exception e){
                e.printStackTrace();
                Log.e("importAssets_error: ", e.getMessage());
                result.append("导入失败，请联系管理员查看");
            }
            finally {
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
        assets.setBARCODEID(safeGet(rowStrArray, 0));
        /*CardID*/
        assets.setCARDID(safeGet(rowStrArray, 1));
        /*取得方式*/
        assets.setGETMODE(safeGet(rowStrArray, 2));
        /*备注*/
        assets.setREMARKS(safeGet(rowStrArray, 3));
        /*产权单位*/
        assets.setPROPERTY(safeGet(rowStrArray, 4));
        /*发动机号*/
        assets.setFDJH(safeGet(rowStrArray, 5));
        /*所在楼层，空值*/
        assets.setSZLC(safeGet(rowStrArray, 6));
        /*使用(保管)人*/
        assets.setCUSTODIAN(safeGet(rowStrArray, 7));
        /*车架号*/
        assets.setCJHM(safeGet(rowStrArray, 8));
        /*账套编号*/
        assets.setACCTSUITEID(safeGet(rowStrArray, 9));
        /*卡片序号*/
        assets.setSERIALNUM(safeGet(rowStrArray, 10));
        /*资产类别*/
        assets.setASSETSTYPE(safeGet(rowStrArray, 11));
        /*资产分类代码*/
        assets.setASSETSSORTCODE(safeGet(rowStrArray, 12));
        /*计量单位*/
        assets.setASSETSMEASURE(safeGet(rowStrArray, 13));
        /*资产编号*/
        assets.setSERIALCODE(safeGet(rowStrArray, 14));
        /*资产名称*/
        assets.setASSETSNAME(safeGet(rowStrArray, 15));
        /*规格型号*/
        assets.setASSETSSTANDARD(safeGet(rowStrArray, 16));
        /*取得日期*/
        assets.setASSETSUSEDATE(safeGet(rowStrArray, 17));
        /*保修截止日期*/
        assets.setASSETSENDUSEDATE(safeGet(rowStrArray, 18));
        /*预计使用月份*/
        assets.setYEARCOUNT(safeGet(rowStrArray, 19));
        /*使用状况*/
        assets.setUSESTATE(safeGet(rowStrArray, 20));
        /*折旧状态*/
        assets.setDPRECIATIONTYPE(safeGet(rowStrArray, 21));
        /*存放地点*/
        assets.setASSETSLAYADD(safeGet(rowStrArray, 22));
        /*使用人*/
        assets.setASSETSUSER(safeGet(rowStrArray, 23));
        /*使用部门*/
        assets.setASSETSDEPT(safeGet(rowStrArray, 24));
        /*卡片金额-原值，设为空值*/
        assets.setKPJE(safeGet(rowStrArray, 25));
        /*累计折旧*/
        assets.setDPRECIATIONADD(safeGet(rowStrArray, 26));
        /*资产原值*/
        assets.setASSETSCURRPRICE(safeGet(rowStrArray, 27));
        /*当前累计折旧*/
        assets.setCURRDPRECIATIONADD(safeGet(rowStrArray, 28));
        /*净值*/
        assets.setLEFTPRICE(safeGet(rowStrArray, 29));
        /*管理部门*/
        assets.setGROUNDMANAGEDEPT(safeGet(rowStrArray, 30));
        /*文物等级*/
        assets.setASSETSCULGRADE(safeGet(rowStrArray, 31));
        /*车辆产地*/
        assets.setASSETSPRODAREA(safeGet(rowStrArray, 32));
        /*车牌号*/
        assets.setVEHICLENO(safeGet(rowStrArray, 33));
        /*发证时间*/
        assets.setASSETSDISPENSEDATE(safeGet(rowStrArray, 34));
        /*权属所有人*/
        assets.setASSETSMANAGER(safeGet(rowStrArray, 35));
        /*厂牌型号*/
        assets.setASSETSFACTORYNO(safeGet(rowStrArray, 36));
        /*排气量*/
        assets.setVEHEXH(safeGet(rowStrArray, 37));
        /*行驶里程*/
        assets.setLONGMILES(safeGet(rowStrArray, 38));
        /*分类用途*/
        assets.setVEHPURPOSE(safeGet(rowStrArray, 39));
        /*编制情况*/
        assets.setASSETSORGANIZATION(safeGet(rowStrArray, 40));
        /*数量*/
        try {
            assets.setASSETSNUM(Integer.parseInt(safeGet(rowStrArray, 41)));
        } catch (NumberFormatException e) {
            assets.setASSETSNUM(0);
        }

        /*录入人*/
        assets.setASSETSWRITER(safeGet(rowStrArray, 42));
        /*录入日期*/
        assets.setASSETSWRITERDATE(safeGet(rowStrArray, 43));
        /*采购组织形式*/
        assets.setPURCHASINGORGANIZATION(safeGet(rowStrArray, 44));
        /*品牌*/
        assets.setBRAND(safeGet(rowStrArray, 45));
        assets.setEMPTY1(safeGet(rowStrArray, 46));
        assets.setEMPTY2(safeGet(rowStrArray, 47));

        assets.setPDZT(safeGet(rowStrArray, 48));
        assets.setZT(safeGet(rowStrArray, 49));

        assets.setISUPLOAD(safeGet(rowStrArray, 50));
        /*是否财政资产*/
        assets.setISCZ(safeGet(rowStrArray, 51));
        /**
         * DOCID资产主键编号，唯一标拾
         */
        assets.setDOCID(safeGet(rowStrArray, 52));
        assets.setASSETSMODEL(safeGet(rowStrArray, 53));
        /**
         * 23-11-25号追加10个字段
         */
        assets.setASSETSSERIALNUMBER(safeGet(rowStrArray, 54));
        assets.setASSETSSUPPLIER(safeGet(rowStrArray, 55));
        assets.setFACTORYNO(safeGet(rowStrArray, 56));
        assets.setMANAGENO(safeGet(rowStrArray, 57));
        assets.setPARAMETERS(safeGet(rowStrArray, 58));
        assets.setTRACEABILITY(safeGet(rowStrArray, 59));
        assets.setTRACEABILITYPE(safeGet(rowStrArray, 60));
        assets.setCALIBRATIONDATE(safeGet(rowStrArray, 61));
        assets.setVALIDDATE(safeGet(rowStrArray, 62));
        assets.setPROBELONG(safeGet(rowStrArray, 63));
        /*63-72：入账单号/财务记账日期/凭证号/项目名称/会计账簿/下发情况/使用年限/已计提折旧月数/月折旧额/设备分类*/
        assets.setPRINTNO(safeGet(rowStrArray, 64));
        assets.setBOOKKEEPINGTIME(safeGet(rowStrArray, 65));
        assets.setVOUCHERNO(safeGet(rowStrArray, 66));
        assets.setPRO(safeGet(rowStrArray, 67));
        assets.setACCOUNTBOOK(safeGet(rowStrArray, 68));
        assets.setDOWNINFO(safeGet(rowStrArray, 69));
        assets.setSUMCOMPLETEMONTH(safeGet(rowStrArray, 70));
        assets.setCOMPLETEMONTH(safeGet(rowStrArray, 71));
        assets.setMONDEPREVALUE(safeGet(rowStrArray, 72));
        assets.setDEVICETYPE(safeGet(rowStrArray, 73));
        assets.setCLASSIFICATION(safeGet(rowStrArray, 74));
        assets.setISSUEDATE(safeGet(rowStrArray, 75));
        assets.setVENDOR(safeGet(rowStrArray, 76));

        assets.setDWCODE(safeGet(rowStrArray, 77));
        assets.setSAMPLDATE(safeGet(rowStrArray, 78));
        assets.setSAMPLCODE(safeGet(rowStrArray, 79));
        assets.setMONITORPRO(safeGet(rowStrArray, 80));
        assets.setSAVEMETHOD(safeGet(rowStrArray, 81));
        assets.setSAMPLESTATE(safeGet(rowStrArray, 82));
        assets.setSAMPLEUSER(safeGet(rowStrArray, 83));

        assets.setHARDID(safeGet(rowStrArray, 84));
        assets.setHARDSN(safeGet(rowStrArray, 85));
        assets.setIP(safeGet(rowStrArray, 86));
        assets.setMAC(safeGet(rowStrArray, 87));
        assets.setINTERNETID(safeGet(rowStrArray, 88));
        assets.setPORT(safeGet(rowStrArray, 89));
        assets.setPURPOSE(safeGet(rowStrArray, 90));
        assets.setINSTALLDATE(safeGet(rowStrArray, 91));
        assets.setOS(safeGet(rowStrArray, 92));

        assets.setCONCENTRATION(safeGet(rowStrArray, 93));
        assets.setPREPARER(safeGet(rowStrArray, 94));
        assets.setMEDIA(safeGet(rowStrArray, 95));
        assets.setSTORAGENEED(safeGet(rowStrArray, 96));

        assets.setNETTYPE(safeGet(rowStrArray, 97));

        /*是否公物仓、所属处室：兼容本地导出文件（98字段）与服务器导入数据（99字段）*/
        assets.setISGWC(safeGet(rowStrArray, 98));
        assets.setDEPARTMENT(safeGet(rowStrArray, 99));

        /*101-106：资产位置/出厂日期/特种设备/设备注册代码/特种设备出厂编号/使用登记证编号*/
        assets.setGROUNDSETPOS(safeGet(rowStrArray, 100));
        assets.setFACTORYDATE(safeGet(rowStrArray, 101));
        assets.setSPECIALDEVICE(safeGet(rowStrArray, 102));
        assets.setDEVICECODE(safeGet(rowStrArray, 103));
        assets.setSPECIALFACTORYNO(safeGet(rowStrArray, 104));
        assets.setCERTIFICATENUMBER(safeGet(rowStrArray, 105));
        assets.setISPRINTBARCODE(safeGet(rowStrArray, 106));
        /*是否国产设备：01是 02否，默认空；旧文件无此列时safeGet返回空*/
        assets.setMADEINCHINA(safeGet(rowStrArray, 107));

    }


    private static String safeGet(String[] arr, int index) {
        if (index < 0 || index >= arr.length) {
            return "";
        }
        String value = arr[index];
        return value != null ? value.trim() : "";
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
            /*** 手机号*/
            try{
                account.setPHONE(rowStrArray[9].trim());
            }catch (Exception e){
                account.setPHONE("");
            }
            /*** 角色*/
            try{
                account.setROLE(rowStrArray[10].trim());
            }catch (Exception e){
                account.setROLE("");
            }
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
//        List<FileBean> fileBeans = FileSearchHelper.searchType(context, "txt");
        return FileSearchHelper.searchFilesInZrDirectory(context, "txt");
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
