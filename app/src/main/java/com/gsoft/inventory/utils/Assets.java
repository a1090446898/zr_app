package com.gsoft.inventory.utils;

import com.orm.SugarRecord;

import java.io.Serializable;
import java.lang.reflect.Field;

/*
资产信息
 */
public class Assets extends SugarRecord implements Serializable {
    /*条码编号*/
    private String BARCODEID;
    /*CardID*/
    private String CARDID;
    /*取得方式*/
    private String GETMODE;
    /*备注*/
    private String REMARKS;
    /*产权单位--名称*/
    private String PROPERTY;
    /*发动机号*/
    private String FDJH;
    /*所在楼层，空值*/
    private String SZLC;
    /*使用(保管)人*/
    private String CUSTODIAN;
    /*车架号*/
    private String CJHM;
    /*账套编号*/
    private String ACCTSUITEID;
    /*卡片序号*/
    private String SERIALNUM;
    /*资产类别*/
    private String ASSETSTYPE;
    /*资产分类代码*/
    private String ASSETSSORTCODE;
    /*计量单位*/
    private String ASSETSMEASURE;
    /*卡片编号*/
    private String SERIALCODE;
    /*资产名称*/
    private String ASSETSNAME;
    /*型号*/
    private String ASSETSSTANDARD;
    /*取得日期*/
    private String ASSETSUSEDATE;
    /*保修截止日期*/
    private String ASSETSENDUSEDATE;
    /*预计使用月份*/
    private String YEARCOUNT;
    /*使用状况*/
    private String USESTATE;
    /*折旧状态*/
    private String DPRECIATIONTYPE;
    /*存放地点*/
    private String ASSETSLAYADD;
    /*使用人*/
    private String ASSETSUSER;
    /*使用部门，名称*/
    private String ASSETSDEPT;
    /*卡片金额-原值，设为空值*/
    private String KPJE;
    /*累计折旧*/
    private String DPRECIATIONADD;
    /*资产原值*/
    private String ASSETSCURRPRICE;
    /*当前累计折旧*/
    private String CURRDPRECIATIONADD;
    /*净值*/
    private String LEFTPRICE;
    /*管理部门*/
    private String GROUNDMANAGEDEPT;
    /*文物等级*/
    private String ASSETSCULGRADE;
    /*车辆产地*/
    private String ASSETSPRODAREA;
    /*车牌号*/
    private String VEHICLENO;
    /*发证时间*/
    private String ASSETSDISPENSEDATE;
    /*权属所有人*/
    private String ASSETSMANAGER;
    /*厂牌型号*/
    private String ASSETSFACTORYNO;
    /*排气量*/
    private String VEHEXH;
    /*行驶里程*/
    private String LONGMILES;
    /*分类用途*/
    private String VEHPURPOSE;
    /*编制情况*/
    private String ASSETSORGANIZATION;
    /*数量*/
    private int ASSETSNUM;
    /*录入人*/
    private String ASSETSWRITER;
    /*录入日期*/
    private String ASSETSWRITERDATE;
    /*采购组织形式*/
    private String PURCHASINGORGANIZATION;
    /*品牌*/
    private String BRAND;
    /*盘点表1*/
    private String PDZT;
    /*盘点表2*/
    private String ZT;

    /*是否修改
    private String ISMODIFY = "0";*/

    /*是否上传*/
    private String ISUPLOAD = "0";

    /*是否财政资产*/
    private String ISCZ = "01";

    private String DOCID;

    /**
     * 规格
     */
    private String ASSETSMODEL;

    //序列号//生产厂家//出厂编号//管理编号//参数//溯源周期//溯源方式01：检定/02：校准//检定校准日期YYYY-MM-DD/有效期至YYYY-MM-DD//所属项目
    /**
     * 序列号
     */
    private String ASSETSSERIALNUMBER;
    /**
     * 生产厂家
     */
    private String ASSETSSUPPLIER;
    /**
     * 出厂编号
     */
    private String FACTORYNO;
    /**
     * 管理编号
     */
    private String MANAGENO;
    /**
     * 参数
     */
    private String PARAMETERS;
    /**
     * 溯源周期
     */
    private String TRACEABILITY;
    /**
     * 溯源方式01：检定/02：校准
     */
    private String TRACEABILITYPE;
    /**
     * 检定校准日期YYYY-MM-DD
     */
    private String CALIBRATIONDATE;
    /**
     * 有效期至YYYY-MM-DD
     */
    private String VALIDDATE;
    /**
     * 所属项目
     */
    private String PROBELONG;

