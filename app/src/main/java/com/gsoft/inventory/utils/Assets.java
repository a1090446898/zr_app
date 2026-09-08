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
    /*使用(保管)人，管理人*/
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
    /*资产编号*/
    private String SERIALCODE;
    /*资产名称*/
    private String ASSETSNAME;
    /*型号*/
    private String ASSETSSTANDARD;
    /*取得日期、 配制日期*/
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

    private String EMPTY1 = "";
    private String EMPTY2 = "";
    /*盘点表1*/
    private String PDZT;
    /*盘点表2*/
    private String ZT;

    /*是否修改
    private String ISMODIFY = "0";*/

    /*是否上传,02未传，01已传*/
    private String ISUPLOAD = "02";

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
     * 有效日期
     */
    private String VALIDDATE;
    /**
     * 所属项目
     */
    private String PROBELONG;

    /**
     * 入账单号
     */
    private String PRINTNO;

    /**
     * 财务记账日期
     */
    private String BOOKKEEPINGTIME;

    /**
     * 凭证号
     */
    private String VOUCHERNO;


    /**
     * 项目名称
     */
    private String PRO;
    /**
     * 会计账簿，字典类型，01：行政账，02：装备账
     */
    private String ACCOUNTBOOK;

    /**
     * 下发情况，字典类型，01：行政下发，02：装备下发
     */
    private String DOWNINFO;

    /**
     * 使用年限
     */
    private String SUMCOMPLETEMONTH;

    /**
     * 已计提折旧月数
     */
    private String COMPLETEMONTH;

    /**
     * 月折旧额
     */
    private String MONDEPREVALUE;

    /**
     * 设备分类，字典类型，01：办公设备，02：实验设备
     */
    private String DEVICETYPE;

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
     * 供货商
     */
    private String VENDOR;


    /**
     * 点位编码
     */
    private String DWCODE;

    /**
     * 采样日期
     */
    private String SAMPLDATE;

    /**
     * 样品编码
     */
    private String SAMPLCODE;

    /**
     * 监测项目
     */
    private String MONITORPRO;

    /**
     * 保存方法
     */
    private String SAVEMETHOD;

    /**
     * 样品状态
     * 01：待检/02：在检/03：检毕
     */
    private String SAMPLESTATE;

    /**
     * 采样人员
     */
    private String SAMPLEUSER;



    /**
     * 硬盘号
     */
    private String HARDID;

    /**
     * 硬盘序列号
     */
    private String HARDSN;

    /**
     * IP
     */
    private String IP;

    /**
     * MAC
     */
    private String MAC;

    /**
     * 入网账号
     */
    private String INTERNETID;

    /**
     * 端口号
     */
    private String PORT;

    /**
     * 用途
     */
    private String PURPOSE;

    /**
     * 操作系统安装时间
     */
    private String INSTALLDATE;

    /**
     * 操作系统
     */
    private String OS;


    /**
     * 配置浓度
     */
    private String CONCENTRATION;

    /**
     * 配制人
     */
    private String PREPARER;

    /**
     * 介质
     */
    private String MEDIA;

    /**
     * 贮存条件
     */
    private String STORAGENEED;

    /**
     * 网络类型
     * 01：互联网/02：纪检监察内网/03：纪检监察外网/04：电子政务内网/05：电子政务外网/06：专网/99：其它
     */
    private String NETTYPE;

    /**
     * 是否公物仓
     * 01：是/02：否，默认02
     */
    private String ISGWC = "02";

    /**
     * 所属处室
     */
    private String DEPARTMENT;

    /**
     * 资产位置
     */
    private String GROUNDSETPOS;
    /**
     * 出厂日期
     */
    private String FACTORYDATE;
    /**
     * 特种设备
     */
    private String SPECIALDEVICE;
    /**
     * 设备注册代码
     */
    private String DEVICECODE;
    /**
     * 特种设备出厂编号
     */
    private String SPECIALFACTORYNO;
    /**
     * 使用登记证编号
     */
    private String CERTIFICATENUMBER;

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

    public String getEMPTY1() {
        return EMPTY1;
    }

    public void setEMPTY1(String EMPTY1) {
        this.EMPTY1 = EMPTY1;
    }

    public String getEMPTY2() {
        return EMPTY2;
    }

    public void setEMPTY2(String EMPTY1) {
        this.EMPTY1 = EMPTY2;
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

    public String getPRINTNO() {
        return PRINTNO;
    }

    public void setPRINTNO(String PRINTNO) {
        this.PRINTNO = PRINTNO;
    }

    public String getPRO() {
        return PRO;
    }

    public void setPRO(String PRO) {
        this.PRO = PRO;
    }

    /**
     * 01：行政账/02：装备账
     *
     * @return 会计账簿名称
     */
    public String getACCOUNTBOOK() {
        return ACCOUNTBOOK;
       /* if ("01".equals(ACCOUNTBOOK)) {
            return "行政账";
        } else if ("02".equals(ACCOUNTBOOK)) {
            return "装备账";
        } else {
            return "";
        }*/
    }



    public void setACCOUNTBOOK(String ACCOUNTBOOK) {
        this.ACCOUNTBOOK = ACCOUNTBOOK;
    }

    /**
     * 01：行政下发/02：装备下发
     *
     * @return 下发情况名称
     */
    public String getDOWNINFO() {
        return DOWNINFO;

       /* if ("01".equals(DOWNINFO)) {
            return "行政下发";
        } else if ("02".equals(DOWNINFO)) {
            return "装备下发";
        } else {
            return "";
        }*/
    }

    /**
     * 下发情况，字典类型，01：行政下发，02：装备下发
     *
     * @return 下发情况编码
     */

    public void setDOWNINFO(String DOWNINFO) {
        this.DOWNINFO = DOWNINFO;
    }

    public String getSUMCOMPLETEMONTH() {
        return SUMCOMPLETEMONTH;
    }

    public void setSUMCOMPLETEMONTH(String SUMCOMPLETEMONTH) {
        this.SUMCOMPLETEMONTH = SUMCOMPLETEMONTH;
    }

    public String getCOMPLETEMONTH() {
        return COMPLETEMONTH;
    }

    public void setCOMPLETEMONTH(String COMPLETEMONTH) {
        this.COMPLETEMONTH = COMPLETEMONTH;
    }

    public String getMONDEPREVALUE() {
        return MONDEPREVALUE;
    }

    public void setMONDEPREVALUE(String MONDEPREVALUE) {
        this.MONDEPREVALUE = MONDEPREVALUE;
    }

    /**
     * 01：办公设备/02：实验设备
     *
     * @return 设备分类名称
     */
    public String getDEVICETYPE() {
        return DEVICETYPE;

      /*  if ("01".equals(DEVICETYPE)) {
            return "办公设备";
        } else if ("02".equals(DEVICETYPE)) {
            return "实验设备";
        } else {
            return "";
        }*/
    }



    public void setDEVICETYPE(String DEVICETYPE) {
        this.DEVICETYPE = DEVICETYPE;
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


    public String getDWCODE() {
        return DWCODE;
    }

    public void setDWCODE(String DWCODE) {
        this.DWCODE = DWCODE;
    }

    public String getSAMPLDATE() {
        return SAMPLDATE;
    }

    public void setSAMPLDATE(String SAMPLDATE) {
        this.SAMPLDATE = SAMPLDATE;
    }

    public String getSAMPLCODE() {
        return SAMPLCODE;
    }

    public void setSAMPLCODE(String SAMPLCODE) {
        this.SAMPLCODE = SAMPLCODE;
    }

    public String getMONITORPRO() {
        return MONITORPRO;
    }

    public void setMONITORPRO(String MONITORPRO) {
        this.MONITORPRO = MONITORPRO;
    }

    public String getSAVEMETHOD() {
        return SAVEMETHOD;
    }

    public void setSAVEMETHOD(String SAVEMETHOD) {
        this.SAVEMETHOD = SAVEMETHOD;
    }


    public String getHARDID() {
        return HARDID;
    }

    public void setHARDID(String HARDID) {
        this.HARDID = HARDID;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public String getINTERNETID() {
        return INTERNETID;
    }

    public void setINTERNETID(String INTERNETID) {
        this.INTERNETID = INTERNETID;
    }

    public String getPORT() {
        return PORT;
    }

    public void setPORT(String PORT) {
        this.PORT = PORT;
    }

    /**
     * 01：办公/02：教学/03：科研/04：培训/05：办案/06：医疗/07：服务窗口/99：其他
     *
     * @return 设备用途名称
     */
    public String getPURPOSE() {
        if ("01".equals(PURPOSE)) {
            return "办公";
        } else if ("02".equals(PURPOSE)) {
            return "教学";
        } else if ("03".equals(PURPOSE)) {
            return "科研";
        } else if ("04".equals(PURPOSE)) {
            return "培训";
        } else if ("05".equals(PURPOSE)) {
            return "办案";
        } else if ("06".equals(PURPOSE)) {
            return "医疗";
        } else if ("07".equals(PURPOSE)) {
            return "服务窗口";
        } else if ("99".equals(PURPOSE)) {
            return "其他";
        } else {
            return "";
        }
    }

    public void setPURPOSE(String PURPOSE) {
        this.PURPOSE = PURPOSE;
    }

    /**
     * 01：互联网/02：纪检监察内网/03：纪检监察外网/04：电子政务内网/05：电子政务外网/06：专网/99：其它
     *
     * @return 网络类型名称
     */
    public String getNETTYPE() {
        if ("01".equals(NETTYPE)) {
            return "互联网";
        } else if ("02".equals(NETTYPE)) {
            return "纪检监察内网";
        } else if ("03".equals(NETTYPE)) {
            return "纪检监察外网";
        } else if ("04".equals(NETTYPE)) {
            return "电子政务内网";
        } else if ("05".equals(NETTYPE)) {
            return "电子政务外网";
        } else if ("06".equals(NETTYPE)) {
            return "专网";
        } else if ("99".equals(NETTYPE)) {
            return "其它";
        } else {
            return "";
        }
    }

    public void setNETTYPE(String NETTYPE) {
        this.NETTYPE = NETTYPE;
    }

    public String getHARDSN() {
        return HARDSN;
    }

    public void setHARDSN(String HARDSN) {
        this.HARDSN = HARDSN;
    }

    public String getINSTALLDATE() {
        return INSTALLDATE;
    }

    public void setINSTALLDATE(String INSTALLDATE) {
        this.INSTALLDATE = INSTALLDATE;
    }

    public String getOS() {
        return OS;
    }

    public void setOS(String OS) {
        this.OS = OS;
    }

    /**
     * 01：待检/02：在检/03：检毕
     * @return
     */
    public String getSAMPLESTATE() {

        return SAMPLESTATE;
      /*  if("01".equals(SAMPLESTATE)){
            return "待检";
        }
        else if("02".equals(SAMPLESTATE)){
            return "在检";
        }
        else if("03".equals(SAMPLESTATE)){
            return "检毕";
        }
        else {
            return "";
        }*/
    }

    public void setSAMPLESTATE(String SAMPLESTATE) {
        this.SAMPLESTATE = SAMPLESTATE;
    }

    public String getSAMPLEUSER() {
        return SAMPLEUSER;
    }

    public void setSAMPLEUSER(String SAMPLEUSER) {
        this.SAMPLEUSER = SAMPLEUSER;
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
                    stringBuilder.append(field.get(this)).append("|");
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
                    try {
                        Object fValue = field.get(this);
                        stringBuilder.append(fValue == null ? "" : fValue).append("|");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
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
                    stringBuilder.append(field.get(this)).append("|");
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
                    try {
                        Object fValue = field.get(this);
                        if (!StringUtils.isNullOrEmpty(this.DOCID) && i == 48) {
                            stringBuilder.append("02" + "|");
                        } else {
                            stringBuilder.append(fValue == null ? "" : fValue).append("|");
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public String getCONCENTRATION() {
        return CONCENTRATION;
    }

    public void setCONCENTRATION(String CONCENTRATION) {
        this.CONCENTRATION = CONCENTRATION;
    }

    public String getPREPARER() {
        return PREPARER;
    }

    public void setPREPARER(String PREPARER) {
        this.PREPARER = PREPARER;
    }

    public String getSTORAGENEED() {
        return STORAGENEED;
    }

    public void setSTORAGENEED(String STORAGENEED) {
        this.STORAGENEED = STORAGENEED;
    }

    public String getMEDIA() {
        return MEDIA;
    }

    public void setMEDIA(String MEDIA) {
        this.MEDIA = MEDIA;
    }

    public String getISGWC() {
        return ISGWC;
    }

    public void setISGWC(String ISGWC) {
        this.ISGWC = ISGWC;
    }

    public String getDEPARTMENT() {
        return DEPARTMENT;
    }

    public void setDEPARTMENT(String DEPARTMENT) {
        this.DEPARTMENT = DEPARTMENT;
    }

    public String getGROUNDSETPOS() {
        return GROUNDSETPOS;
    }

    public void setGROUNDSETPOS(String GROUNDSETPOS) {
        this.GROUNDSETPOS = GROUNDSETPOS;
    }

    public String getFACTORYDATE() {
        return FACTORYDATE;
    }

    public void setFACTORYDATE(String FACTORYDATE) {
        this.FACTORYDATE = FACTORYDATE;
    }

    public String getSPECIALDEVICE() {
        return SPECIALDEVICE;
    }

    public void setSPECIALDEVICE(String SPECIALDEVICE) {
        this.SPECIALDEVICE = SPECIALDEVICE;
    }

    public String getDEVICECODE() {
        return DEVICECODE;
    }

    public void setDEVICECODE(String DEVICECODE) {
        this.DEVICECODE = DEVICECODE;
    }

    public String getSPECIALFACTORYNO() {
        return SPECIALFACTORYNO;
    }

    public void setSPECIALFACTORYNO(String SPECIALFACTORYNO) {
        this.SPECIALFACTORYNO = SPECIALFACTORYNO;
    }

    public String getCERTIFICATENUMBER() {
        return CERTIFICATENUMBER;
    }

    public void setCERTIFICATENUMBER(String CERTIFICATENUMBER) {
        this.CERTIFICATENUMBER = CERTIFICATENUMBER;
    }
}
