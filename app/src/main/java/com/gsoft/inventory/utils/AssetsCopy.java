package com.gsoft.inventory.utils;

import android.content.Context;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.NewAssetsActivity;
import com.orm.SugarRecord;
import com.orm.dsl.Column;
import com.rscja.team.qcom.deviceapi.S;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/*
资产信息
 */
public class AssetsCopy extends SugarRecord implements Serializable {

    private static final Gson gson = new Gson();

    // 必须包含无参构造器
    public AssetsCopy() {}

    private Integer IDNUM;


    /**
     * 新增选中状态字段
     */
    @Column(name = "IS_SELECTED")
    private String IS_SELECTED;

    @Column(name = "DOCID", unique = true)
    private String DOCID;

    @Column(name = "COPY_DOC_ID")
    private String COPY_DOC_ID;

    /**
     * 是否复制图片
     */
    @Column(name = "COPY_IMG")
    private String COPY_IMG;

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

    public Integer getIDNUM() {
        if(IDNUM == null){
            return 0;
        }
        return IDNUM;
    }

    public void setIDNUM(Integer IDNUM) {
        this.IDNUM = IDNUM;
    }

    public static void deleteJson(Context mContent, String copyDocId) {
        List<AssetsCopy> assetsCopyList = null;
        String assetsCopyListJson = SharedPreferencesUtils.getString(mContent, "COPY_ASSETS_JSON", "");
        if (!TextUtils.isEmpty(assetsCopyListJson)) {
            assetsCopyList = gson.fromJson(assetsCopyListJson, new TypeToken<List<AssetsCopy>>() {}.getType());
            Iterator<AssetsCopy> iterator = assetsCopyList.iterator();
            while (iterator.hasNext()) {
                AssetsCopy assetsCopy = iterator.next();
                if (assetsCopy.getDOCID().equals(copyDocId)) {
                    iterator.remove();
                }
            }
            String assetsCopyListJsonNew = gson.toJson(assetsCopyList);
            SharedPreferencesUtils.putString(mContent, "COPY_ASSETS_JSON", assetsCopyListJsonNew);
        }
    }


    public String getISCZ() {
        return ISCZ;
    }

    public void setISCZ(String ISCZ) {
        this.ISCZ = ISCZ;
    }

    /*是否财政资产*/
    private String ISCZ = "01";

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

    public String getASSETSTYPE() {
        return ASSETSTYPE;
    }

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

    public void setPURCHASINGORGANIZATION(String PURCHASINGORGANIZATION) {
        this.PURCHASINGORGANIZATION = PURCHASINGORGANIZATION;
    }

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

    /*public String getISMODIFY() {
        return ISMODIFY;
    }

    public void setISMODIFY(String ISMODIFY) {
        this.ISMODIFY = ISMODIFY;
    }*/


    public String getASSETSMODEL() {
        return ASSETSMODEL;
    }

    public void setASSETSMODEL(String ASSETSMODEL) {
        this.ASSETSMODEL = ASSETSMODEL;
    }

    /**
     * 规格
     */
    private String ASSETSMODEL;

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

    // 231125添加10个字段
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

    /**
     * 字典数据：01/绝密，02/机密，03/秘密，无默认值
     */
    private String CLASSIFICATION;
    /**
     * 配发日期  YYYY-MM-DD
     */

    private String ISSUEDATE;

