package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class AssetRepair {

    /**
     * 编号
     */
    private String DOCID;


    /**
     * 报修人
     */
    private String SENDUSER;

    /**
     * 联系电话
     */
    private String SENDPHONE;


    /**
     * 报修日期
     */
    private String SENDDATE;

    /**
     * 报修原因
     */
    private String SENDREASON;

    /**
     * 经办人
     */
    private String DOUSER;

    /**
     * 维修情况
     */
    private String RESULT;

    /**
     * 办结时间
     */
    private String DODATE;

    /**
     * 资产编号
     */
    private String FID;


    public String getDOCID() {
        return DOCID;
    }

    public void setDOCID(String DOCID) {
        this.DOCID = DOCID;
    }



    public String getSENDUSER() {
        return SENDUSER;
    }

    public void setSENDUSER(String SENDUSER) {
        this.SENDUSER = SENDUSER;
    }

    public String getSENDPHONE() {
        return SENDPHONE;
    }

    public void setSENDPHONE(String SENDPHONE) {
        this.SENDPHONE = SENDPHONE;
    }

    public String getSENDDATE() {
        return SENDDATE;
    }

    public void setSENDDATE(String SENDDATE) {
        this.SENDDATE = SENDDATE;
    }

    public String getSENDREASON() {
        return SENDREASON;
    }

    public void setSENDREASON(String SENDREASON) {
        this.SENDREASON = SENDREASON;
    }

    public String getDOUSER() {
        return DOUSER;
    }

    public void setDOUSER(String DOUSER) {
        this.DOUSER = DOUSER;
    }

    public String getRESULT() {
        return RESULT;
    }

    public void setRESULT(String RESULT) {
        this.RESULT = RESULT;
    }

    public String getDODATE() {
        return DODATE;
    }

    public void setDODATE(String DODATE) {
        this.DODATE = DODATE;
    }

    public String getFID() {
        return FID;
    }

    public void setFID(String FID) {
        this.FID = FID;
    }
}
