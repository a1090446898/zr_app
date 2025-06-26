package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class AssetTransfer {

    /**
     * 编号
     */
    private String DOCID;

    /**
     * 移交人
     */
    private String NAME;

    /**
     * 移交部门
     */
    private String UNITNAME;

    /**
     * 移交说明
     */
    private String APPLY_EXPLAIN;

    /**
     * 审核状态
     */
    private String STATE;

    /**
     * 移交时间
     */
    private String APPLICANTDATE;

    public String getDOCID() {
        return DOCID;
    }

    public void setDOCID(String DOCID) {
        this.DOCID = DOCID;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getUNITNAME() {
        return UNITNAME;
    }

    public void setUNITNAME(String UNITNAME) {
        this.UNITNAME = UNITNAME;
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

    public String getAPPLICANTDATE() {
        return APPLICANTDATE;
    }

    public void setAPPLICANTDATE(String APPLICANTDATE) {
        this.APPLICANTDATE = APPLICANTDATE;
    }


}