    /**
     * 字典数据：01/绝密，02/机密，03/秘密，无默认值
     */
    private String CLASSIFICATION;
    /**
     * 配发日期  YYYY-MM-DD
     */

    private String ISSUEDATE;

    /**
     * 0是不需要复制图片，1是需要复制图片
     */
    private Integer ISUPIMG;

    private String COPY_DOC_ID;

    /**
     *财务记账时间
     */
    private String BOOKKEEPINGTIME;

    /**
     *凭证号
     */
    private String VOUCHERNO;

    /**
     * 供货商
     */
    private String VENDOR;


    public String getISCZ() {
        return ISCZ;
    }

    public void setISCZ(String ISCZ) {
        this.ISCZ = ISCZ;
    }


    public String getDOCID() {
        return DOCID;
    }

    public void setDOCID(String DOCID) {
        this.DOCID = DOCID;
    }

    public String getBARCODEID() {
        return BARCODEID;
    }

    public void setBARCODEID(String BARCODEID) {
        this.BARCODEID = BARCODEID;
    }

    public String getCARDID() {
        return CARDID;
    }

    public void setCARDID(String CARDID) {
        this.CARDID = CARDID;
    }

    public String getGETMODE() {
        return GETMODE;
    }

    public void setGETMODE(String GETMODE) {
        this.GETMODE = GETMODE;
    }

    public String getREMARKS() {
        return REMARKS;
    }

    public void setREMARKS(String REMARKS) {
        this.REMARKS = REMARKS;
    }

    public String getPROPERTY() {
        return PROPERTY;
    }

    public void setPROPERTY(String PROPERTY) {
        this.PROPERTY = PROPERTY;
    }

    public String getFDJH() {
        return FDJH;
    }

    public void setFDJH(String FDJH) {
        this.FDJH = FDJH;
    }

    public String getSZLC() {
        return SZLC;
    }

    public void setSZLC(String SZLC) {
        this.SZLC = SZLC;
    }

    public String getCUSTODIAN() {
        return CUSTODIAN;
    }

    public void setCUSTODIAN(String CUSTODIAN) {
        this.CUSTODIAN = CUSTODIAN;
    }

    public String getCJHM() {
        return CJHM;
    }

    public void setCJHM(String CJHM) {
        this.CJHM = CJHM;
    }

    public String getACCTSUITEID() {
        return ACCTSUITEID;
    }

    public void setACCTSUITEID(String ACCTSUITEID) {
        this.ACCTSUITEID = ACCTSUITEID;
    }

    public String getSERIALNUM() {
        return SERIALNUM;
    }

    public void setSERIALNUM(String SERIALNUM) {
        this.SERIALNUM = SERIALNUM;
    }

    public String getASSETSTYPE() {return ASSETSTYPE;}

    public void setASSETSTYPE(String ASSETSTYPE) {
        this.ASSETSTYPE = ASSETSTYPE;
    }

    public String getASSETSSORTCODE() {
        return ASSETSSORTCODE;
    }

    public void setASSETSSORTCODE(String ASSETSSORTCODE) {
        this.ASSETSSORTCODE = ASSETSSORTCODE;
    }

    public String getASSETSMEASURE() {
        return ASSETSMEASURE;
    }

    public void setASSETSMEASURE(String ASSETSMEASURE) {
        this.ASSETSMEASURE = ASSETSMEASURE;
    }

    public String getSERIALCODE() {
        return SERIALCODE;
    }

    public void setSERIALCODE(String SERIALCODE) {
        this.SERIALCODE = SERIALCODE;
    }

    public String getASSETSNAME() {
        return ASSETSNAME;
    }

    public void setASSETSNAME(String ASSETSNAME) {
        this.ASSETSNAME = ASSETSNAME;
    }

    public String getASSETSSTANDARD() {
        return ASSETSSTANDARD;
    }

    public void setASSETSSTANDARD(String ASSETSSTANDARD) {
        this.ASSETSSTANDARD = ASSETSSTANDARD;
    }

    public String getASSETSUSEDATE() {
        return ASSETSUSEDATE;
    }

    public void setASSETSUSEDATE(String ASSETSUSEDATE) {
        this.ASSETSUSEDATE = ASSETSUSEDATE;
    }

