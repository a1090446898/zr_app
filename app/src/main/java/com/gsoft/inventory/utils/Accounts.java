package com.gsoft.inventory.utils;

import com.orm.SugarRecord;

import java.io.Serializable;

public class Accounts extends SugarRecord implements Serializable {
    /**
     * 账套编号
     */
    private String ZTBH;
    /**
     * 账套名称
     */
    private String ZTMC;
    /**
     * 单位编号
     */
    private String DWBH;
    /**
     * 单位名称
     */
    private String DWMC;
    /**
     * 部门编号
     */
    private String BMBH;
    /**
     * 部门名称
     */
    private String BMMC;
    /**
     * 用户编号
     */
    private String YHBH;
    /**
     * 用户姓名
     */
    private String YHXM;
    /**
     * 密码
     */
    private String DLMM;

    public String getZTBH() {
        return ZTBH;
    }

    public void setZTBH(String ZTBH) {
        this.ZTBH = ZTBH;
    }

    public String getZTMC() {
        return ZTMC;
    }

    public void setZTMC(String ZTMC) {
        this.ZTMC = ZTMC;
    }

    public String getDWBH() {
        return DWBH;
    }

    public void setDWBH(String DWBH) {
        this.DWBH = DWBH;
    }

    public String getDWMC() {
        return DWMC;
    }

    public void setDWMC(String DWMC) {
        this.DWMC = DWMC;
    }

    public String getBMBH() {
        return BMBH;
    }

    public void setBMBH(String BMBH) {
        this.BMBH = BMBH;
    }

    public String getBMMC() {
        return BMMC;
    }

    public void setBMMC(String BMMC) {
        this.BMMC = BMMC;
    }

    public String getYHBH() {
        return YHBH;
    }

    public void setYHBH(String YHBH) {
        this.YHBH = YHBH;
    }

    public String getYHXM() {
        return YHXM;
    }

    public void setYHXM(String YHXM) {
        this.YHXM = YHXM;
    }

    public String getDLMM() {
        return DLMM;
    }

    public void setDLMM(String DLMM) {
        this.DLMM = DLMM;
    }

}
