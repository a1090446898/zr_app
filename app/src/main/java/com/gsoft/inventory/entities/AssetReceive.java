package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class AssetReceive {

    /**
     * 领用编号
     */
    private String DOCID;

    /**
     * 领用人
     */
    private String APPLICANT;

    /**
     * 领用部门
     */
    private String USEDEPT;

    /**
     * 领用说明
     */
    private String APPLY_EXPLAIN;

    /**
     * 审核状态
     */
    private String STATE;

    /**
     * 领用日期
     */
    private String APPLICANTDATE;


    public String getDOCID() {
        return DOCID;
    }

    public void setDOCID(String DOCID) {
        this.DOCID = DOCID;
    }

    public String getAPPLICANT() {
        return APPLICANT;
    }

    public void setAPPLICANT(String APPLICANT) {
        this.APPLICANT = APPLICANT;
    }

    public String getAPPLICANTDATE() {
        return APPLICANTDATE;
    }

    public void setAPPLICANTDATE(String APPLICANTDATE) {
        this.APPLICANTDATE = APPLICANTDATE;
    }

    public String getUSEDEPT() {
        return USEDEPT;
    }

    public void setUSEDEPT(String USEDEPT) {
        this.USEDEPT = USEDEPT;
    }

    public String getAPPLY_EXPLAIN() {
        return APPLY_EXPLAIN;
    }

    public void setAPPLY_EXPLAIN(String APPLY_EXPLAIN) {
        this.APPLY_EXPLAIN = APPLY_EXPLAIN;
    }

    public String getSTATE() {
        return STATE;
    }

    public void setSTATE(String STATE) {
        this.STATE = STATE;
    }
}