    public String getASSETSENDUSEDATE() {
        return ASSETSENDUSEDATE;
    }

    public void setASSETSENDUSEDATE(String ASSETSENDUSEDATE) {
        this.ASSETSENDUSEDATE = ASSETSENDUSEDATE;
    }

    public String getYEARCOUNT() {
        return YEARCOUNT;
    }

    public void setYEARCOUNT(String YEARCOUNT) {
        this.YEARCOUNT = YEARCOUNT;
    }

    public String getUSESTATE() {
        return USESTATE;
    }

    public void setUSESTATE(String USESTATE) {
        this.USESTATE = USESTATE;
    }

    public String getDPRECIATIONTYPE() {
        return DPRECIATIONTYPE;
    }

    public void setDPRECIATIONTYPE(String DPRECIATIONTYPE) {
        this.DPRECIATIONTYPE = DPRECIATIONTYPE;
    }

    public String getASSETSLAYADD() {
        return ASSETSLAYADD;
    }

    public void setASSETSLAYADD(String ASSETSLAYADD) {
        this.ASSETSLAYADD = ASSETSLAYADD;
    }

    public String getASSETSUSER() {
        return ASSETSUSER;
    }

    public void setASSETSUSER(String ASSETSUSER) {
        this.ASSETSUSER = ASSETSUSER;
    }

    public String getASSETSDEPT() {
        return ASSETSDEPT;
    }

    public void setASSETSDEPT(String ASSETSDEPT) {
        this.ASSETSDEPT = ASSETSDEPT;
    }

    public String getKPJE() {
        return KPJE;
    }

    public void setKPJE(String KPJE) {
        this.KPJE = KPJE;
    }

    public String getDPRECIATIONADD() {
        return DPRECIATIONADD;
    }

    public void setDPRECIATIONADD(String DPRECIATIONADD) {
        this.DPRECIATIONADD = DPRECIATIONADD;
    }

    public String getASSETSCURRPRICE() {
        return ASSETSCURRPRICE;
    }

    public void setASSETSCURRPRICE(String ASSETSCURRPRICE) {
        this.ASSETSCURRPRICE = ASSETSCURRPRICE;
    }

    public String getCURRDPRECIATIONADD() {
        return CURRDPRECIATIONADD;
    }

    public void setCURRDPRECIATIONADD(String CURRDPRECIATIONADD) {
        this.CURRDPRECIATIONADD = CURRDPRECIATIONADD;
    }

    public String getLEFTPRICE() {
        return LEFTPRICE;
    }

    public void setLEFTPRICE(String LEFTPRICE) {
        this.LEFTPRICE = LEFTPRICE;
    }

    public String getGROUNDMANAGEDEPT() {
        return GROUNDMANAGEDEPT;
    }

    public void setGROUNDMANAGEDEPT(String GROUNDMANAGEDEPT) {
        this.GROUNDMANAGEDEPT = GROUNDMANAGEDEPT;
    }

    public String getASSETSCULGRADE() {
        return ASSETSCULGRADE;
    }

    public void setASSETSCULGRADE(String ASSETSCULGRADE) {
        this.ASSETSCULGRADE = ASSETSCULGRADE;
    }

    public String getASSETSPRODAREA() {
        return ASSETSPRODAREA;
    }

    public void setASSETSPRODAREA(String ASSETSPRODAREA) {
        this.ASSETSPRODAREA = ASSETSPRODAREA;
    }

    public String getVEHICLENO() {
        return VEHICLENO;
    }

    public void setVEHICLENO(String VEHICLENO) {
        this.VEHICLENO = VEHICLENO;
    }

    public String getASSETSDISPENSEDATE() {
        return ASSETSDISPENSEDATE;
    }

    public void setASSETSDISPENSEDATE(String ASSETSDISPENSEDATE) {
        this.ASSETSDISPENSEDATE = ASSETSDISPENSEDATE;
    }

    public String getASSETSMANAGER() {
        return ASSETSMANAGER;
    }

    public void setASSETSMANAGER(String ASSETSMANAGER) {
        this.ASSETSMANAGER = ASSETSMANAGER;
    }

    public String getASSETSFACTORYNO() {
        return ASSETSFACTORYNO;
    }

    public void setASSETSFACTORYNO(String ASSETSFACTORYNO) {
        this.ASSETSFACTORYNO = ASSETSFACTORYNO;
    }

    public String getVEHEXH() {
        return VEHEXH;
    }