    public String toLineString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (SysConfig.assetsFieldsArray == null) {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    stringBuilder.append(field.get(this) + "|");
                } catch (IllegalAccessException e) {
                    stringBuilder.append("|");
                }
            }
        } else {
            for (int i = 0; i < SysConfig.assetsFieldsArray.length; i++) {
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
                try {
                    stringBuilder.append(field.get(this) + "|");
                } catch (IllegalAccessException e) {
                    stringBuilder.append("|");
                }
            }
        } else {
            for (int i = 0; i < SysConfig.assetsFieldsArray.length; i++) {
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

    // 实现深拷贝
    public void deepCopyAssets(Assets source) {
        this.setBARCODEID(source.getBARCODEID());
        this.setCARDID(source.getCARDID());
        this.setGETMODE(source.getGETMODE());
        this.setREMARKS(source.getREMARKS());
        this.setPROPERTY(source.getPROPERTY());
        this.setFDJH(source.getFDJH());
        this.setSZLC(source.getSZLC());
        this.setCUSTODIAN(source.getCUSTODIAN());
        this.setCJHM(source.getCJHM());
        this.setACCTSUITEID(source.getACCTSUITEID());
        this.setSERIALNUM(source.getSERIALNUM());
        this.setASSETSTYPE(source.getASSETSTYPE());
        this.setASSETSSORTCODE(source.getASSETSSORTCODE());
        this.setASSETSMEASURE(source.getASSETSMEASURE());
        this.setSERIALCODE(source.getSERIALCODE());
        this.setASSETSNAME(source.getASSETSNAME());
        this.setASSETSSTANDARD(source.getASSETSSTANDARD());
        this.setASSETSUSEDATE(source.getASSETSUSEDATE());
        this.setASSETSENDUSEDATE(source.getASSETSENDUSEDATE());
        this.setYEARCOUNT(source.getYEARCOUNT());
        this.setUSESTATE(source.getUSESTATE());
        this.setDPRECIATIONTYPE(source.getDPRECIATIONTYPE());
        this.setASSETSLAYADD(source.getASSETSLAYADD());
        this.setASSETSUSER(source.getASSETSUSER());
        this.setKPJE(source.getKPJE());
        this.setDPRECIATIONADD(source.getDPRECIATIONADD());
        this.setASSETSCURRPRICE(source.getASSETSCURRPRICE());
        this.setCURRDPRECIATIONADD(source.getCURRDPRECIATIONADD());
        this.setLEFTPRICE(source.getLEFTPRICE());
        this.setGROUNDMANAGEDEPT(source.getGROUNDMANAGEDEPT());
        this.setASSETSCULGRADE(source.getASSETSCULGRADE());
        this.setASSETSPRODAREA(source.getASSETSPRODAREA());
        this.setVEHICLENO(source.getVEHICLENO());
        this.setASSETSDISPENSEDATE(source.getASSETSDISPENSEDATE());
        this.setASSETSMANAGER(source.getASSETSMANAGER());
        this.setASSETSFACTORYNO(source.getASSETSFACTORYNO());
        this.setVEHEXH(source.getVEHEXH());
        this.setLONGMILES(source.getLONGMILES());
        this.setVEHPURPOSE(source.getVEHPURPOSE());
        this.setASSETSORGANIZATION(source.getASSETSORGANIZATION());
        this.setASSETSNUM(source.getASSETSNUM());
        this.setASSETSWRITER(source.getASSETSWRITER());
        this.setASSETSWRITERDATE(source.getASSETSWRITERDATE());
        this.setPURCHASINGORGANIZATION(source.getPURCHASINGORGANIZATION());
        this.setBRAND(source.getBRAND());
        this.setPDZT(source.getPDZT());
        this.setZT(source.getZT());
        this.setISUPLOAD(source.getISUPLOAD());
        this.setISCZ(source.getISCZ());
        this.setDOCID(source.getDOCID());
        this.setASSETSMODEL(source.getASSETSMODEL());
        this.setASSETSSERIALNUMBER(source.getASSETSSERIALNUMBER());
        this.setASSETSSUPPLIER(source.getASSETSSUPPLIER());
        this.setFACTORYNO(source.getFACTORYNO());
        this.setMANAGENO(source.getMANAGENO());
        this.setPARAMETERS(source.getPARAMETERS());
        this.setTRACEABILITY(source.getTRACEABILITY());
        this.setTRACEABILITYPE(source.getTRACEABILITYPE());
        this.setCALIBRATIONDATE(source.getCALIBRATIONDATE());
        this.setVALIDDATE(source.getVALIDDATE());
        this.setPROBELONG(source.getPROBELONG());
        this.setCLASSIFICATION(source.getCLASSIFICATION());
        this.setISSUEDATE(source.getISSUEDATE());
        this.setASSETSDEPT(source.getASSETSDEPT());
    }

    public void toDeepCopyAssets(Assets source) {
        source.setBARCODEID(this.getBARCODEID());
        source.setCARDID(this.getCARDID());
        source.setGETMODE(this.getGETMODE());
        source.setREMARKS(this.getREMARKS());
        source.setPROPERTY(this.getPROPERTY());
        source.setFDJH(this.getFDJH());
        source.setSZLC(this.getSZLC());
        source.setCUSTODIAN(this.getCUSTODIAN());
        source.setCJHM(this.getCJHM());
        source.setACCTSUITEID(this.getACCTSUITEID());
        source.setSERIALNUM(this.getSERIALNUM());
        source.setASSETSTYPE(this.getASSETSTYPE());
        source.setASSETSSORTCODE(this.getASSETSSORTCODE());
        source.setASSETSMEASURE(this.getASSETSMEASURE());
        source.setSERIALCODE(this.getSERIALCODE());
        source.setASSETSNAME(this.getASSETSNAME());
        source.setASSETSSTANDARD(this.getASSETSSTANDARD());
        source.setASSETSUSEDATE(this.getASSETSUSEDATE());
        source.setASSETSENDUSEDATE(this.getASSETSENDUSEDATE());
        source.setYEARCOUNT(this.getYEARCOUNT());
        source.setUSESTATE(this.getUSESTATE());
        source.setDPRECIATIONTYPE(this.getDPRECIATIONTYPE());
        source.setASSETSLAYADD(this.getASSETSLAYADD());
        source.setASSETSUSER(this.getASSETSUSER());
        source.setKPJE(this.getKPJE());
        source.setDPRECIATIONADD(this.getDPRECIATIONADD());
        source.setASSETSCURRPRICE(this.getASSETSCURRPRICE());
        source.setCURRDPRECIATIONADD(this.getCURRDPRECIATIONADD());
        source.setLEFTPRICE(this.getLEFTPRICE());
        source.setGROUNDMANAGEDEPT(this.getGROUNDMANAGEDEPT());
        source.setASSETSCULGRADE(this.getASSETSCULGRADE());
        source.setASSETSPRODAREA(this.getASSETSPRODAREA());
        source.setVEHICLENO(this.getVEHICLENO());
        source.setASSETSDISPENSEDATE(this.getASSETSDISPENSEDATE());
        source.setASSETSMANAGER(this.getASSETSMANAGER());
        source.setASSETSFACTORYNO(this.getASSETSFACTORYNO());
        source.setVEHEXH(this.getVEHEXH());
        source.setLONGMILES(this.getLONGMILES());
        source.setVEHPURPOSE(this.getVEHPURPOSE());
        source.setASSETSORGANIZATION(this.getASSETSORGANIZATION());
        source.setASSETSNUM(this.getASSETSNUM());
        source.setASSETSWRITER(this.getASSETSWRITER());
        source.setASSETSWRITERDATE(this.getASSETSWRITERDATE());
        source.setPURCHASINGORGANIZATION(this.getPURCHASINGORGANIZATION());
        source.setBRAND(this.getBRAND());
        source.setPDZT(this.getPDZT());
        source.setZT(this.getZT());
        source.setISUPLOAD(this.getISUPLOAD());
        source.setISCZ(this.getISCZ());
        source.setDOCID(this.getDOCID());
        source.setASSETSMODEL(this.getASSETSMODEL());
        source.setASSETSSERIALNUMBER(this.getASSETSSERIALNUMBER());
        source.setASSETSSUPPLIER(this.getASSETSSUPPLIER());
        source.setFACTORYNO(this.getFACTORYNO());
        source.setMANAGENO(this.getMANAGENO());
        source.setPARAMETERS(this.getPARAMETERS());
        source.setTRACEABILITY(this.getTRACEABILITY());
        source.setTRACEABILITYPE(this.getTRACEABILITYPE());
        source.setCALIBRATIONDATE(this.getCALIBRATIONDATE());
        source.setVALIDDATE(this.getVALIDDATE());
        source.setPROBELONG(this.getPROBELONG());
        source.setCLASSIFICATION(this.getCLASSIFICATION());
        source.setISSUEDATE(this.getISSUEDATE());
        source.setASSETSDEPT(this.getASSETSDEPT());
    }

    public boolean getSELECTED() {
        if("1".equals(IS_SELECTED)){
            return true;
        }
        return false;
    }

    public void setSELECTED(boolean param) {
        if(param){
            this.IS_SELECTED = "1";
        }
        else{
            this.IS_SELECTED = "0";
        }
    }

    public String getIS_SELECTED() {
        return IS_SELECTED;
    }

    public void setIS_SELECTED(String IS_SELECTED) {
        this.IS_SELECTED = IS_SELECTED;
    }

    public String getCOPY_DOC_ID() {
        return COPY_DOC_ID;
    }

    public void setCOPY_DOC_ID(String COPY_DOC_ID) {
        this.COPY_DOC_ID = COPY_DOC_ID;
    }


    public String getCOPY_IMG() {
        return COPY_IMG;
    }

    public void setCOPY_IMG(String COPY_IMG) {
        this.COPY_IMG = COPY_IMG;
    }
}