    public void setVEHEXH(String VEHEXH) {
        this.VEHEXH = VEHEXH;
    }

    public String getLONGMILES() {
        return LONGMILES;
    }

    public void setLONGMILES(String LONGMILES) {
        this.LONGMILES = LONGMILES;
    }

    public String getVEHPURPOSE() {
        return VEHPURPOSE;
    }

    public void setVEHPURPOSE(String VEHPURPOSE) {
        this.VEHPURPOSE = VEHPURPOSE;
    }

    public String getASSETSORGANIZATION() {
        return ASSETSORGANIZATION;
    }

    public void setASSETSORGANIZATION(String ASSETSORGANIZATION) {
        this.ASSETSORGANIZATION = ASSETSORGANIZATION;
    }

    public int getASSETSNUM() {
        return ASSETSNUM;
    }

    public void setASSETSNUM(int ASSETSNUM) {
        this.ASSETSNUM = ASSETSNUM;
    }

    public String getASSETSWRITER() {
        return ASSETSWRITER;
    }

    public void setASSETSWRITER(String ASSETSWRITER) {
        this.ASSETSWRITER = ASSETSWRITER;
    }

    public String getASSETSWRITERDATE() {
        return ASSETSWRITERDATE;
    }

    public void setASSETSWRITERDATE(String ASSETSWRITERDATE) {
        this.ASSETSWRITERDATE = ASSETSWRITERDATE;
    }

    public String getPURCHASINGORGANIZATION() {
        return PURCHASINGORGANIZATION;
    }

    public void setPURCHASINGORGANIZATION(String PURCHASINGORGANIZATION) {this.PURCHASINGORGANIZATION = PURCHASINGORGANIZATION;}

    public String getBRAND() {
        return BRAND;
    }

    public void setBRAND(String BRAND) {
        this.BRAND = BRAND;
    }

    public String getZT() {
        return ZT;
    }

    public void setZT(String ZT) {
        this.ZT = ZT;
    }

    public String getZTString() {
        /*盘点表2此字段对应“盘点情况”的值，该字段为字典数据，该字段的值有5中类型：
            空值、01：待盘点、02：盘实、03：盘盈、04：盘亏，导入模板中只显示：空值、01、02、03、04等内容*/
        if (StringUtils.isNullOrEmpty(this.ZT)) return "";
        if (this.ZT.equals("01")) return "待盘点";
        else if (this.ZT.equals("02")) return "盘实";
        else if (this.ZT.equals("03")) return "盘盈";
        else if (this.ZT.equals("04")) return "盘亏";
        return "";
    }

    public String getPDZT() {
        return PDZT;
    }

    public void setPDZT(String PDZT) {
        this.PDZT = PDZT;
    }



    public String getASSETSMODEL() {
        return ASSETSMODEL;
    }

    public void setASSETSMODEL(String ASSETSMODEL) {
        this.ASSETSMODEL = ASSETSMODEL;
    }



    public String getISUPLOAD() {
        return ISUPLOAD;
    }

    public void setISUPLOAD(String ISUPLOAD) {
        this.ISUPLOAD = ISUPLOAD;
    }


    public String getASSETSSERIALNUMBER() {
        return ASSETSSERIALNUMBER;
    }

    public void setASSETSSERIALNUMBER(String ASSETSSERIALNUMBER) {
        this.ASSETSSERIALNUMBER = ASSETSSERIALNUMBER;
    }

    public String getASSETSSUPPLIER() {
        return ASSETSSUPPLIER;
    }

    public void setASSETSSUPPLIER(String ASSETSSUPPLIER) {
        this.ASSETSSUPPLIER = ASSETSSUPPLIER;
    }

    public String getFACTORYNO() {
        return FACTORYNO;
    }

    public void setFACTORYNO(String FACTORYNO) {
        this.FACTORYNO = FACTORYNO;
    }

    public String getMANAGENO() {
        return MANAGENO;
    }

    public void setMANAGENO(String MANAGENO) {
        this.MANAGENO = MANAGENO;
    }

    public String getPARAMETERS() {
        return PARAMETERS;
    }

    public void setPARAMETERS(String PARAMETERS) {
        this.PARAMETERS = PARAMETERS;
    }

    public String getTRACEABILITY() {
        return TRACEABILITY;
    }

    public void setTRACEABILITY(String TRACEABILITY) {
        this.TRACEABILITY = TRACEABILITY;
    }

    public String getTRACEABILITYPE() {
        return TRACEABILITYPE;
    }

    public void setTRACEABILITYPE(String TRACEABILITYPE) {
        this.TRACEABILITYPE = TRACEABILITYPE;
    }

    public String getCALIBRATIONDATE() {
        return CALIBRATIONDATE;
    }

    public void setCALIBRATIONDATE(String CALIBRATIONDATE) {
        this.CALIBRATIONDATE = CALIBRATIONDATE;
    }

    public String getVALIDDATE() {
        return VALIDDATE;
    }

    public void setVALIDDATE(String VALIDDATE) {
        this.VALIDDATE = VALIDDATE;
    }

    public String getPROBELONG() {
        return PROBELONG;
    }

    public void setPROBELONG(String PROBELONG) {
        this.PROBELONG = PROBELONG;
    }

    public String getCLASSIFICATION() {
        return CLASSIFICATION;
    }

    public void setCLASSIFICATION(String CLASSIFICATION) {
        this.CLASSIFICATION = CLASSIFICATION;
    }

    public String getISSUEDATE() {
        return ISSUEDATE;
    }

    public void setISSUEDATE(String ISSUEDATE) {
        this.ISSUEDATE = ISSUEDATE;
    }


    public Integer getISUPIMG() {
        return ISUPIMG;
    }

    public void setISUPIMG(Integer ISUPIMG) {
        this.ISUPIMG = ISUPIMG;
    }

    public String getCOPY_DOC_ID() {
        return COPY_DOC_ID;
    }

    public void setCOPY_DOC_ID(String COPY_DOC_ID) {
        this.COPY_DOC_ID = COPY_DOC_ID;
    }


    public String getBOOKKEEPINGTIME() {
        return BOOKKEEPINGTIME;
    }

    public void setBOOKKEEPINGTIME(String BOOKKEEPINGTIME) {
        this.BOOKKEEPINGTIME = BOOKKEEPINGTIME;
    }

    public String getVOUCHERNO() {
        return VOUCHERNO;
    }

    public void setVOUCHERNO(String VOUCHERNO) {
        this.VOUCHERNO = VOUCHERNO;
    }

    public String getVENDOR() {
        return VENDOR;
    }

    public void setVENDOR(String VENDOR) {
        this.VENDOR = VENDOR;
    }

    public String toLineString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (SysConfig.assetsFieldsArray == null) {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                // 添加排除条件
                if ("ISUPIMG".equals(field.getName())) {continue;}
                if ("COPY_DOC_ID".equals(field.getName())) {continue;}
                try {
                    stringBuilder.append(field.get(this) + "|");
                } catch (IllegalAccessException e) {
                    stringBuilder.append("|");
                }
            }
        } else {
            for (int i = 0; i < SysConfig.assetsFieldsArray.length; i++) {
                if ("ISUPIMG".equals(SysConfig.assetsFieldsArray[i])) {continue;}
                if ("COPY_DOC_ID".equals(SysConfig.assetsFieldsArray[i])) {continue;}
                try {
                    Field field = this.getClass().getDeclaredField(SysConfig.assetsFieldsArray[i]);
                    if (field != null) {
                        try {
                            Object fValue = field.get(this);
                            stringBuilder.append((fValue == null ? "" : fValue) + "|");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public String toLineAddString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (SysConfig.assetsFieldsArray == null) {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                // 添加排除条件
                if ("ISUPIMG".equals(field.getName())) {continue;}
                if ("COPY_DOC_ID".equals(field.getName())) {continue;}
                try {
                    stringBuilder.append(field.get(this) + "|");
                } catch (IllegalAccessException e) {
                    stringBuilder.append("|");
                }
            }
        } else {
            for (int i = 0; i < SysConfig.assetsFieldsArray.length; i++) {
                if ("ISUPIMG".equals(SysConfig.assetsFieldsArray[i])) {continue;}
                if ("COPY_DOC_ID".equals(SysConfig.assetsFieldsArray[i])) {continue;}
                try {
                    Field field = this.getClass().getDeclaredField(SysConfig.assetsFieldsArray[i]);
                    if (field != null) {
                        try {
                            Object fValue = field.get(this);
                            if(!StringUtils.isNullOrEmpty(this.DOCID) && i == 48 ){
                                stringBuilder.append("02" + "|");
                            }
                            else{
                                stringBuilder.append((fValue == null ? "" : fValue) + "|");
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }


}
